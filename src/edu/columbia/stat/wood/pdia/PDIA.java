package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.MutableInteger;
import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.math.special.Gamma;

public class PDIA implements Serializable {

    public RestaurantFranchise rf;
    public HashMap<Pair, Integer> dMatrix;
    public HashMap<Integer, int[]> cMatrix;
    public int nSymbols;
    public double beta;
    public double logLike;
    public static Random RNG = new Random(0L);
    public static final long serialVersionUID = 1L;

    public PDIA(int n) {
        rf = new RestaurantFranchise(1);
        nSymbols = n;
        dMatrix = new HashMap<Pair, Integer>();
        beta = 10.0;
    }

    /**
     * Given one or more arrays of data, returns an iterator over all
     * state/symbol pairs in that sequence
     * @param data An arbitrary number of arrays of lines of data
     * @return An iterator over state/symbol pairs
     */
    public PDIASequence run(int[][]... data) {
        return new PDIASequence(this,0,data);
    }

    /**
     * Same as run(int[][]... data), but with a specific initial state
     * @param init Initial state for the first line of the data
     * @param data
     * @return
     */
    public PDIASequence run(int init, int[][]... data) {
        return new PDIASequence(this,init,data);
    }

    /**
     * Given data, returns an iterator over PDIA that forms an MCMC sampler
     * @param nSymbols The size of the alphabet for each data type
     * @param data
     * @return The iterator over PDIA
     */
    public static PDIASample sample(int[] nSymbols, int[][]... data) {
        assert nSymbols.length == data.length : "Number of data types is inconsistent!";
        return new PDIASample(nSymbols[0],data);
    }

