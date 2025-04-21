package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PuzzleState {


    private final int rows;
    private final int cols;
    private final int[][] board;

    private int zeroRow;
    private int zeroCol;

    public PuzzleState(int rows, int cols, int[][] board) {
        this.rows = rows;
        this.cols = cols;

        this.board = new int[rows][cols];
        int foundZeroRow = -1;
        int foundZeroCol = -1;

        for (int i = 0; i < rows; i++) {
            this.board[i] = java.util.Arrays.copyOf(board[i], cols);

            for (int j = 0; j < cols; j++) {
                if (this.board[i][j] == 0) {
                    foundZeroRow = i;
                    foundZeroCol = j;
                }

            }

        }

        this.zeroRow = foundZeroRow;
        this.zeroCol = foundZeroCol;
    }

    public PuzzleState(PuzzleState puzzleState) {
        this.rows = puzzleState.rows;
        this.cols = puzzleState.cols;
        this.board = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            this.board[i] = java.util.Arrays.copyOf(puzzleState.board[i], cols);
        }
        this.zeroRow = puzzleState.zeroRow;
        this.zeroCol = puzzleState.zeroCol;
    }

    //sprawdzamy czy uklad jest docelowy
    public boolean isGoal() {
        int expected = 1;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                if (i == rows - 1 && j == cols - 1) {
                    if (board[i][j] != 0) {
                        return false;
                    }
                } else {
                    if (board[i][j] != expected) {
                        return false;
                    }
                    expected++;
                }


            }

        }
        return true;
    }

    //przesuniecie 0 w lewo, prawo, gora, dol
    PuzzleState moveZero(char d) {
        int newRow = zeroRow;
        int newCol = zeroCol;

        switch (d) {
            case 'L':
                newCol = zeroCol - 1;
                break;
            case 'R':
                newCol = zeroCol + 1;
                break;
            case 'U':
                newRow = zeroRow - 1;
                break;
            case 'D':
                newRow = zeroRow + 1;
                break;
            default:
                return null;

        }

        // sprawdzamy czy po przesunieciu dalej jestesmy w planszy
        if (newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols) {
            return null;
        }

        //kopia bieżącego stanu
        PuzzleState copy = new PuzzleState(this);
        //zamieniamy wartosci
        int temp = copy.board[copy.zeroRow][copy.zeroCol];
        copy.board[copy.zeroRow][copy.zeroCol] = copy.board[newRow][newCol];
        copy.board[newRow][newCol] = temp;

        copy.zeroRow = newRow;
        copy.zeroCol = newCol;
        return copy;

    }

    //lista sasiadow w kolejnosci s np RDUL prawo dol gora lewo
    public List<PuzzleNeighbor> getNeighbor(char[] s) {
        List<PuzzleNeighbor> result = new ArrayList<>();
        for (char move : s) {
            PuzzleState puzzleState = moveZero(move);
            if (puzzleState != null) {
                result.add(new PuzzleNeighbor(puzzleState, move));
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PuzzleState)) return false;
        PuzzleState other = (PuzzleState) o;

        if (this.rows != other.rows || this.cols != other.cols) {
            return false;
        }

        for (int i = 0; i < rows; i++) {
            if (!java.util.Arrays.equals(this.board[i], other.board[i])) {
                return false;
            }
        }
        return true;

    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, cols, Arrays.deepHashCode(board), zeroRow, zeroCol);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.append(board[i][j]).append('\t');
            }
            result.append('\n');
        }
        return result.toString();
    }

    public int hamming() {
        int wrong = 0; // licznik źle ustawionych klocków
        int shouldBe = 1; // numer ktory powinien stac w biezacej kratce
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                if (i == rows - 1 && j == cols - 1) { // ostatnia kratka
                    if (board[i][j] != 0) { // tu powinno byc 0
                        wrong++;
                    }
                } else {
                    if (board[i][j] != shouldBe) {  //pozostale kratki
                        wrong++;
                    }

                    shouldBe++;
                }
            }
        }

        return wrong;

    }

    public int manhattan() {
        int totalDistance = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int value = board[i][j];
                if (value == 0) {
                    continue;
                }
                int goalRow = (value - 1) / cols; // rzad docelowy
                int goalCol = (value - 1) % cols; // kolumna docelow

                int distance = Math.abs(i - goalRow) + Math.abs(j - goalCol);

                totalDistance += distance;
            }
        }
        return totalDistance;
    }
}
