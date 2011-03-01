/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
        HashSet tt = new HashSet(t.values());
        tables = tt.size();

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

    public int customers() {
        if (customers == customerToTables.size()) {
            return customers;
        } else {
            return -1;
        }
    }

    public int tables() {
        return tables;
    }

    // Returns collection of customers
    public Set<C> getCustomers() {
        return ((HashMap<C,Table<D>>)customerToTables.clone()).keySet();
    }

    // Returns array list of *unique* tables, no duplicates
    public ArrayList<Table<D>> getTables() {
        return new ArrayList<Table<D>>(new HashSet<Table<D>>(customerToTables.values()));
    }

    public boolean serving(C c) {
        return customerToTables.containsKey(c);
    }

    private Table<D> sampleTable(D d) {
        ArrayList<Table<D>> uniqueTables = getTables();
        ArrayList<Table<D>> tablesServingD = new ArrayList<Table<D>>();
        for (Table<D> t : uniqueTables) {
            if ( t.dish() == d ) {
                tablesServingD.add(t);
            }
        }
        return crp(tablesServingD);
    }

    private Table<D> crp(ArrayList<Table<D>> uniqueTables) {
        double cumSum = 0.0;
        int nTables = uniqueTables.size();
        double[] sums = new double[nTables];
        for (int i = 0; i < nTables; i++) {
            assert uniqueTables.get(i) != null : "Have null table that should not be there";
            cumSum += (double)(uniqueTables.get(i).customers() - discount);
            sums[i] = cumSum;
        }
        cumSum += (double)(concentration + discount*tables);
        // NOTICE! We use tables above, rather than nTables, because this is
        // used for conditional sampling from the given tables, rather than generic
        // sampling from some arbitrary CRP.  The input uniqueTables is always
        // some subset of the value set for customersToTables
        double samp = cumSum*rnd.nextDouble();
        for (int i = 0; i < nTables; i++) {
            if (sums[i] > samp) {
                return uniqueTables.get(i);
            }
        }
        return null;
    }

    @Override
    public D sample() {
        Table<D> t = crp(getTables());
        if (t == null) {
            return base.sample();
        } else {
            return t.dish();
        }
    }

    @Override
    public double probability (D d) {
        double prob = 0.0;
        for (Table<D> t : customerToTables.values()) {
            if (t.dish() == d) {
                prob += (t.customers() - discount)/(concentration + customers);
            }
        }
        return prob + (concentration + discount*tables)/(concentration + customers)*base.probability(d);
    }

    public D seat(C c) {
        Table<D> t = crp(getTables());
        if (t == null) {
            tables++;
            if (base instanceof Restaurant) { // If this is an HPYP
                t = new Table<D>(null);
                D s = (D)((Restaurant)base).seat(t);
                t.set(s);
            } else {
                t = new Table<D>(base.sample());
            }
        }
        put(c,t);
        return t.dish();
    }

    // Only seats at tables serving the specified dish
    public void seat(C c, D d) {
        Table<D> t = sampleTable(d);
        if (t == null) {
            tables++;
            if (base instanceof Restaurant) { // If this is an HPYP
                t = new Table<D>(d);
                ((Restaurant)base).seat(t,d);
            } else {
                t = new Table<D>(d);
            }
        }
        put(c,t);
    }

    // For putting an observation back in after rejecting MH step
    public void seat(C c, LinkedList<Table<D>> ts) {
        Table<D> t = ts.remove();
        if (!customerToTables.containsValue(t)) {
            tables++;
            if (!ts.isEmpty()) { // Should only be true if next level up is also Restaurant
                ((Restaurant)base).seat(t,ts);
            }
        }
        put(c,t);
    }

    public LinkedList<Table<D>> unseat(C c) {
        assert customers == customerToTables.size() : "Alert 1!";
        Table<D> t = customerToTables.remove(c);
        if (t != null) {
            customers--;
            assert customers == customerToTables.size() : "Alert 2!";
            t.remove();
            LinkedList<Table<D>> ts = new LinkedList<Table<D>>();
            ts.add(t);
            if (t.customers() == 0) {
                tables--;
                if (base instanceof Restaurant) {
                    // garbage collection will take care of it unless it is part of an HPYP
                    LinkedList<Table<D>> tt = ((Restaurant)base).unseat(t);
                    assert tt != null : "Table not found in top-level restaurant!";
                    ts.addAll(tt);
                }
            }
            return ts;
        } else {
            return null;
        }
    }

    // Avoids code duplication for the different "seat" methods
    private void put(C c, Table<D> t) {
        assert !customerToTables.containsKey(c) : "trying to add customer that's already there";
        customerToTables.put(c,t);
        t.add();
        customers++;
        if (c instanceof Table) {
            ((Table)c).set(t.dish());
        }
    }
}
