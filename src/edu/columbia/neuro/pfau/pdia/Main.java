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
            PDIA[] ps = PDIA.sample(pdia,100,10,1000);
            System.out.println(PDIA.logLoss(ps, 10));

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}