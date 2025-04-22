package org.example;
import java.util.*;

import java.util.PriorityQueue;

public class AStar {

    private static class Node implements Comparable<Node> {
        final PuzzleState state;
        final int g;
        final int f;

        Node(PuzzleState state, int g, int f) {
            this.state = state;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.f, o.f);
        }

        @Override
        public int hashCode() {
            return state.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Node) && state.equals(((Node) o).state);
        }
    }
        private final PriorityQueue<Node> open = new PriorityQueue<>();
        private final Set<PuzzleState> closed = new HashSet<>();
        private final Map<PuzzleState, ParentInfo> prev = new HashMap<>();

    private final boolean useHamming;      // true = hamming, false = manhattan
    private final char[] order = {'R','D','U','L'};
    private final long startTime;

    public AStar(PuzzleState start, String s ) {
        this.useHamming = s.equalsIgnoreCase("hamm");
        int h0 = heuristic(start);
        open.add(new Node(start, 0, h0));
        prev.put(start, new ParentInfo(null, 'X', 0));
        startTime = System.nanoTime();

    }
    private int heuristic(PuzzleState s) {
        if( useHamming ) {
            return s.hamming();
        } else {
            return s.manhattan();
        }
    }

    public SearchResult solve() {
        SearchResult result = new SearchResult();
        result.visitedCount = 1;

        while(!open.isEmpty()) {

            Node current = open.poll();
            PuzzleState state = current.state;
            if (state.isGoal()) {
                return finish(result, state);
        }
            if (closed.add(state)) {
            result.processedCount++;
            for (PuzzleNeighbor n : state.getNeighbor(order)) {
                if (closed.contains(n.state)) {continue;}

                int g = current.g + 1;
                int f = g +  heuristic(n.state);

                Node candidate = new Node(n.state, g, f);

                if (!open.contains(candidate)) {
                    open.add(candidate);
                }
                prev.putIfAbsent(n.state, new ParentInfo(state, n.move, g));

                result.visitedCount++;
                result.maxDepth = Math.max(result.maxDepth, g);

            }
            }
        }
        result.length = -1;
        return result;
    }


    private SearchResult finish(SearchResult r, PuzzleState goal) {
        List<Character> path = new ArrayList<>();
        for (PuzzleState p = goal; ; ) {
            ParentInfo parentp = prev.get(p);
            if(parentp == null || parentp.parent == null) {break;}
            path.add(parentp.move);
            p = parentp.parent;
        }
        Collections.reverse(path);
        r.length = path.size();
        r.moves = path;
        return r;

    }




}




