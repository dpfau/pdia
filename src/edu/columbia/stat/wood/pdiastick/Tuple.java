/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import java.util.Arrays;

/**
 *
 * @author davidpfau
 */
public class Tuple {
    private int[] tuple;

    public Tuple( int... x ) { tuple = x; }

    public int get( int i ) { return tuple[i]; }

    public int[] get() { return tuple; }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        return Arrays.equals( tuple, ( (Tuple) obj ).get() );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        int step = 29;
        for ( int i = 0; i < tuple.length; i++ ) {
            hash = step * hash + tuple[i];
            step += 8;
        }
        return hash;
    }
}
