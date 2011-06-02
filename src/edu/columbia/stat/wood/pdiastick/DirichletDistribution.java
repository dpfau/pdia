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
public class DirichletDistribution implements Distribution<CategoricalDistribution> {
    private double[] params;
    private GammaDistribution[] gammas;

    public DirichletDistribution( double[] par ) {
        params = par;
        gammas = new GammaDistribution[ par.length ];
        for ( int i = 0; i < par.length; i++ ) {
            gammas[i] = new GammaDistribution( par[i], 1 );
        }
    }

    public double probability( CategoricalDistribution x ) {
        return Math.exp( logProbability( x ) );
    }

    public double logProbability( CategoricalDistribution x ) {
        double[] probs = x.parameters();
        assert probs.length == params.length : "Data has the wrong number of categories!";
        double logProb = Gamma.logGamma( Util.sum( params ) );
        for ( int i = 0; i < probs.length; i++ ) {
            logProb += ( params[i] - 1 ) * Math.log( probs[i] );
            logProb -= Gamma.logGamma( params[i] );
        }
        return logProb;
    }

    public CategoricalDistribution sample() {
        double[] samp = new double[ params.length ];
        for ( int i = 0; i < params.length; i++ ) {
            samp[i] = gammas[i].sample();
        }
        return new CategoricalDistribution( samp );
    }

    public double[] parameters() {
        return params;
    }
}
