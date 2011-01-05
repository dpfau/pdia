/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.ArrayList;

/**
 *
 * @author davidpfau
 */
public class Table<Customer,Dish> {
    private Dish dish;
    private ArrayList<Customer> customers;

    public Table(Dish d) {
        customers = new ArrayList<Customer>();
        dish = d;
    }

    public int customers() { return customers.size(); }
    public Dish dish() { return dish; }
    public void add(Customer c) { customers.add(c); }
    public void remove(Customer c) { customers.remove(c); }
    public void set(Dish d) { dish = d; }
}
