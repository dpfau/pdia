/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import java.io.Serializable;
import java.util.Iterator;

/**
 *
 * @author davidpfau
 */
public class PDIASample implements Serializable, Iterable<PDIA>, Iterator<PDIA> {
    public PDIA pdia;
    public int[][][] data;
    public double score;
    private static final long serialVersionUID = 1L;
    private int n; // Number of samples you've run for
    private double annealRate;

    public Iterator<PDIA> iterator() { return this; }

    public PDIASample(int nSymbols, int[][]... data) {
        pdia = new PDIA_Dirichlet(nSymbols);
        this.data = data;
        pdia.count(data);
        score = pdia.jointScore();
        n = 1;
        annealRate = 100.0;
    }

    public void setAnnealRate(double d) { annealRate = d; }

    public PDIASample(PDIA p, int[][]... data) {
        pdia = p;
        this.data = data;
        pdia.count(data);
        score = pdia.jointScore();
        n = 1;
        annealRate = 100.0;
    }

    public boolean hasNext() { return true; }

    public PDIA next() {
        pdia.sampleOnce(Math.exp(-annealRate*n),data);
        score = pdia.jointScore();
        n++;
        return pdia;
    }

    public void remove() {}
}
