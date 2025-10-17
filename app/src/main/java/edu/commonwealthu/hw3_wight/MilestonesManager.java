package edu.commonwealthu.hw3_wight;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages milestone achievements for the Revolution game.
 * Tracks completed puzzles by grid size and solution depth,
 * storing data in SharedPreferences.
 *
 * @author Ethan Wight
 */
public class MilestonesManager {
    private static final String PREFS_NAME = "RevolutionMilestones";
    private static final String KEY_GRID_SIZES = "completed_grid_sizes";
    private static final String KEY_SOLUTION_DEPTHS = "completed_solution_depths";
    private static final String KEY_TOTAL_WINS = "total_wins";
    private static final String KEY_FIRST_WIN_TIME = "first_win_time";

    private final SharedPreferences preferences;

    /**
     * Constructs a MilestonesManager with the given context.
     *
     * @param context The application context
     */
    public MilestonesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Records a puzzle completion with the given grid size and solution depth.
     *
     * @param rows      Number of rows in the completed puzzle
     * @param cols      Number of columns in the completed puzzle
     * @param solDepth  Solution depth of the completed puzzle
     */
    public void recordCompletion(int rows, int cols, int solDepth) {
        SharedPreferences.Editor editor = preferences.edit();

        // Record grid size
        Set<String> gridSizes = preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>());
        gridSizes = new HashSet<>(gridSizes); // Create mutable copy
        gridSizes.add(rows + "x" + cols);
        editor.putStringSet(KEY_GRID_SIZES, gridSizes);

        // Record solution depth
        Set<String> solDepths = preferences.getStringSet(KEY_SOLUTION_DEPTHS, new HashSet<>());
        solDepths = new HashSet<>(solDepths); // Create mutable copy
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
     * @return True if this grid size has been completed at least once
     */
    public boolean hasCompletedGridSize(int rows, int cols) {
        Set<String> gridSizes = preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>());
        return gridSizes.contains(rows + "x" + cols);
    }

    /**
     * Gets the set of all completed grid sizes.
     *
     * @return Set of grid size strings (e.g., "3x3", "4x4")
     */
    public Set<String> getCompletedGridSizes() {
        return preferences.getStringSet(KEY_GRID_SIZES, new HashSet<>());
    }

    /**
     * Gets the set of all completed solution depths.
     *
     * @return Set of solution depth strings
     */
    public Set<String> getCompletedSolutionDepths() {
        return preferences.getStringSet(KEY_SOLUTION_DEPTHS, new HashSet<>());
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
     * Checks if all grid sizes have been completed.
     *
     * @return True if 3x3, 3x4, and 4x4 have all been completed
     */
    public boolean hasCompletedAllGridSizes() {
        Set<String> gridSizes = getCompletedGridSizes();
        return gridSizes.contains("3x3") && 
               gridSizes.contains("3x4") && 
               gridSizes.contains("4x4");
    }

    /**
     * Checks if any "hard" solution depths (10+) have been completed.
     *
     * @return True if at least one puzzle with solution depth >= 10 has been completed
     */
    public boolean hasCompletedHardPuzzle() {
        Set<String> solDepths = getCompletedSolutionDepths();
        for (String depth : solDepths) {
            try {
                if (Integer.parseInt(depth) >= 10) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * Checks if any "expert" solution depths (15+) have been completed.
     *
     * @return True if at least one puzzle with solution depth >= 15 has been completed
     */
    public boolean hasCompletedExpertPuzzle() {
        Set<String> solDepths = getCompletedSolutionDepths();
        for (String depth : solDepths) {
            try {
                if (Integer.parseInt(depth) >= 15) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * Checks if player has won at least 10 puzzles.
     *
     * @return True if total wins >= 10
     */
    public boolean hasWon10Times() {
        return getTotalWins() >= 10;
    }

    /**
     * Checks if player has won at least 25 puzzles.
     *
     * @return True if total wins >= 25
     */
    public boolean hasWon25Times() {
        return getTotalWins() >= 25;
    }

    /**
     * Checks if player has won at least 50 puzzles.
     *
     * @return True if total wins >= 50
     */
    public boolean hasWon50Times() {
        return getTotalWins() >= 50;
    }

    /**
     * Resets all milestone data.
     */
    public void resetAllMilestones() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Gets the total number of unique milestones achieved.
     *
     * @return Count of achieved milestones
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
}
