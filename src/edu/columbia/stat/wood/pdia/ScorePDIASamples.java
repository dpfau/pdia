/*
 * Created on May 31, 2011
 */
package edu.columbia.stat.wood.pdia;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Jonathan Huggins
 *
 */
public class ScorePDIASamples {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length < 3)
			System.err.println("At least three args required: fileBase fileArgs trainFile [testFile]");

		boolean haveTest = args.length > 3;
		String fileBase = args[0];
		String[] fileArgs = args[1].split(" ");
		String trainFile = args[2];
		
		int N = fileArgs.length;
		
		String testFile = null;
		double[][] testPerps = null;
		if (haveTest) { 
			testFile = args[3];
			testPerps = new double[N][];
		} 
		
		double[][] trainPerps = new double[N][];

		for (int i = 0; i < N; i++) {
		    System.out.println(i);
		    double[][] scores = Util.score(String.format(fileBase, fileArgs[i]), trainFile, testFile);
		    
		    trainPerps[i] = scores[0];
		    if (haveTest) {
		    	testPerps[i] = scores[1];
		    } 
		}
	}

}
