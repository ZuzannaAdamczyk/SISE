package org.example;

public class ParentInfo {

    public PuzzleState parent;
    public char move;
    public int depth;

    public ParentInfo(PuzzleState parent, char move, int depth) {
        this.parent = parent;
        this.move = move;
        this.depth = depth;

    }

}
