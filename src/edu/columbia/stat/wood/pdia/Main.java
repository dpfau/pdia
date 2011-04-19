package edu.columbia.stat.wood.pdia;

import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

public class Main {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		File objs = new File(args[0] + "results/PDIA_DMMs.gz");
		
		ObjectOutputStream oos = null;
		HashMap<Integer,Integer> alphabet  = new HashMap<Integer,Integer>();
		int[][] train = Util.loadText(args[0] + "data/aiw.train", alphabet);
		int[][] test = Util.loadText(args[0] + "data/aiw.train", alphabet);

		try {
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));
			
			PDIA_Dirichlet[] pdias = PDIA_Dirichlet.sample(Integer.parseInt(args[1]), 5, Integer.parseInt(args[2]), alphabet.size()-1, train);
			for (PDIA_Dirichlet pdia : pdias) {
				double[] score = PDIA_Dirichlet.score(new PDIA_Dirichlet[]{pdia}, 0, test);
				System.out.println("SingleMachinePrediction = " + Util.scoreToLogLoss(score));
			}
			System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA_Dirichlet.score(pdias, 0, test)));
			oos.writeObject(pdias);
		} finally {
			if (oos != null) {
				oos.close();
			}
		}
    }
}
