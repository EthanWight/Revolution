package edu.commonwealthu.hw3_wight;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages milestone achievements for the Revolution game.
 * Tracks completed puzzles by grid size and solution depth using SharedPreferences.
 *
 * @author Ethan Wight
 */
public class MilestonesManager {

    // Preferences keys
    private static final String PREFS_NAME = "RevolutionMilestones";
    private static final String KEY_GRID_SIZES = "completed_grid_sizes";
    private static final String KEY_SOLUTION_DEPTHS = "completed_solution_depths";
    private static final String KEY_TOTAL_WINS = "total_wins";
    private static final String KEY_FIRST_WIN_TIME = "first_win_time";

    // Milestone thresholds
    private static final int HARD_PUZZLE_DEPTH = 10;
    private static final int EXPERT_PUZZLE_DEPTH = 15;
    private static final int DEDICATED_WIN_COUNT = 10;
    private static final int ENTHUSIAST_WIN_COUNT = 25;
    private static final int MASTER_WIN_COUNT = 50;

    private final SharedPreferences preferences;

    /**
     * Constructs a MilestonesManager.
     *
     * @param context The application context
     */
    public MilestonesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Records completion of a puzzle.
     *
     * @param rows     Number of rows in the completed puzzle
     * @param cols     Number of columns in the completed puzzle
     * @param solDepth Solution depth of the completed puzzle
     */
    public void recordCompletion(int rows, int cols, int solDepth) {
        SharedPreferences.Editor editor = preferences.edit();

        // Record grid size
        Set<String> gridSizes = new HashSet<>(
                preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>()));
        gridSizes.add(formatGridSize(rows, cols));
        editor.putStringSet(KEY_GRID_SIZES, gridSizes);

        // Record solution depth
        Set<String> solDepths = new HashSet<>(
                preferences.getStringSet(KEY_SOLUTION_DEPTHS, new HashSet<>()));
        solDepths.add(String.valueOf(solDepth));
        editor.putStringSet(KEY_SOLUTION_DEPTHS, solDepths);

        // Increment total wins
        int totalWins = preferences.getInt(KEY_TOTAL_WINS, 0);
        editor.putInt(KEY_TOTAL_WINS, totalWins + 1);

        // Record first win time if not set
        if (!preferences.contains(KEY_FIRST_WIN_TIME)) {
            editor.putLong(KEY_FIRST_WIN_TIME, System.currentTimeMillis());
        }

        editor.apply();
    }

    /**
     * Checks if a specific grid size has been completed.
     *
     * @param rows Number of rows
     * @param cols Number of columns
     * @return True if this grid size has been completed
     */
    public boolean hasCompletedGridSize(int rows, int cols) {
        Set<String> gridSizes = preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>());
        return gridSizes.contains(formatGridSize(rows, cols));
    }

    /**
     * Checks if all grid sizes (3×3, 3×4, 4×4) have been completed.
     *
     * @return True if all grid sizes completed
     */
    public boolean hasCompletedAllGridSizes() {
        return hasCompletedGridSize(3, 3) &&
                hasCompletedGridSize(3, 4) &&
                hasCompletedGridSize(4, 4);
    }

    /**
     * Checks if any puzzle with solution depth >= 10 has been completed.
     *
     * @return True if at least one hard puzzle completed
     */
    public boolean hasCompletedHardPuzzle() {
        return hasCompletedPuzzleWithMinDepth(HARD_PUZZLE_DEPTH);
    }

    /**
     * Checks if any puzzle with solution depth >= 15 has been completed.
     *
     * @return True if at least one expert puzzle completed
     */
    public boolean hasCompletedExpertPuzzle() {
        return hasCompletedPuzzleWithMinDepth(EXPERT_PUZZLE_DEPTH);
    }

    /**
     * Checks if player has won at least 10 puzzles.
     *
     * @return True if >= 10 wins
     */
    public boolean hasWon10Times() {
        return getTotalWins() >= DEDICATED_WIN_COUNT;
    }

    /**
     * Checks if player has won at least 25 puzzles.
     *
     * @return True if >= 25 wins
     */
    public boolean hasWon25Times() {
        return getTotalWins() >= ENTHUSIAST_WIN_COUNT;
    }

    /**
     * Checks if player has won at least 50 puzzles.
     *
     * @return True if >= 50 wins
     */
    public boolean hasWon50Times() {
        return getTotalWins() >= MASTER_WIN_COUNT;
    }

    /**
     * Gets the total number of puzzles won.
     *
     * @return Total win count
     */
    public int getTotalWins() {
        return preferences.getInt(KEY_TOTAL_WINS, 0);
    }

    /**
     * Gets the timestamp of the first win.
     *
     * @return Timestamp in milliseconds, or -1 if no wins yet
     */
    public long getFirstWinTime() {
        return preferences.getLong(KEY_FIRST_WIN_TIME, -1);
    }

    /**
     * Gets the total number of unique milestones achieved.
     *
     * @return Count of achieved milestones (out of 9)
     */
    public int getTotalMilestonesAchieved() {
        int count = 0;

        // Grid size milestones (3)
        if (hasCompletedGridSize(3, 3)) count++;
        if (hasCompletedGridSize(3, 4)) count++;
        if (hasCompletedGridSize(4, 4)) count++;

        // Difficulty milestones (2)
        if (hasCompletedHardPuzzle()) count++;
        if (hasCompletedExpertPuzzle()) count++;

        // Win count milestones (3)
        if (hasWon10Times()) count++;
        if (hasWon25Times()) count++;
        if (hasWon50Times()) count++;

        // Master milestone (1)
        if (hasCompletedAllGridSizes()) count++;

        return count;
    }

    /**
     * Resets all milestone data.
     */
    public void resetAllMilestones() {
        preferences.edit().clear().apply();
    }

    /**
     * Gets all completed grid sizes.
     * This method is part of the public API and may be used for future features
     * such as detailed statistics display.
     *
     * @return Set of grid size strings (e.g., "3x3")
     */
    @SuppressWarnings("unused")
    public Set<String> getCompletedGridSizes() {
        return preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>());
    }

    /**
     * Gets all completed solution depths.
     * Used internally by difficulty checking methods.
     *
     * @return Set of solution depth strings
     */
    private Set<String> getCompletedSolutionDepths() {
        return preferences.getStringSet(KEY_SOLUTION_DEPTHS, new HashSet<>());
    }

    /**
     * Formats grid dimensions as a string.
     *
     * @param rows Number of rows
     * @param cols Number of columns
     * @return Formatted string (e.g., "3x3")
     */
    private String formatGridSize(int rows, int cols) {
        return rows + "x" + cols;
    }

    /**
     * Checks if any puzzle with at least the specified depth has been completed.
     *
     * @param minDepth Minimum solution depth
     * @return True if such a puzzle has been completed
     */
    private boolean hasCompletedPuzzleWithMinDepth(int minDepth) {
        Set<String> solDepths = getCompletedSolutionDepths();
        for (String depth : solDepths) {
            try {
                if (Integer.parseInt(depth) >= minDepth) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Skip invalid entries
            }
        }
        return false;
    }
}