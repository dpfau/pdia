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
            pdia.sample(100,10,1000,args[0] + "results/" + args[1] + "." + args[2]);

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        ArrayList<PDIA> ps = PDIA.load(args[0] + "results/" + args[1] + "." + args[2]);
        System.out.println(PDIA.logLoss(ps.subList(50, 60), 10));
    }
}