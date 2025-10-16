package edu.commonwealthu.hw3_wight;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Random;
import java.util.Stack;

/**
 * Implements the backend logic for the Revolution puzzle game. This class
 * manages the game's state, including the grid of numbers, move history, and
 * the rules for rotating sub-grids. The game is won when the numbers are sorted
 * in ascending order.
 *
 * Enhanced with surrender mode that allows undoing through the scrambling sequence
 * to reveal the solution.
 *
 * @author Ethan Wight
 */
public class Revolution implements Serializable {

    private final int[][] grid;
    private final int rows;
    private final int cols;
    private final Stack<int[][]> moveHistory;
    private final Stack<Move> scrambleMoves;
    private boolean surrenderMode;
    private final Random random;

    private static final int DEFAULT_GRID_SIZE = 3;

    /**
     * Represents a single rotation move in the puzzle.
     */
    private static class Move implements Serializable {
        final int row;
        final int col;
        final boolean isClockwise;

        Move(int row, int col, boolean isClockwise) {
            this.row = row;
            this.col = col;
            this.isClockwise = isClockwise;
        }
    }

    /**
     * Constructs a new Revolution game with a specified grid size and solution depth.
     *
     * @param rows     The number of rows in the grid.
     * @param cols     The number of columns in the grid.
     * @param solDepth The number of random moves to perform to scramble the puzzle.
     */
    public Revolution(int rows, int cols, int solDepth) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new int[rows][cols];
        this.moveHistory = new Stack<>();
        this.scrambleMoves = new Stack<>();
        this.surrenderMode = false;
        this.random = new Random();

