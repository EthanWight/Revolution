package edu.commonwealthu.hw3_wight;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Plays the Revolution puzzle game. This class manages the user interface,
 * game state interactions, and visual feedback for the player.
 * Supports 3×3, 3×4, and 4×4 grid sizes.
 * <p>
 * Enhanced with surrender mode that allows viewing the solution and
 * milestones tracking for achievements.
 *
 * @author Ethan Wight
 */
public class MainActivity extends AppCompatActivity {

    private Revolution game;
    private GridLayout gridLayout;
    private Button[][] gameButtons;
    private MilestonesManager milestonesManager;

    private static final String GAME_STATE = "gameState";
    private static final String GRID_ROWS = "gridRows";
    private static final String GRID_COLS = "gridCols";
    private static final String SELECTED_GRID_SIZE = "selectedGridSize";
    private static final int DEFAULT_SOLUTION_DEPTH = 5;
    private static final int MIN_SOLUTION_DEPTH = 1;
    private static final int MAX_SOLUTION_DEPTH = 20;
    private static final int DEFAULT_GRID_ROWS = 3;
    private static final int DEFAULT_GRID_COLS = 3;
    private static final int SUBGRID_SIZE = 2;
    private static final long ROTATION_ANIMATION_DURATION = 400;

    // Current grid dimensions
    private int currentRows = DEFAULT_GRID_ROWS;
    private int currentCols = DEFAULT_GRID_COLS;
    private int currentSolutionDepth = DEFAULT_SOLUTION_DEPTH;

    private int selectedAnchorRow = -1;
    private int selectedAnchorCol = -1;

    private Button rotateLeftButton;
    private Button rotateRightButton;
    private NumberPicker solutionDepthPicker;
    private Spinner gridSizeSpinner;
    private Button undoButton;
    private int defaultButtonBackgroundColor;
    private int selectedButtonBackgroundColor;
    private int surrenderModeBackgroundColor;

    // Animation state
    private boolean isAnimating = false;

    // For flashing visual effect
    private Handler flashHandler;
    private Runnable flashRunnable;
    private final int[] flashColors = new int[5];
    private int flashColorIndex = 0;
    private int flashCount = 0;
    private static final int MAX_FLASH_COUNT = 10;
    private static final long FLASH_INTERVAL_MS = 250;

    // Sound effects
    private MediaPlayer rotationSoundPlayer;
    private MediaPlayer winSoundPlayer;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(GAME_STATE, game);
        outState.putInt(GRID_ROWS, currentRows);
        outState.putInt(GRID_COLS, currentCols);
        outState.putInt(SELECTED_GRID_SIZE, gridSizeSpinner.getSelectedItemPosition());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install splash screen before calling super.onCreate()
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize milestones manager
        milestonesManager = new MilestonesManager(this);

        // Initialize colors
        defaultButtonBackgroundColor = ContextCompat.getColor(this, R.color.tile_background);
        selectedButtonBackgroundColor = ContextCompat.getColor(this, R.color.selected_tile_background);
        surrenderModeBackgroundColor = ContextCompat.getColor(this, R.color.surrender_mode_background);

        flashColors[0] = ContextCompat.getColor(this, R.color.flash_green);
        flashColors[1] = ContextCompat.getColor(this, R.color.flash_yellow);
        flashColors[2] = ContextCompat.getColor(this, R.color.flash_magenta);
        flashColors[3] = ContextCompat.getColor(this, R.color.flash_cyan);
        flashColors[4] = ContextCompat.getColor(this, R.color.flash_red);

        // Initialize components
        flashHandler = new Handler(Looper.getMainLooper());
        gridLayout = findViewById(R.id.grid);
        solutionDepthPicker = findViewById(R.id.solutionDepthPicker);
        solutionDepthPicker.setMinValue(MIN_SOLUTION_DEPTH);
        solutionDepthPicker.setMaxValue(MAX_SOLUTION_DEPTH);
        solutionDepthPicker.setValue(DEFAULT_SOLUTION_DEPTH);

        gridSizeSpinner = findViewById(R.id.gridSizeSpinner);
        setupGridSizeSpinner();

        initializeSoundEffect();

        Button newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(v -> startNewGame(solutionDepthPicker.getValue()));
        undoButton = findViewById(R.id.undoButton);
        undoButton.setOnClickListener(v -> performUndo());
        rotateLeftButton = findViewById(R.id.rotateLeftButton);
        rotateRightButton = findViewById(R.id.rotateRightButton);

        setupRotationButtons();

