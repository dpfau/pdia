/*
 * Created on Apr 17, 2011
 */
package edu.columbia.stat.wood.pdia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Simple implementation of <tt>SamplerUpdateHandler</tt> that
 * writes the samples to disk, compressed as a GZip file.
 * 
 * @author Jonathan Huggins
 *
 */
public class SaveSamplesHandler implements SamplerUpdateHandler {

	File saveFile;
	File backupFile;
	
	public SaveSamplesHandler(File file) {
		saveFile = file;
		backupFile = new File(file.getAbsolutePath() + ".backup");
	}
	/**
	 * Write the samples to disk.  
	 * 
	 * @see edu.columbia.stat.wood.pdia.SamplerUpdateHandler#update(edu.columbia.stat.wood.pdia.PDIA[], int)
	 */
	public void update(PDIA[] pdias, int count) {
		System.out.println("Saving " + count + " PDIAs to disk");
		// in necessary, backup file first
		if (saveFile.exists()) {
			try {
				Util.copyFile(saveFile, backupFile);
			} catch (IOException e) {
				System.err.println("Unable to back up PDIA file first: " + e.getMessage());
			}
		}
		
		// now write out
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(saveFile)));
			oos.writeObject(pdias);
		} catch (FileNotFoundException e) {
			System.err.println("SamplerUpdateHandler: unable to write samples disk: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("SamplerUpdateHandler: unable to write samples disk: " + e.getMessage());
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					System.err.println("SamplerUpdateHandler: error closing file: " + e.getMessage());
				}
			}
		}
	}

}
