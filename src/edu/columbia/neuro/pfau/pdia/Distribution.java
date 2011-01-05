/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.Random;

/**
 *
 * @author davidpfau
 */
public abstract class Distribution<Space> {
    protected static Random rnd = new Random();
    public abstract Space sample();
    public abstract double probability(Space i);
}
