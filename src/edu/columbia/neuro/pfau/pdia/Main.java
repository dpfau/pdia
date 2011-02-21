package edu.columbia.neuro.pfau.pdia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File("/Users/davidpfau/Documents/Wood Group/aiw/aiw_small_sent_clean")));
            ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
            String line;
            while((line = br.readLine()) != null) {
                ArrayList<Object> foo = new ArrayList<Object>();
                for (Character c : line.toCharArray()) {
                    foo.add((Object)c);
                }
                data.add(foo); //It's shit like this that makes me wish this were Python.
            }
            PDIA pdia = new PDIA(data,100);
            for (int i = 0; i < 100; i++) {
                System.out.println(pdia.trainingLogLikelihood() + ", " + pdia.numStates());
                pdia = PDIA.sample(pdia);
            }
            //PDIA pdia2 = pdia.clone();
            //pdia.clear();
            //pdia2.clear();
            System.out.println("OK!");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        /*ArrayList<Restaurant<Integer,Integer>> restaurants = new ArrayList<Restaurant<Integer,Integer>>(); // Maps a symbol in the alphabet to the corresponding restaurant
        Restaurant<Table<Integer>,Integer> top = new Restaurant<Table<Integer>,Integer>(1.0,0,new Geometric(0.001));
        restaurants.add(new Restaurant<Integer,Integer>(1.0,0,top));
        restaurants.add(new Restaurant<Integer,Integer>(1.0,0,top));
        for (int i = 0; i < 100; i++) {
            restaurants.get(0).seat(i);
            restaurants.get(1).seat(i);
        }
        for (int i = 0; i < 100; i++) {
            restaurants.get(0).unseat(i);
            restaurants.get(1).unseat(i);
        }
        System.out.println("golly");*/
    }
}