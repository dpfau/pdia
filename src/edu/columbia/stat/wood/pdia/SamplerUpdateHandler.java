/*
 * Created on Apr 17, 2011
 */
package edu.columbia.stat.wood.pdia;

/**
 * Interface for classes that can take action at 
 * periodic intervals while a PDIA sampler is running
 * in order to, for example, save the array of 
 * current samples to disk.
 * 
 * @author Jonathan Huggins
 *
 */
public interface SamplerUpdateHandler {

	/**
	 * Take some action using the current array of samples.
	 * 
	 * @param pdias arrays of PDIA samples
	 * @param count only the first <tt>count</tt> slots in the array
	 * will contain samples while the rest will be <tt>null</tt>
	 */
	public void update(PDIA[] pdias, int count);
	
}
