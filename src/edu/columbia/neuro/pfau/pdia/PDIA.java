/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author davidpfau
 */
public class PDIA implements Cloneable {

    private HashMap<Integer,Integer>[] delta;
    private double alpha;
    private double alpha0;
    private double beta;
    private int numSymbols;

    private ArrayList<Object> alphabet;
    private ArrayList<ArrayList<Integer>> trainingData;
    private ArrayList<ArrayList<Integer>> testingData;

    private ArrayList<Restaurant<Integer,Integer>> restaurants; // Maps a symbol in the alphabet to the corresponding restaurant
    private Restaurant<Table<Integer>,Integer> top;

    public PDIA(int nsym) {
        numSymbols = nsym;
        delta = new HashMap[nsym];
        for (int i = 0; i < nsym; i++) {
            delta[i] = new HashMap<Integer,Integer>();
        }
        alpha = 8.0;
        alpha0 = 20.0;
        beta = 6.0;
        alphabet = new ArrayList<Object>();
        top = new Restaurant<Table<Integer>,Integer>(alpha0,0,new Geometric(0.001));

        trainingData = new ArrayList<ArrayList<Integer>>();
        testingData = new ArrayList<ArrayList<Integer>>();
        restaurants = new ArrayList<Restaurant<Integer,Integer>>();
    }

    public PDIA(ArrayList<ArrayList<Object>> data, int nTrain, int nsym) {
        numSymbols = nsym;
        delta = new HashMap[nsym];
        for (int i = 0; i < nsym; i++) {
            delta[i] = new HashMap<Integer,Integer>();
        }        
        alpha = 8.0;
        alpha0 = 20.0;
        beta = 6.0;
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
        assert numSymbols == alphabet.size() : "Incorrect alphabet size!";
    }

    public int next(int state, int symbol) {
        Integer nxt = delta[symbol].get(state);
        if (nxt == null) {
            Restaurant r = restaurants.get(symbol);
            Integer dish = (Integer)r.seat(state);
            delta[symbol].put(state, dish);
            return dish;
        } else {
            return nxt;
        }
    }

    public double dataLogLikelihood(HashMap<Integer,Integer>[] counts) {
        HashMap<Integer,Integer> stateCounts = stateCount(counts);
        double logLike = 0.0;
        for (int symbol = 0; symbol < counts.length; symbol++) {
            for (Integer i : counts[symbol].values()) {
                logLike += Gamma.logGamma(i + beta/numSymbols) - Gamma.logGamma(beta/numSymbols);
            }
        }
        for (Integer j : stateCounts.values()) {
            logLike -= Gamma.logGamma(j + beta) - Gamma.logGamma(beta);
        }
        return logLike;
    }

    public double trainingLogLikelihood() {
        return dataLogLikelihood(count(trainingData));
    }

    public double testingLogLikelihood() {
        return dataLogLikelihood(count(testingData));
    }

    public int trainLen() {
        int n = 0;
        for (ArrayList a : trainingData) {
            n += a.size();
        }
        return n;
    }

    public int testLen() {
        int n = 0;
        for (ArrayList a : testingData) {
            n += a.size();
        }
        return n;
    }

    public HashMap<Integer,Integer>[] count(ArrayList<ArrayList<Integer>> data) {
        HashMap<Integer,Integer>[] counts = new HashMap[numSymbols];
        for (int i = 0; i < numSymbols; i++) {
            counts[i] = new HashMap<Integer,Integer>();
        }
        for (int i = 0; i < data.size(); i++) {
            int state = 0;
            for (int j = 0; j < data.get(i).size(); j ++) {
                int datum = data.get(i).get(j);
                Integer ct = counts[datum].get(state);
                if (ct == null) {
                    counts[datum].put(state, 1);
                } else {
                    counts[datum].put(state, ct + 1);
                }
                state = next(state, data.get(i).get(j));
            }
        }
        return counts;
    }

    public HashMap<Integer,Integer>[] trainCount() { // Hacky, but leads to important speedup because we don't need to repeat calls to count
        return count(trainingData);
    }

    public HashMap<Integer,Integer>[] testCount() {
        return count(testingData);
    }

    public HashMap<Integer,Integer> stateCount(HashMap<Integer,Integer>[] counts) {
        HashMap<Integer,Integer> stateCounts = new HashMap<Integer,Integer>();
        for (int i = 0; i < counts.length; i++) {
            for (int state : counts[i].keySet()) {
                Integer ct = stateCounts.get(state);
                if (ct == null) {
                    stateCounts.put(state, counts[i].get(state));
                } else {
                    stateCounts.put(state, ct + counts[i].get(state));
                }
            }
        }
        return stateCounts;
    }

    public int numPairs() {
        int n = 0;
        for (int i = 0; i < delta.length; i++) {
            n += delta[i].size();
        }
        return n;
    }

    //number of states, counting the zero state
    public int numStates() {
        return top.dishes() + 1;
    }

    public double alpha() {
        return alpha;
    }

    public double alpha0() {
        return alpha0;
    }

    public double beta() {
        return beta;
    }

