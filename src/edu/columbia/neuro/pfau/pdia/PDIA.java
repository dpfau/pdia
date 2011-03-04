/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.columbia.neuro.pfau.pdia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author davidpfau
 */
public class PDIA implements Cloneable {

    private HashMap<Integer,Integer>[] delta;
    private double beta;
    private int numSymbols;

    private ArrayList<Object> alphabet;
    private ArrayList<ArrayList<Integer>> trainingData;
    private ArrayList<ArrayList<Integer>> testingData;

    private ArrayList<Restaurant<Integer,Integer>> restaurants; // Maps a symbol in the alphabet to the corresponding restaurant
    private Restaurant<Table<Integer>,Integer> top;
    private static Random rnd = Restaurant.rnd;

    public PDIA(int nsym) {
        rnd.setSeed(1234); // for debugging only
        beta = 1.0;
        alphabet = new ArrayList<Object>();
        top = new Restaurant<Table<Integer>,Integer>(1,0.1,new Geometric(0.001));

        numSymbols = nsym;
        delta = new HashMap[nsym];
        restaurants = new ArrayList<Restaurant<Integer,Integer>>();
        for (int i = 0; i < nsym; i++) {
            delta[i] = new HashMap<Integer,Integer>();
            restaurants.add(new Restaurant<Integer,Integer>(1,0.1,top));
        }

        trainingData = new ArrayList<ArrayList<Integer>>();
        testingData = new ArrayList<ArrayList<Integer>>();
    }

    public PDIA(ArrayList<ArrayList<Object>> data, int nTrain, int nsym) {
        rnd.setSeed(1234); // for debugging only
        numSymbols = nsym;
        delta = new HashMap[nsym];
        for (int i = 0; i < nsym; i++) {
            delta[i] = new HashMap<Integer,Integer>();
        }        
        beta = 1.0;
        alphabet = new ArrayList<Object>();
        top = new Restaurant<Table<Integer>,Integer>(1,0.1,new Geometric(0.001));

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
                        restaurants.add(new Restaurant<Integer,Integer>(1,0.1,top));
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

    public double modelLogLikelihood() {
        double logLike = top.seatingLogLikelihood() - alpha0() - alpha();
        for (Restaurant r : restaurants) {
            logLike += r.seatingLogLikelihood();
        }
        return logLike;
    }

    public double jointLogLikelihood() {
        return trainingLogLikelihood() + modelLogLikelihood();
    }

    public double trainingLogLikelihood() {
        return dataLogLikelihood(trainCount());
    }

    // Note: remove added transition elements after calling this f'n
    public double testingLogLikelihood() {
        return dataLogLikelihood(testCount());
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

    // number of states, counting the zero state
    public int numStates() {
        return top.dishes() + 1;
    }

    // methods for accessing hyperparameters
    public double alpha() { return restaurants.get(0).concentration; }
    public double alpha0() { return top.concentration; }
    public double d() { return restaurants.get(0).discount; }
    public double d0() { return top.discount; }
    public double beta() { return beta; }

    public void sample() {
        suffStat s = new suffStat();
        for (int i = 0; i < numSymbols; i++) {
            s = sampleEntries(restaurants.get(i),s);
        }
        s = sampleEntries(top,s);
        HMCHyperparameters();
        for (int x = 0; x < 5; x++) {
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
                    boolean acc = s2.score - s1.score > Math.log(rnd.nextDouble());
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

    /**
     * Hamiltonian Monte Carlo sampling for the HPYP hyperparameters.
     * 
     */
    public void HMCHyperparameters() {
        double[] grad    = gradLogPosteriorHyperparameters();
        double[] params  = {alpha0(),d0(),alpha(),d()};
        double   logPost = modelLogLikelihood();
        double   e       = 0.005; // step size

        for (int l = 0; l < 5; l++) {
            double[] p = new double[4];
            double energy = logPost;
            for (int i = 0; i < p.length; i++) {
                p[i] = rnd.nextGaussian();
                energy += p[i]*p[i]/2;
            }
            
            double[] newParams = {alpha0(),d0(),alpha(),d()};
            double[] newGrad   = grad;
            for (int t = 0; t < 10; t++) { // run Hamiltonian dynamics
                for (int i = 0; i < p.length; i++) {
                    p[i] -= e*newGrad[i]/2;
                    newParams[i] += e*p[i];
                }
                if (newParams[0] < 0) { // Take care of reflections off the boundary
                    p[0] = -p[0];
                    newParams[0] = -newParams[0];
                }
                if (newParams[1] > 1 ) {
                    p[1] = -p[1];
                    newParams[1] = 2 - newParams[1];
                }
                if (newParams[1] < 0) {
                    p[1] = -p[1];
                    newParams[1] = -newParams[1];
                }
                if (newParams[2] < 0) {
                    p[2] = -p[2];
                    newParams[2] = -newParams[2];
                }
                if (newParams[3] > 1 ) {
                    p[3] = -p[3];
                    newParams[3] = 2 - newParams[3];
                }
                if (newParams[3] < 0) {
                    p[3] = -p[3];
                    newParams[3] = -newParams[3];
                }
                setHyperparameters(newParams);
                newGrad = gradLogPosteriorHyperparameters();
                for (int i = 0; i < p.length; i++) {
                    p[i] -= e*newGrad[i]/2;
                }
            }

            double newLogPost = modelLogLikelihood();
            double newEnergy  = newLogPost;
            for (int i = 0; i < p.length; i++) {
                newEnergy += p[i]*p[i]/2;
            }
            if (Math.log(rnd.nextDouble()) > newEnergy - energy) { // reject the sample
                setHyperparameters(params);
            } else {
                grad    = newGrad;
                logPost = newLogPost;
            }
        }
    }

    public void setHyperparameters(double[] params) {
        top.concentration = params[0];
        top.discount      = params[1];
        for (Restaurant r : restaurants) {
            r.concentration = params[2];
            r.discount      = params[3];
        }
    }

    /**
     * The gradient of the log posterior of the HPYP hyperparameters.
     * Used for Hamiltonian Monte Carlo
     * @return[0] - derivative wrt alpha0
     * @return[1] - derivative wrt d0
     * @return[2] - derivative wrt alpha
     * @return[3] - derivative wrt d
     */
    public double[] gradLogPosteriorHyperparameters() {
        double[] grad = new double[]{-1,0,-1,0}; // Initialize with the gradient of the log prior
        double[] subGrad = top.gradSeatingLogLikelihood();
        grad[0] += subGrad[0];
        grad[1] += subGrad[1];
        for (Restaurant r : restaurants) {
            subGrad = r.gradSeatingLogLikelihood();
            grad[2] += subGrad[0];
            grad[3] += subGrad[1];
        }
        return grad;
    }

    public void sampleBeta(double score, HashMap<Integer,Integer>[] counts) {
        double oldLikelihood = score - beta + Math.log(beta);
        double oldBeta = beta;
        beta = Math.exp(rnd.nextGaussian() + Math.log(beta));
        if (Math.log(rnd.nextDouble()) > dataLogLikelihood(counts) - beta + Math.log(beta) - oldLikelihood) {
            beta = oldBeta;
        }
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
                assert delta[i].containsKey((Integer)o) : "Mismatch between restaurants and delta";
            }
        }
    }
}