    /**
     * Runs an MCMC sampler a specified number of times, saving samples along the way
     * @param burnIn Number of burn in samples
     * @param interval Number of samples between saves
     * @param samples Number of saved samples
     * @param nSymbols The size of the alphabet for each data type
     * @param data
     * @return An array of posterior samples from the Markov chain
     */
    public static PDIA[] sample(int burnIn, int interval, int samples, int[] nSymbols, int[][]... data) {
        PDIA[] ps = new PDIA[samples];
        int i = 0;
        for (PDIA p : PDIA.sample(nSymbols,data)) {
            if (i < burnIn) {
                System.out.println("Burn In Sample " + i + " of " + burnIn);
            }
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = Util.copy(p);
                System.out.println("Wrote sample " + Integer.toString((i-burnIn)/interval) + " of " + samples);
            }
            i++;
            if (i == burnIn + interval*samples) break;
        }
        return ps;
    }

    /**
     * @param return The number of states the data visits.
     * Depends on the most recent int[][] data on which count(data) was called.
     */
    public int states() { return cMatrix.size(); }

    /**
     * @return A deterministic map from state/symbol pairs to next states.
     * Returns null if that state/symbol pair is not in the transition matrix.
     */
    public HashMap<Pair,Integer> transition() { return dMatrix; }

    /**
     * Given a state and symbol observed in that state, returns the next state,
     * or null if that state is not in the transition matrix
     * @param state
     * @param symbol
     * @return
     */
    public Integer transition(int state, int symbol) {
        return dMatrix.get(new Pair(state,symbol));
    }

    /**
     * Runs over all the data, filling the field cMatrix.
     * cMatrix - Hash map from states to array of symbols, giving the number
     * of times that symbol is emitted from that state.
     * @param data
     */
    public void count(int[][]... data) {
        cMatrix = new HashMap<Integer, int[]>();
        for (Pair p : run(data)) {
            int[] counts = cMatrix.get(p.state);
            if (counts == null) {
                counts = new int[nSymbols];
                cMatrix.put(p.state, counts);
            }
            counts[p.symbol] ++;
        }
        logLike = logLik();
    }

    /**
     * @return The joint log likelihood of the model and the data
     */
    public double jointScore() {
        return logLike + rf.logLik();
    }

    /**
     * @return The log likelihood of the data (for which cMatrix is a sufficient statistic)
     */
    public double logLik() {
        double logLik = 0;
        double lgb = Gamma.logGamma(beta);
        double bn = beta / nSymbols;
        double lgbn = Gamma.logGamma(bn);

        for (int[] arr : cMatrix.values()) {
            for (int count : arr) {
                if (count != 0) {
                    logLik += Gamma.logGamma(count + bn) - lgbn;
                }
            }
            logLik -= Gamma.logGamma(Util.sum(arr) + beta) - lgb;
        }

        return logLik;
    }

    /**
     * Samples the hyperparameter over the emission distribution
     * @param proposalSTD
     */
    public void sampleBeta(double proposalSTD) {
        double currentBeta = beta;

        double proposal = currentBeta + RNG.nextGaussian() * proposalSTD;
        if (proposal <= 0) {
            return;
        }
        beta = proposal;
        double pLogLik = logLik();
        double r = Math.exp(pLogLik - logLike - proposal + currentBeta);
        if (RNG.nextDouble() >= r) {
            beta = currentBeta;
        } else {
            logLike = pLogLik;
        }
    }

    /**
     * One sweep of sampling over the transition matrix.
     * @param data
     */
    public void sampleD(int[][]... data) {
        for (Pair p : randomPairArray()) {
            if (dMatrix.get(p) != null) {
                sampleD(p,data);
            }
            fixDMatrix();
        }
    }

    /**
     * Samples a single entry of the transition matrix
     * @param p The state/symbol pair to be sampled
     * @param data
     */
    public void sampleD(Pair p, int[][]... data) {
        int[] context = new int[]{p.symbol};
        double cLogLik = logLike;
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        dMatrix.put(p, proposedType);

        HashMap<Integer, int[]> oldCounts = (HashMap<Integer, int[]>)cMatrix.clone();
        count(data);
        double pLogLik = logLik();

        if (Math.log(RNG.nextDouble()) < pLogLik - cLogLik) { // accept
            rf.seat(proposedType, context);
        } else { // reject
            cMatrix = oldCounts;
            logLike = cLogLik;
            rf.seat(currentType, context);
            dMatrix.put(p, currentType);
        }
    }

    /**
     * After sampling, clears out state/symbol pairs for which there are no observed data
     */
    public void fixDMatrix() {
        HashSet<Pair> keysToDiscard = new HashSet<Pair>();

        for (Pair p : dMatrix.keySet()) {
            int[] counts = cMatrix.get(p.state);
            if ((counts == null) || (counts[p.symbol] == 0)) {
                keysToDiscard.add(p);
            }
        }

        int[] context = new int[1];
        for (Pair p : keysToDiscard) {
            context[0] = p.symbol;
            rf.unseat(dMatrix.get(p), context);
            dMatrix.remove(p);
        }
    }

    /**
     * Returns an array that gives the predictive probability of each data point,
     * given the training data and the previous data in the same array
     * @param init Initial state of the PDIA
     * @param data
     * @return The predictive probability of each element of data
     */
    public double[] score(int init, int[][]... data) {
        int totalLength = 0;
        for (int i = 0; i < data[0].length; i++) {
            totalLength += data[0][i].length;
        }

        double[] score = new double[totalLength];

        int index = 0;
        for (Pair p : run(init,data)) {
            int[] counts = cMatrix.get(p.state);
            if (counts == null) {
                counts = new int[nSymbols];
                cMatrix.put(p.state,counts);
            }

            int totalCount = Util.sum(counts);
            score[(index++)] = ((counts[p.symbol] + beta / nSymbols) / (totalCount + beta));
            counts[p.symbol]++;
        }

        return score;
    }

    /**
     * Same as above, but with predictions averaged over multiple PDIAs
     * We do things in this order because we need to average single-datum
     * probabilities before taking the sum of log probabilities.
     * @param ps Array of PDIA posterior samples
     * @param init
     * @param data
     * @return
     */
    public static double[] score(PDIA[] ps, int init, int[][]... data) {
        int n = 10;
        double[] score = new double[Util.totalLen(data[0])];
        for (PDIA pdia : ps) {
            for (int i = 0; i < n; i++) {
                PDIA copy = Util.<PDIA>copy(pdia);
                Util.addArrays(score, copy.score(init, data));
            }
        }

        for (int i = 0; i < score.length; i++) {
            score[i] /= (n*ps.length);
        }
        return score;
    }

    /**
     * Checks for consistency between the fields rf and dMatrix
     */
    public void check() {
    	HashMap<Integer, HashMap<Integer, MutableInteger>> dCustomerCounts = new HashMap<Integer, HashMap<Integer, MutableInteger>>();
        int[] context = new int[1];

        for (Pair p : dMatrix.keySet()) {
            context[0] = p.symbol;

            HashMap<Integer, MutableInteger> typeCountMap = dCustomerCounts.get(p.symbol);
            if (typeCountMap == null) {
                typeCountMap = new HashMap<Integer, MutableInteger>();
                dCustomerCounts.put(p.symbol, typeCountMap);
            }

            Integer tKey = dMatrix.get(p);
            MutableInteger count = typeCountMap.get(tKey);

            if (count == null) {
                count = new MutableInteger(0);
                typeCountMap.put(tKey, count);
            }

            count.increment();
        }

        int[] tCounts = new int[2];
        for (int i = 0; i < nSymbols; i++) {
            HashMap<Integer, MutableInteger> hm = dCustomerCounts.get(i);
            for (Integer type : hm.keySet()) {
                rf.get(context).getCounts(type.intValue(), tCounts);
                assert (tCounts[0] == hm.get(type).intVal());
            }
        }
    }

    private Pair[] randomPairArray() {
        Object[] roa = Util.randArray(dMatrix.keySet());
        Pair[]   rpa = new Pair[roa.length];
        for (int i = 0; i < roa.length; i++) {
            rpa[i] = (Pair)roa[i];
        }
        return rpa;
    }

}
