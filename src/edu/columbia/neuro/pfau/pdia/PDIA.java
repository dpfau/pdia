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
        alpha = 1.0;
        alpha0 = 1.0;
        beta = 1.0;
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
        alpha = 1.0;
        alpha0 = 1.0;
        beta = 1.0;
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

    public void add(int symbol, int state, LinkedList<Table<Integer>> ts) {
        delta[symbol].put(state, ts.getFirst().dish());
        restaurants.get(symbol).seat(state, ts);
    }

    public LinkedList<Table<Integer>> remove(int symbol, int state) {
        delta[symbol].remove(state);
        return restaurants.get(symbol).unseat(state);
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

    public void sample() {
        suffStat s = new suffStat();
        for (int i = 0; i < numSymbols; i++) {
            s = sampleEntries(restaurants.get(i),s);
        }
        s = sampleEntries(top,s);
        for (int x = 0; x < 5; x++) {
            sampleAlpha();
            sampleAlpha0();
            sampleBeta(s.score, s.count);
        }
    }

    private suffStat sampleEntries(Restaurant r, suffStat s1) {
        Set cust = r.getCustomers();
        for (Object c : cust) {
            if (r.serving(c)) { // If the current entry hasn't been removed in the course of sampling other entries
                for (int i = 0; i < 5; i++) {
                    LinkedList<Table> ts = r.unseat(c);
                    r.seat(c);
                    fix();
                    suffStat s2 = new suffStat();
                    boolean acc = s2.score - s1.score > Math.log(Math.random());
                    ArrayList<Integer> toClear = new ArrayList<Integer>();
                    for (int j = 0; j < numSymbols; j++) {
                        for (Integer q : delta[j].keySet()) {
                            if (acc && !s2.count[j].containsKey(q) || !acc && !s1.count[j].containsKey(q)) {
                                toClear.add(q);
                            }
                        }
                        for (Integer q : toClear) {
                            remove(j, q);
                        }
                        toClear.clear();
                    }
                    if (acc) {
                        s1 = s2;
                    } else {
                        r.unseat(c);
                        r.seat(c, ts);
                        fix();
                    }
                }
            }
        }
        return s1;
    }

    private class suffStat {
        double score;
        HashMap<Integer,Integer>[] count;

        public suffStat() {
            count = trainCount();
            score = dataLogLikelihood(count);
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

    /*@Override
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
    }*/

    public void clear() {
        for (int i = 0; i < delta.length; i++) {
            for (int j : delta[i].keySet()) {
                LinkedList<Table<Integer>> ts = restaurants.get(i).unseat(j);
                assert ts != null : "Cleared customer that wasn't in the restaurant!";
            }
            delta[i].clear();
        }
    }


    // clear existing data structures and replace them with given transition matrix
    public void fix(HashMap<Integer,Integer>[] transition) {
        clear();
        for (int i = 0; i < transition.length; i++) {
            for (Integer j : transition[i].keySet()) {
                Integer k = transition[i].get(j);
                delta[i].put(j, k);
                restaurants.get(i).seat(j, k);
            }
        }
    }

    // replace elements of delta with the correct dishes
    public void fix() {
        for (int i = 0; i < numSymbols; i++) {
            Restaurant r = restaurants.get(i);
            for (Integer j : delta[i].keySet()) {
                delta[i].put(j, (Integer)(r.dish(j)));
            }
            for (Object o : r.getCustomers()) {
                if (!delta[i].containsKey((Integer)o)) {
                    System.out.println("meshuggah");
                }
            }
        }
    }
}

