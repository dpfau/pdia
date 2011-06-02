/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

/**
 *
 * @author davidpfau
 */
public class CategoricalDistribution implements Distribution<Integer> {

    private double[] probs;
    
    public CategoricalDistribution( double[] pi ) {
        double sum = Util.sum( pi );
        probs = new double[ pi.length ];
        for ( int i = 0; i < pi.length; i++ ) {
            probs[i] = pi[i] / sum;
        }
    }

    public double probability( Integer i ) {
        if ( i >= 0 && i < probs.length ) {
            return probs[i];
        } else {
            return 0;
        }
    }

    public double logProbability( Integer i ) {
        if ( i >= 0 && i < probs.length ) {
            return Math.log( probs[i] );
        } else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public Integer sample() {
        double samp = rng.nextDouble();
        double cumsum = 0;
        for ( int i = 0; i < probs.length; i++ ) {
            cumsum += probs[i];
            if ( samp < cumsum ) {
                return i;
            }
        }
        return probs.length - 1;
    }

    public double[] parameters() {
        return probs;
    }
}
