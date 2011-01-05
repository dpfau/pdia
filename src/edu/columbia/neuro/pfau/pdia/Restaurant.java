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
public class Restaurant<C,D> extends Distribution<D> implements Cloneable {
    private Distribution<D> base;

    private int customers;
    private int tables;

    private double concentration;
    private float discount;

    private HashMap<C,Table<D>> customerToTables;

    public Restaurant(double a, float d, Distribution<D> h) {
        concentration = a;
        discount = d;
        base = h;

        customers = 0;
        tables = 0;

        customerToTables = new HashMap<C,Table<D>>();
    }

    public Restaurant(double a, float d, Distribution<D> h, HashMap<C,Table<D>> t) {
        concentration = a;
        discount = d;
        base = h;

        customers = t.size();
        tables = t.values().size();

        customerToTables = t;
    }

    public D dish(C c) {
        return customerToTables.get(c).dish();
    }

    // return the number of unique dishes
    public int dishes() {
        int n = 0;
        HashSet<D> uniqueDishes = new HashSet<D>();
        for (Table<D> t : customerToTables.values()) {
            if (!uniqueDishes.contains(t.dish())) {
                n++;
                uniqueDishes.add(t.dish());
            }
        }
        return n;
    }

    private Table<D> sampleTable() {
        double cumSum = 0.0;
        HashMap<Double,Table<D>> sums = new HashMap<Double,Table<D>>();
        for (Table<D> t : customerToTables.values()) {
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
        Table<D> t = sampleTable();
        if (t == null) {
            return base.sample();
        } else {
            return t.dish();
        }
    }

    @Override
    public double probability (D i) {
        double prob = 0.0;
        for (Table<D> t : customerToTables.values()) {
            if (t.dish() == i) {
                prob += (t.customers() - discount)/(concentration + customers);
            }
        }
        return prob + (concentration + discount*tables)/(concentration + customers)*base.probability(i);
    }

    public D seat(C c) {
        Table<D> t = sampleTable();
        if (t == null) {
            tables++;
            if (base instanceof Restaurant) { // If this is an HPYP
                Restaurant higher = (Restaurant)base;
                t = new Table<D>(null);
                D s = (D)higher.seat(t);
                t.set(s);
            } else {
                t = new Table<D>(base.sample());
            }
        }
        customerToTables.put(c, t);
        t.add();
        customers++;
        return t.dish();
    }

    public void unseat(C c) {
        Table<D> t = customerToTables.remove(c);
        if (t != null) {
            customers--;
            t.remove();
            if (t.customers() == 0 && base instanceof Restaurant) {
                // garbage collection will take care of it unless it is part of an HPYP
                Restaurant higher = (Restaurant)base;
                higher.unseat(t);
            }
        }
    }

    @Override
    public Restaurant<C,D> clone() {
        return new Restaurant<C,D>(concentration,discount,base,(HashMap<C,Table<D>>)customerToTables.clone());
    }

    // A method which replaces every key in customerToTables with a clone, only used for cloning an HPYP.
    // Returns a HashMap from the original customers to the clones
    public HashMap<C,C> cloneCustomers() {
        boolean begin = true;
        HashMap<C,C> map = new HashMap<C,C>();
        for (C c : customerToTables.keySet()) {
            if(begin) {
                if (c instanceof Table) { // Check the first key in the iterator to see if it's a type with public clone method
                    begin = false;
                } else {
                    return map;
                }
            }
            Table t = (Table)c;
            Table u = t.clone();
            Table<D> v = customerToTables.remove(c);
            customerToTables.put((C)u, v);
            map.put((C)t, (C)u);
        }
        return map;
    }

    // Also for cloning an HPYP, but for the tables of a low-level restaurant
    // rather than customers at high level restaurant.  Uses output of cloneCustomers()
    public void swapTables(HashMap<Table<D>,Table<D>> map) {
        for (C c : customerToTables.keySet()) {
            Table<D> t = customerToTables.remove(c);
            customerToTables.put(c, map.get(t));
        }
    }

    // Be careful with this one!  Again used only for PDIA clone method.
    public void setBaseDistribution(Distribution<D> d) {
        base = d;
    }
}
