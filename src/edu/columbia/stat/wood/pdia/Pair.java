package edu.columbia.stat.wood.pdia;

import java.io.Serializable;

public class Pair implements Serializable {

    public int state;
    public int symbol;
    private static final long serialVersionUID = 1L;

    public Pair(int state, int symbol) {
        this.state = state;
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        return (((Pair) obj).state == this.state) && (((Pair) obj).symbol == this.symbol);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.state;
        hash = 29 * hash + this.symbol;
        return hash;
    }
}
