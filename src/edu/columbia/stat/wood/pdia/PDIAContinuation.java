/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Generates a sequence from a given PDIA, with either a fixed or unbounded length
 * @author davidpfau
 */
public class PDIAContinuation implements Serializable, Iterable<Pair>, Iterator<Pair> {
    public PDIA pdia;
    private Integer state;
    private int length;
    double betaOverNSymbols;
    private static final long serialVersionUID = 1L;

    /**
     * Initialize a new PDIA continuation with fixed length
     * @param p
     * @param init The initial state of the continuation
     * @param len The length of the continuation
     */
    public PDIAContinuation(PDIA p, int init, int len) {
        pdia = p;
        state = init;
        length = len;
        betaOverNSymbols = pdia.beta/pdia.nSymbols;
    }

    /**
     * Initialize a new PDIA continuation with unbounded length
     * @param p
     * @param init The initial state of the continuation
     */
    public PDIAContinuation(PDIA p, int init) {
        pdia = p;
        state = init;
        length = -1;
        betaOverNSymbols = pdia.beta/pdia.nSymbols;
    }

    public Pair next() {
        int symbol = pdia.nSymbols;
        int[] cts = pdia.cMatrix.get(state);
        if (cts == null) {
            cts = new int[pdia.nSymbols];
            pdia.cMatrix.put(state, cts);
            symbol = PDIA.RNG.nextInt(pdia.nSymbols); // sample new symbol uniformly for unobserved state
        } else {
            double[] cuSum = new double[pdia.nSymbols];
            cuSum[0] = cts[0] + betaOverNSymbols;
            for (int i = 1; i < pdia.nSymbols; i++) {
                cuSum[i] = cuSum[i-1] + cts[i] + betaOverNSymbols;
            }
            double samp = cuSum[pdia.nSymbols-1] * PDIA.RNG.nextDouble();
            for (int i = 0; i < pdia.nSymbols; i++) {
                if (cuSum[i] >= samp) symbol = i;
            }
        }

        cts[symbol] ++;
        Pair p = new Pair(state,symbol);
        state = pdia.transition(state, symbol);
        if (state == null) {
            int[] context = {symbol};
            state = pdia.rf.generate(context);
            pdia.rf.seat(state, context);
            pdia.dMatrix.put(p, state);
        }
        if (length != -1) length--;
        return p;
    }

    public Iterator<Pair> iterator() { return this; }
    public boolean hasNext() { return length == -1 || length > 0; }
    public void remove() {}
}
