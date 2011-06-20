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
public class GammaDistribution implements Distribution<Double> {
    private double theta;
    private double k;

    public GammaDistribution( double shape, double scale ) {
        k = shape;
        theta = scale;
    }

    public double probability( Double x ) {
        if ( x > 0 ) {
            return Math.pow( x, k - 1 )
                * Math.exp( -x / theta )
                / Math.exp( Gamma.logGamma( k ) )
                / Math.pow( theta, k );
        } else {
            return 0;
        }
    }

    public double logProbability( Double x ) {
        if ( x > 0 ) {
            return ( k - 1 ) * Math.log( x )
                    + ( -x / theta )
                    - Gamma.logGamma( k )
                    - k * Math.log( theta );
        } else {
            return Double.NEGATIVE_INFINITY;
        }
    }

    public Double sample() {
        boolean accept = false;
        if (k < 1) {
            // Weibull algorithm
            double c = (1 / k);
            double d = ((1 - k) * Math.pow(k, (k / (1 - k))));
            double u, v, z, e, x;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                z = -Math.log(u);
                e = -Math.log(v);
                x = Math.pow(z, c);
                if ((z + e) >= (d + x)) {
                    accept = true;
                }
            } while (!accept);
            return (x * theta);
        } else {
            // Cheng's algorithm
            double b = (k - Math.log(4));
            double c = (k + Math.sqrt(2 * k - 1));
            double lam = Math.sqrt(2 * k - 1);
            double cheng = (1 + Math.log(4.5));
            double u, v, x, y, z, r;
            do {
                u = rng.nextDouble();
                v = rng.nextDouble();
                y = ((1 / lam) * Math.log(v / (1 - v)));
                x = (k * Math.exp(y));
                z = (u * v * v);
                r = (b + (c * y) - x);
                if ((r >= ((4.5 * z) - cheng))
                        || (r >= Math.log(z))) {
                    accept = true;
                }
            } while (!accept);
            return (x * theta);
        }
    }

    public double[] parameters() {
        return new double[] { theta, k };
    }
}
