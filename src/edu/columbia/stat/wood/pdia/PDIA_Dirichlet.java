package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.MutableInteger;
import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.math.special.Gamma;

/**
 * The default PDIA implementation, with iid Dirichlet emission distributions
 *
 * If multiple data arrays are passed to any method, all after the first will
 * be ignored.  Varargs are only used for consistency with the interface.
 *
 * @author Nick Bartlett and David Pfau, 2010-11
 */
public class PDIA_Dirichlet implements Serializable, PDIA {

    protected RestaurantFranchise rf;
    protected HashMap<SinglePair, Integer> dMatrix;
    protected HashMap<Integer, int[]> cMatrix;
    protected int nSymbols;
    protected double beta;
    protected double logLike;
    protected static Random RNG = new Random(0L);
    private static final long serialVersionUID = 1L;

    public PDIA_Dirichlet(int n) {
        rf = new RestaurantFranchise(1);
        nSymbols = n;
        dMatrix = new HashMap<SinglePair, Integer>();
        beta = 10.0;
    }

    public PDIASequence run(int[][]... data) {
        PDIASequence seq = new PDIASequence(this,0,data);
        seq.multi = false;
        return seq;
    }

    public PDIASequence run(int init, int[][]... data) {
        PDIASequence seq = new PDIASequence(this,init,data);
        seq.multi = false;
        return seq;
    }


    /**
     * Samples a new string from the current PDIA without changing any data structures
     * @param init The initial state of the new string
     * @param length The length of the new string
     * @return
     */
    public int[] generate(int init, int length) {
        int[] line = new int[length];
        int i = 0;
        for (SinglePair p : new PDIAContinuation(Util.copy(this),init,length)) {
            line[i] = p.symbol(0);
            i++;
        }
        return line;
    }
    
    /**
     * Set the seed of RNG using the current system time. 
     */
    public static void reseedRNG() {
    	RNG.setSeed(System.currentTimeMillis());
    }

    /**
     * Given data, returns an iterator over PDIA that forms an MCMC sampler
     * @param nSymbols The size of the alphabet for each data type
     * @param data
     * @return The iterator over PDIA
     */
    public static PDIASample sample(int nSymbols, int[][] data) {
        return new PDIASample(nSymbols,data);
    }

    // Same but for Object arrays, because fsck Matlab
    public static PDIASample sample(int nSymbols, Object[] data) {
        return new PDIASample(nSymbols, Util.objectArrayTo2DIntArray(data));
    }
    
    public static PDIA_Dirichlet[] sample(int burnIn, int interval, int samples, int nSymbols, int[][] data) {
    	return sample(burnIn, interval, samples, nSymbols, data, null, 0);
    }

