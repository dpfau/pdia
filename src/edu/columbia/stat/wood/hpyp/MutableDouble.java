package edu.columbia.stat.wood.hpyp;

import java.io.Serializable;

public class MutableDouble implements Serializable {

    private double d = 0.0;
    private static final long serialVersionUID = 1L;

    public MutableDouble(double d) {
        this.d = d;
    }

    public double set(double newD) {
        return d = newD;
    }

    public double doubleVal() {
        return d;
    }

    public double times(double dd) {
        return d *= dd;
    }

    public void print() {
        System.out.println(d);
    }

    public static void main(String[] args) {
        MutableDouble md = new MutableDouble(1.0);
        md.print();

        md.set(30.0);
        md.print();
    }
}
