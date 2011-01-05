/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * C - customer class, the space in which the data lives
 * D - dish class, the space in which the parameters live
 *
 * @author davidpfau
 */
public class Restaurant<C,D> extends Distribution<D> {
    private Distribution<D> base;

    private int customers;
    private int tables;

    private double concentration;
    private float discount;

    private HashMap<C,Table<C,D>> CustomerToTables;

    public Restaurant(double a, float d, Distribution<D> h) {
        concentration = a;
        discount = d;
        base = h;

        customers = 0;
        tables = 0;

        CustomerToTables = new HashMap<C,Table<C,D>>();
    }

    public Restaurant(double a, float d, Distribution<D> h, HashMap<C,Table<C,D>> t) {
        concentration = a;
        discount = d;
        base = h;

        customers = t.size();
        tables = t.values().size();

        CustomerToTables = t;
    }

    public D dish(C c) {
        return CustomerToTables.get(c).dish();
    }

    // return the number of unique dishes
    public int dishes() {
        int n = 0;
        HashSet<D> uniqueDishes = new HashSet<D>();
        for (Table<C,D> t : CustomerToTables.values()) {
            if (!uniqueDishes.contains(t.dish())) {
                n++;
                uniqueDishes.add(t.dish());
            }
        }
        return n;
    }

    private Table<C,D> sampleTable() {
        double cumSum = 0.0;
        HashMap<Double,Table<C,D>> sums = new HashMap<Double,Table<C,D>>();
        for (Table<C,D> t : CustomerToTables.values()) {
            cumSum += (double)(t.customers() - discount);
            sums.put(cumSum,t);
        }
        assert sums.size() == tables : "Number of Tables is Inconsistent!";
        cumSum += (double)(concentration + discount*tables);
        double samp = cumSum*rnd.nextDouble();
        double last = 0.0; // for debugging only
        for (Double d : sums.keySet()) {
            assert d >= last : "Iteration is not in order!";
            if (d > samp) {
                return sums.get(d);
            }
            last = d;
        }
        return null;
    }

    @Override
    public D sample() {
        Table<C,D> t = sampleTable();
        if (t == null) {
            return base.sample();
        } else {
            return t.dish();
        }
    }

    @Override
    public double probability (D i) {
        double prob = 0.0;
        for (Table<C,D> t : CustomerToTables.values()) {
            if (t.dish() == i) {
                prob += (t.customers() - discount)/(concentration + customers);
            }
        }
        return prob + (concentration + discount*tables)/(concentration + customers)*base.probability(i);
    }

    public D seat(C c) {
        Table<C,D> t = sampleTable();
        if (t == null) {
            tables++;
            if (base instanceof Restaurant) { // If this is an HPYP
                Restaurant higher = (Restaurant)base;
                t = new Table<C,D>(null);
                D s = (D)higher.seat(t);
                t.set(s);
            } else {
                t = new Table<C,D>(base.sample());
            }
        }
        CustomerToTables.put(c, t);
        t.add(c);
        customers++;
        return t.dish();
    }

    public void unseat(C c) {
        Table<C,D> t = CustomerToTables.remove(c);
        if (t != null) {
            customers--;
            t.remove(c);
            if (t.customers() == 0 && base instanceof Restaurant) {
                // garbage collection will take care of it unless it is part of an HPYP
                Restaurant higher = (Restaurant)base;
                higher.unseat(t);
            }
        }
    }
}
