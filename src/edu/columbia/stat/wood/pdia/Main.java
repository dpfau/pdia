package edu.columbia.stat.wood.pdia;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class Main {

    public static void main(String[] args)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        File train = new File(args[0] + "data/aiw.train");
        File test = new File(args[0] + "data/aiw.test");
        File objs = new File(args[0] + "results/objectsFromPDIA_hpy.txt.gz");

        BufferedInputStream bis = null;
        ObjectOutputStream oos = null;
        int[] symbols;
        int[][] symbolLines;
        HashMap<Integer, Integer> alphabet = new HashMap<Integer, Integer>(28);
        alphabet.put(10, -1);
        try {
            bis = new BufferedInputStream(new FileInputStream(train));

            symbols = new int[(int) train.length()];

            int ind = 0;
            int b;
            while ((b = bis.read()) > -1) {
                Integer c = alphabet.get(b);
                if (c != null) {
                    symbols[(ind++)] = c;
                } else {
                    symbols[(ind++)] = alphabet.size() - 1;
                    alphabet.put(b, alphabet.size() - 1);
                }
            }

            symbolLines = new int[99][];
            int i = 0;
            int line = 0;
            for (int j = 0; j < symbols.length; j++) {
                if (symbols[j] == -1) {
                    symbolLines[line] = new int[j - i];
                    System.arraycopy(symbols, i, symbolLines[line], 0, j - i);
                    i = j + 1;
                    line++;
                }
            }
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
        int[] testSymbols;
        int[][] testSymbolLines;
        try {
            bis = new BufferedInputStream(new FileInputStream(test));
            testSymbols = new int[(int) test.length()];

            int ind = 0;
            int b;
            while ((b = bis.read()) > -1) {
                testSymbols[(ind++)] = alphabet.get(b);
            }
            testSymbolLines = new int[49][];
            int i = 0;
            int line = 0;
            for (int j = 0; j < testSymbols.length; j++) {
                if (testSymbols[j] == -1) {
                    testSymbolLines[line] = new int[j - i];
                    System.arraycopy(testSymbols, i, testSymbolLines[line], 0, j - i);
                    i = j + 1;
                    line++;
                }
            }
        } finally {
            if (bis != null) {
                bis.close();
            }

        }

        try {
            oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));

            int s = 1;

            PDIA[] pdias = PDIA.sample(15000, 5, 3500, new int[]{27}, symbolLines);
            for (PDIA pdia : pdias) {
                double[] score = PDIA.score(new PDIA[]{pdia}, 0, testSymbolLines);

                oos.writeObject(pdia.beta);
                oos.writeObject(pdia.dMatrix);
                oos.writeObject(pdia.rf);

                System.out.println("Iteration = " + s / 10 + " : SingleMachinePrediction = " + Util.scoreToLogLoss(score));
            }
            System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA.score(pdias, 0, testSymbolLines)));
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
    }
}