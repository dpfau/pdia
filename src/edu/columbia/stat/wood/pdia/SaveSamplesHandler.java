/*
 * Created on Apr 17, 2011
 */
package edu.columbia.stat.wood.pdia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/**
 * Simple implementation of <tt>SamplerUpdateHandler</tt> that
 * writes the samples to disk, compressed as a GZip file.
 * 
 * @author Jonathan Huggins
 *
 */
public class SaveSamplesHandler implements SamplerUpdateHandler {

	File samplesFile;
	File samplesBackupFile;
	File resultsFile;
	File resultsBackupFile;
	
	int[][] train;
	int[][] test;
	int trainSize; 
	int testSize;
	
	// for matlab 
	public SaveSamplesHandler(File sampFile, File resFile, Object[] train, Object[] test) {
		this(sampFile, resFile, Util.objectArrayTo2DIntArray(train), Util.objectArrayTo2DIntArray(test));
	}
	
	public SaveSamplesHandler(File sampFile, File resFile, int[][] train, int[][] test) {
		samplesFile       = sampFile;
		resultsFile       = resFile;
		samplesBackupFile = new File(sampFile.getAbsolutePath() + ".backup");
		resultsBackupFile = new File(resFile.getAbsolutePath() + ".backup");
		this.train = train;
		this.test  = test; 
		trainSize  = Util.totalLen(train);
		testSize   = Util.totalLen(test);
	}
	/**
	 * Write the samples to disk.  
	 * 
	 * @see edu.columbia.stat.wood.pdia.SamplerUpdateHandler#update(edu.columbia.stat.wood.pdia.PDIA[], int)
	 */
	public void update(PDIA[] pdias, int count) {
		System.out.println("Saving " + count + " PDIAs to disk");
		// in necessary, backup file first
		if (samplesFile.exists()) {
			try {
				Util.copyFile(samplesFile, samplesBackupFile);
			} catch (IOException e) {
				System.err.println("Unable to back up PDIA file first: " + e.getMessage());
			}
		}
		if (resultsFile.exists()) {
			try {
				Util.copyFile(resultsFile, resultsBackupFile);
			} catch (IOException e) {
				System.err.println("Unable to back up PDIA file first: " + e.getMessage());
			}
		}
		
		// write out samples
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(samplesFile)));
			oos.writeObject(pdias);
		} catch (FileNotFoundException e) {
			System.err.println("SamplerUpdateHandler: unable to write samples to disk: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("SamplerUpdateHandler: unable to write samples to disk: " + e.getMessage());
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					System.err.println("SamplerUpdateHandler: error closing sample file: " + e.getMessage());
				}
			}
		}
		// write out results
		PrintWriter pw;
		try {
			pw = new PrintWriter(resultsFile);
			double[][] scores = Util.score((PDIA_Dirichlet[])pdias, train, trainSize, test, testSize);
			for (double[] s : scores) {
				pw.println(Arrays.toString(s));	
			}
			pw.flush();
			//oos = new ObjectOutputStream(new FileOutputStream(resultsFile));
			//oos.writeObject(Util.score((PDIA_Dirichlet[])pdias, train, trainSize, test, testSize));
			
		} catch (FileNotFoundException e) {
			System.err.println("SamplerUpdateHandler: unable to write results to disk: " + e.getMessage());
//		} catch (IOException e) {
//			System.err.println("SamplerUpdateHandler: unable to write results to disk: " + e.getMessage());
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					System.err.println("SamplerUpdateHandler: error closing results file: " + e.getMessage());
				}
			}
		}
	}

}
