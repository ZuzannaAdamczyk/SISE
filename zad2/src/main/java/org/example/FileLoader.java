package org.example;

import org.nd4j.linalg.factory.Nd4j;
import java.io.BufferedReader;
import java.io.FileReader;
import org.nd4j.linalg.api.ndarray.INDArray;
import java.util.ArrayList;
import java.util.List;
import org.nd4j.linalg.dataset.DataSet;
import java.io.*;

public class FileLoader {

    public static DataSet loadDataFromFolders(List<String> csvFolders) throws IOException {
        List<double[]> inputList = new ArrayList<>();
        List<double[]> outputList = new ArrayList<>();

        for (String folderPath : csvFolders) {
            File folder = new File(folderPath);
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files == null) continue;

            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length < 4) continue;

                        double xMeasured = Double.parseDouble(tokens[0]);
                        double yMeasured = Double.parseDouble(tokens[1]);
                        double xActual = Double.parseDouble(tokens[2]);
                        double yActual = Double.parseDouble(tokens[3]);

                        inputList.add(new double[]{xMeasured, yMeasured});
                        outputList.add(new double[]{xActual, yActual});
                    }
                }
            }
        }

        INDArray input = Nd4j.create(inputList.toArray(new double[0][]));
        INDArray labels = Nd4j.create(outputList.toArray(new double[0][]));
        return new DataSet(input, labels);
    }
}
