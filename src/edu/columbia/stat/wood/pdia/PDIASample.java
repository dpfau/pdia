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
    public PDIA pdia;
    public HashMap<Integer,int[]> counts;
    public double score;
    public static final long serialVersionUID = 1L;

    public Iterator<PDIASample> iterator() { return this; }

    public boolean hasNext() { return true; }

    public PDIASample next() {
        pdia.sample();
        score = pdia.jointScore();
        return this;
    }

    public void remove() {}
}
