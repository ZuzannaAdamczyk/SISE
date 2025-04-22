package org.example;

import java.util.*;

public class BFS {

    private final  Queue<PuzzleState> queue = new ArrayDeque<>();
    private final  HashSet<PuzzleState> visited = new HashSet<>();
    private final Map<PuzzleState, ParentInfo> prev = new HashMap<>();
    private final char[] order;



    public BFS(PuzzleState start, String moveOrder) {

        this.order = moveOrder.toCharArray();

        queue.add(start);
        visited.add(start);
        prev.put(start, new ParentInfo(null, 'X', 0));
    }

    public SearchResult bfs() {
        SearchResult result = new SearchResult();

        result.visitedCount = 1;
        result.processedCount = 0;
        result.maxDepth  = 0;

        while (!queue.isEmpty()) {
            PuzzleState current = queue.poll();


            if(current.isGoal()) {
                return finalizeResult(result, current);}


            result.processedCount++;
            int depthHere = prev.get(current).depth;

            for(PuzzleNeighbor neighbor : current.getNeighbor(order)){

                if(visited.add(neighbor.state)) {
                    result.visitedCount++;

                    prev.put(neighbor.state, new ParentInfo(current, neighbor.move, depthHere + 1));

                    result.maxDepth = Math.max(result.maxDepth, depthHere + 1);
                    queue.add(neighbor.state);
                }
            }
        }
        result.length = - 1;

        return result;




    }

    private SearchResult finalizeResult(SearchResult result, PuzzleState p) {
        List<Character> path = new ArrayList<>();
        PuzzleState tmp = p;

        while (true) {
            ParentInfo parent = prev.get(tmp);
            if (parent == null || parent.parent == null) break;

            path.add(parent.move);
            tmp = parent.parent;
        }
        Collections.reverse(path);
        result.length = path.size();
        result.moves = path;
        return result;
    }

}
