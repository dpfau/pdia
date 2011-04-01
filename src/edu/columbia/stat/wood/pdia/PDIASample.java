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

    public Iterator<PDIA> iterator() { return this; }

    public PDIASample(int nSymbols, int[][]... data) {
        pdia = new PDIA_Dirichlet(nSymbols);
        this.data = data;
        pdia.count(data);
        score = pdia.jointScore();
    }

    public PDIASample(PDIA_Dirichlet p, int[][]... data) {
        pdia = p;
        this.data = data;
        pdia.count(data);
        score = pdia.jointScore();
    }

    public boolean hasNext() { return true; }

    public PDIA next() {
        pdia.sampleOnce(data);
        score = pdia.jointScore();
        return pdia;
    }

    public void remove() {}
}
