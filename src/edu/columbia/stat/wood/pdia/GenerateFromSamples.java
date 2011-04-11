package edu.columbia.stat.wood.pdia;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author davidpfau
 */
public class GenerateFromSamples {

    public static void main(String[] args) {
        String path = "/Users/davidpfau/Documents/Wood Group/PDIA/";
        File objs = new File(path + "results/aiw_full_samples.gz");
        PDIA_Dirichlet[] ps = null;
        int[][] train = null;
        int[][] test = null;
        HashMap<Integer, Integer> alphabet = new HashMap<Integer, Integer>();
        try {
            train = Util.loadText(path + "data/aiw_full.train", alphabet);
            test = Util.loadText(path + "data/aiw_full.test", alphabet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<Integer,Integer> alphInv = new HashMap<Integer, Integer>();
        for (Integer i : alphabet.keySet()) {
            alphInv.put(alphabet.get(i),i);
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(objs)));
            ps = (PDIA_Dirichlet[])ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[][] gen = new int[100][100];
        for (int i = 0; i < 200; i++) {
            System.out.println(ps[i].states());
        }
        for (int i = 0; i < 100; i++) {
            ps[199].nSymbols = 27;
            gen[i] = ps[199].generate(0, 100);
        }
        String generate = "";
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                Integer c = alphInv.get(gen[i][j]);
                generate += (char)(int)c;
            }
            generate += '\n';
        }
        System.out.println(generate);
    }
}
