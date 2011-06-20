/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

import java.util.Set;

/**
 *
 * @author davidpfau
 */
public interface PDIA {

     /**
     * Given one or more arrays of data, returns an iterator over all
     * state/symbol pairs in that sequence
     * @param data An arbitrary number of arrays of lines of data
     * @return An iterator over state/symbol pairs
     */
    public PDIASequence run(int[][]... data);

    /**
     * Same as run(int[][]... data), but with a specific initial state
     * @param init Initial state for the first line of the data
     * @param data
     * @return
     */
    public PDIASequence run(int init, int[][]... data);
    //public int[][] generate(int[] init, int length);

    /**
     * Given a state/symbol pair, returns the next state, or null if that state
     * is not in the transition matrix
     * @param p
     * @return
     */
    public Integer transition(Pair p);

    /**
     * Given a state/symbol pair, returns the next state, and modifies the
     * transition matrix to fill in a new state if one is not there already.
     * @param p
     * @return
     */
    public Integer transitionAndAdd(Pair p);

    /**
     * Runs over all the data, filling the field cMatrix.
     * cMatrix - Hash map from states to array of symbols, giving the number
     * of times that symbol is emitted from that state.
     * @param data
     */
    public void count(int[][]... data);

    /**
     * Given training data, run one sweep of the MCMC sampler
     * @param data
     */
    public void sampleOnce(double temp, int[][]... data);

    /**
     * @return The joint log likelihood of the model and the data
     */
    public double jointScore();

    /**
     * @return The log likelihood of the data (for which cMatrix is a sufficient statistic)
     */
    public double logLik();

    /**
     * @param return The set of states which the data visits
     * Depends on the most recent int[][] data on which count(data) was called.
     */
    public Set<Integer> states();

    /**
     * Returns an array that gives the predictive probability of each data point,
     * given the training data and the previous data in the same array
     * @param init Initial state of the PDIA
     * @param data
     * @return The predictive probability of each element of data
     */
    public double[] score(int init, int[][]... data);

    /**
     * Checks for consistency between the fields rf and dMatrix
     */
    public void check();
}
