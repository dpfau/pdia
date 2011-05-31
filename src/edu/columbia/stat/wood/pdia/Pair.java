/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdia;

/**
 *
 * @author davidpfau
 */
public interface Pair {
    public int     state();
    public int[]   symbol();
    public int     symbol(int i);
}
