package edu.columbia.stat.wood.pdia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class Main {

    public static void main(String[] args)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        File objs = new File(args[0] + "results/objectsFromPDIA_hpy.txt.gz");

        ObjectOutputStream oos = null;
        HashMap<Integer,Integer> alphabet = new HashMap<Integer,Integer>();
        int[][] train = Util.loadText(args[0] + "data/aiw.train", alphabet);
        int[][] test = Util.loadText(args[0] + "data/aiw.test", alphabet);

        //try {
        //    oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));

            int s = 1;

            PDIA[] pdias = PDIA.sample(15000, 5, 3500, new int[]{27}, train);
            for (PDIA pdia : pdias) {
                double[] score = PDIA.score(new PDIA[]{pdia}, 0, test);

          //      oos.writeObject(pdia.beta);
          //      oos.writeObject(pdia.dMatrix);
          //      oos.writeObject(pdia.rf);

                System.out.println("SingleMachinePrediction = " + Util.scoreToLogLoss(score));
            }
            System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA.score(pdias, 0, test)));
        //} finally {
        //    if (oos != null) {
        //        oos.close();
        //    }
        //}
    }
}
