package edu.columbia.stat.wood.pdia;

import java.io.Serializable;

/**
 * State/Symbol pair.  Distinct from MultiPair, which has multiple symbols
 * paired with a single state.
 * @author davidpfau
 */
public class SinglePair implements Serializable, Pair {

    private int state;
    private int symbol;
    private static final long serialVersionUID = 1L;

    public SinglePair(int state, int symbol) {
        this.state = state;
        this.symbol = symbol;
    }

    public int state() { return state; }

    public int[] symbol() { return new int[] {symbol}; }

    public int symbol(int i) { return symbol; }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        return (((SinglePair) obj).state == this.state) && (((SinglePair) obj).symbol == this.symbol);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.state;
        hash = 29 * hash + this.symbol;
        return hash;
    }
}
