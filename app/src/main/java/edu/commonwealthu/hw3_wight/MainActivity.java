package edu.commonwealthu.hw3_wight;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Main activity for the Revolution puzzle game.
 * Manages UI interactions, game state, and visual feedback.
 *
 * @author Ethan Wight
 */
public class MainActivity extends AppCompatActivity {

    // Constants
    private static final String PREFS_NAME = "RevolutionSettings";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_GAME_STATE = "gameState";
    private static final String KEY_GRID_ROWS = "gridRows";
    private static final String KEY_GRID_COLS = "gridCols";
    private static final String KEY_SELECTED_GRID_SIZE = "selectedGridSize";

    private static final int DEFAULT_SOLUTION_DEPTH = 5;
    private static final int MIN_SOLUTION_DEPTH = 1;
    private static final int MAX_SOLUTION_DEPTH = 20;
    private static final int DEFAULT_GRID_ROWS = 3;
    private static final int DEFAULT_GRID_COLS = 3;
    private static final int SUBGRID_SIZE = 2;

    private static final long ROTATION_ANIMATION_DURATION = 400;
    private static final long FLASH_INTERVAL_MS = 250;
    private static final int MAX_FLASH_COUNT = 10;

    // Game components
    private Revolution game;
    private MilestonesManager milestonesManager;
    private SharedPreferences preferences;

    // UI components
    private GridLayout gridLayout;
    private Button[][] gameButtons;
    private Button rotateLeftButton;
    private Button rotateRightButton;
    private Button undoButton;
    private NumberPicker solutionDepthPicker;
    private Spinner gridSizeSpinner;

    // Game state
    private int currentRows = DEFAULT_GRID_ROWS;
    private int currentCols = DEFAULT_GRID_COLS;
    private int currentSolutionDepth = DEFAULT_SOLUTION_DEPTH;
    private int selectedAnchorRow = -1;
    private int selectedAnchorCol = -1;
    private boolean isAnimating = false;
    private boolean soundEnabled = true;

    // Colors
    private int defaultButtonBackgroundColor;
    private int selectedButtonBackgroundColor;
    private int surrenderModeBackgroundColor;
    private final int[] flashColors = new int[5];

    // Animation
    private Handler flashHandler;
    private Runnable flashRunnable;
    private int flashColorIndex = 0;
    private int flashCount = 0;

