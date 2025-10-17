package edu.commonwealthu.hw3_wight;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity that displays gameplay milestones and achievements.
 * Shows which grid sizes and difficulties have been completed,
 * total wins, and other accomplishments.
 *
 * @author Ethan Wight
 */
public class MilestonesActivity extends AppCompatActivity {

    private static final String TAG = "MilestonesActivity";
    private MilestonesManager milestonesManager;
    private LinearLayout milestonesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milestones);

        try {
            Log.d(TAG, "onCreate started");

            // Setup toolbar
            try {
                MaterialToolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                }
                Log.d(TAG, "Toolbar setup complete");
            } catch (Exception e) {
                Log.e(TAG, "Error setting up toolbar", e);
            }

            milestonesContainer = findViewById(R.id.milestonesContainer);
            Log.d(TAG, "Layout created");

            // Initialize manager
            try {
                milestonesManager = new MilestonesManager(this);
                Log.d(TAG, "MilestonesManager created");
            } catch (Exception e) {
                Log.e(TAG, "Error creating MilestonesManager", e);
                Toast.makeText(this, "Error loading milestones data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Display content
            try {
                displayMilestones();
                displayStats();
                Log.d(TAG, "Content displayed");
            } catch (Exception e) {
                Log.e(TAG, "Error displaying content", e);
                Toast.makeText(this, "Error displaying milestones", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Helper to safely get color resources with fallback
     */
    private int getColorResource(int resId, int fallback) {
        try {
            return ContextCompat.getColor(this, resId);
        } catch (Exception e) {
            Log.w(TAG, "Color resource not found: " + resId, e);
            return fallback;
        }
    }

    /**
     * Displays all milestone achievements.
     */
    private void displayMilestones() {
        Log.d(TAG, "displayMilestones started");

        milestonesContainer.removeAllViews();

        // Header
        addSectionHeader("Grid Sizes Completed");

        // Grid size milestones
        addMilestone(
                "Beginner",
                "Complete a 3×3 puzzle",
                milestonesManager.hasCompletedGridSize(3, 3)
        );

        addMilestone(
                "Intermediate",
                "Complete a 3×4 puzzle",
                milestonesManager.hasCompletedGridSize(3, 4)
        );

        addMilestone(
                "Advanced",
                "Complete a 4×4 puzzle",
                milestonesManager.hasCompletedGridSize(4, 4)
        );

        // Difficulty milestones
        addSectionHeader("Difficulty Levels");

        addMilestone(
                "Challenger",
                "Complete a puzzle with solution depth 10+",
                milestonesManager.hasCompletedHardPuzzle()
        );

        addMilestone(
                "Expert",
                "Complete a puzzle with solution depth 15+",
                milestonesManager.hasCompletedExpertPuzzle()
        );

        // Win count milestones
        addSectionHeader("Total Victories");

        addMilestone(
                "Dedicated Player",
                "Win 10 puzzles",
                milestonesManager.hasWon10Times()
        );

        addMilestone(
                "Puzzle Enthusiast",
                "Win 25 puzzles",
                milestonesManager.hasWon25Times()
        );

        addMilestone(
                "Revolution Master",
                "Win 50 puzzles",
                milestonesManager.hasWon50Times()
        );

        // Master milestone
        addSectionHeader("Master Achievement");

        addMilestone(
                "Grand Master",
                "Complete all grid sizes (3×3, 3×4, and 4×4)",
                milestonesManager.hasCompletedAllGridSizes()
        );

        Log.d(TAG, "displayMilestones completed");
    }

    /**
     * Displays statistics section at the bottom.
     */
    private void displayStats() {
        Log.d(TAG, "displayStats started");

        addSectionHeader("Statistics");

        int totalWins = milestonesManager.getTotalWins();
        int totalMilestones = milestonesManager.getTotalMilestonesAchieved();
        long firstWinTime = milestonesManager.getFirstWinTime();

        TextView statsText = new TextView(this);
        StringBuilder stats = new StringBuilder();

        stats.append("Total Puzzles Solved: ").append(totalWins);
        stats.append("\n");
        stats.append("Achievements Unlocked: ").append(totalMilestones).append(" / 9");

        if (firstWinTime > 0) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = dateFormat.format(new Date(firstWinTime));
                stats.append("\n");
                stats.append("First Victory: ").append(dateStr);
            } catch (Exception e) {
                Log.w(TAG, "Error formatting date", e);
            }
        }

        statsText.setText(stats.toString());
        statsText.setTextSize(16);
        statsText.setTextColor(getColorResource(R.color.milestone_text, 0xFF212121));
        statsText.setPadding(32, 24, 32, 24);

        milestonesContainer.addView(statsText);

        // Add reset button at bottom
        addResetButton();

        Log.d(TAG, "displayStats completed");
    }

    /**
     * Adds a section header to the milestones list.
     *
     * @param headerText The header text to display
     */
    private void addSectionHeader(String headerText) {
        TextView header = new TextView(this);
        header.setText(headerText);
        header.setTextSize(18);
        header.setTextColor(getColorResource(R.color.colorAccent, 0xFF03DAC5));
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(32, 40, 32, 16);
        milestonesContainer.addView(header);
    }

    /**
     * Adds a milestone item to the display.
     *
     * @param title       The milestone title
     * @param description The milestone description
     * @param achieved    Whether this milestone has been achieved
     */
    private void addMilestone(String title, String description, boolean achieved) {
        LinearLayout milestoneItem = new LinearLayout(this);
        milestoneItem.setOrientation(LinearLayout.HORIZONTAL);
        milestoneItem.setPadding(32, 16, 32, 16);

        // Checkmark or empty box
        TextView checkmark = new TextView(this);
        if (achieved) {
            checkmark.setText("✓");
            checkmark.setTextColor(getColorResource(R.color.milestone_achieved, 0xFF4CAF50));
        } else {
            checkmark.setText("○");
            checkmark.setTextColor(getColorResource(R.color.milestone_unachieved, 0xFF9E9E9E));
        }
        checkmark.setTextSize(24);
        checkmark.setPadding(0, 0, 24, 0);
        LinearLayout.LayoutParams checkmarkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        checkmark.setLayoutParams(checkmarkParams);

        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        textContainer.setLayoutParams(textParams);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(achieved ?
                getColorResource(R.color.milestone_text, 0xFF212121) :
                getColorResource(R.color.milestone_unachieved, 0xFF9E9E9E));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        // Description
        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(14);
        descView.setTextColor(getColorResource(R.color.milestone_description, 0xFF757575));
        descView.setPadding(0, 4, 0, 0);

        textContainer.addView(titleView);
        textContainer.addView(descView);

        milestoneItem.addView(checkmark);
        milestoneItem.addView(textContainer);

        milestonesContainer.addView(milestoneItem);
    }

    /**
     * Adds a reset button to clear all milestones.
     */
    private void addResetButton() {
        TextView resetButton = new TextView(this);
        resetButton.setText(R.string.reset_all_milestones);
        resetButton.setTextSize(16);
        resetButton.setTextColor(getColorResource(R.color.milestone_reset, 0xFFF44336));
        resetButton.setPadding(32, 40, 32, 40);
        resetButton.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        resetButton.setClickable(true);
        resetButton.setFocusable(true);

        // Add ripple effect
        try {
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            try (android.content.res.TypedArray ta = obtainStyledAttributes(attrs)) {
                android.graphics.drawable.Drawable drawable = ta.getDrawable(0);
                resetButton.setBackground(drawable);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set ripple effect", e);
        }

        resetButton.setOnClickListener(v -> showResetConfirmation());

        milestonesContainer.addView(resetButton);
    }

    /**
     * Shows a confirmation dialog before resetting milestones.
     */
    private void showResetConfirmation() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Milestones?")
                    .setMessage("This will clear all your milestone progress. Are you sure?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        try {
                            milestonesManager.resetAllMilestones();
                            displayMilestones();
                            displayStats();
                            Toast.makeText(this, "Milestones reset", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error resetting milestones", e);
                            Toast.makeText(this, "Error resetting milestones", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing reset dialog", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
