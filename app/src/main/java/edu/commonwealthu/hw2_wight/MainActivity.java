package edu.commonwealthu.hw2_wight;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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
 * Note: Sound effects for game actions have not been implemented yet.
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
    private LinearLayout rotationControlsLayout;
    private NumberPicker solutionDepthPicker;
    private Button undoButton;
    private int defaultButtonBackgroundColor;

    // For flashing visual effect
    private Handler flashHandler;
    private Runnable flashRunnable;
    private final int[] flashColors = {Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.RED};
    private int flashColorIndex = 0;
    private int flashCount = 0;
    private static final int MAX_FLASH_COUNT = 10;
    private static final long FLASH_INTERVAL_MS = 250;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(GAME_STATE, game);
    }

    /**
     * Initializes the activity, sets up the UI, and starts a new game.
     * @param savedInstanceState If the activity is being re-initialized, this Bundle
     * contains the data it most recently supplied in onSaveInstanceState(Bundle).
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

        // Initialize game components
        flashHandler = new Handler(Looper.getMainLooper());
        gridLayout = findViewById(R.id.grid);
        gameButtons = new Button[GRID_SIZE][GRID_SIZE];
        solutionDepthPicker = findViewById(R.id.solutionDepthPicker);
        solutionDepthPicker.setMinValue(MIN_SOLUTION_DEPTH);
        solutionDepthPicker.setMaxValue(MAX_SOLUTION_DEPTH);
        solutionDepthPicker.setValue(DEFAULT_SOLUTION_DEPTH);
        defaultButtonBackgroundColor = ContextCompat.getColor(this, R.color.colorPrimary);

        // Setup UI controls and listeners
        Button newGameButton = findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(v -> startNewGame(solutionDepthPicker.getValue()));
        undoButton = findViewById(R.id.undoButton);
        undoButton.setOnClickListener(v -> performUndo());
        rotateLeftButton = findViewById(R.id.rotateLeftButton);
        rotateRightButton = findViewById(R.id.rotateRightButton);
        rotationControlsLayout = findViewById(R.id.rotationControlsLayout);
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
                tileButton.setTextColor(ContextCompat.getColor(this, R.color.button_text_color));

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
                rotationControlsLayout.setVisibility(View.GONE);
            } else {
                selectedAnchorRow = r;
                selectedAnchorCol = c;
                highlightSubgrid(r, c);
                rotationControlsLayout.setVisibility(View.VISIBLE);
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
                    gameButtons[i][j].setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
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
            if (isLeftRotation) {
                game.rotateLeft(selectedAnchorRow, selectedAnchorCol);
            } else {
                game.rotateRight(selectedAnchorRow, selectedAnchorCol);
            }
            updateGridNumbers();
            clearSubgridHighlight();
            selectedAnchorRow = -1;
            selectedAnchorCol = -1;
            rotationControlsLayout.setVisibility(View.GONE);
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
        rotationControlsLayout.setVisibility(View.GONE);
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
     * This method uses a Handler and a Runnable to create a flashing color effect on the grid buttons.
     * The handler repeatedly executes the runnable, which changes the background color of the buttons
     * to a new color from the flashColors array every FLASH_INTERVAL_MS milliseconds. This continues
     * until the effect has flashed MAX_FLASH_COUNT times.
     */
    private void showCongratulationsVisualEffect() {
        Toast.makeText(this, getString(R.string.congratulations), Toast.LENGTH_LONG).show();
        rotationControlsLayout.setVisibility(View.GONE);
        setGridButtonsEnabled(false);
        undoButton.setEnabled(false);

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

        MenuItem aboutItem = menu.findItem(R.id.action_about);
        if (aboutItem != null) {
            String title = getString(R.string.about);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spannableString.length(), 0);
            aboutItem.setTitle(spannableString);
        }

        MenuItem exitItem = menu.findItem(R.id.action_exit);
        if (exitItem != null) {
            String title = getString(R.string.exit);
            SpannableString spannableString = new SpannableString(title);
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spannableString.length(), 0);
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
    }
}