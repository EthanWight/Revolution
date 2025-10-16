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
import android.view.View;
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
 *
 * @author Ethan Wight
 */
public class MainActivity extends AppCompatActivity {

    private Revolution game;
    private GridLayout gridLayout;
    private Button[][] gameButtons;

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

    // Current grid dimensions
    private int currentRows = DEFAULT_GRID_ROWS;
    private int currentCols = DEFAULT_GRID_COLS;

    private int selectedAnchorRow = -1;
    private int selectedAnchorCol = -1;

    private Button rotateLeftButton;
    private Button rotateRightButton;
    private NumberPicker solutionDepthPicker;
    private Spinner gridSizeSpinner;
    private Button undoButton;
    private int defaultButtonBackgroundColor;
    private int selectedButtonBackgroundColor;

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

        // Initialize colors
        defaultButtonBackgroundColor = ContextCompat.getColor(this, R.color.tile_background);
        selectedButtonBackgroundColor = ContextCompat.getColor(this, R.color.selected_tile_background);

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

        game = new Revolution(currentRows, currentCols, solDepth);
        selectedAnchorRow = -1;
        selectedAnchorCol = -1;
        gameButtons = new Button[currentRows][currentCols];
        populateGrid();
        updateUndoButtonAndMenuState();
        setGridButtonsEnabled(true);
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
                tileButton.setBackgroundColor(defaultButtonBackgroundColor);

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
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (gameButtons[i][j] != null) {
                    gameButtons[i][j].setBackgroundColor(defaultButtonBackgroundColor);
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
        if (selectedAnchorRow != -1 && selectedAnchorCol != -1) {
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

    private void updateUndoButtonAndMenuState() {
        boolean canUndo = game != null && game.moves() > 0;
        if (undoButton != null) {
            undoButton.setEnabled(canUndo);
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