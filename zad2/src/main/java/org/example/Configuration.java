package org.example;

import org.nd4j.linalg.activations.Activation;

public class Configuration {
    public int hiddenNeurons;
    public Activation activation; // "sigmoid", "tanh", "relu"
    public double learningRate;
    public int epochs;
    public int patience;
    public int batchSize;
    public Configuration(int hiddenNeurons, String activation, double learningRate, int epochs, int patience, int batchSize) {
        this.hiddenNeurons = hiddenNeurons;
        this.activation = getActivationFromString(activation);
        this.learningRate = learningRate;
        this.epochs = epochs;
        this.patience = patience;
        this.batchSize = batchSize;
    }

    public static Configuration defaultConfig() {
        return new Configuration(10, "relu", 0.01, 100, 10, 32);
    }

    private static Activation getActivationFromString(String act) {
        switch (act.toLowerCase()) {
            case "relu":
                return Activation.RELU;
            case "tanh":
                return Activation.TANH;
            case "sigmoid":
                return Activation.SIGMOID;
            default:
                System.err.println("Nieznana funkcja aktywacji, używam relu");
                return Activation.RELU;
        }
    }
}
