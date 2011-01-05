/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

/**
 *
 * @author davidpfau
 */
public class Table<D> implements Cloneable {
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
}