        if (savedInstanceState != null) {
            currentRows = savedInstanceState.getInt(GRID_ROWS, DEFAULT_GRID_ROWS);
            currentCols = savedInstanceState.getInt(GRID_COLS, DEFAULT_GRID_COLS);
            int selectedPosition = savedInstanceState.getInt(SELECTED_GRID_SIZE, 0);
            gridSizeSpinner.setSelection(selectedPosition);
            game = (Revolution) savedInstanceState.getSerializable(GAME_STATE);
            gameButtons = new Button[currentRows][currentCols];
            populateGrid();
            updateUndoButtonAndMenuState();
        } else {
            startNewGame(solutionDepthPicker.getValue());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh menu to update milestone indicator if needed
        invalidateOptionsMenu();
    }

    private void setupGridSizeSpinner() {
        String[] gridSizes = getResources().getStringArray(R.array.grid_sizes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, gridSizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gridSizeSpinner.setAdapter(adapter);

        gridSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        currentRows = 3;
                        currentCols = 3;
                        break;
                    case 1:
                        currentRows = 3;
                        currentCols = 4;
                        break;
                    case 2:
                        currentRows = 4;
                        currentCols = 4;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initializeSoundEffect() {
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

    private void playRotationSound() {
        if (rotationSoundPlayer != null) {
            try {
                if (rotationSoundPlayer.isPlaying()) {
                    rotationSoundPlayer.seekTo(0);
                } else {
                    rotationSoundPlayer.start();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void playWinSound() {
        if (winSoundPlayer != null) {
            try {
                if (winSoundPlayer.isPlaying()) {
                    winSoundPlayer.seekTo(0);
                } else {
                    winSoundPlayer.start();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void startNewGame(int solDepth) {
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }

        currentSolutionDepth = solDepth;
        game = new Revolution(currentRows, currentCols, solDepth);
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        gameButtons = new Button[currentRows][currentCols];
        populateGrid();
        updateUndoButtonAndMenuState();
        setGridButtonsEnabled(true);
        updateSurrenderModeUI();
    }

    private void populateGrid() {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(currentCols);
        gridLayout.setRowCount(currentRows);
        int[][] currentGridState = game.getGrid();

        // Calculate button size based on grid dimensions
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int maxGridWidth = Math.min(screenWidth - 100, 800); // Max 800dp width
        int buttonSize = (maxGridWidth - 40) / Math.max(currentRows, currentCols);

        int margin = 4;
        float textSize = buttonSize / 3f;

        for (int r = 0; r < currentRows; r++) {
            for (int c = 0; c < currentCols; c++) {
                Button tileButton = new Button(this);
                tileButton.setText(String.valueOf(currentGridState[r][c]));
                tileButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                tileButton.setTextColor(ContextCompat.getColor(this, R.color.tile_text_color));

                // Use different color if in surrender mode
                if (game.isSurrenderMode()) {
                    tileButton.setBackgroundColor(surrenderModeBackgroundColor);
                } else {
                    tileButton.setBackgroundColor(defaultButtonBackgroundColor);
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = buttonSize;
                params.height = buttonSize;
                params.setMargins(margin, margin, margin, margin);
                tileButton.setLayoutParams(params);

                final int row = r;
                final int col = c;
                tileButton.setOnClickListener(v -> onTileClicked(row, col));
                gameButtons[r][c] = tileButton;
                gridLayout.addView(tileButton);
            }
        }

        // Force layout update
        gridLayout.requestLayout();
        clearSubgridHighlight();
    }

    private void onTileClicked(int r, int c) {
        if (isAnimating) {
            return; // Don't allow selection during animation
        }

        boolean isAnchor = r < currentRows - 1 && c < currentCols - 1;

        if (isAnchor) {
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
            String message = (selectedAnchorRow != -1)
                    ? getString(R.string.invalid_tile_selection)
                    : getString(R.string.select_subgrid_prompt);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

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

    private void clearSubgridHighlight() {
        int baseColor = game.isSurrenderMode() ? surrenderModeBackgroundColor : defaultButtonBackgroundColor;
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setBackgroundColor(baseColor);
                }
            }
        }
    }

    private void setGridButtonsEnabled(boolean enabled) {
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setEnabled(enabled);
                }
            }
        }
    }

    private void setupRotationButtons() {
        rotateLeftButton.setOnClickListener(v -> rotateSelectedSubgrid(true));
        rotateRightButton.setOnClickListener(v -> rotateSelectedSubgrid(false));
    }

    private void rotateSelectedSubgrid(boolean isLeftRotation) {
        if (isAnimating) {
            Toast.makeText(this, "Animation in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAnchorRow != -1 && selectedAnchorCol != -1) {
            playRotationSound();
            animateRotation(selectedAnchorRow, selectedAnchorCol, isLeftRotation);
        } else {
            Toast.makeText(this, getString(R.string.subgrid_selection_prompt), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Animates the rotation of a 2x2 subgrid with smooth visual effects.
     *
     * @param anchorRow      The top-left row of the subgrid
     * @param anchorCol      The top-left column of the subgrid
     * @param isLeftRotation True for counter-clockwise, false for clockwise
     */
    private void animateRotation(int anchorRow, int anchorCol, boolean isLeftRotation) {
        isAnimating = true;
        setControlsEnabled(false);

        // Get the four buttons in the subgrid
        Button topLeft = gameButtons[anchorRow][anchorCol];
        Button topRight = gameButtons[anchorRow][anchorCol + 1];
        Button bottomLeft = gameButtons[anchorRow + 1][anchorCol];
        Button bottomRight = gameButtons[anchorRow + 1][anchorCol + 1];

        // Calculate button dimensions for translation
        int buttonWidth = topLeft.getWidth();
        int buttonHeight = topLeft.getHeight();

        // Create animation sets for each button
        AnimatorSet animatorSet = new AnimatorSet();

        // Rotation angle: positive for clockwise, negative for counter-clockwise
        float rotationAngle = isLeftRotation ? -90f : 90f;

        if (isLeftRotation) {
            // Counter-clockwise: TL→BL, TR→TL, BR→TR, BL→BR
            animatorSet.playTogether(
                    createTileAnimation(topLeft, 0, buttonHeight, rotationAngle),
                    createTileAnimation(topRight, -buttonWidth, 0, rotationAngle),
                    createTileAnimation(bottomRight, 0, -buttonHeight, rotationAngle),
                    createTileAnimation(bottomLeft, buttonWidth, 0, rotationAngle)
            );
        } else {
            // Clockwise: TL→TR, TR→BR, BR→BL, BL→TL
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
                // Update the game state
                if (isLeftRotation) {
                    game.rotateLeft(anchorRow, anchorCol);
                } else {
                    game.rotateRight(anchorRow, anchorCol);
                }

                // Reset visual properties
                resetTileProperties(topLeft);
                resetTileProperties(topRight);
                resetTileProperties(bottomLeft);
                resetTileProperties(bottomRight);

                // Update grid with new numbers
                updateGridNumbers();
                clearSubgridHighlight();
                selectedAnchorRow = -1;
                selectedAnchorCol = -1;

                // Re-enable controls
                isAnimating = false;
                setControlsEnabled(true);
                updateUndoButtonAndMenuState();

                // Check for win condition
                if (game.isOver()) {
                    onPuzzleSolved();
                }
            }
        });

        animatorSet.start();
    }

    /**
     * Creates an animation set for a single tile including translation, rotation, and scale.
     *
     * @param button     The button to animate
     * @param deltaX     Translation in X direction
     * @param deltaY     Translation in Y direction
     * @param rotation   Rotation angle in degrees
     * @return AnimatorSet containing all animations for this tile
     */
    private AnimatorSet createTileAnimation(Button button, float deltaX, float deltaY,
                                            float rotation) {
        // Translation animations
        ObjectAnimator translateX = ObjectAnimator.ofFloat(button, "translationX", 0f, deltaX);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(button, "translationY", 0f, deltaY);

        // Rotation animation
        ObjectAnimator rotate = ObjectAnimator.ofFloat(button, "rotation", 0f, rotation);

        // Scale animation for emphasis
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.15f, 1f);

        // Subtle alpha for smoothness
        ObjectAnimator alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.85f, 1f);

        // Elevation for depth effect
        ObjectAnimator elevation = ObjectAnimator.ofFloat(button, "elevation", 0f, 12f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateX, translateY, rotate, scaleX, scaleY, alpha, elevation);

        return animatorSet;
    }

    /**
     * Resets all animation properties of a button to their default values.
     *
     * @param button The button to reset
     */
    private void resetTileProperties(Button button) {
        button.setTranslationX(0f);
        button.setTranslationY(0f);
        button.setRotation(0f);
        button.setScaleX(1f);
        button.setScaleY(1f);
        button.setAlpha(1f);
        button.setElevation(0f);
    }

    /**
     * Enables or disables control buttons during animations.
     *
     * @param enabled True to enable controls, false to disable
     */
    private void setControlsEnabled(boolean enabled) {
        rotateLeftButton.setEnabled(enabled);
        rotateRightButton.setEnabled(enabled);
        undoButton.setEnabled(enabled && game.canUndo());
        setGridButtonsEnabled(enabled);
    }

    private void performUndo() {
        if (isAnimating) {
            return;
        }

        if (!game.undo()) {
            Toast.makeText(this, getString(R.string.undo_error), Toast.LENGTH_SHORT).show();
            return;
        }
        updateGridNumbers();
        clearSubgridHighlight();
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        updateUndoButtonAndMenuState();

        String message = game.isSurrenderMode() && game.remainingScrambleMoves() > 0
                ? getString(R.string.undo_surrender_success, game.remainingUndos())
                : getString(R.string.undo_success);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Check if we've reached the solved state
        if (game.isOver()) {
            onPuzzleSolved();
        }
    }

    /**
     * Called when the puzzle is solved.
     * Records the milestone and shows congratulations.
     */
    private void onPuzzleSolved() {
        // Only record milestone if not in surrender mode
        if (!game.isSurrenderMode()) {
            milestonesManager.recordCompletion(currentRows, currentCols, currentSolutionDepth);
        }

        showCongratulationsVisualEffect();
    }

    private void updateUndoButtonAndMenuState() {
        boolean canUndo = game != null && game.canUndo();
        if (undoButton != null) {
            undoButton.setEnabled(canUndo && !isAnimating);

            // Update button text to show remaining undos in surrender mode
            if (game != null && game.isSurrenderMode() && canUndo) {
                undoButton.setText(getString(R.string.undo_with_count, game.remainingUndos()));
            } else {
                undoButton.setText(getString(R.string.undo));
            }
        }
    }

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

    private void showSurrenderDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.surrender_title)
                .setMessage(R.string.surrender_message)
                .setPositiveButton(R.string.surrender_step_by_step, (dialog, which) -> {
                    game.enableSurrenderMode();
                    updateSurrenderModeUI();
                    updateUndoButtonAndMenuState();
                    Toast.makeText(this, getString(R.string.surrender_mode_enabled,
                            game.remainingUndos()), Toast.LENGTH_LONG).show();
                })
                .setNeutralButton(R.string.surrender_show_all, (dialog, which) -> {
                    game.enableSurrenderMode();
                    int undoCount = game.revealFullSolution();
                    updateGridNumbers();
                    updateSurrenderModeUI();
                    updateUndoButtonAndMenuState();
                    Toast.makeText(this, getString(R.string.solution_revealed, undoCount),
                            Toast.LENGTH_LONG).show();
                    if (game.isOver()) {
                        showCongratulationsVisualEffect();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateSurrenderModeUI() {
        // Update toolbar title to indicate surrender mode
        if (getSupportActionBar() != null) {
            if (game.isSurrenderMode()) {
                getSupportActionBar().setTitle(R.string.app_name_surrender);
            } else {
                getSupportActionBar().setTitle(R.string.app_name);
            }
        }

        // Update grid colors
        populateGrid();
    }

    private void showCongratulationsVisualEffect() {
        Toast.makeText(this, getString(R.string.congratulations), Toast.LENGTH_LONG).show();
        setGridButtonsEnabled(false);
        undoButton.setEnabled(false);

        playWinSound();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        updateUndoButtonAndMenuState();

        int menuTextColor = ContextCompat.getColor(this, R.color.menu_text_color);

        // Color all menu items
        MenuItem milestonesItem = menu.findItem(R.id.action_milestones);
        if (milestonesItem != null) {
            String title = getString(R.string.milestones);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spannableString.length(), 0);
            milestonesItem.setTitle(spannableString);
        }

        MenuItem surrenderItem = menu.findItem(R.id.action_surrender);
        if (surrenderItem != null) {
            String title = getString(R.string.surrender);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spannableString.length(), 0);
            surrenderItem.setTitle(spannableString);
        }

        MenuItem aboutItem = menu.findItem(R.id.action_about);
        if (aboutItem != null) {
            String title = getString(R.string.about);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spannableString.length(), 0);
            aboutItem.setTitle(spannableString);
        }

        MenuItem exitItem = menu.findItem(R.id.action_exit);
        if (exitItem != null) {
            String title = getString(R.string.exit);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spannableString.length(), 0);
            exitItem.setTitle(spannableString);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem surrenderItem = menu.findItem(R.id.action_surrender);
        if (surrenderItem != null) {
            // Disable surrender option if already in surrender mode
            surrenderItem.setEnabled(!game.isSurrenderMode());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_milestones) {
            Intent intent = new Intent(this, MilestonesActivity.class);
            startActivity(intent);
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
    protected void onDestroy() {
        super.onDestroy();
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }

        if (rotationSoundPlayer != null) {
            rotationSoundPlayer.release();
            rotationSoundPlayer = null;
        }
        if (winSoundPlayer != null) {
            winSoundPlayer.release();
            winSoundPlayer = null;
        }
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
}