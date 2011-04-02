package edu.columbia.stat.wood.pdia;

import java.io.Serializable;
import java.util.Arrays;

/**
 * State/Symbol array pair.
 * @author davidpfau
 */
public class MultiPair implements Serializable, Pair {

    private int state;
    private int[] symbol;
    private static final long serialVersionUID = 1L;

    public MultiPair(int state, int... symbol) {
        this.state = state;
        this.symbol = symbol;
    }

    public int state() { return state; }

    public int[] symbol() { return symbol; }

    public int symbol(int i) { return symbol[i]; }

    public SinglePair toSingle() {
        return new SinglePair(state,symbol[0]);
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        MultiPair mp = (MultiPair) obj;
        return (mp.state == this.state) && Arrays.equals(mp.symbol, this.symbol);
    }

    @Override
    public int hashCode() {
        return this.state ^ Arrays.hashCode(this.symbol);
    }
}