    /**
     * Runs an MCMC sampler a specified number of times, saving samples along the way
     * 
     * @param burnIn Number of burn in samples
     * @param interval Number of samples between saves
     * @param samples Number of saved samples
     * @param nSymbols The size of the alphabet for each data type
     * @param data
     * @return An array of posterior samples from the Markov chain
     */
    public static PDIA_Dirichlet[] sample(int burnIn, int interval, int samples, int nSymbols, int[][] data, SamplerUpdateHandler h, int updateInterval) {
        PDIA_Dirichlet[] ps = new PDIA_Dirichlet[samples];
        int i = 0;
        for (PDIA p : PDIA_Dirichlet.sample(nSymbols,data)) {
            if (i < burnIn) {
                System.out.println("Burn In Sample " + (i+1) + " of " + burnIn);
            }
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = (PDIA_Dirichlet)Util.copy(p);
                System.out.println("Wrote sample " + ((i-burnIn)/interval+1) + " of " + samples);
            }
            if (h != null && i > burnIn && (i-burnIn) % (updateInterval*interval) == 0) {
               h.update(ps, (i - burnIn)/interval+1);
            }
            i++;
            if (i == burnIn + interval*samples) break;
        }
        return ps;
    }
    
    // Same but for Object arrays, because fsck Matlab
    public static PDIA_Dirichlet[] sample(int burnIn, int interval, int samples, int nSymbols, Object[] data) {
        return sample(burnIn, interval, samples, nSymbols, Util.objectArrayTo2DIntArray(data));
    }
    
    
    // Same but for Object arrays, because fsck Matlab
    public static PDIA_Dirichlet[] sample(int burnIn, int interval, int samples, int nSymbols, Object[] data, SamplerUpdateHandler h, int updateInterval) {
        return sample(burnIn, interval, samples, nSymbols, Util.objectArrayTo2DIntArray(data), h, updateInterval);
    }
    
    
    public Set<Integer> states() { return cMatrix.keySet(); }

    /**
     * @return A deterministic map from state/symbol pairs to next states.
     * Returns null if that state/symbol pair is not in the transition matrix.
     */
    public HashMap<SinglePair,Integer> transition() { return dMatrix; }

    /**
     * Given a state and symbol observed in that state, returns the next state,
     * or null if that state is not in the transition matrix
     * @param state
     * @param symbol
     * @return
     */
    public Integer transition(int state, int symbol) {
        return dMatrix.get(new SinglePair(state,symbol));
    }

    public Integer transition(Pair p) {
        return dMatrix.get((SinglePair)p);
    }

    public Integer transitionAndAdd(Pair p) {
        Integer state = transition(p);
        if (state == null) {
            int[] context = p.symbol();
            state = rf.generate(context);
            rf.seat(state, context);
            dMatrix.put((SinglePair)p, state);
        }
        return state;
    }

    public void count(int[][]... data) {
        cMatrix = new HashMap<Integer, int[]>();
        for (Pair p : run(data)) {
            int[] counts = cMatrix.get(p.state());
            if (counts == null) {
                counts = new int[nSymbols];
                cMatrix.put(p.state(), counts);
            }
            counts[p.symbol(0)] ++;
        }
        logLike = logLik();
    }

    public double jointScore() {
        return logLike + rf.logLik();
    }

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
    protected void sampleBeta(double proposalSTD) {
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

    public void sampleOnce(int[][]... data) {
        sampleD(data);
        rf.sample();
        sampleBeta(1.0);
    }

    /**
     * One sweep of sampling over the transition matrix.
     * @param data
     */
    private void sampleD(int[][]... data) {
        for (SinglePair p : randomPairArray()) {
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
    private void sampleD(SinglePair p, int[][]... data) {
        int[] context = p.symbol();
        double cLogLik = logLike;
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        rf.seat(currentType, context);
        dMatrix.put(p, proposedType);

        HashMap<Integer, int[]> oldCounts = (HashMap<Integer,int[]>)Util.intArrayMapCopy(cMatrix);
        count(data);
        double pLogLik = logLik();

        if (Math.log(RNG.nextDouble()) < pLogLik - cLogLik) { // accept
            rf.unseat(currentType, context);
            rf.seat(proposedType, context);
        } else { // reject
            cMatrix = oldCounts;
            logLike = cLogLik;
            dMatrix.put(p, currentType);
        }
    }

    /**
     * After sampling, clears out state/symbol pairs for which there are no observed data
     */
    private void fixDMatrix() {
        HashSet<SinglePair> keysToDiscard = new HashSet<SinglePair>();

        for (SinglePair p : dMatrix.keySet()) {
            int[] counts = cMatrix.get(p.state());
            if ((counts == null) || (counts[p.symbol(0)] == 0)) {
                keysToDiscard.add(p);
            }
        }

        int[] context = new int[1];
        for (SinglePair p : keysToDiscard) {
            context[0] = p.symbol(0);
            rf.unseat(dMatrix.get(p), context);
            dMatrix.remove(p);
        }
    }

    public double[] score(int init, int[][]... data) {
        int totalLength = 0;
        for (int i = 0; i < data[0].length; i++) {
            totalLength += data[0][i].length;
        }

        double[] score = new double[totalLength];

        int index = 0;
        for (Pair p : run(init,data)) {
            int[] counts = cMatrix.get(p.state());
            if (counts == null) {
                counts = new int[nSymbols];
                cMatrix.put(p.state(),counts);
            }

            int totalCount = Util.sum(counts);
            score[(index++)] = ((counts[p.symbol(0)] + beta / nSymbols) / (totalCount + beta));
            counts[p.symbol(0)]++;
        }

        return score;
    }

    /**
     * Same as above, but with predictions averaged over multiple PDIAs
     * We do things in this order because we need to average single-datum
     * probabilities before taking the sum of log probabilities.
     * 
     * @param ps Array of PDIA posterior samples
     * @param init
     * @param data
     * @return
     */
    public static double[] score(PDIA_Dirichlet[] ps, int init, int[][]... data) {
        int n = 10;
        double[] score = new double[Util.totalLen(data[0])];
        for (PDIA_Dirichlet pdia : ps) {
            for (int i = 0; i < n; i++) {
                PDIA_Dirichlet copy = Util.<PDIA_Dirichlet>copy(pdia);
                Util.addArrays(score, copy.score(init, data));
            }
        }

        for (int i = 0; i < score.length; i++) {
            score[i] /= (n*ps.length);
        }
        return score;
    }
    
    // Matlab friendly version
    public static double[] mscore(PDIA_Dirichlet[] ps, int init, Object[] data) {
    	return score(ps, init, Util.objectArrayTo2DIntArray(data));
    }

    public void check() {
    	HashMap<Integer, HashMap<Integer, MutableInteger>> dCustomerCounts = new HashMap<Integer, HashMap<Integer, MutableInteger>>();
        int[] context = new int[1];

        for (SinglePair p : dMatrix.keySet()) {
            context[0] = p.symbol(0);

            HashMap<Integer, MutableInteger> typeCountMap = dCustomerCounts.get(p.symbol(0));
            if (typeCountMap == null) {
                typeCountMap = new HashMap<Integer, MutableInteger>();
                dCustomerCounts.put(p.symbol(0), typeCountMap);
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
            context[0] = i;
            for (Integer type : hm.keySet()) {
                rf.get(context).getCounts(type.intValue(), tCounts);
                assert (tCounts[0] == hm.get(type).intVal());
            }
        }
    }

    protected SinglePair[] randomPairArray() {
        Object[] oa = Util.randArray(dMatrix.keySet());
        SinglePair[] pa = new SinglePair[oa.length];
        System.arraycopy(oa, 0, pa, 0, oa.length);
        return pa;
    }  
    
    
    

}
