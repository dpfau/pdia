/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.stat.wood.pdiastick;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author davidpfau
 */
public class Util {

    private static int[][] stirling = {{1}};
    private static final int NEWLINE = -1;
    
    public static double sum( double[] d ) {
        double x = 0;
        for ( int i = 0; i < d.length; i++ ) {
            x += d[i];
        }
        return x;
    }

    public static int sum( int[] d ) {
        int x = 0;
        for ( int i = 0; i < d.length; i++ ) {
            x += d[i];
        }
        return x;
    }

    // Deletes from index i inclusive to j exclusive
    public static int[] delete( int[] x, int i, int j ) {
        int[] y = new int[ x.length - ( j - i ) ];
        System.arraycopy( x, 0, y, 0, i );
        System.arraycopy( x, j, y, i, x.length - j );
        return y;
    }

    // Deletes from index i inclusive to j exclusive
    public static double[] delete( double[] x, int i, int j ) {
        double[] y = new double[ x.length - ( j - i ) ];
        System.arraycopy( x, 0, y, 0, i );
        System.arraycopy( x, j, y, i, x.length - j );
        return y;
    }

    public static int[][] delete( int[][] x, int i, int j ) {
        int[][] y = new int[ x.length - ( j - i ) ][];
        System.arraycopy( x, 0, y, 0, i );
        System.arraycopy( x, j, y, i, x.length - j );
        return y;
    }

    public static int[] append( int[] x, int... y ) {
        int[] z = new int[ x.length + y.length ];
        System.arraycopy( x, 0, z, 0, x.length );
        System.arraycopy( y, 0, z, x.length, y.length );
        return z;
    }

    public static int[][] append( int[][] x, int[]... y ) {
        int[][] z = new int[ x.length + y.length ][];
        System.arraycopy( x, 0, z, 0, x.length );
        System.arraycopy( y, 0, z, x.length, y.length );
        return z;
    }

    public static double[] append( double[] x, double... y ) {
        double[] z = new double[ x.length + y.length ];
        System.arraycopy( x, 0, z, 0, x.length );
        System.arraycopy( y, 0, z, x.length, y.length );
        return z;
    }

    // Delete some range of entries and insert a different array in its place
    public static int[] replace( int[] x, int i, int j, int... y ) {
        int[] z = new int[ x.length - ( j - i ) + y.length ];
        System.arraycopy( x, 0, z, 0, i );
        System.arraycopy( y, 0, z, i, y.length );
        System.arraycopy( x, j, z, i + y.length, x.length - j );
        return z;
    }

    // Delete some range of entries and insert a different array in its place
    public static double[] replace( double[] x, int i, int j, double... y ) {
        double[] z = new double[ x.length - ( j - i ) + y.length ];
        System.arraycopy( x, 0, z, 0, i );
        System.arraycopy( y, 0, z, i, y.length );
        System.arraycopy( x, j, z, i + y.length, x.length - j );
        return z;
    }

    public static int[][] copy( int[][] x ) {
        int[][] y = new int[ x.length ][];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = new int[ x[i].length ];
            System.arraycopy( x[i], 0, y[i], 0, x[i].length );
        }
        return y;
    }

    // Note to people who know more Java than me: is there a way to create an
    // "apply" function that applies a given function to each element of an
    // array?  Or would it be easier to just use Scala or Clojure if I wanted
    // functional programming constructs built on top of the JVM?
    public static double[] exp( double[] x ) {
        double[] y = new double[x.length];
        for ( int i = 0; i < x.length; i++ ) {
            y[i] = Math.exp(x[i]);
        }
        return y;
    }

    // Unsigned Stirling numbers of the first kind
    public static int stirling( int n, int k ) {
        if ( ( n > 0 && k == 0 ) || k > n ) return 0;
        if ( stirling.length <= n ) {
            int[][] newStirling = new int[n+1][n+1];
            for ( int i = 0; i <= n; i++ ) {
                if ( i < stirling.length ) {
                    System.arraycopy( stirling[i], 0, newStirling[i], 0, i+1 );
                } else {
                    for ( int j = 1; j <= i; j++ ) {
                        newStirling[i][j] = ( i - 1 ) * newStirling[i-1][j] + newStirling[i-1][j-1];
                    }
                }
            }
            stirling = newStirling;
        }
        return stirling[n][k];
    }

    public static Object[] randArray(Collection in) {
        int[] order = randPermute(in.size());
        Object[] randArray = new Object[in.size()];
        int i = 0;
        for (Object o : in) {
            randArray[order[i++]] = o;
        }
        return randArray;
    }

    public static int[] randPermute(int n) {
    	int[] order = new int[n];
    	order[0] = 0;
    	for (int i = 1; i < n; i++) {
    		int j = Distribution.rng.nextInt(i+1);
    		order[i] = order[j];
    		order[j] = i;
    	}
    	return order;
    }

    public static int[][] loadText(String path, HashMap<Integer,Integer> alphabet) throws FileNotFoundException, IOException {
        File in = new File(path);
        alphabet.put((int)'\n', NEWLINE); // assign newline a special value
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
        int[] symbols = new int[(int) in.length() + 1]; // +1 so we can add a newline at the end

        int ind = 0;
        int b;
        int len = 0;
        int numLines = 1;
        while ((b = bis.read()) > -1) {
            Integer c = alphabet.get(b);
            if (c != null) {
                symbols[(ind++)] = c;
                if (c == NEWLINE) len++;
            } else {
                symbols[(ind++)] = alphabet.size() - 1;
                alphabet.put(b, alphabet.size() - 1);
            }
            if (b == '\n') {
            	numLines++;
            }
        }

        symbols[symbols.length - 1] = NEWLINE;
        len++;

        assert len == symbols.length;

        int[][] data = new int[numLines][];
        int i = 0;
        int line = 0;
        for (int j = 0; j < symbols.length; j++) {
            if (symbols[j] == NEWLINE) {
                data[line] = new int[j - i];
                System.arraycopy(symbols, i, data[line], 0, j - i);
                i = j + 1;
                line++;
            }
        }
        if (bis != null) bis.close();
        return data;
    }

    public static int[][] loadText( String path ) throws FileNotFoundException, IOException {
        HashMap<Integer,Integer> alphabet = new HashMap<Integer,Integer>();
        return loadText( path, alphabet );
    }
}