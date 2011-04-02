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
public class PDIASequence implements Serializable, Iterator<Pair>, Iterable<Pair> {
    public PDIA pdia;
    public int[][][] data; // First index is type of data, second is line, third is position in line
    private int line;
    private int pos;
    private Integer state;
    private int[] current;
    public boolean multi; // use SinglePair or MultiPair?
    private static final long serialVersionUID = 1L;

    public PDIASequence(PDIA p, int init, int[][]... data) {
        pdia = p;
        this.data = data;
        line = 0;
        pos  = 0;
        state = init;
        multi = data.length > 1;
        if (multi) current = new int[data.length - 1];
    }

    public Iterator<Pair> iterator() {
        return this;
    }

    public boolean hasNext() {
        return data.length != 0 && (line < data[0].length - 1 || (line == data[0].length - 1 && pos < data[0][line].length));
    }

    public Pair next() {
        Pair p = null;
        if (multi) {
            int [] symbol = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                symbol[i] = data[i][line][pos];
            }
            p = new MultiPair(state,symbol);
        } else {
            p = new SinglePair(state,data[0][line][pos]);
        }
        if (pos == data[0][line].length - 1) {
            pos = 0;
            line++;
            state = 0;
        } else {
            pos++;
            state = pdia.transitionAndAdd(p);
        }
        return p;
    }

    public void remove() {}
}
