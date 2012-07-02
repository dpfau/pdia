/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import java.util.HashSet;

/**
 *
 * @author davidpfau
 */
public class GenSym implements Distribution<Integer> {
    private HashSet<Integer> set;

    public GenSym() {
        set = new HashSet<Integer>();
    }

    public double probability( Integer i ) { return 0; }

    public double logProbability( Integer i ) { return Double.NEGATIVE_INFINITY; }

    public Integer sample() {
        int i = rng.nextInt(Integer.MAX_VALUE);
        while ( set.contains(i) ) {
            i = rng.nextInt(Integer.MAX_VALUE);
        }
        set.add(i);
        return i;
    }

    public Object parameters() { return null; }
}
