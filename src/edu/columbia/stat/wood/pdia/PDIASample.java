/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import java.util.Iterator;

/**
 *
 * @author davidpfau
 */
public class PDIASample implements Iterable<PDIA>, Iterator<PDIA> {
    public PDIA pdia;
    public int[][][] data;
    public double score;

    public Iterator<PDIA> iterator() { return this; }

    public PDIASample(PDIA p, int[][]... data) {
        pdia = p;
        this.data = data;
        pdia.count(data);
        score = pdia.logLik();
    }

    public boolean hasNext() { return true; }

    public PDIA next() {
        pdia.sample();
        score = pdia.jointScore();
        return pdia;
    }

    public void remove() {}

    public void sample() {
        pdia.sampleD();
        pdia.rf.sample();
        pdia.sampleBeta(1.0);
    }
}
