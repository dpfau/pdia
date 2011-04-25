package edu.columbia.stat.wood.pdia;

import java.io.*;
import java.util.HashMap;

public class Main {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		if (args.length < 3) {
			System.err.println("Insufficient arguments\nUsage: java Main <working-dir> <burn-in> <samples>");
			System.exit(1);
		}
//		File objs = new File(args[0] + "results/PDIA_DMMs.gz");
		
//		ObjectOutputStream oos = null;
		HashMap<Integer,Integer> alphabet  = new HashMap<Integer,Integer>();

		int[][] train = Util.loadText(args[0] + "data/aiw_full.test", alphabet);
		int[][] test = Util.loadText(args[0] + "data/aiw_full.train", alphabet);
		
		int burnin =  Integer.parseInt(args[1]);
		int samples = Integer.parseInt(args[2]);
		SaveSamplesHandler updater = new SaveSamplesHandler(new File(args[0] + "results/PDIA_DMMs.gz"));
		int updateInterval = Math.max(1, (int)(.5 + samples/10.0));
//		try {
//			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));
			System.out.println("Sampling...");
			PDIA_Dirichlet[] pdias = PDIA_Dirichlet.sample(burnin, 5, samples, alphabet.size()-1, 
					train, updater, updateInterval);
			for (PDIA_Dirichlet pdia : pdias) {
				double[] score = PDIA_Dirichlet.score(new PDIA_Dirichlet[]{pdia}, 0, test);
				System.out.println("SingleMachinePrediction = " + Util.scoreToLogLoss(score));
			}
			System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA_Dirichlet.score(pdias, 0, test)));
//			oos.writeObject(pdias);
//		} finally {
//			if (oos != null) {
//				oos.close();
//			}
//		}
    }
}
