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
public class Geometric extends Distribution<Integer> implements Serializable {

    private double p; // the probability of success on any trial

    public Geometric(double p) {this.p = p;}

    @Override
    public Integer sample() {
        for (int i = 1; true; i++) {
            if (rnd.nextFloat() <= p) {
                return i;
            }
        }
    }

    @Override
    public double probability(Integer i) {
        return p*Math.pow(1 - p, i - 1);
    }
}
