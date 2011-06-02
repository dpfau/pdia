/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

/**
 *
 * @author davidpfau
 */
public class Util {
    public static double sum( double[] d ) {
        double x = 0;
        for ( int i = 0; i < d.length; i++ ) {
            x += d[i];
        }
        return x;
    }

    public static int sum( int[] d ) {
        int x = 0;
        for ( int i = 0; i < d.length; i++ ) {
            x += d[i];
        }
        return x;
    }
}
