/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

/**
 *
 * @author davidpfau
 */
public interface PDIA {
    public PDIASequence run(int[][]... data);
    public PDIASequence run(int init, int[][]... data);
    //public int[][] generate(int[] init, int length);
    public Integer transition(SinglePair p);
    public Integer transitionAndAdd(SinglePair p);
    public void count(int[][]... data);
    public void sampleOnce(int[][]... data);
    public double jointScore();
    public double logLik();
    public int states();
    public double[] score(int init, int[][]... data);
    public void check();
}
