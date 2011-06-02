/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import java.util.Calendar;
import java.util.Random;

/**
 *
 * @author davidpfau
 */
public interface Distribution<E> {
    public static Random rng = new Random(
            Calendar.getInstance().getTimeInMillis()
            + Thread.currentThread().getId());
    public double probability(E e);
    public double logProbability(E e);
    public E sample();
    public Object parameters();
}
