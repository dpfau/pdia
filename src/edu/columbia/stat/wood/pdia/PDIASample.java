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

    public PDIASample(int nSymbols, int[][]... data) {
        pdia = new PDIA(nSymbols);
        this.data = data;
        pdia.count(data);
        score = pdia.logLik();
    }

    public boolean hasNext() { return true; }

    public PDIA next() {
        pdia.sampleD(data);
        pdia.rf.sample();
        pdia.sampleBeta(1.0);
        score = pdia.jointScore();
        return pdia;
    }

    public void remove() {}
}
