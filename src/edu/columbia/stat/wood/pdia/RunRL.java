package edu.columbia.stat.wood.pdia;

import java.io.*;
import java.util.HashMap;
import java.util.zip.*;

public class RunRL {

	public static void main(String[] args) throws FileNotFoundException, IOException {

		File objs = new File(args[0] + "results/PDIA_DMMs.gz");

		ObjectOutputStream oos = null;
		HashMap<Integer,Integer> actions      = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> observations = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> rewards      = new HashMap<Integer,Integer>();
		int[][] train_a = Util.loadText(args[0] + "RL/linworld.a", actions);
		int[][] test_a = Util.loadText(args[0] + "RL/linworld2.a", actions);

                int[][] train_o = Util.loadText(args[0] + "RL/linworld.o", observations);
		int[][] test_o = Util.loadText(args[0] + "RL/linworld2.o", observations);

		int[][] train_r = Util.loadText(args[0] + "RL/linworld.r", rewards);
		int[][] test_r = Util.loadText(args[0] + "RL/linworld2.r", rewards);

		try {
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objs)));

			PDIA_DMM[] pdias = PDIA_DMM.sample(Integer.parseInt(args[1]), 5, Integer.parseInt(args[2]), new int[]{actions.size(),observations.size(),rewards.size()}, train_a, train_o, train_r);
			for (PDIA_DMM pdia : pdias) {
				double[] score = PDIA_DMM.score(new PDIA_DMM[]{pdia}, 0, test_a, test_o, test_r);
				System.out.println("SingleMachinePrediction = " + Util.scoreToLogLoss(score));
			}
			System.out.println("Average Prediction = " + Util.scoreToLogLoss(PDIA_DMM.score(pdias, 0, test_a, test_o, test_r)));
			oos.writeObject(pdias);
		} finally {
			if (oos != null) {
				oos.close();
			}
		}
    }
}
