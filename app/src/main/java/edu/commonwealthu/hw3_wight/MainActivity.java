package edu.commonwealthu.hw3_wight;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Plays the Revolution puzzle game. This class manages the user interface,
 * game state interactions, and visual feedback for the player.
 *
 * @author Ethan Wight
 */
public class MainActivity extends AppCompatActivity {

    private Revolution game;
    private GridLayout gridLayout;
    private Button[][] gameButtons;

    private static final String GAME_STATE = "gameState";
    private static final int DEFAULT_SOLUTION_DEPTH = 5;
    private static final int MIN_SOLUTION_DEPTH = 1;
    private static final int MAX_SOLUTION_DEPTH = 20;
    private static final int GRID_SIZE = 3;
    private static final int SUBGRID_SIZE = 2;

    private int selectedAnchorRow = -1;
    private int selectedAnchorCol = -1;

    private Button rotateLeftButton;
    private Button rotateRightButton;
    private NumberPicker solutionDepthPicker;
    private Button undoButton;
    private int defaultButtonBackgroundColor;
    private int selectedButtonBackgroundColor;

    // For flashing visual effect
    private Handler flashHandler;
    private Runnable flashRunnable;
    private final int[] flashColors = new int[5]; // Will be populated from resources
    private int flashColorIndex = 0;
    private int flashCount = 0;
    private static final int MAX_FLASH_COUNT = 10;
    private static final long FLASH_INTERVAL_MS = 250;

    // Sound effects
    private MediaPlayer rotationSoundPlayer;
    private MediaPlayer winSoundPlayer;

    /**
     * Saves the current game state before the activity is destroyed.
     * This allows the game to be restored after configuration changes like screen rotation.
     * @param outState Bundle in which to place the saved state
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(GAME_STATE, game);
    }

    /**
     * Initializes the activity, sets up the UI, and starts a new game.
     * If the activity is being re-initialized after a configuration change (such as
     * screen rotation), the saved game state is restored from the Bundle.
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down, this Bundle contains the saved game state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Initialize colors from resources
        defaultButtonBackgroundColor = ContextCompat.getColor(this, R.color.tile_background);
        selectedButtonBackgroundColor = ContextCompat.getColor(this, R.color.selected_tile_background);

        // Initialize flash colors from resources
        flashColors[0] = ContextCompat.getColor(this, R.color.flash_green);
        flashColors[1] = ContextCompat.getColor(this, R.color.flash_yellow);
        flashColors[2] = ContextCompat.getColor(this, R.color.flash_magenta);
        flashColors[3] = ContextCompat.getColor(this, R.color.flash_cyan);
        flashColors[4] = ContextCompat.getColor(this, R.color.flash_red);

        // Initialize game components
        flashHandler = new Handler(Looper.getMainLooper());
        gridLayout = findViewById(R.id.grid);
        gameButtons = new Button[GRID_SIZE][GRID_SIZE];
        solutionDepthPicker = findViewById(R.id.solutionDepthPicker);
        solutionDepthPicker.setMinValue(MIN_SOLUTION_DEPTH);
        solutionDepthPicker.setMaxValue(MAX_SOLUTION_DEPTH);
        solutionDepthPicker.setValue(DEFAULT_SOLUTION_DEPTH);

        // Initialize sound effect
        initializeSoundEffect();

        // Setup UI controls and listeners
        Button newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(v -> startNewGame(solutionDepthPicker.getValue()));
        undoButton = findViewById(R.id.undoButton);
        undoButton.setOnClickListener(v -> performUndo());
        rotateLeftButton = findViewById(R.id.rotateLeftButton);
        rotateRightButton = findViewById(R.id.rotateRightButton);

        // Rotation controls are now always visible in both portrait and landscape

        setupRotationButtons();

        if (savedInstanceState != null) {
            game = (Revolution) savedInstanceState.getSerializable(GAME_STATE);
            populateGrid();
            updateUndoButtonAndMenuState();
        } else {
            // Start the first game
            startNewGame(solutionDepthPicker.getValue());
        }
    }

    /**
     * Initializes the sound effects for rotations and winning.
     */
    private void initializeSoundEffect() {
        try {
            rotationSoundPlayer = MediaPlayer.create(this, R.raw.rotation_sound);
            if (rotationSoundPlayer != null) {
                rotationSoundPlayer.setOnCompletionListener(mp -> {
                    // Reset the player to be ready for next play
                    mp.seekTo(0);
                });
            }

            winSoundPlayer = MediaPlayer.create(this, R.raw.win_sound);
            if (winSoundPlayer != null) {
                winSoundPlayer.setOnCompletionListener(mp -> {
                    // Reset the player to be ready for next play
                    mp.seekTo(0);
                });
            }
        } catch (Exception e) {
            // If sound initialization fails, continue without sound
            rotationSoundPlayer = null;
            winSoundPlayer = null;
        }
    }

