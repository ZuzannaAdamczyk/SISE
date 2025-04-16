package org.example;

public class PuzzleNeighbor {

    public final PuzzleState state;
    public final char move;
    public PuzzleNeighbor(PuzzleState state, char move) {
        this.state = state;
        this.move = move;
    }
}
