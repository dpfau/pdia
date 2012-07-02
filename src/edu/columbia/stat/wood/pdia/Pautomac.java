/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author davidpfau
 */
public class Pautomac {

    public static int[][] load(String filename) {
        File in = new File(filename);
        try {
            BufferedReader bis = new BufferedReader(new FileReader(filename));
            String header = bis.readLine();
            StringTokenizer st = new StringTokenizer(header);
            int nLines = Integer.parseInt(st.nextToken());
            int nSymbols = Integer.parseInt(st.nextToken());
            int[][] data = new int[nLines + 1][];
            data[0] = new int[]{nSymbols};
            String line = null;
            for (int i = 0; i < nLines; i++) {
                st = new StringTokenizer(bis.readLine());
                int nData = Integer.parseInt(st.nextToken());
                data[i + 1] = new int[nData];
                for (int j = 0; j < nData; j++) {
                    data[i + 1][j] = Integer.parseInt(st.nextToken());
                }
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int[][] clean(int[][] data) {
        // Clears zeros from data
        int nonzero = 0;
        for (int i = 1; i < data.length; i++) {
            if (data[i].length > 0) {
                nonzero++;
            }
        }
        int[][] cleanData = new int[nonzero][];
        int j = 0;
        for (int i = 1; i < data.length; i++) {
            if (data[i].length > 0) {
                cleanData[j++] = data[i];
            }
        }
        return cleanData;
    }

    public static int[][] stripFirstLine(int[][] data) {
        int[][] cleanData = new int[data.length-1][];
        for (int i = 1; i < data.length; i++) {
            cleanData[i-1] = data[i];
        }
        return cleanData;
    }

    public static double mean(int[][] data) {
        double sum = 0.0;
        for (int i = 1; i < data.length; i++) {
            sum += data[i].length;
        }
        return sum / data.length;
    }

    public static ArrayList<PDIA_Dirichlet> loadModels(String path) {
        File objs = new File(path);
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(objs)));
            Object o = null;
            ArrayList<PDIA_Dirichlet> pdias = new ArrayList();
            while ((o = ois.readObject()) != null) {
                pdias.add((PDIA_Dirichlet) o);
            }
            ois.close();
            return pdias;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String path, prefix;
        if ( args.length > 0 ) {
            path = "/hpc/stats/users/dbp2112/PAutomaC/";
            prefix = args[0];
        } else {
            path = "/Users/davidpfau/Documents/Distractions/Living/PAutomaC/";
            prefix = "real/1";
        }
        System.out.println("Loading " + path + "data/" + prefix + ".pautomac.train");
        int[][] train = Pautomac.load(path + "data/" + prefix + ".pautomac.train");
        System.out.println("Loading " + path + "data/" + prefix + ".pautomac.test");
        int[][] test = Pautomac.load(path + "data/" + prefix + ".pautomac.test");
        int nSymbols = train[0][0];
        double meanLen = Pautomac.mean(train);

        train = Pautomac.clean(train);
        test = Pautomac.stripFirstLine(test);

        PDIA_Dirichlet p = new PDIA_Dirichlet(nSymbols);
        // Some genius behind PAutomaC decided the artificial data would be indexed from zero but the real data would be indexed from one.
        boolean indexFromZero = true;
        for ( int i = 0; i < train.length; i++ ) {
            for ( int j = 0; j < train[i].length; j++ ) {
                if ( train[i][j] == nSymbols ) {
                    indexFromZero = false;
                }
            }
        }
        if ( !indexFromZero ) {
            for ( int i = 0; i < train.length; i++ ) {
                for ( int j = 0; j < train[i].length; j++ ) {
                    train[i][j] --;
                }
            }
            for ( int i = 0; i < test.length; i++ ) {
                for ( int j = 0; j < test[i].length; j++ ) {
                    test[i][j] --;
                }
            }
        }
        p.count(train);
        for( int i = 0; true; i++ ) {
            p.sampleOnce(1.0,train);
            System.out.println("Sample " + i);
            if ( i % 100 == 0 ) {
                PDIA_Dirichlet copy = Util.<PDIA_Dirichlet>copy(p);
                double[] lineScores = new double[test.length];
                double totalProb = 0;
                for ( int j = 0; j < test.length; j++ ) {
                    double[] charScore = copy.score(0, new int[][]{test[j]});
                    lineScores[j] = 1.0;
                    for ( int k = 0; k < test[j].length; k++ ) {
                        lineScores[j] *= charScore[k];
                    }
                    lineScores[j] *= Math.exp(-meanLen)*Math.pow(meanLen, test[j].length)/factorial(test[j].length); // Treat the length as sampled iid Poisson
                    totalProb += lineScores[j];
                }
                for ( int j = 0; j < test.length; j++ ) {
                    lineScores[j] /= totalProb;
                }
                try {
                    File f = new File( path + "/results/" + prefix + "." + i + ".test_score.2.txt" );
                    if( !f.exists() ) {
                        f.createNewFile();
                    }
                    System.out.println( "Writing result file: " + f.getName() );
                    BufferedWriter bw = new BufferedWriter( new FileWriter( f ) );
                    bw.write( Integer.toString(lineScores.length) + "\n" );
                    for( int j = 0; j < test.length; j++ ) {
                        bw.write( Double.toString(lineScores[j]) + "\n" );
                    }
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static double factorial(int n) {
        if ( n > 0 ) {
            double i = 1.0;
            for ( int t = 1; t < n; t++ ) {
                i *= t;
            }
            return i;
        } else {
            return 1;
        }
    }
}
