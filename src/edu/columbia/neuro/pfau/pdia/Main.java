/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 *
 * @author davidpfau
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String path1 = "/Users/davidpfau/Documents/Wood Group/aiw/aiw.train";
        String path2 = "/Users/davidpfau/Documents/Wood Group/aiw/aiw.test";

        String line = "";
        ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(path1));
            while ((line = in.readLine()) != null) {
                ArrayList<Object> chars = new ArrayList<Object>();
                for (int i = 0; i < line.length(); i++) {
                    chars.add(line.charAt(i));
                }
                data.add(chars);
            }
            in.close();
            int nTrain = data.size();

            in = new BufferedReader(new FileReader(path2));
            while ((line = in.readLine()) != null) {
                ArrayList<Object> chars = new ArrayList<Object>();
                for (int i = 0; i < line.length(); i++) {
                    chars.add(line.charAt(i));
                }
                data.add(chars);
            }
            in.close();

            PDIA pdia = new PDIA(data,nTrain);
            System.out.println(pdia.trainingLogLikelihood());
            System.out.println(pdia.numStates());
            PDIA clone = pdia.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Restaurant e = new Restaurant(1,0,new Geometric(0.001));
    }

}
