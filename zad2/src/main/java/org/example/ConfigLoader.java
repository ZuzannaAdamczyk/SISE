package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    public static Configuration LoadConfig(String filename) {
        Map<String, String> configMap = new HashMap<>();

        try (BufferedReader configReader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = configReader.readLine()) != null) {
                line = line.split("#")[0].trim(); // obsluga komentarzy
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    configMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("Problem z ładowaniem konfiguracji, użyto wartości domyślnych.");
            return null;
        }

        try {
            int hiddenNeurons = Integer.parseInt(configMap.getOrDefault("hidden_neurons", "10"));
            String activation = configMap.getOrDefault("activation", "tanh").toLowerCase();
            double learningRate = Double.parseDouble(configMap.getOrDefault("learning_rate", "0.01"));
            int epochs = Integer.parseInt(configMap.getOrDefault("epochs", "100"));
            int patience = Integer.parseInt(configMap.getOrDefault("patience", "10"));
            int batchSize = Integer.parseInt(configMap.getOrDefault("batch_size", "32"));


            return new Configuration(hiddenNeurons, activation, learningRate, epochs, patience, batchSize);
        } catch (Exception e) {
            System.err.println("Błąd w danych konfiguracyjnych, użyto wartości domyślnych.");
            return null;
        }
    }
}
