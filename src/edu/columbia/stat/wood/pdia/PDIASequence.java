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
    public PDIAInterface pdia;
    public int[][][] data; // First index is type of data, second is line, third is position in line
    private int line;
    private int pos;
    private Integer state;
    private static final long serialVersionUID = 1L;

    public PDIASequence(PDIAInterface p, int init, int[][]... data) {
        pdia = p;
        this.data = data;
        line = 0;
        pos  = 0;
        state = init;
    }

    public Iterator<Pair> iterator() {
        return this;
    }

    public boolean hasNext() {
        return data.length != 0 && (line < data[0].length - 1 || (line == data[0].length - 1 && pos < data[0][line].length));
    }

    public Pair next() {
        Pair p = new Pair(state,data[0][line][pos]);
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
