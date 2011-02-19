/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author davidpfau
 */
public class PDIA implements Cloneable {

    private HashMap<Pair,Integer> delta;
    private double alpha;
    private double alpha0;
    private double beta;
    private int numSymbols;

    private ArrayList<Object> alphabet;
    private ArrayList<ArrayList<Integer>> trainingData;
    private ArrayList<ArrayList<Integer>> testingData;

    private ArrayList<Restaurant<Integer,Integer>> restaurants; // Maps a symbol in the alphabet to the corresponding restaurant
    private Restaurant<Table<Integer>,Integer> top;

    public PDIA() {
        delta = new HashMap<Pair,Integer>();
        alpha = 1.0;
        alpha0 = 1.0;
        beta = 1.0;
        numSymbols = 0;
        alphabet = new ArrayList<Object>();
        top = new Restaurant<Table<Integer>,Integer>(alpha0,0,new Geometric(0.001));

        trainingData = new ArrayList<ArrayList<Integer>>();
        testingData = new ArrayList<ArrayList<Integer>>();
        restaurants = new ArrayList<Restaurant<Integer,Integer>>();
    }

    public PDIA(ArrayList<ArrayList<Object>> data, int nTrain) {
        delta = new HashMap<Pair,Integer>();
        alpha = 1.0;
        alpha0 = 1.0;
        beta = 1.0;
        numSymbols = 0;
        alphabet = new ArrayList<Object>();
        top = new Restaurant<Table<Integer>,Integer>(alpha0,0,new Geometric(0.001));

        trainingData = new ArrayList<ArrayList<Integer>>();
        testingData = new ArrayList<ArrayList<Integer>>();
        restaurants = new ArrayList<Restaurant<Integer,Integer>>();
        for (int i = 0; i < data.size(); i++) {
            int state = 0;
            ArrayList<Integer> line = new ArrayList<Integer>();
            if (i < nTrain) {
                for (int j = 0; j < data.get(i).size(); j++) {
                    if (alphabet.contains(data.get(i).get(j))) {
                        line.add(alphabet.indexOf(data.get(i).get(j)));
                        state = next(state,alphabet.indexOf(data.get(i).get(j)));
                    } else {
                        numSymbols++;
                        alphabet.add(data.get(i).get(j));
                        restaurants.add(new Restaurant<Integer,Integer>(alpha,0,top));
                        line.add(alphabet.size()-1);
                        state = next(state,alphabet.size()-1);
                    }
                }
                trainingData.add(line);
            } else {
                for (int j = 0; j < data.get(i).size(); j++) {
                    line.add(alphabet.indexOf(data.get(i).get(j)));
                }
                testingData.add(line);
            }
        }
    }

    public int next(int state, int symbol) {
        Pair p = new Pair(state,symbol);
        if (delta.containsKey(p)) {
            return delta.get(p);
        } else {
            Restaurant r = restaurants.get(symbol);
            Integer dish = (Integer)r.seat(state);
            delta.put(p, dish);
            return dish;
        }
    }

    public double dataLogLikelihood(ArrayList<ArrayList<Integer>> data) {
        HashMap<Pair,Integer> counts = count(data);
        HashMap<Integer,Integer> stateCounts = stateCount(counts);
        double logLike = 0.0;
        for (Integer i : counts.values()) {
            logLike += Gamma.logGamma(i + beta/numSymbols) - Gamma.logGamma(beta/numSymbols);
        }
        for (Integer j : stateCounts.values()) {
            logLike -= Gamma.logGamma(j + beta) - Gamma.logGamma(beta);
        }
        return logLike;
    }

    public double trainingLogLikelihood() {
        return dataLogLikelihood(trainingData);
    }

    public double testingLogLikelihood() {
        return dataLogLikelihood(testingData);
    }

    public HashMap<Pair,Integer> count(ArrayList<ArrayList<Integer>> data) {
        HashMap<Pair,Integer> counts = new HashMap<Pair,Integer>();
        for (int i = 0; i < data.size(); i++) {
            int state = 0;
            for (int j = 0; j < data.get(i).size(); j ++) {
                Pair p = new Pair(state,data.get(i).get(j));
                if (counts.containsKey(p)) {
                    counts.put(p, counts.get(p) + 1);
                } else {
                    counts.put(p,1);
                }
                state = next(state, data.get(i).get(j));
            }
        }
        return counts;
    }

    public HashMap<Integer,Integer> stateCount(HashMap<Pair,Integer> counts) {
        HashMap<Integer,Integer> stateCounts = new HashMap<Integer,Integer>();
        for (Pair p : counts.keySet()) {
            int state = p.state();
            if (stateCounts.containsKey(state)) {
                stateCounts.put(state, stateCounts.get(state) + counts.get(p));
            } else {
                stateCounts.put(state, counts.get(p));
            }
        }
        return stateCounts;
    }

    public int numPairs() {
        return delta.size();
    }

    //number of states, counting the zero state
    public int numStates() {
        return top.dishes() + 1;
    }

    @Override
    public PDIA clone() {
        PDIA p = new PDIA();
        p.alpha     = this.alpha;
        p.alpha0    = this.alpha0;
        p.beta      = this.beta;
        p.alphabet  = this.alphabet;
        p.numSymbols = this.numSymbols;

        p.trainingData = this.trainingData;
        p.testingData  = this.testingData; // cloning ought not to matter since this isn't really mutable

        p.top = this.top.clone();
        HashMap<Table<Integer>,Table<Integer>> tableMap = p.top.cloneCustomers();

        p.restaurants = new ArrayList<Restaurant<Integer,Integer>>();
        for (Restaurant r : this.restaurants) {
            Restaurant<Integer,Integer> s = (Restaurant<Integer,Integer>)r.clone();
            s.swapTables(tableMap);
            s.setBaseDistribution(p.top);
            p.restaurants.add(s);
        }

        p.delta = (HashMap<Pair,Integer>)delta.clone();

        return p;
    }

    public void clear() {
        for (Pair p : delta.keySet()) {
            boolean b = restaurants.get(p.symbol()).unseat(p.state());
            assert b : "Cleared customer that wasn't in the restaurant!";
        }
        delta.clear();
    }

    private class Pair {
        private int state;
        private int symbol;

        public Pair(int a, int b) {
            this.state = a;
            this.symbol = b;
        }

        public int state() { return state; }
        public int symbol() { return symbol; }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) {
                return false;
            } else if ( o.getClass() != this.getClass() ) {
                return false;
            } else {
                Pair po = (Pair)o;
                return this.state == po.state() && this.symbol == po.symbol();
            }
        }

        @Override
        public int hashCode() {
            return (int)(Math.pow(2, state) * Math.pow(3, symbol));
        }
    }
}
