/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import java.util.HashMap;

/**
 *
 * @author davidpfau
 */
public class PDIA {
    private HashMap<Integer,int[]> cMatrix;
    private HashMap<Tuple,Integer> dMatrix;
    private int nsymb;
    public double[] concentrations;
    public double[] discounts;

    public PDIA( int n ) {
        nsymb = n;
        dMatrix = new HashMap<Tuple,Integer>();
        concentrations = new double[]{ 10, 10 };
        discounts      = new double[]{ 0.1, 0.1 };
    }

    public void count( int[][] data ) {
        cMatrix = new HashMap<Integer,int[]>();
        for ( int i = 0; i < data.length; i++ ) {
            Integer state = 0;
            for ( int j = 0; j < data[i].length; j++ ) {
                int[] cts = cMatrix.get( state );
                if ( cts == null ) {
                    cts = new int[ nsymb ];
                    cMatrix.put( state, cts );
                }
                cts[ data[i][j] ] ++;

                state = dMatrix.get( new Tuple( state, data[i][j] ) );
                if ( state == null ) {
                    
                }
            }
        }
    }
}
