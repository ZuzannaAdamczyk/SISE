package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java Main <strategy> <param> <input_file> <solution_file> <stats_file>");
            return;
        }

        String strategy = args[0];
        String param = args[1];
        String inputFile = args[2];
        String solutionFile = args[3];
        String statsFile = args[4];

        PuzzleState initial = null;
        try {
            initial = readInitialState(inputFile);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }

        SearchResult result = null;

        long start = System.nanoTime();
        switch (strategy) {
            case "bfs":
                result = new BFS(initial, param).bfs();
                break;
            case "dfs":
                result = new DFS(initial, param).dfs();
                break;
            case "astr":
                result = new AStar(initial, param).solve();
                break;
            default:
                System.err.println("Unknown strategy: " + strategy);
                return;
        }
        long end = System.nanoTime();
        result.time = (end - start) / 1e6; // ms
        try {
            writeSolutionFile(result, solutionFile);
            writeStatsFile(result, statsFile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        }

    private static PuzzleState readInitialState(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        String[] first = lines.get(0).trim().split("\\s+");
        boolean hasHeader = first.length == 2 && first[0].matches("\\d+") && first[1].matches("\\d+");

        int rows, cols, start = 0;
        if (hasHeader) {
            rows = Integer.parseInt(first[0]);
            cols = Integer.parseInt(first[1]);
            start = 1;                 // dane zaczynają się w drugiej linii
        } else {
            rows = lines.size();
            cols = lines.get(0).trim().split("\\s+").length;
        }

        int[][] board = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            String[] tokens = lines.get(i + start).trim().split("\\s+");
            for (int j = 0; j < cols; j++) {
                board[i][j] = Integer.parseInt(tokens[j]);
            }
        }
        return new PuzzleState(rows, cols, board);
    }

    private static void writeSolutionFile(SearchResult result, String path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            writer.write(Integer.toString(result.length));
            writer.newLine();
            if (result.length > 0) {
                for (char move : result.moves) {
                    writer.write(move);
                }
                writer.newLine();
            }
        }
    }

    private static void writeStatsFile(SearchResult result, String path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
            writer.write(String.valueOf(result.length));
            writer.newLine();
            writer.write(String.valueOf(result.visitedCount));
            writer.newLine();
            writer.write(String.valueOf(result.processedCount));
            writer.newLine();
            writer.write(String.valueOf(result.maxDepth));
            writer.newLine();
            writer.write(String.format("%.3f", result.time));
            writer.newLine();
        }
    }
    }