package edu.columbia.stat.wood.hpyp;

import java.io.Serializable;

public class Concentrations implements Serializable {

    private MutableDouble[] concentrations;
    private static final long serialVersionUID = 1L;

    public Concentrations(double[] cs) {
        concentrations = new MutableDouble[cs.length];
        for (int i = 0; i < cs.length; i++) {
            concentrations[i] = new MutableDouble(cs[i]);
        }
    }

    public Concentrations() {
        this(new double[]{0.001});
    }

    public MutableDouble get(int d) {
        if (d < concentrations.length) {
            return concentrations[d];
        }
        return concentrations[(concentrations.length - 1)];
    }

    public double[] get() {
        double[] d = new double[concentrations.length];
        for (int i = 0; i < concentrations.length; i++) {
            d[i] = concentrations[i].doubleVal();
        }
        return d;
    }

    public int length() {
        return concentrations.length;
    }

    public void print() {
        System.out.print("[" + concentrations[0].doubleVal());
        for (int i = 1; i < concentrations.length; i++) {
            System.out.print(", " + concentrations[i].doubleVal());
        }
        System.out.println("]");
    }
}
