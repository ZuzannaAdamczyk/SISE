package org.example;

import java.util.*;

public class DFS {

    private static  final int LIMIT = 30;
    private final Deque<PuzzleState> stack = new ArrayDeque<>();
    private final Set<PuzzleState> visited = new HashSet<>();
    private final Map<PuzzleState, ParentInfo> prev = new HashMap<>();
    private final char[] order;

    public DFS(PuzzleState start, String moveOrder) {
        this.order = moveOrder.toCharArray();
         stack.push(start);
         visited.add(start);
         prev.put(start, new ParentInfo(null, 'X', 0));
    }

    public SearchResult dfs() {
        SearchResult result = new SearchResult();
        result.visitedCount = 1;

        while (!stack.isEmpty()) {
            PuzzleState current = stack.pop();

            if(current.isGoal()) {
                return finalizeResult(result, current);

        }
            result.processedCount ++;
            int depth = prev.get(current).depth;
            if(depth >= LIMIT) { continue;}

            for(int i = order.length - 1; i >= 0; i--) {
            char mv = order[i];
            PuzzleState next = current.moveZero(mv);
            if (next == null ) { continue; }
            if (visited.add(next)) {
                result.visitedCount ++;
                prev.put(next, new ParentInfo(current, mv, depth + 1));
                result.maxDepth = Math.max(result.maxDepth, depth + 1);
                stack.push(next);
            }
            }
        }
        result.length= -1;
        return result;
    }


    private SearchResult finalizeResult(SearchResult result, PuzzleState goal) {
        List<Character> path = new ArrayList<>();
        for(PuzzleState s = goal; ;) {
            ParentInfo p = prev.get(s);
            if(p == null || p.parent == null) {break;}
            path.add(p.move);
            s = p.parent;


        }
        Collections.reverse(path);
        result.length = path.size();
        result.moves = path;
        result.maxDepth = Math.max(result.maxDepth, prev.get(goal).depth);
        return result;
    }


}
