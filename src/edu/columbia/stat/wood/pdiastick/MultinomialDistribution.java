/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import org.apache.commons.math.special.Gamma;

/**
 *
 * @author davidpfau
 */
public class MultinomialDistribution implements Distribution<int[]> {
    private int n;
    private double[] probs;
    private CategoricalDistribution cat;

    public MultinomialDistribution( double[] pi, int num ) {
        cat = new CategoricalDistribution( pi );
        probs = cat.parameters();
        n = num;
    }

    public MultinomialDistribution( CategoricalDistribution cd, int num ) {
        cat = cd;
        probs = cat.parameters();
        n = num;
    }

    public double probability( int[] x ) {
        assert x.length == probs.length : "Counts have the wrong number of categories!";
        double prob = Math.exp( Gamma.logGamma( Util.sum( x ) + 1 ) );
        for ( int i = 0; i < x.length; i++ ) {
            if ( x[i] < 0 ) return 0;
            prob *= Math.pow( probs[i], x[i] );
            prob /= Math.exp( Gamma.logGamma( x[i] + 1 ) );
        }
        return prob;
    }

    public double logProbability( int[] x ) {
        assert x.length == probs.length : "Counts have the wrong number of categories!";
        int num = Util.sum( x );
        assert num == n : "Wrong number of total counts!";
        double logProb = Gamma.logGamma( Util.sum( x ) + 1 );
        for ( int i = 0; i < x.length; i++ ) {
            if ( x[i] < 0 ) return Double.NEGATIVE_INFINITY;
            logProb += x[i] * Math.log( probs[i] );
            logProb -= Gamma.logGamma( x[i] + 1 );
        }
        return logProb;
    }

    public int[] sample() {
        int[] samp = new int[ probs.length ];
        for ( int i = 0; i < n; i++ ) {
            samp[ cat.sample() ] ++;
        }
        return samp;
    }

    public Object[] parameters() {
        return new Object[] { n, probs };
    }
}
