/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author davidpfau
 */
public class PDIASequence implements Serializable, Iterator<Pair>, Iterable<Pair> {
    public RestaurantFranchise rf;
    public HashMap<Pair,Integer> trans;
    public int[][][] data; // First index is type of data, second is line, third is position in line
    private int line;
    private int pos;
    private Integer state;
    private static final long serialVersionUID = 1L;

    public PDIASequence(PDIA p, int init, int[][]... data) {
        rf = p.rf;
        trans = p.dMatrix;
        this.data = data;
        line = 0;
        pos  = 0;
        state = init;
    }

    public PDIASequence(PDIA p, int[][]... data) {
        rf = p.rf;
        trans = p.dMatrix;
        this.data = data;
        line = 0;
        pos  = 0;
        state = 0;
    }

    public Iterator<Pair> iterator() {
        return this;
    }

    public boolean hasNext() {
        return line < data[0].length - 1 || (line == data[0].length - 1 && pos < data[0][line].length);
    }

    public Pair next() {
        Pair p = new Pair(state,data[0][line][pos]);
        if (pos == data[0][line].length - 1) {
            pos = 0;
            line ++;
            state = 0;
        } else {
            pos ++;
            state = trans.get(p);
            if (state == null) {
                int[] context = {p.symbol};
                state = rf.generate(context);
                rf.seat(state, context);
                trans.put(p,state);
            }
        }
        return p;
    }

    public void remove() {}
}
