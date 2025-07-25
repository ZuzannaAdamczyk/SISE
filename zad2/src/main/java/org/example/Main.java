package org.example;

import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        String configPath = "config.txt";
        // Wczytaj konfigurację
        Configuration config = ConfigLoader.LoadConfig(configPath);
        if (config == null) {
            System.out.println("Nie udało się wczytać konfiguracji. Używam domyślnych wartości.");
            config = Configuration.defaultConfig();
        }

        String resultsDir = "ostateczne5plsrelu";
        if (new File(resultsDir).mkdir()) {
            System.out.println("Utworzono folder: " + resultsDir);
        }

        System.out.println("=== UKRYTE NEURONY = " + config.hiddenNeurons);
        System.out.println("=== FUNKCJA AKTYWACJI = " + config.activation.toString().toUpperCase());
        System.out.println("=== EPOKI = " + config.epochs);
        System.out.println("=== WSPÓŁCZYNNIK UCZENIA = " + config.learningRate);
        System.out.println("=== CIERPLIWOŚĆ = " + config.patience);
        System.out.println("=== BATCH SIZE = " + config.batchSize);

        // Ścieżki do folderów
        List<String> trainingFolders = Arrays.asList(
                "data/f8/stat",
                "data/f10/stat"
        );
        List<String> testingFolders = Arrays.asList(
                "data/f8/dyn",
                "data/f10/dyn"
        );

        // wczytujemy dane
        DataSet trainData = FileLoader.loadDataFromFolders(trainingFolders);
        DataSet testData = FileLoader.loadDataFromFolders(testingFolders);

        // normalizacja danych
        NormalizerStandardize normalizer = new NormalizerStandardize();
        normalizer.fitLabel(true);
        normalizer.fit(trainData);          // uczymy normalizator na danych treningowych
        normalizer.transform(trainData);    // normalizujemy dane treningowe
        normalizer.transform(testData);     // normalizujemy dane testowe

        // iteratory na danych przeskalowanych
        DataSetIterator trainIter = new ListDataSetIterator<>(trainData.asList(), config.batchSize);
        DataSetIterator testIter = new ListDataSetIterator<>(testData.asList(), config.batchSize);

        // konfiguracja sieci z parametrami z config.txt
        MultiLayerConfiguration networkConfig = new NeuralNetConfiguration.Builder()
                .seed(123456)
                .updater(new Adam(config.learningRate))
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(2)
                        .nOut(config.hiddenNeurons)
                        .weightInit(WeightInit.XAVIER)
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

        List<Double> trainMSEList = new ArrayList<>();
        List<Double> testMSEList = new ArrayList<>();


        int noImprovementCount = 0;
        double bestTestScore = Double.MAX_VALUE;

        for (int epoch = 0; epoch < config.epochs; epoch++) {
            trainIter.reset();
            model.fit(trainIter);


            double trainScore = model.score(trainData);
            double testScore = model.score(testData);

            trainMSEList.add(trainScore);
            testMSEList.add(testScore);

            System.out.printf("Epoka %d | Train MSE: %.6f | Test MSE: %.6f%n", epoch + 1, trainScore, testScore);

            // early stopping
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
        try (PrintWriter mseWriter = new PrintWriter(resultsDir + "/mse_per_epoch_" + config.activation + ".csv")) {
            mseWriter.println("Epoch;TrainMSE;TestMSE");
            for (int i = 0; i < trainMSEList.size(); i++) {
                mseWriter.println((i + 1) + ";" + trainMSEList.get(i) + ";" + testMSEList.get(i));
            }
        }

        // TESTOWANIE I ZAPIS WYNIKÓW (W ORYGINALNEJ SKALI)
        testIter.reset();
        String resultFilename = resultsDir + "/results_" + config.activation.toString() + ".csv";
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(resultFilename)))) {
            writer.println("Example;PredX;PredY;ActualX;ActualY;ErrorX;ErrorY;Distance;MeasX;MeasY");

            System.out.println("\nTest predictions (original scale):");
            int example = 1;
            while (testIter.hasNext()) {
                DataSet ds = testIter.next();

                INDArray predicted = model.output(ds.getFeatures(), false);
                INDArray actual = ds.getLabels();
                INDArray measured = ds.getFeatures();

                // Tworzymy kopie do odwrócenia normalizacji

                INDArray predictedUnscaled = predicted.dup().castTo(DataType.DOUBLE);
                INDArray actualUnscaled = actual.dup().castTo(DataType.DOUBLE);
                INDArray measuredUnscaled = measured.dup().castTo(DataType.DOUBLE);

                // Odwracamy normalizację na etykietach i cechach
                normalizer.revertLabels(predictedUnscaled);
                normalizer.revertLabels(actualUnscaled);
                normalizer.revertFeatures(measuredUnscaled);

                for (int i = 0; i < predictedUnscaled.rows(); i++) {
                    double predX = predictedUnscaled.getDouble(i, 0);
                    double predY = predictedUnscaled.getDouble(i, 1);
                    double actualX = actualUnscaled.getDouble(i, 0);
                    double actualY = actualUnscaled.getDouble(i, 1);

                    double errorX = predX - actualX;
                    double errorY = predY - actualY;
                    double distance = Math.sqrt(errorX * errorX + errorY * errorY);

                    double measX = measuredUnscaled.getDouble(i, 0);
                    double measY = measuredUnscaled.getDouble(i, 1);

                    writer.printf("%d;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f;%.6f%n",
                            example, predX, predY, actualX, actualY, errorX, errorY, distance, measX, measY);

                    example++;
                }
            }
        }

    }

}
