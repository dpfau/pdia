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
public class PDIASample implements Serializable, Iterable<PDIAInterface>, Iterator<PDIAInterface> {
    public PDIAInterface pdia;
    public int[][][] data;
    public double score;
    private static final long serialVersionUID = 1L;

    public Iterator<PDIAInterface> iterator() { return this; }

    public PDIASample(int nSymbols, int[][]... data) {
        pdia = new PDIA(nSymbols);
        this.data = data;
        pdia.count(data);
        score = pdia.jointScore();
    }

    public boolean hasNext() { return true; }

    public PDIAInterface next() {
        pdia.sampleOnce(data);
        score = pdia.jointScore();
        return pdia;
    }

    public void remove() {}
}
