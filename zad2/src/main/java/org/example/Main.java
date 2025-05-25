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

        // Ścieżki do folderów
        List<String> trainingFolders = Arrays.asList(
                "data/f8/stat",
                "data/f10/stat"
        );
        List<String> testingFolders = Arrays.asList(
                "data/f8/dyn",
                "data/f10/dyn"
        );

        // Wczytaj dane
        DataSet trainData = FileLoader.loadDataFromFolders(trainingFolders);
        DataSet testData = FileLoader.loadDataFromFolders(testingFolders);

        // Normalizacja
        NormalizerMinMaxScaler scaler = new NormalizerMinMaxScaler();
        scaler.fit(trainData);
        scaler.transform(trainData);
        scaler.transform(testData);

        // Iterator
        DataSetIterator trainIter = new ListDataSetIterator<>(trainData.asList(), 50);
        DataSetIterator testIter = new ListDataSetIterator<>(testData.asList(), 50);

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

        // === RĘCZNE TRENING I ZAPIS MSE DO CSV ===
        List<Double> trainMSEList = new ArrayList<>();
        List<Double> testMSEList = new ArrayList<>();

        int noImprovementCount = 0;
        double bestTestScore = Double.MAX_VALUE;  // minimalizujemy MSE, więc zaczynamy od dużej wartości

        for (int epoch = 0; epoch < config.epochs; epoch++) {
            model.fit(trainIter);
            trainIter.reset();
            testIter.reset();

            double trainScore = model.score(trainData);
            double testScore = model.score(testData);

            trainMSEList.add(trainScore);
            testMSEList.add(testScore);

            System.out.printf("Epoka %d | Train MSE: %.6f | Test MSE: %.6f%n", epoch + 1, trainScore, testScore);

            // Early stopping logic:
            if (testScore < bestTestScore) {
                bestTestScore = testScore;
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }

            if (noImprovementCount >= config.patience) {
                System.out.println("Brak poprawy test MSE przez " + config.patience + " epok. Przerywam trening.");
                break;
            }
        }

        // Zapis MSE do pliku CSV
        try (PrintWriter mseWriter = new PrintWriter(new BufferedWriter(new FileWriter("results/mse_per_epoch_" + config.activation.toString() + ".csv")))) {
            mseWriter.println("Epoch;TrainMSE;TestMSE");
            for (int i = 0; i < trainMSEList.size(); i++) {
                mseWriter.printf("%d;%.8f;%.8f%n", i + 1, trainMSEList.get(i), testMSEList.get(i));
            }
        }

        // === TESTOWANIE I ZAPIS WYNIKÓW W ORYGINALNEJ SKALI ===
        testIter.reset();
        String resultFilename = "results/results_" + config.activation.toString() + ".csv";
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(resultFilename)))) {
            writer.println("Example;PredX;PredY;ActualX;ActualY;ErrorX;ErrorY;Distance;MeasX;MeasY");

            System.out.println("\nTest predictions (original scale):");
            int example = 1;
            while (testIter.hasNext()) {
                DataSet ds = testIter.next();

                INDArray predictedNorm = model.output(ds.getFeatures(), false);
                INDArray actualNorm = ds.getLabels();
                INDArray inputNorm = ds.getFeatures();

                // przywracamy do oryginalnej skali
                INDArray predicted = predictedNorm.dup();
                scaler.revertLabels(predicted);

                INDArray actual = actualNorm.dup();
                scaler.revertLabels(actual);

                INDArray measured = inputNorm.dup();
                scaler.revertFeatures(measured);


                for (int i = 0; i < predicted.rows(); i++) {
                    double predX = predicted.getDouble(i, 0);
                    double predY = predicted.getDouble(i, 1);
                    double actualX = actual.getDouble(i, 0);
                    double actualY = actual.getDouble(i, 1);

                    double errorX = predX - actualX;
                    double errorY = predY - actualY;
                    double distance = Math.sqrt(errorX * errorX + errorY * errorY);

                    double measX = measured.getDouble(i, 0);
                    double measY = measured.getDouble(i, 1);


                    writer.printf("%d;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f%n",
                            example, predX, predY, actualX, actualY, errorX, errorY, distance, measX, measY);

                    example++;
                }
            }
        }
    }
}
