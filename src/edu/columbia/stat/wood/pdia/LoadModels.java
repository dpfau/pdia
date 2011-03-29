/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author davidpfau
 */
public class LoadModels {

    public static void main(String[] args) {
        String path = "/Users/davidpfau/Documents/Wood Group/PDIA/";
        File objs = new File(path + "results/objectsFromPDIA_hpy.txt.gz");
        PDIA[] ps = new PDIA[3500];
        int[][] train = null;
        int[][] test = null;
        try {
            HashMap<Integer, Integer> alphabet = new HashMap<Integer, Integer>();
            train = Util.loadText(path + "data/aiw.train", alphabet);
            test = Util.loadText(path + "data/aiw.test", alphabet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int i = 0;
        int j = 0;
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(objs)));
            Object o = null;
            Double beta = 0.0;
            HashMap<Pair,Integer> dMatrix = null;
            RestaurantFranchise rf = null;
            while ((o = ois.readObject()) != null) {
                if (i % 3 == 0) beta = (Double) o;
                if (i % 3 == 1) dMatrix = (HashMap<Pair, Integer>) o;
                if (i % 3 == 2) {
                    rf = (RestaurantFranchise) o;
                    ps[j] = new PDIA(27);
                    ps[j].rf = rf;
                    ps[j].beta = beta;
                    ps[j].dMatrix = dMatrix;
                    ps[j].count(train);
                    j++;
                }
                i++;
            }
        } catch (EOFException e) {
            System.out.println(j);
            PDIA[] ps2 = new PDIA[j];
            System.arraycopy(ps,0,ps2,0,j);
            System.out.println(Util.scoreToLogLoss(PDIA.score(ps2, 0, test)));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
