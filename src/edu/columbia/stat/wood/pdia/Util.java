/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.Restaurant;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author davidpfau
 */
public class Util {

    public static <E> E copy(E orig) {
        E obj = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));

            obj = (E)in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }

    public static void write(Object o, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(o);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <E> Map<E,int[]> intArrayMapCopy(Map<E,int[]> map) {
        Map<E,int[]> copy = null;
        if (map instanceof HashMap<?,?>) {
            copy = new HashMap<E,int[]>();
        } else if (map instanceof TreeMap<?,?>) {
            copy = new TreeMap<E,int[]>();
        }
        for (E e : map.keySet()) {
            int[] cts = map.get(e);
            int[] cpy = new int[cts.length];
            System.arraycopy(cts,0,cpy,0,cts.length);
            copy.put(e,cpy);
        }
        return copy;
    }

    // OK, this is getting slightly ridiculous, but so it goes.
    public static <E> Map<E,int[][]> intTwoDArrayMapCopy(Map<E,int[][]> map) {
        Map<E,int[][]> copy = null;
        if (map instanceof HashMap<?,?>) {
            copy = new HashMap<E,int[][]>();
        } else if (map instanceof TreeMap<?,?>) {
            copy = new TreeMap<E,int[][]>();
        }
        for (E e : map.keySet()) {
            int[][] cts = map.get(e);
            int[][] cpy = new int[cts.length][];
            for (int i = 0; i < cts.length; i++) {
                if (cts[i] != null) {
                    cpy[i] = new int[cts[i].length];
                    System.arraycopy(cts[i],0,cpy[i],0,cts[i].length);
                }
            }
            copy.put(e,cpy);
        }
        return copy;
    }

    // JHH: updated this to be more efficient; it now runs in O(n) time
    public static int[] randPermute(int n) {
    	int[] order = new int[n];
    	order[0] = 0;
    	for (int i = 1; i < n; i++) {
    		int j = Restaurant.RNG.nextInt(i+1);
    		order[i] = order[j];
    		order[j] = i;
    	}
    	return order;
//        int[] order = new int[n];
//        LinkedList<Integer> ind = new LinkedList<Integer>();
//        for (int i = 0; i < n; i++) {
//            ind.addLast(i);
//        }
//        for (int i = 0; i < n; i++) {
//            order[i] = ind.remove(Restaurant.RNG.nextInt(n - i));
//        }
//        return order;
    }

    public static<T> T[] randArray(Collection<T> in) {
        int[] order = randPermute(in.size());
        T[] randArray = (T[])new Object[in.size()];
        int i = 0;
        for (T o : in) {
            randArray[order[i++]] = o;
        }
        return randArray;
    }

    public static int sum(int[] arr) {
        int s = 0;
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                s += arr[i];
            }
        }
        return s;
    }

    public static double sum(double[] arr) {
        double s = 0;
        if (arr != null) {
            for (int i = 0; i < arr.length; i++) {
                s += arr[i];
            }
        }
        return s;
    }

    public static <E> double sum(E arr) {
        double s = 0;
        if (arr != null) {
            if (arr instanceof int[]) {
                int[] a = (int[])arr;
                for (int i = 0; i < a.length; i++) {
                    s += a[i];
                }
            } else if (arr instanceof double[]) {
                double[] a = (double[])arr;
                for (int i = 0; i < a.length; i++) {
                    s += a[i];
                }
            } else if (arr instanceof Object[]) {
                Object[] a = (Object[])arr;
                for (int i = 0; i < a.length; i++) {
                    s += sum(a[i]);
                }
            }
        }
        return s;
    }

    public static double scoreToLogLoss(double[] score) {
        double logLoss = 0.0;
        for (double d : score) {
            logLoss += Math.log(d);
        }
        return -logLoss/score.length/Math.log(2.0);
    }

    public static void addArrays(double[] base, double[] other) {
        assert (base.length == other.length);
        for (int i = 0; i < base.length; i++) {
            base[i] += other[i];
        }
    }

    public static int totalLen(int[][] data) {
        int len = 0;
        for (int i = 0; i < data.length; i++) {
            len += data[i].length;
        }
        return len;
    }

	private static final int NEWLINE = -1;
	
	public static int[][] loadText(String path, HashMap<Integer,Integer> alphabet) throws FileNotFoundException, IOException {
		return loadText(path, alphabet, Integer.MAX_VALUE);
	}
		
	public static int[][] loadText(String path, HashMap<Integer,Integer> alphabet, int maxLen) throws FileNotFoundException, IOException {
        File in = new File(path);
        alphabet.put((int)'\n', NEWLINE); // assign newline a special value
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(in));
        int[] symbols = new int[(int) (Math.min(in.length(), maxLen) + 1)]; // +1 so we can add a newline at the end

        int ind = 0;
        int b;
        int len = 0;
        int numLines = 1;
        while ((b = bis.read()) > -1 && (ind + numLines - 1) < maxLen) {
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
//        if (symbols[symbols.length - 1] != NEWLINE) {
//            len ++;
//            int[] newSymbols = new int[symbols.length + 1];
//            System.arraycopy(symbols, 0, newSymbols, 0, symbols.length);
//            newSymbols[newSymbols.length - 1] = NEWLINE;
//            symbols = newSymbols;
//        } // if the file does not end with a newline

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

    /**
     * Assign an array into a larger ragged array
     * @param array ragged array you're building
     * @param i index into array
     * @param subarray this gets autoboxed to int[] from Matlab
     */
    public static void assignIntArray(Object[] array, int i, int[] subarray) {
        array[i] = subarray;
    }
    
    public static int[][] objectArrayTo2DIntArray(Object[] data) {
    	int[][] castData = new int[data.length][];
    	for (int i = 0; i < data.length; i++) {
    		castData[i] = (int[])data[i];
    	}
    	return castData;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
    	if(!destFile.exists()) {
    		destFile.createNewFile();
    	}

    	FileChannel source = null;
    	FileChannel destination = null;
    	try {
    		source = new FileInputStream(sourceFile).getChannel();
    		destination = new FileOutputStream(destFile).getChannel();
    		destination.transferFrom(source, 0, source.size());
    	}
    	finally {
    		if(source != null) {
    			source.close();
    		}
    		if(destination != null) {
    			destination.close();
    		}
    	}
    }

    
    public static PDIA[] loadPDIAs(String filename) {
    	PDIA[] pdias = null;
		ObjectInputStream oos = null;
		try {
			oos = new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(filename))));
			pdias = (PDIA[])oos.readObject();
		} catch (FileNotFoundException e) {
			System.err.println("Util.loadPDIAs: unable to read samples from disk: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Util.loadPDIAs: unable to read samples from disk: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Util.loadPDIAs: unable to read samples form disk: " + e.getMessage());
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					System.err.println("Util.loadPDIAs: error closing file: " + e.getMessage());
				}
			}
		}
		return pdias;
    	
    }
    
    public static double[][] score(String pdiaFile, String trainFile, String testFile) throws FileNotFoundException, IOException {
    	return score(pdiaFile, trainFile, Integer.MAX_VALUE, testFile);
    }
    
    
    public static double[][] score(String pdiaFile, String trainFile, int maxLen, String testFile) throws FileNotFoundException, IOException {
    	boolean hasTest = testFile != null;
    	HashMap<Integer,Integer> alphabet  = new HashMap<Integer,Integer>();
    	int[][] train = Util.loadText(trainFile, alphabet, maxLen);
    	int trainSize = Util.totalLen(train);
    	int[][] test = hasTest ? Util.loadText(testFile, alphabet) : null;
    	int testSize = hasTest ? Util.totalLen(test) : 0;
    	 
        PDIA_Dirichlet[] pdias = (PDIA_Dirichlet[])Util.loadPDIAs(pdiaFile);
        return score(pdias, train, trainSize, test, testSize);
        
    }
    
    
    public static double[][] score(PDIA_Dirichlet[] pdias, int[][] train, int trainSize, int test[][], int testSize) {
    	boolean hasTest = test != null;
        if (pdias == null) {
        	return new double[2][0];
        }
        int count = 0;
        for(PDIA pdia : pdias) {
        	if (pdia == null) break;
        	count++;
        }
        
        double[][] scores = new double[hasTest ? 2 : 1][count];
        double[] trainProbs = new double[trainSize];
        double[] testProbs = new double[testSize];
        
        for (int i = 0; i < count; i++) {
        	updateMean(trainProbs, pdias[i].meanScore(0, 10, train), i+1);
        	scores[0][i] = Util.scoreToLogLoss(trainProbs);
        	if (hasTest) {
        		updateMean(testProbs, pdias[i].meanScore(0, 10, test), i+1);
        		scores[1][i] = Util.scoreToLogLoss(testProbs);
        	}
        }
        
        return scores;
        
    }
    
    private static void updateMean(double[] mean, double[] newData, int n) {
    	for(int i = 0; i < mean.length; i++) {
    		mean[i] = (mean[i]*(n-1) + newData[i])/n;
    	}
    }
}