    /*public static PDIA sample(PDIA p1) {
        HashMap<Integer,Integer>[] cts1 = p1.trainCount();
        double score1 = p1.dataLogLikelihood(cts1);
        for (int symbol = 0; symbol < p1.numSymbols; symbol++) {
            Set<Integer> states = p1.delta[symbol].keySet();
            for (Integer state : states) {
                for (int x = 0; x < 5; x++) { // loop this step x times
                    PDIA p2 = p1.clone();
                    p2.clear(symbol, state);
                    HashMap<Integer, Integer>[] cts2 = p2.trainCount();
                    double score2 = p2.dataLogLikelihood(cts2);
                    if (score2 - score1 > Math.log(Math.random())) {
                        for (int j = 0; j < p2.numSymbols; j++) {
                            ArrayList<Integer> empty = new ArrayList<Integer>();
                            for (Integer q : p2.delta[j].keySet()) {
                                if (!cts2[j].containsKey(q)) { // If the count for that state/symbol pair is zero
                                    empty.add(q);
                                }
                            }
                            for (Integer q : empty) { // Avoids concurrent modification problems
                                p2.clear(j, q);
                            }
                        }
                        p1 = p2;
                        cts1 = cts2;
                        score1 = score2;
                        System.gc();
                    }
                }
            }
        }
        for (int x = 0; x < 5; x++) {
            p1.sampleAlpha();
            p1.sampleAlpha0();
            p1.sampleBeta(score1, cts1);
        }
        return p1;
    }*/

    public void sample() {
        HashMap<Integer,Integer>[] cts1 = trainCount();
        double score1 = dataLogLikelihood(cts1);
        for (int i = 0; i < numSymbols; i++) {
            Set<Integer> states = ((HashMap<Integer,Integer>)delta[i].clone()).keySet();
            for (Integer p : states) {
                for (int x = 0; x < 5; x++) { // loop this step x times
                    LinkedList<Table<Integer>> ts = clear(i,p);
                    HashMap<Integer, Integer>[] cts2 = trainCount();
                    double score2 = dataLogLikelihood(cts2);
                    boolean acc = score2 - score1 > Math.log(Math.random()); // accept the new sample?
                    ArrayList<Integer> toClear = new ArrayList<Integer>();
                    for (int j = 0; j < numSymbols; j++) {
                        for (Integer q : delta[j].keySet()) {
                            if (acc && !cts2[j].containsKey(q) || !acc && !cts1[j].containsKey(q)) { // Clear out the unused state/symbol pairs of whichever machine is accepted
                                toClear.add(q);
                            }
                        }
                        for (Integer q : toClear) {
                            clear(j,q);
                        }
                    }
                    if (acc) {
                        cts1 = cts2;
                        score1 = score2;
                    } else {
                        restaurants.get(i).seat(p,ts);
                    }
                }
            }
        }
        for (int x = 0; x < 5; x++) {
            sampleAlpha();
            sampleAlpha0();
            sampleBeta(score1, cts1);
        }
    }

    public void sampleAlpha() {
        double alphaNew = 0.5*top.rnd.nextGaussian() + alpha;
        int k = 0;
        int n = 0;
        for (Restaurant r : restaurants) {
            k += r.tables();
            n += r.customers();
        }
        if (alphaNew > 0) {
            double oldLikelihood = k*Math.log(alpha) + Gamma.logGamma(alpha) - Gamma.logGamma(alpha + n) - alpha;
            double newLikelihood = k*Math.log(alphaNew) + Gamma.logGamma(alphaNew) - Gamma.logGamma(alphaNew + n) - alphaNew;
            if (Math.log(Math.random()) < newLikelihood - oldLikelihood) {
                alpha = alphaNew;
            }
        }
    }

    public void sampleAlpha0() { // This seems to be written very differently in the original Python implementation than any way I remember doing it.  Try both ways?
        double alpha0New = 0.5*top.rnd.nextGaussian() + alpha0;
        if (alpha0New > 0) {
            double oldLikelihood = top.tables()*Math.log(alpha0) + Gamma.logGamma(alpha0) - Gamma.logGamma(alpha0 + top.customers()) - alpha0;
            double newLikelihood = top.tables()*Math.log(alpha0New) + Gamma.logGamma(alpha0New) - Gamma.logGamma(alpha0New + top.customers()) - alpha0New;
            if (Math.log(Math.random()) < newLikelihood - oldLikelihood) {
                alpha0 = alpha0New;
            }
        }
    }

    public void sampleBeta(double score, HashMap<Integer,Integer>[] counts) {
        double oldLikelihood = score - beta;
        double oldBeta = beta;
        beta += 0.5*top.rnd.nextGaussian();
        if (beta > 0) {
            if (Math.log(Math.random()) > dataLogLikelihood(counts) - beta - oldLikelihood) {
                beta = oldBeta;
            }
        } else {
            beta = oldBeta;
        }
    }

    @Override
    public PDIA clone() {
        PDIA p = new PDIA(numSymbols);
        p.alpha     = this.alpha;
        p.alpha0    = this.alpha0;
        p.beta      = this.beta;
        p.alphabet  = this.alphabet;

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

        p.delta = new HashMap[numSymbols];
        for (int i = 0; i < numSymbols; i++) {
            p.delta[i] = (HashMap<Integer,Integer>)delta[i].clone();
        }

        return p;
    }

    public void clear() {
        for (int i = 0; i < delta.length; i++) {
            for (int j : delta[i].keySet()) {
                LinkedList<Table<Integer>> ts = restaurants.get(i).unseat(j);
                assert ts != null : "Cleared customer that wasn't in the restaurant!";
            }
            delta[i].clear();
        }
    }

    public LinkedList<Table<Integer>> clear(int symbol, int state) {
        delta[symbol].remove(state);
        return restaurants.get(symbol).unseat(state);
    }
}