    /**
     * Plays the rotation sound effect.
     */
    private void playRotationSound() {
        if (rotationSoundPlayer != null) {
            try {
                if (rotationSoundPlayer.isPlaying()) {
                    rotationSoundPlayer.seekTo(0);
                } else {
                    rotationSoundPlayer.start();
                }
            } catch (Exception e) {
                // Silently fail if sound cannot be played
            }
        }
    }

    /**
     * Plays the win sound effect.
     */
    private void playWinSound() {
        if (winSoundPlayer != null) {
            try {
                if (winSoundPlayer.isPlaying()) {
                    winSoundPlayer.seekTo(0);
                } else {
                    winSoundPlayer.start();
                }
            } catch (Exception e) {
                // Silently fail if sound cannot be played
            }
        }
    }

    /**
     * Starts a new game with a specified solution depth.
     * @param solDepth The number of random moves to make to scramble the puzzle.
     */
    private void startNewGame(int solDepth) {
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }

        game = new Revolution(solDepth);
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        populateGrid();
        updateUndoButtonAndMenuState();
        setGridButtonsEnabled(true);
    }

    /**
     * Creates and populates the grid of buttons with numbers from the game state.
     */
    private void populateGrid() {
        gridLayout.removeAllViews();
        gridLayout.setColumnCount(GRID_SIZE);
        gridLayout.setRowCount(GRID_SIZE);
        int[][] currentGridState = game.getGrid();
        int margin = getResources().getDimensionPixelSize(R.dimen.small_margin);
        float textSize = getResources().getDimension(R.dimen.tile_text_size);

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                Button tileButton = new Button(this);
                tileButton.setText(String.valueOf(currentGridState[r][c]));
                tileButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                tileButton.setTextColor(ContextCompat.getColor(this, R.color.tile_text_color));

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(r, 1f);
                params.columnSpec = GridLayout.spec(c, 1f);
                params.setMargins(margin, margin, margin, margin);
                tileButton.setLayoutParams(params);

                final int row = r;
                final int col = c;
                tileButton.setOnClickListener(v -> onTileClicked(row, col));
                gameButtons[r][c] = tileButton;
                gridLayout.addView(tileButton);
            }
        }
        clearSubgridHighlight();
    }

    /**
     * Handles clicks on the grid tiles, selecting or deselecting a 2x2 subgrid anchor.
     * @param r The row of the clicked tile.
     * @param c The column of the clicked tile.
     */
    private void onTileClicked(int r, int c) {
        boolean isAnchor = r < GRID_SIZE - 1 && c < GRID_SIZE - 1;

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

    /**
     * Highlights the 2x2 subgrid starting at the given anchor coordinates.
     * @param anchorRow The top row of the subgrid.
     * @param anchorCol The left column of the subgrid.
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
     * Resets the background color of all grid buttons to the default.
     */
    private void clearSubgridHighlight() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setBackgroundColor(defaultButtonBackgroundColor);
                }
            }
        }
    }

    /**
     * Enables or disables all buttons in the grid.
     * @param enabled True to enable the buttons, false to disable.
     */
    private void setGridButtonsEnabled(boolean enabled) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setEnabled(enabled);
                }
            }
        }
    }

    /**
     * Sets up the click listeners for the left and right rotation buttons.
     */
    private void setupRotationButtons() {
        rotateLeftButton.setOnClickListener(v -> rotateSelectedSubgrid(true));
        rotateRightButton.setOnClickListener(v -> rotateSelectedSubgrid(false));
    }

    /**
     * Rotates the currently selected subgrid.
     * @param isLeftRotation True for a left rotation, false for a right rotation.
     */
    private void rotateSelectedSubgrid(boolean isLeftRotation) {
        if (selectedAnchorRow != -1 && selectedAnchorCol != -1) {
            // Play sound effect before rotation
            playRotationSound();

            if (isLeftRotation) {
                game.rotateLeft(selectedAnchorRow, selectedAnchorCol);
            } else {
                game.rotateRight(selectedAnchorRow, selectedAnchorCol);
            }
            updateGridNumbers();
            clearSubgridHighlight();
            selectedAnchorRow = -1;
            selectedAnchorCol = -1;
            updateUndoButtonAndMenuState();
            if (game.isOver()) {
                showCongratulationsVisualEffect();
            }
        } else {
            Toast.makeText(this, getString(R.string.subgrid_selection_prompt), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Undoes the last move made in the game.
     */
    private void performUndo() {
        if (!game.undo()) {
            Toast.makeText(this, getString(R.string.undo_error), Toast.LENGTH_SHORT).show();
            return;
        }
        updateGridNumbers();
        clearSubgridHighlight();
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        updateUndoButtonAndMenuState();
        Toast.makeText(this, getString(R.string.undo_success), Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the enabled state of the undo button based on the game state.
     */
    private void updateUndoButtonAndMenuState() {
        boolean canUndo = game != null && game.moves() > 0;
        if (undoButton != null) {
            undoButton.setEnabled(canUndo);
        }
    }

    /**
     * Refreshes the numbers displayed on the grid buttons from the game state.
     */
    private void updateGridNumbers() {
        int[][] currentGridState = game.getGrid();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (gameButtons[r][c] != null) {
                    gameButtons[r][c].setText(String.valueOf(currentGridState[r][c]));
                }
            }
        }
    }

    /**
     * Displays a congratulatory message and visual effect when the puzzle is solved.
     *
     * This method creates a flashing color effect on the grid buttons using Android's
     * Handler and Runnable mechanism for delayed execution:
     *
     * <ul>
     * <li>The Handler schedules the Runnable to execute at regular intervals (FLASH_INTERVAL_MS)</li>
     * <li>Each execution changes all button background colors to the next color in the sequence</li>
     * <li>The Runnable reschedules itself using postDelayed() until MAX_FLASH_COUNT is reached</li>
     * <li>Colors cycle through the flashColors array for a vibrant celebration effect</li>
     * </ul>
     *
     * The grid buttons and undo button are disabled during the effect to prevent
     * interaction while the celebration animation is playing.
     */
    private void showCongratulationsVisualEffect() {
        Toast.makeText(this, getString(R.string.congratulations), Toast.LENGTH_LONG).show();
        setGridButtonsEnabled(false);
        undoButton.setEnabled(false);

        // Play win sound
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
                for (int i = 0; i < GRID_SIZE; i++) {
                    for (int j = 0; j < GRID_SIZE; j++) {
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
     * Inflates the options menu.
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        updateUndoButtonAndMenuState();

        // Set menu item text colors using color resource
        int menuTextColor = ContextCompat.getColor(this, R.color.menu_text_color);

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

    /**
     * Handles selections from the options menu.
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
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

    /**
     * Cleans up resources when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (flashHandler != null && flashRunnable != null) {
            flashHandler.removeCallbacks(flashRunnable);
        }

        // Release sound resources
        if (rotationSoundPlayer != null) {
            rotationSoundPlayer.release();
            rotationSoundPlayer = null;
        }
        if (winSoundPlayer != null) {
            winSoundPlayer.release();
            winSoundPlayer = null;
        }
    }

    /**
     * Pauses audio when activity is paused.
     */
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