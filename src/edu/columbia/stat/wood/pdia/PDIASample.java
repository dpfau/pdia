/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author davidpfau
 */
public class PDIASample implements Serializable, Iterable<PDIASample>, Iterator<PDIASample> {
    public PDIASequence seq;
    public HashMap<Integer,int[]> counts;
    public double score;
    public static final long serialVersionUID = 1L;

    @Override
    public Iterator<PDIASample> iterator() { return this; }

    @Override
    public boolean hasNext() { return true; }

    @Override
    public PDIASample next() {
        seq.pdia.sample();
        return this;
    }

    @Override
    public void remove() {}
}
