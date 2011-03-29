package edu.columbia.stat.wood.pdia;

import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

public class Main {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		File objs = new File(args[0] + "results/PDIAs_hpy.txt.gz");
		
		ObjectOutputStream oos = null;
		HashMap<Integer,Integer> alphabet = new HashMap<Integer,Integer>();
		int[][] train = Util.loadText(args[0] + "data/aiw.train", alphabet);
		int[][] test = Util.loadText(args[0] + "data/aiw.test", alphabet);

		try {
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));

			PDIA_HPYP[] pdias = PDIA_HPYP.sample(Integer.parseInt(args[1]), 5, Integer.parseInt(args[2]), new int[]{alphabet.size()}, train);
			for (PDIA_HPYP pdia : pdias) {
				double[] score = PDIA_HPYP.score(new PDIA[]{pdia}, 0, test);
				System.out.println("SingleMachinePrediction = " + Util.scoreToLogLoss(score));
			}
			System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA.score(pdias, 0, test)));
			oos.writeObject(pdias);
		} finally {
			if (oos != null) {
				oos.close();
			}
		}
    }
}
