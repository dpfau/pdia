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

    /**
     * Constructor for an empty PDIA
     * @param nsym The number of symbols in your (yet to be provided) data
     */
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

    /**
     * Constructor for a PDIA with data
     * @param data The training and testing data for this PDIA
     * @param nTrain The number of elements in the array data that are for training (the rest are for testing)
     * @param nsym The number of unique symbols in the data
     */
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

    /**
     * Gives a state of the automata and a symbol observed in that state, what is the next state.
     * This next state is deterministic if (state,symbol) has already been observed, probabilistic if not.
     * BE AWARE: If hasPair(state,symbol) is false, the PDIA will be modified by calling
     * this method.  To avoid modifying the PDIA, call hasPair(state,symbol) first to check.
     * @param state The current state
     * @param symbol The symbol observed in this state
     * @return The state that follows this one given the observed symbol
     */
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

    private void add(int symbol, int state, LinkedList<Table<Integer>> ts) {
        delta[symbol].put(state, ts.getFirst().dish());
        restaurants.get(symbol).seat(state, ts);
    }

    private LinkedList<Table<Integer>> remove(int symbol, int state) {
        delta[symbol].remove(state);
        return restaurants.get(symbol).unseat(state);
    }

    /**
     * Given the counts of how often a symbol is observed following a state,
     * returns the log likelihood of the sequence that generated those counts.
     * @param counts An array of counts.  Each index in the array corresponds to
     * one symbol.  Each key in the HashMap corresponds to one state.  Each
     * value corresponds to the number of times that that symbol is observed in
     * that state
     * @return Log likelihood of the sequence that generated the counts
     */
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

    /**
     * @return The log probability of the model, being the probability of the
     * hyperparameters and the seating arrangements in all the different restaurants.
     */
    public double modelLogLikelihood() {
        double logLike = top.seatingLogLikelihood() - alpha0() - alpha();
        for (Restaurant r : restaurants) {
            logLike += r.seatingLogLikelihood();
        }
        return logLike;
    }

    /**
     * @return The log probability of everything: the training data, the seating
     * arrangement of the restaurants, the hyperparameters.
     */
    public double jointLogLikelihood() {
        return trainingLogLikelihood() + modelLogLikelihood();
    }

    /**
     *
     * @return The log likelihood of the training data
     */
    public double trainingLogLikelihood() {
        return dataLogLikelihood(trainCount());
    }

    // Note: remove added transition elements after calling this f'n
    public double testingLogLikelihood() {
        return dataLogLikelihood(testCount());
    }

    /**
     * @return The number of tokens in the traning data
     */
    public int trainLen() {
        int n = 0;
        for (ArrayList a : trainingData) {
            n += a.size();
        }
        return n;
    }

    /**
     * @return The number of tokens in the testing data
     */
    public int testLen() {
        int n = 0;
        for (ArrayList a : testingData) {
            n += a.size();
        }
        return n;
    }

    /**
     * Given an array of data, returns the number of times each symbol is observed
     * in each state given the current transition structure of the data.
     * BE AWARE: if there are state/symbol pairs in the data not in the training
     * data, then the PDIA will be modified.  Use clean() afterward to remove
     * any state/symbol pairs not in the training data.
     * @param data The sequences to be counted.  Each entry of the ArrayList is
     * one sequence.  Each sequence starts in state 0.
     * @return The counts.  The array indexes symbols, the HashMap keys index
     * states, the values are the counts of the corresponding state/symbol pair.
     */
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

    /**
     * @return For each state/symbol pair, how many times is that pair observed
     * in the training data?
     */
    public HashMap<Integer,Integer>[] trainCount() {
        return count(trainingData);
    }

    /**
     * @return For each state/symbol pair, how many times is that pair observed
     * in the testing data?
     */
    public HashMap<Integer,Integer>[] testCount() {
        HashMap<Integer,Integer>[] counts = count(testingData);
        clean();
        return counts;
    }

    /**
     * @param counts The output of calling count(data)
     * @return Sums over the different symbols, returning the number of times
     * a state is visted by the data, regardless of what symbol it emits.
     */
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

    /**
     * @return The number of state/symbol pairs in the current transition matrix
     */
    public int numPairs() {
        int n = 0;
        for (int i = 0; i < delta.length; i++) {
            n += delta[i].size();
        }
        return n;
    }

    /**
     *
     * @param state
     * @param symbol
     * @return Is the given symbol observed in the given state anywhere in the
     * training data
     */
    public boolean hasPair(int state, int symbol) {
        return delta[symbol].get(state) != null;
    }

    /**
     * @return The number of states, counting the zero state
     */
    public int numStates() {
        return top.dishes() + 1;
    }

    /**
     * @return The concentration for the symbol-specific restaurants.
     */
    public double alpha() { return restaurants.get(0).concentration; }
    /**
     * @return The concentration for the top-level restaurant.
     */
    public double alpha0() { return top.concentration; }
    /**
     * @return The discount for the symbol-specific restaurants.
     */
    public double d() { return restaurants.get(0).discount; }
    /**
     * @return The discount for the top-level restaurant.
     */
    public double d0() { return top.discount; }
    /**
     * @return The number of psuedo-counts for the emissions distribution
     * for each state.
     */
    public double beta() { return beta; }

    /**
     * Runs one sweep of Metropolis-Hastings sampling over the entries of the
     * transition matrix for the PDIA
     */
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
                    if (s2.score - s1.score > Math.log(rnd.nextDouble())) { // accept
                        clean(s2.count);
                        s1 = s2;
                    } else { // reject
                        clean(s1.count);
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
            if (Math.log(rnd.nextDouble()) > newEnergy - energy || newParams[0] < 0 || newParams[1] < 0 || newParams[1] > 1 || newParams[2] < 0 || newParams[3] < 0 || newParams[3] > 1) { // reject the sample
                setHyperparameters(params);
            } else {
                grad    = newGrad;
                logPost = newLogPost;
            }
        }
    }

    private void setHyperparameters(double[] params) {
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

    /**
     * Samples the pseudo-counts for the emissions distribution, using already-
     * computed scores and counts to speed things up.
     * @param score
     * @param counts
     */
    public void sampleBeta(double score, HashMap<Integer,Integer>[] counts) {
        double oldLikelihood = score - beta + Math.log(beta);
        double oldBeta = beta;
        beta = Math.exp(rnd.nextGaussian() + Math.log(beta));
        if (Math.log(rnd.nextDouble()) > dataLogLikelihood(counts) - beta + Math.log(beta) - oldLikelihood) {
            beta = oldBeta;
        }
    }

    /**
     * Remove all state/symbol pairs, removing everything from the model but the
     * data and hyperparameters.
     */
    public void clear() {
        for (int i = 0; i < delta.length; i++) {
            for (int j : delta[i].keySet()) {
                LinkedList<Table<Integer>> ts = restaurants.get(i).unseat(j);
                assert ts != null : "Cleared customer that wasn't in the restaurant!";
            }
            delta[i].clear();
        }
    }

    /**
     * Removes state/symbol pairs not in the training data from the PDIA.
     * Useful after calling count() on data other than the training data.
     */
    public void clean() {
        clean(trainCount());
    }

    /**
     * Same as clean(), but with the count pre-computed for speed
     * @param counts
     */
    public void clean(HashMap<Integer,Integer>[] counts) {
        ArrayList<Integer> toClear = new ArrayList<Integer>();
        for (int i = 0; i < numSymbols; i++) {
            for (Integer j : delta[i].keySet()) {
                if (!counts[i].containsKey(j)) {
                    toClear.add(j);
                }
            }
            for (Integer j : toClear) {
                remove(i,j);
            }
            toClear.clear();
        }
    }

    /**
     * Given the transition matrix for an arbitrary PDFA, adds them in as the
     * initial state of the PDIA
     * @param transition
     */
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

    /**
     * To avoid conflicts between the data structure "delta" and the data
     * structure "restaurants", goes through
     */
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

