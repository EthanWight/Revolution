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
 * Displays gameplay milestones and achievements.
 * Shows completed grid sizes, difficulty levels, and statistics.
 *
 * @author Ethan Wight
 */
public class MilestonesActivity extends AppCompatActivity {

    private static final String TAG = "MilestonesActivity";
    private static final int TOTAL_MILESTONES = 9;

    private MilestonesManager milestonesManager;
    private LinearLayout milestonesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milestones);

        try {
            setupToolbar();
            initializeComponents();
            displayContent();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading milestones", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Sets up the toolbar with back navigation.
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Initializes components and managers.
     */
    private void initializeComponents() {
        milestonesContainer = findViewById(R.id.milestonesContainer);
        milestonesManager = new MilestonesManager(this);
    }

    /**
     * Displays all milestone content.
     */
    private void displayContent() {
        displayMilestones();
        displayStatistics();
    }

    /**
     * Displays all milestone achievements.
     */
    private void displayMilestones() {
        milestonesContainer.removeAllViews();

        addSectionHeader("Grid Sizes Completed");
        addMilestone("Beginner", "Complete a 3×3 puzzle",
                milestonesManager.hasCompletedGridSize(3, 3));
        addMilestone("Intermediate", "Complete a 3×4 puzzle",
                milestonesManager.hasCompletedGridSize(3, 4));
        addMilestone("Advanced", "Complete a 4×4 puzzle",
                milestonesManager.hasCompletedGridSize(4, 4));

        addSectionHeader("Difficulty Levels");
        addMilestone("Challenger", "Complete a puzzle with solution depth 10+",
                milestonesManager.hasCompletedHardPuzzle());
        addMilestone("Expert", "Complete a puzzle with solution depth 15+",
                milestonesManager.hasCompletedExpertPuzzle());

        addSectionHeader("Total Victories");
        addMilestone("Dedicated Player", "Win 10 puzzles",
                milestonesManager.hasWon10Times());
        addMilestone("Puzzle Enthusiast", "Win 25 puzzles",
                milestonesManager.hasWon25Times());
        addMilestone("Revolution Master", "Win 50 puzzles",
                milestonesManager.hasWon50Times());

        addSectionHeader("Master Achievement");
        addMilestone("Grand Master", "Complete all grid sizes (3×3, 3×4, and 4×4)",
                milestonesManager.hasCompletedAllGridSizes());
    }

    /**
     * Displays statistics section.
     */
    private void displayStatistics() {
        addSectionHeader("Statistics");

        int totalWins = milestonesManager.getTotalWins();
        int totalMilestones = milestonesManager.getTotalMilestonesAchieved();
        long firstWinTime = milestonesManager.getFirstWinTime();

        StringBuilder stats = new StringBuilder();
        stats.append("Total Puzzles Solved: ").append(totalWins).append("\n");
        stats.append("Achievements Unlocked: ").append(totalMilestones)
                .append(" / ").append(TOTAL_MILESTONES);

        if (firstWinTime > 0) {
            String dateStr = formatDate(firstWinTime);
            if (dateStr != null) {
                stats.append("\n").append("First Victory: ").append(dateStr);
            }
        }

        TextView statsText = createTextView(stats.toString(), 16,
                getColorSafe(R.color.milestone_text));
        statsText.setPadding(32, 24, 32, 24);
        milestonesContainer.addView(statsText);

        addResetButton();
    }

    /**
     * Adds a section header.
     *
     * @param headerText The header text
     */
    private void addSectionHeader(String headerText) {
        TextView header = createTextView(headerText, 18,
                getColorSafe(R.color.colorAccent));
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(32, 40, 32, 16);
        milestonesContainer.addView(header);
    }

    /**
     * Adds a milestone item.
     *
     * @param title       Milestone title
     * @param description Milestone description
     * @param achieved    Whether achieved
     */
    private void addMilestone(String title, String description, boolean achieved) {
        LinearLayout milestoneItem = new LinearLayout(this);
        milestoneItem.setOrientation(LinearLayout.HORIZONTAL);
        milestoneItem.setPadding(32, 16, 32, 16);

        // Add checkmark/indicator
        TextView checkmark = createCheckmark(achieved);
        milestoneItem.addView(checkmark);

        // Add text container
        LinearLayout textContainer = createMilestoneTextContainer(title, description, achieved);
        milestoneItem.addView(textContainer);

        milestonesContainer.addView(milestoneItem);
    }

    /**
     * Creates a checkmark indicator.
     *
     * @param achieved Whether the milestone is achieved
     * @return TextView with checkmark or circle
     */
    private TextView createCheckmark(boolean achieved) {
        TextView checkmark = new TextView(this);
        checkmark.setText(achieved ? "✓" : "○");
        checkmark.setTextColor(achieved ?
                getColorSafe(R.color.milestone_achieved) :
                getColorSafe(R.color.milestone_unachieved));
        checkmark.setTextSize(24);
        checkmark.setPadding(0, 0, 24, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        checkmark.setLayoutParams(params);

        return checkmark;
    }

    /**
     * Creates the text container for a milestone.
     *
     * @param title       Milestone title
     * @param description Milestone description
     * @param achieved    Whether achieved
     * @return LinearLayout containing title and description
     */
    private LinearLayout createMilestoneTextContainer(String title, String description, boolean achieved) {
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        textContainer.setLayoutParams(params);

        // Title
        TextView titleView = createTextView(title, 16, achieved ?
                getColorSafe(R.color.milestone_text) :
                getColorSafe(R.color.milestone_unachieved));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        textContainer.addView(titleView);

        // Description
        TextView descView = createTextView(description, 14,
                getColorSafe(R.color.milestone_description));
        descView.setPadding(0, 4, 0, 0);
        textContainer.addView(descView);

        return textContainer;
    }

    /**
     * Creates a TextView with specified text, size, and color.
     *
     * @param text  The text to display
     * @param size  Text size in sp
     * @param color Text color
     * @return Configured TextView
     */
    private TextView createTextView(String text, int size, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(color);
        return textView;
    }

    /**
     * Adds reset button.
     */
    private void addResetButton() {
        TextView resetButton = createTextView(
                getString(R.string.reset_all_milestones),
                16,
                getColorSafe(R.color.milestone_reset)
        );
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
     * Shows confirmation dialog before resetting milestones.
     */
    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.milestone_reset_title)
                .setMessage(R.string.milestone_reset_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    try {
                        milestonesManager.resetAllMilestones();
                        displayContent();
                        Toast.makeText(this, "Milestones reset", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error resetting milestones", e);
                        Toast.makeText(this, "Error resetting milestones",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Safely gets a color resource with fallback.
     *
     * @param resId Color resource ID
     * @return Color value
     */
    private int getColorSafe(int resId) {
        try {
            return ContextCompat.getColor(this, resId);
        } catch (Exception e) {
            Log.w(TAG, "Color resource not found: " + resId, e);
            return 0xFF212121; // Default dark gray
        }
    }

    /**
     * Formats a timestamp as a date string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string or null on error
     */
    private String formatDate(long timestamp) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            Log.w(TAG, "Error formatting date", e);
            return null;
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