    // Sound
    private MediaPlayer rotationSoundPlayer;
    private MediaPlayer winSoundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        initializeColors();
        setupToolbar();
        setupControlListeners();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            startNewGame(solutionDepthPicker.getValue());
        }
    }

    /**
     * Initializes all UI components and managers.
     */
    private void initializeComponents() {
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true);
        milestonesManager = new MilestonesManager(this);
        flashHandler = new Handler(Looper.getMainLooper());

        gridLayout = findViewById(R.id.grid);
        solutionDepthPicker = findViewById(R.id.solutionDepthPicker);
        solutionDepthPicker.setMinValue(MIN_SOLUTION_DEPTH);
        solutionDepthPicker.setMaxValue(MAX_SOLUTION_DEPTH);
        solutionDepthPicker.setValue(DEFAULT_SOLUTION_DEPTH);

        gridSizeSpinner = findViewById(R.id.gridSizeSpinner);
        setupGridSizeSpinner();

        rotateLeftButton = findViewById(R.id.rotateLeftButton);
        rotateRightButton = findViewById(R.id.rotateRightButton);
        undoButton = findViewById(R.id.undoButton);

        initializeSoundEffects();
    }

    /**
     * Initializes color values from resources.
     */
    private void initializeColors() {
        defaultButtonBackgroundColor = ContextCompat.getColor(this, R.color.tile_background);
        selectedButtonBackgroundColor = ContextCompat.getColor(this, R.color.selected_tile_background);
        surrenderModeBackgroundColor = ContextCompat.getColor(this, R.color.surrender_mode_background);

        flashColors[0] = ContextCompat.getColor(this, R.color.flash_green);
        flashColors[1] = ContextCompat.getColor(this, R.color.flash_yellow);
        flashColors[2] = ContextCompat.getColor(this, R.color.flash_magenta);
        flashColors[3] = ContextCompat.getColor(this, R.color.flash_cyan);
        flashColors[4] = ContextCompat.getColor(this, R.color.flash_red);
    }

    /**
     * Sets up the toolbar with proper styling.
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    /**
     * Sets up grid size spinner with change listener.
     */
    private void setupGridSizeSpinner() {
        String[] gridSizes = getResources().getStringArray(R.array.grid_sizes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gridSizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gridSizeSpinner.setAdapter(adapter);

        gridSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0: currentRows = 3; currentCols = 3; break;
                    case 1: currentRows = 3; currentCols = 4; break;
                    case 2: currentRows = 4; currentCols = 4; break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Sets up click listeners for control buttons.
     */
    private void setupControlListeners() {
        findViewById(R.id.newGameButton).setOnClickListener(v ->
                startNewGame(solutionDepthPicker.getValue()));
        undoButton.setOnClickListener(v -> performUndo());
        rotateLeftButton.setOnClickListener(v -> rotateSelectedSubgrid(true));
        rotateRightButton.setOnClickListener(v -> rotateSelectedSubgrid(false));
    }

    /**
     * Initializes sound effect players.
     */
    private void initializeSoundEffects() {
        try {
            rotationSoundPlayer = MediaPlayer.create(this, R.raw.rotation_sound);
            if (rotationSoundPlayer != null) {
                rotationSoundPlayer.setOnCompletionListener(mp -> mp.seekTo(0));
            }

            winSoundPlayer = MediaPlayer.create(this, R.raw.win_sound);
            if (winSoundPlayer != null) {
                winSoundPlayer.setOnCompletionListener(mp -> mp.seekTo(0));
            }
        } catch (Exception e) {
            rotationSoundPlayer = null;
            winSoundPlayer = null;
        }
    }

    /**
     * Starts a new game with the specified solution depth.
     */
    private void startNewGame(int solDepth) {
        stopFlashAnimation();

        currentSolutionDepth = solDepth;
        game = new Revolution(currentRows, currentCols, solDepth);
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        gameButtons = new Button[currentRows][currentCols];

        populateGrid();
        updateUndoButton();
        setGridButtonsEnabled(true);
        updateSurrenderModeUI();
    }

    /**
     * Populates the grid with buttons representing the puzzle tiles.
     */
    private void populateGrid() {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(currentCols);
        gridLayout.setRowCount(currentRows);

        int[][] currentGridState = game.getGrid();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int maxGridWidth = Math.min(screenWidth - 100, 800);
        int buttonSize = (maxGridWidth - 40) / Math.max(currentRows, currentCols);
        int margin = 4;
        float textSize = buttonSize / 3f;

        for (int r = 0; r < currentRows; r++) {
            for (int c = 0; c < currentCols; c++) {
                Button tileButton = createTileButton(currentGridState[r][c], buttonSize, textSize, margin, r, c);
                gameButtons[r][c] = tileButton;
                gridLayout.addView(tileButton);
            }
        }

        gridLayout.requestLayout();
        clearSubgridHighlight();
    }

    /**
     * Creates a single tile button with proper styling.
     */
    private Button createTileButton(int value, int size, float textSize, int margin, int row, int col) {
        Button tileButton = new Button(this);
        tileButton.setText(String.valueOf(value));
        tileButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        tileButton.setTextColor(ContextCompat.getColor(this, R.color.tile_text_color));

        int backgroundColor = game.isSurrenderMode() ?
                surrenderModeBackgroundColor : defaultButtonBackgroundColor;
        tileButton.setBackgroundColor(backgroundColor);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(margin, margin, margin, margin);
        tileButton.setLayoutParams(params);

        tileButton.setOnClickListener(v -> onTileClicked(row, col));
        return tileButton;
    }

    /**
     * Handles tile click events.
     */
    private void onTileClicked(int r, int c) {
        if (isAnimating) return;

        boolean isValidAnchor = r < currentRows - 1 && c < currentCols - 1;

        if (isValidAnchor) {
            if (selectedAnchorRow == r && selectedAnchorCol == c) {
                selectedAnchorRow = -1;
                selectedAnchorCol = -1;
                clearSubgridHighlight();
            } else {
                selectedAnchorRow = r;
                selectedAnchorCol = c;
                highlightSubgrid(r, c);
            }
        } else {
            String message = (selectedAnchorRow != -1) ?
                    getString(R.string.invalid_tile_selection) :
                    getString(R.string.select_subgrid_prompt);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Highlights the selected 2x2 subgrid.
     */
    private void highlightSubgrid(int anchorRow, int anchorCol) {
        clearSubgridHighlight();
        for (int i = anchorRow; i < anchorRow + SUBGRID_SIZE; i++) {
            for (int j = anchorCol; j < anchorCol + SUBGRID_SIZE; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setBackgroundColor(selectedButtonBackgroundColor);
                }
            }
        }
    }

    /**
     * Clears all subgrid highlights.
     */
    private void clearSubgridHighlight() {
        int baseColor = game.isSurrenderMode() ?
                surrenderModeBackgroundColor : defaultButtonBackgroundColor;

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setBackgroundColor(baseColor);
                }
            }
        }
    }

    /**
     * Rotates the selected subgrid with animation.
     */
    private void rotateSelectedSubgrid(boolean isLeftRotation) {
        if (isAnimating) {
            Toast.makeText(this, "Animation in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAnchorRow != -1 && selectedAnchorCol != -1) {
            playSound(rotationSoundPlayer);
            animateRotation(selectedAnchorRow, selectedAnchorCol, isLeftRotation);
        } else {
            Toast.makeText(this, getString(R.string.subgrid_selection_prompt), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Animates the rotation of a 2x2 subgrid.
     */
    private void animateRotation(int anchorRow, int anchorCol, boolean isLeftRotation) {
        isAnimating = true;
        setControlsEnabled(false);

        Button topLeft = gameButtons[anchorRow][anchorCol];
        Button topRight = gameButtons[anchorRow][anchorCol + 1];
        Button bottomLeft = gameButtons[anchorRow + 1][anchorCol];
        Button bottomRight = gameButtons[anchorRow + 1][anchorCol + 1];

        int buttonWidth = topLeft.getWidth();
        int buttonHeight = topLeft.getHeight();
        float rotationAngle = isLeftRotation ? -90f : 90f;

        AnimatorSet animatorSet = new AnimatorSet();

        if (isLeftRotation) {
            animatorSet.playTogether(
                    createTileAnimation(topLeft, 0, buttonHeight, rotationAngle),
                    createTileAnimation(topRight, -buttonWidth, 0, rotationAngle),
                    createTileAnimation(bottomRight, 0, -buttonHeight, rotationAngle),
                    createTileAnimation(bottomLeft, buttonWidth, 0, rotationAngle)
            );
        } else {
            animatorSet.playTogether(
                    createTileAnimation(topLeft, buttonWidth, 0, rotationAngle),
                    createTileAnimation(topRight, 0, buttonHeight, rotationAngle),
                    createTileAnimation(bottomRight, -buttonWidth, 0, rotationAngle),
                    createTileAnimation(bottomLeft, 0, -buttonHeight, rotationAngle)
            );
        }

        animatorSet.setDuration(ROTATION_ANIMATION_DURATION);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isLeftRotation) {
                    game.rotateLeft(anchorRow, anchorCol);
                } else {
                    game.rotateRight(anchorRow, anchorCol);
                }

                resetTileProperties(topLeft, topRight, bottomLeft, bottomRight);
                updateGridNumbers();
                clearSubgridHighlight();
                selectedAnchorRow = -1;
                selectedAnchorCol = -1;

                isAnimating = false;
                setControlsEnabled(true);
                updateUndoButton();

                if (game.isOver()) {
                    onPuzzleSolved();
                }
            }
        });

        animatorSet.start();
    }

    /**
     * Creates animation set for a single tile.
     */
    private AnimatorSet createTileAnimation(Button button, float deltaX, float deltaY, float rotation) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(button, "translationX", 0f, deltaX),
                ObjectAnimator.ofFloat(button, "translationY", 0f, deltaY),
                ObjectAnimator.ofFloat(button, "rotation", 0f, rotation),
                ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.15f, 1f),
                ObjectAnimator.ofFloat(button, "alpha", 1f, 0.85f, 1f),
                ObjectAnimator.ofFloat(button, "elevation", 0f, 12f, 0f)
        );
        return animatorSet;
    }

    /**
     * Resets animation properties for buttons.
     */
    private void resetTileProperties(Button... buttons) {
        for (Button button : buttons) {
            button.setTranslationX(0f);
            button.setTranslationY(0f);
            button.setRotation(0f);
            button.setScaleX(1f);
            button.setScaleY(1f);
            button.setAlpha(1f);
            button.setElevation(0f);
        }
    }

    /**
     * Updates grid button numbers after a move.
     */
    private void updateGridNumbers() {
        int[][] currentGridState = game.getGrid();
        for (int r = 0; r < currentRows; r++) {
            for (int c = 0; c < currentCols; c++) {
                if (gameButtons[r][c] != null) {
                    gameButtons[r][c].setText(String.valueOf(currentGridState[r][c]));
                }
            }
        }
    }

    /**
     * Performs undo operation.
     */
    private void performUndo() {
        if (isAnimating || !game.undo()) {
            Toast.makeText(this, getString(R.string.undo_error), Toast.LENGTH_SHORT).show();
            return;
        }

        updateGridNumbers();
        clearSubgridHighlight();
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        updateUndoButton();

        String message = game.isSurrenderMode() && game.remainingScrambleMoves() > 0 ?
                getString(R.string.undo_surrender_success, game.remainingUndos()) :
                getString(R.string.undo_success);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (game.isOver()) {
            onPuzzleSolved();
        }
    }

    /**
     * Handles puzzle completion.
     */
    private void onPuzzleSolved() {
        if (!game.isSurrenderMode()) {
            milestonesManager.recordCompletion(currentRows, currentCols, currentSolutionDepth);
        }
        showVictoryAnimation();
    }

    /**
     * Shows victory animation with colored flashing.
     */
    private void showVictoryAnimation() {
        Toast.makeText(this, getString(R.string.congratulations), Toast.LENGTH_LONG).show();
        setGridButtonsEnabled(false);
        undoButton.setEnabled(false);
        playSound(winSoundPlayer);

        flashCount = 0;
        flashColorIndex = 0;
        flashRunnable = new Runnable() {
            @Override
            public void run() {
                if (flashCount >= MAX_FLASH_COUNT) {
                    clearSubgridHighlight();
                    return;
                }

                int currentColor = flashColors[flashColorIndex % flashColors.length];
                for (int i = 0; i < currentRows; i++) {
                    for (int j = 0; j < currentCols; j++) {
                        if (gameButtons[i][j] != null) {
                            gameButtons[i][j].setBackgroundColor(currentColor);
                        }
                    }
                }

                flashColorIndex++;
                flashCount++;
                flashHandler.postDelayed(this, FLASH_INTERVAL_MS);
            }
        };
        flashHandler.postDelayed(flashRunnable, FLASH_INTERVAL_MS);
    }

    /**
     * Stops the flash animation.
     */
    private void stopFlashAnimation() {
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }
    }

    /**
     * Updates undo button state and text.
     */
    private void updateUndoButton() {
        boolean canUndo = game != null && game.canUndo();
        undoButton.setEnabled(canUndo && !isAnimating);

        if (game != null && game.isSurrenderMode() && canUndo) {
            undoButton.setText(getString(R.string.undo_with_count, game.remainingUndos()));
        } else {
            undoButton.setText(getString(R.string.undo));
        }
    }

    /**
     * Enables or disables control buttons.
     */
    private void setControlsEnabled(boolean enabled) {
        rotateLeftButton.setEnabled(enabled);
        rotateRightButton.setEnabled(enabled);
        undoButton.setEnabled(enabled && game.canUndo());
        setGridButtonsEnabled(enabled);
    }

    /**
     * Enables or disables grid buttons.
     */
    private void setGridButtonsEnabled(boolean enabled) {
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setEnabled(enabled);
                }
            }
        }
    }

    /**
     * Updates UI for surrender mode.
     */
    private void updateSurrenderModeUI() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(game.isSurrenderMode() ?
                    R.string.app_name_surrender : R.string.app_name);
        }
        populateGrid();
    }

    /**
     * Shows surrender dialog.
     */
    private void showSurrenderDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.surrender_title)
                .setMessage(R.string.surrender_message)
                .setPositiveButton(R.string.surrender_step_by_step, (dialog, which) -> {
                    game.enableSurrenderMode();
                    updateSurrenderModeUI();
                    updateUndoButton();
                    Toast.makeText(this, getString(R.string.surrender_mode_enabled,
                            game.remainingUndos()), Toast.LENGTH_LONG).show();
                })
                .setNeutralButton(R.string.surrender_show_all, (dialog, which) -> {
                    game.enableSurrenderMode();
                    int undoCount = game.revealFullSolution();
                    updateGridNumbers();
                    updateSurrenderModeUI();
                    updateUndoButton();
                    Toast.makeText(this, getString(R.string.solution_revealed, undoCount),
                            Toast.LENGTH_LONG).show();
                    if (game.isOver()) {
                        showVictoryAnimation();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Plays a sound effect if enabled.
     */
    private void playSound(MediaPlayer player) {
        if (soundEnabled && player != null) {
            try {
                if (player.isPlaying()) {
                    player.seekTo(0);
                } else {
                    player.start();
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Toggles sound effects on/off.
     */
    private void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
        String message = enabled ? getString(R.string.sound_enabled) : getString(R.string.sound_disabled);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Applies color to menu items.
     */
    private void colorMenuItem(MenuItem item, int stringResId) {
        if (item != null) {
            String title = getString(stringResId);
            SpannableString spannableString = new SpannableString(title);
            int color = ContextCompat.getColor(this, R.color.menu_text_color);
            spannableString.setSpan(new ForegroundColorSpan(color), 0, spannableString.length(), 0);
            item.setTitle(spannableString);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_GAME_STATE, game);
        outState.putInt(KEY_GRID_ROWS, currentRows);
        outState.putInt(KEY_GRID_COLS, currentCols);
        outState.putInt(KEY_SELECTED_GRID_SIZE, gridSizeSpinner.getSelectedItemPosition());
    }

    /**
     * Restores state from saved instance.
     */
    private void restoreState(Bundle savedInstanceState) {
        currentRows = savedInstanceState.getInt(KEY_GRID_ROWS, DEFAULT_GRID_ROWS);
        currentCols = savedInstanceState.getInt(KEY_GRID_COLS, DEFAULT_GRID_COLS);
        int selectedPosition = savedInstanceState.getInt(KEY_SELECTED_GRID_SIZE, 0);
        gridSizeSpinner.setSelection(selectedPosition);
        game = (Revolution) savedInstanceState.getSerializable(KEY_GAME_STATE);
        gameButtons = new Button[currentRows][currentCols];
        populateGrid();
        updateUndoButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem soundItem = menu.findItem(R.id.action_sound_toggle);
        if (soundItem != null) {
            soundItem.setChecked(soundEnabled);
        }

        colorMenuItem(menu.findItem(R.id.action_sound_toggle), R.string.sound_effects);
        colorMenuItem(menu.findItem(R.id.action_milestones), R.string.milestones);
        colorMenuItem(menu.findItem(R.id.action_surrender), R.string.surrender);
        colorMenuItem(menu.findItem(R.id.action_about), R.string.about);
        colorMenuItem(menu.findItem(R.id.action_exit), R.string.exit);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem surrenderItem = menu.findItem(R.id.action_surrender);
        if (surrenderItem != null) {
            surrenderItem.setEnabled(!game.isSurrenderMode());
        }

        MenuItem soundItem = menu.findItem(R.id.action_sound_toggle);
        if (soundItem != null) {
            soundItem.setChecked(soundEnabled);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_sound_toggle) {
            setSoundEnabled(!soundEnabled);
            item.setChecked(soundEnabled);
            return true;
        } else if (itemId == R.id.action_milestones) {
            startActivity(new Intent(this, MilestonesActivity.class));
            return true;
        } else if (itemId == R.id.action_surrender) {
            showSurrenderDialog();
            return true;
        } else if (itemId == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        } else if (itemId == R.id.action_exit) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit)
                    .setMessage(getString(R.string.exit_confirmation_message))
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> finish())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rotationSoundPlayer != null && rotationSoundPlayer.isPlaying()) {
            rotationSoundPlayer.pause();
        }
        if (winSoundPlayer != null && winSoundPlayer.isPlaying()) {
            winSoundPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFlashAnimation();

        if (rotationSoundPlayer != null) {
            rotationSoundPlayer.release();
            rotationSoundPlayer = null;
        }
        if (winSoundPlayer != null) {
            winSoundPlayer.release();
            winSoundPlayer = null;
        }
    }
}