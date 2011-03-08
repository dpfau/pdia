/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.io.Serializable;

/**
 *
 * @author davidpfau
 */
public class Table<D> implements Serializable {
    private D dish;
    private int customers;

    public Table(D d) {
        customers = 0;
        dish = d;
    }

    public int customers() { return customers; }
    public D dish() { return dish; }
    public void add() { customers++; }
    public void remove() { if (customers > 0) {customers--;} }
    public void set(D d) { dish = d; }

    @Override
    public Table<D> clone() {
        Table<D> t = new Table<D>(dish);
        t.customers = this.customers;
        return t;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Table) {
            Table<D> t = (Table<D>)o;
            return t.dish() == dish && t.customers() == customers;
        } else {
            return false;
        }
    }

    /*@Override
    public int hashCode() {
        if (dish == null) {
            return 42 + 73*customers;
        } else {
            return 37*dish.hashCode() + 73*customers;
        }
    }*/
}
