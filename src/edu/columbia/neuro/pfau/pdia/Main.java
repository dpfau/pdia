package edu.columbia.neuro.pfau.pdia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File(args[0])));
            ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();
            String line;
            while((line = br.readLine()) != null) {
                ArrayList<Object> foo = new ArrayList<Object>();
                for (Character c : line.toCharArray()) {
                    foo.add((Object)c);
                }
                data.add(foo);
            }
            PDIA pdia = new PDIA(data,100,27);
            pdia.sample(100,10,100,"/Users/davidpfau/Documents/Wood Group/PDIA/results/","2011.03.08");
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