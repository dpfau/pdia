package edu.columbia.neuro.pfau.pdia;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File(args[0]+"data/" + args[1] + ".dat")));
            ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
            String line;
            while((line = br.readLine()) != null) {
                ArrayList<Object> foo = new ArrayList<Object>();
                for (Character c : line.toCharArray()) {
                    foo.add((Object)c);
                }
                data.add(foo);
            }
            PDIA pdia = new PDIA(data,Integer.parseInt(args[3]),Integer.parseInt(args[4]));
            PDIA[] ps = new PDIA[1000];
            for (int i = 0; i < 1000; i++) {
                for (int j = 0; j < 10; j++) {
                    pdia.sample();
                }
                System.out.println("Iteration = " + i + " : " + "Single Machine Prediction = " + PDIA.logLoss(new PDIA[]{pdia}, 10));
                ps[i] = pdia.clone();
            }
            System.out.println("Multi Machine Prediction = " + PDIA.logLoss(ps, 10));

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}