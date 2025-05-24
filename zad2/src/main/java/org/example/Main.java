package org.example;

import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        // Wczytaj konfigurację
        Configuration config = ConfigLoader.LoadConfig("config.txt");
        if (config == null) {
            System.out.println("Nie udało się wczytać konfiguracji. Używam domyślnych wartości.");
            config = Configuration.defaultConfig();
        }

        System.out.println("=== UKRYTE NEURONY = " + config.hiddenNeurons);
        System.out.println("=== FUNKCJA AKTYWACJI = " + config.activation.toString().toUpperCase());
        System.out.println("=== EPOKI = " + config.epochs);
        System.out.println("=== WSPÓŁCZYNNIK UCZENIA = " + config.learningRate);

        // Wczytaj dane
        List<double[]> inputList = new ArrayList<>();
        List<double[]> outputList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("data/data.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 4) continue; // pomijaj błędne linie
                double xMeasured = Double.parseDouble(tokens[0]);
                double yMeasured = Double.parseDouble(tokens[1]);
                double xActual = Double.parseDouble(tokens[2]);
                double yActual = Double.parseDouble(tokens[3]);

                inputList.add(new double[]{xMeasured, yMeasured});
                outputList.add(new double[]{xActual, yActual});
            }
        }

        INDArray input = Nd4j.create(inputList.toArray(new double[0][]));
        INDArray labels = Nd4j.create(outputList.toArray(new double[0][]));
        DataSet allData = new DataSet(input, labels);

        // Skalowanie danych
        NormalizerMinMaxScaler scaler = new NormalizerMinMaxScaler();
        scaler.fit(allData);
        scaler.transform(allData);

        // Podział na zbiór treningowy i testowy
        List<DataSet> list = allData.asList();
        Collections.shuffle(list, new Random(123));
        int splitIndex = (int) (0.8 * list.size());
        List<DataSet> trainList = list.subList(0, splitIndex);
        List<DataSet> testList = list.subList(splitIndex, list.size());

        DataSetIterator trainIter = new ListDataSetIterator<>(trainList, 5);
        DataSetIterator testIter = new ListDataSetIterator<>(testList, 5);

        // Konfiguracja sieci z parametrami z config
        MultiLayerConfiguration networkConfig = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Adam(config.learningRate))
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(2)
                        .nOut(config.hiddenNeurons)
                        .activation(config.activation)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .nIn(config.hiddenNeurons)
                        .nOut(2)
                        .activation(org.nd4j.linalg.activations.Activation.IDENTITY)
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(networkConfig);
        model.init();

        // Trenowanie modelu
        for (int epoch = 0; epoch < config.epochs; epoch++) {
            trainIter.reset();
            model.fit(trainIter);

            double trainMSE = model.score();
            System.out.printf("Epoka %d - Średni błąd kwadratowy: %.6f%n", epoch + 1, trainMSE);
        }

        // Testowanie i wypisanie wyników
        System.out.println("\nTest predictions:");
        while (testIter.hasNext()) {
            DataSet ds = testIter.next();
            INDArray predicted = model.output(ds.getFeatures(), false);
            System.out.println("=== Predicted === \n" + predicted + "\n=== Actual === \n" + ds.getLabels());
        }
    }
}
