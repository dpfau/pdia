/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

/**
 * Note that this is not technically a Dirichlet Process, but a sample *from* a
 * Dirichlet Process.  To sample multiple times from the same DP, just generate
 * multiple DP objects with the same concentration and base distribution
 * @author davidpfau
 */
public class DirichletProcess<E> implements Distribution<E> {
    private double concentration;
    private Distribution<E> base;
    private DirichletDistribution betaDistribution;
    private double[] pi; // stick lengths
    private Object[] sticks;

    public DirichletProcess( double alpha, Distribution<E> H ) {
        assert alpha >= 0 : "Negative concentration!";
        concentration = alpha;
        base = H;
        betaDistribution = new DirichletDistribution( new double[] { 1 , concentration } );
        pi = null;
        sticks = null;
    }

    public double probability( E e ) {
        double prob = 0;
        double cumsum = 0;
        for ( int i = 0; i < sticks.length; i++ ) {
            if ( e == (E)sticks[i] ) {
                prob += pi[i];
                cumsum += pi[i];
            }
        }
        return prob + ( 1 - cumsum ) * base.probability(e);
    }

    public double logProbability( E e ) {
        return Math.log( probability( e ) );
    }

    public E sample() {
        double samp = rng.nextDouble();
        if ( pi != null ) {
            double cumsum = 0;
            for ( int i = 0; i < pi.length; i++ ) {
                cumsum += pi[i];
                if ( samp < cumsum ) return (E)sticks[i];
            }

            double[] pi2 = new double[ pi.length + 1 ];
            Object[] sticks2       = new Object[ sticks.length + 1];

            System.arraycopy( pi, 0, pi2, 0, pi.length );
            pi = pi2;

            System.arraycopy( sticks, 0, sticks2, 0, sticks.length );
            sticks = sticks2;

            double newStick = betaDistribution.sample().probability(0);
            pi[ pi.length - 1 ] = ( 1 - cumsum ) * newStick;
            sticks[ sticks.length - 1 ] = base.sample();
            return (E)sticks[ sticks.length - 1 ];
        } else {
            pi = new double[]{ betaDistribution.sample().probability(0) };
            sticks = new Object[]{ base.sample() };
            return (E)sticks[0];
        }
    }

    public Object[] parameters() {
        return new Object[] { concentration, base };
    }
}
