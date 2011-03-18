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
    public PDIA p;
    public int[][][] data; // First index is type of data, second is line, third is position in line
    private int line;
    private int pos;
    private Integer state;
    private static final long serialVersionUID = 1L;

    public PDIASequence(PDIA p, int[][]... data) {
        this.p = p;
        this.data = data;
        line = 0;
        pos  = 0;
        state = 0;
    }

    @Override
    public Iterator<Pair> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return line < data[0].length - 1 || (line == data[0].length - 1 && pos < data[0][line].length);
    }

    @Override
    public Pair next() {
        Pair nxt = new Pair(state,data[0][line][pos]);
        if (pos == data[0][line].length - 1) {
            pos = 0;
            line ++;
            state = 0;
        } else {
            pos ++;
            state = p.dMatrix.get(nxt);
            if (state == null) {
                int[] context = {nxt.symbol};
                state = p.rf.generate(context);
                p.rf.seat(state, context);
                p.dMatrix.put(nxt,state);
            }
        }
        return nxt;
    }

    @Override
    public void remove() {}
}