        initializeGrid();
        scrambleGrid(solDepth);
    }

    /**
     * Constructs a new Revolution game on a default 3x3 grid.
     *
     * @param solDepth The number of random moves to perform to scramble the puzzle.
     */
    public Revolution(int solDepth) {
        this(DEFAULT_GRID_SIZE, DEFAULT_GRID_SIZE, solDepth);
    }

    /**
     * Initializes the grid with numbers in ascending order, representing the solved state.
     */
    private void initializeGrid() {
        int value = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = value++;
            }
        }
    }

    /**
     * Scrambles the grid by applying a specified number of random rotations.
     * Each rotation is recorded so it can be undone in surrender mode.
     *
     * @param solDepth The number of random rotations to apply.
     */
    private void scrambleGrid(int solDepth) {
        for (int i = 0; i < solDepth; i++) {
            randomRotation();
        }
    }

    /**
     * Performs a single random rotation on a valid 2x2 subgrid.
     * The move is recorded in scrambleMoves for potential reversal in surrender mode.
     */
    private void randomRotation() {
        int anchorRow = random.nextInt(rows - 1);
        int anchorCol = random.nextInt(cols - 1);
        boolean isClockwise = random.nextBoolean();

        if (isClockwise) {
            rotateClockwise(anchorRow, anchorCol);
        } else {
            rotateCounterclockwise(anchorRow, anchorCol);
        }

        // Record this scramble move for surrender mode
        scrambleMoves.push(new Move(anchorRow, anchorCol, isClockwise));
    }

    /**
     * Rotates the 2x2 subgrid anchored at the specified position to the right (clockwise).
     *
     * @param row The top row of the 2x2 subgrid to rotate.
     * @param col The left column of the 2x2 subgrid to rotate.
     */
    public void rotateRight(int row, int col) {
        if (isValidAnchor(row, col)) {
            saveState();
            rotateClockwise(row, col);
        }
    }

    /**
     * Rotates the 2x2 subgrid anchored at the specified position to the left (counter-clockwise).
     *
     * @param row The top row of the 2x2 subgrid to rotate.
     * @param col The left column of the 2x2 subgrid to rotate.
     */
    public void rotateLeft(int row, int col) {
        if (isValidAnchor(row, col)) {
            saveState();
            rotateCounterclockwise(row, col);
        }
    }

    /**
     * Helper method to perform a clockwise rotation.
     */
    private void rotateClockwise(int row, int col) {
        int temp = grid[row][col];
        grid[row][col] = grid[row + 1][col];
        grid[row + 1][col] = grid[row + 1][col + 1];
        grid[row + 1][col + 1] = grid[row][col + 1];
        grid[row][col + 1] = temp;
    }

    /**
     * Helper method to perform a counter-clockwise rotation.
     */
    private void rotateCounterclockwise(int row, int col) {
        int temp = grid[row][col];
        grid[row][col] = grid[row][col + 1];
        grid[row][col + 1] = grid[row + 1][col + 1];
        grid[row + 1][col + 1] = grid[row + 1][col];
        grid[row + 1][col] = temp;
    }

    /**
     * Checks if the given coordinates are a valid top-left anchor for a 2x2 subgrid.
     *
     * @param r The row to check.
     * @param c The column to check.
     * @return True if the anchor is valid, false otherwise.
     */
    private boolean isValidAnchor(int r, int c) {
        return r >= 0 && r < rows - 1 && c >= 0 && c < cols - 1;
    }

    /**
     * Saves the current state of the grid to the move history stack.
     */
    private void saveState() {
        moveHistory.push(copyOfGrid());
    }

    /**
     * Creates and returns a deep copy of the current grid.
     *
     * @return A new 2D integer array containing a copy of the grid.
     */
    private int[][] copyOfGrid() {
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, cols);
        }
        return copy;
    }

    /**
     * Returns a copy of the current grid to prevent external modification.
     *
     * @return The current state of the grid.
     */
    public int[][] getGrid() {
        return copyOfGrid();
    }

    /**
     * Enables surrender mode, allowing undo operations to go back through
     * the scrambling sequence to reveal the solution.
     */
    public void enableSurrenderMode() {
        surrenderMode = true;
    }

    /**
     * Checks if surrender mode is currently enabled.
     *
     * @return True if surrender mode is active, false otherwise.
     */
    public boolean isSurrenderMode() {
        return surrenderMode;
    }

    /**
     * Undoes the last move, reverting the grid to its previous state.
     * In surrender mode, this can undo scramble moves to reveal the solution.
     *
     * @return True if the undo was successful, false if there are no moves to undo.
     */
    public boolean undo() {
        // First, undo user moves
        if (!moveHistory.isEmpty()) {
            int[][] previousGrid = moveHistory.pop();
            for (int r = 0; r < rows; r++) {
                System.arraycopy(previousGrid[r], 0, this.grid[r], 0, cols);
            }
            return true;
        }
        // In surrender mode, also undo scramble moves
        else if (surrenderMode && !scrambleMoves.isEmpty()) {
            Move move = scrambleMoves.pop();
            // Reverse the rotation (clockwise becomes counter-clockwise and vice versa)
            if (move.isClockwise) {
                rotateCounterclockwise(move.row, move.col);
            } else {
                rotateClockwise(move.row, move.col);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if an undo operation is possible.
     * In surrender mode, this includes scramble moves.
     *
     * @return True if undo is possible, false otherwise.
     */
    public boolean canUndo() {
        return !moveHistory.isEmpty() || (surrenderMode && !scrambleMoves.isEmpty());
    }

    /**
     * Returns the total number of undoes available.
     * In surrender mode, this includes both user moves and scramble moves.
     *
     * @return The total number of undo operations available.
     */
    public int remainingUndos() {
        if (surrenderMode) {
            return moveHistory.size() + scrambleMoves.size();
        }
        return moveHistory.size();
    }

    /**
     * Returns the number of scramble moves remaining to undo in surrender mode.
     *
     * @return The number of scramble moves that can be undone.
     */
    public int remainingScrambleMoves() {
        return scrambleMoves.size();
    }

    /**
     * Automatically reveals the full solution by undoing all moves and scrambles.
     * Only works in surrender mode.
     *
     * @return The number of moves undone, or -1 if not in surrender mode.
     */
    public int revealFullSolution() {
        if (!surrenderMode) {
            return -1;
        }

        int undoCount = 0;
        while (canUndo()) {
            if (undo()) {
                undoCount++;
            }
        }
        return undoCount;
    }

    /**
     * Checks if the puzzle is solved.
     *
     * @return True if the grid is in its solved state, false otherwise.
     */
    public boolean isOver() {
        int expectedValue = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != expectedValue++) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a string representation of the grid, formatted for console output.
     *
     * @return A string showing the current grid state.
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                sb.append(String.format(Locale.getDefault(), "%3d", grid[r][c]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}