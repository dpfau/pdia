package edu.columbia.stat.wood.hpyp;

import java.io.Serializable;

public class Discounts implements Serializable {

    private MutableDouble[] discounts;
    private static final long serialVersionUID = 1L;

    public Discounts(double[] ds) {
        discounts = new MutableDouble[ds.length];

        for (int i = 0; i < ds.length; i++) {
            discounts[i] = new MutableDouble(ds[i]);
        }
    }

    public Discounts() {
        this(new double[]{0.5});
    }

    public MutableDouble get(int d) {
        if (d < discounts.length) {
            return discounts[d];
        }
        return discounts[(discounts.length - 1)];
    }

    public int length() {
        return discounts.length;
    }

    public void print() {
        System.out.print("[" + discounts[0].doubleVal());
        for (int i = 1; i < discounts.length; i++) {
            System.out.print(", " + discounts[i].doubleVal());
        }
        System.out.println("]");
    }
}
