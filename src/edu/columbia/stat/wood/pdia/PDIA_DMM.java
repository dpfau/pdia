
package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.MutableInteger;
import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.math.special.Gamma;
/**
 * Implements a PDIA to learn a Directed Markov Model, in the sense of
 * "Constructing States for Reinforcement Learning" MM Mahmud, ICML 2010
 *
 * When passing data, it is assumed that the first array is actions, second is
 * observations, and third is rewards
 * @author David Pfau, 2011
 */
public class PDIA_DMM implements Serializable, PDIA {

    protected RestaurantFranchise rf;
    protected HashMap<MultiPair, Integer> dMatrix;
    protected HashMap<SinglePair, int[][]> cMatrix; // the number of times a given reward is observed following a given state and action
                                                    // the first index of the key is the state, the second is the action
                                                    // the first index of the value is the observation, the second is the reward
    protected int[] nSymbols;
    protected double beta;
    protected double logLike;
    protected static Random RNG = new Random(0L);
    private static final long serialVersionUID = 1L;

    public PDIA_DMM(int[] n) {
        assert n.length == 3 : "Need size of action, observation, and reward space.";
        rf = new RestaurantFranchise(1);
        nSymbols = n;
        dMatrix = new HashMap<MultiPair, Integer>();
        beta = 10.0;
    }

    public PDIASequence run(int[][]... data) {
        assert data.length == 3 : "Need actions, observations and rewards.";
        return new PDIASequence(this,0,data);
    }

    public PDIASequence run(int init, int[][]... data) {
        assert data.length == 3 : "Need actions, observations and rewards";
        return new PDIASequence(this,init,data);
    }


    /**
     * Samples a new string from the current PDIA without changing any data structures
     * @param init The initial state of the new string
     * @param length The length of the new string
     * @return
     */
    /*public int[] generate(int init, int length) {
        int[] line = new int[length];
        int i = 0;
        for (SinglePair p : new PDIAContinuation(Util.copy(this),init,length)) {
            line[i] = p.symbol(0);
            i++;
        }
        return line;
    }*/

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
    public static PDIASample sample(int[] nSymbols, int[][]... data) {
        assert nSymbols.length == data.length : "Number of data types is inconsistent!";
        return new PDIASample(new PDIA_DMM(nSymbols),data);
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
    public static PDIA_DMM[] sample(int burnIn, int interval, int samples, int[] nSymbols, int[][]... data) {
        PDIA_DMM[] ps = new PDIA_DMM[samples];
        int i = 0;
        for (PDIA p : PDIA_DMM.sample(nSymbols,data)) {
            if (i < burnIn) {
                System.out.println("Burn In Sample " + i + " of " + burnIn);
            }
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = (PDIA_DMM)Util.copy(p);
                System.out.println("Wrote sample " + Integer.toString((i-burnIn)/interval) + " of " + samples);
            }
            i++;
            if (i == burnIn + interval*samples) break;
        }
        return ps;
    }

    public int states() { return cMatrix.size(); }

    /**
     * @return A deterministic map from state/symbol pairs to next states.
     * Returns null if that state/symbol pair is not in the transition matrix.
     */
    public HashMap<MultiPair,Integer> transition() { return dMatrix; }

    /**
     * Given a state and symbol observed in that state, returns the next state,
     * or null if that state is not in the transition matrix
     * @param state
     * @param symbol
     * @return
     */
    public Integer transition(int state, int[] symbol) {
        assert symbol.length == 2; // check to make sure we're leaving out the reward when transitioning
        return dMatrix.get(new MultiPair(state,symbol));
    }

    public Integer transition(int state, int action, int observation) {
        return dMatrix.get(new MultiPair(state, new int[]{action, observation}));
    }

    public Integer transition(Pair p) {
        int[] symbols = new int[]{p.symbol(0),p.symbol(1)};
        return dMatrix.get(new MultiPair(p.state(),symbols));
    }

    public Integer transitionAndAdd(Pair p) {
        Integer state = transition(p);
        if (state == null) {
            int[] context = new int[]{p.symbol(0),p.symbol(1)};
            state = rf.generate(context);
            rf.seat(state, context);
            dMatrix.put(new MultiPair(p.state(),context), state);
        }
        return state;
    }

    public void count(int[][]... data) {
        cMatrix = new HashMap<SinglePair, int[][]>();
        for (Pair p : run(data)) {
            SinglePair sa = new SinglePair(p.state(),p.symbol(0)); // state/action pair
            int[][] counts = cMatrix.get(new SinglePair(p.state(),p.symbol(0)));
            if (counts == null) {
                counts = new int[nSymbols[1]][];
                cMatrix.put(sa, counts);
            }
            if (counts[p.symbol(1)] == null) {
                counts[p.symbol(1)] = new int[nSymbols[2]];
            }
            counts[p.symbol(1)][p.symbol(2)] ++;
        }
        logLike = logLik();
    }

    public double jointScore() {
        return logLike + rf.logLik();
    }

    public double logLik() {
        double logLik = 0;
        double lgb = Gamma.logGamma(beta);
        double bn = beta / nSymbols[nSymbols.length - 1];
        double lgbn = Gamma.logGamma(bn);

        for (int[][] arr : cMatrix.values()) {
            int[] counts = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                counts[i] = Util.sum(arr[i]);
                if (counts[i] != 0) {
                    logLik += Gamma.logGamma(counts[i] + bn) - lgbn;
                }
            }
            logLik -= Gamma.logGamma(Util.sum(counts) + beta) - lgb;
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
        for (MultiPair p : randomPairArray()) {
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
    private void sampleD(MultiPair p, int[][]... data) {
        int[] context = p.symbol();
        double cLogLik = logLike;
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        rf.seat(currentType, context);
        dMatrix.put(p, proposedType);

        HashMap<SinglePair, int[][]> oldCounts = (HashMap<SinglePair,int[][]>)Util.intTwoDArrayMapCopy(cMatrix);
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
        HashSet<MultiPair> keysToDiscard = new HashSet<MultiPair>();

        for (MultiPair p : dMatrix.keySet()) {
            int[][] counts = cMatrix.get(p.toSingle());
            if (counts == null || counts[p.symbol(1)] == null || counts[p.symbol(1)][p.symbol(2)] == 0) {
                keysToDiscard.add(p);
            }
        }

        int[] context = new int[2];
        for (MultiPair p : keysToDiscard) {
            context[0] = p.symbol(0);
            context[1] = p.symbol(1);
            rf.unseat(dMatrix.get(p), context);
            dMatrix.remove(p);
        }
    }

    public double[] score(int init, int[][]... data) {
        int totalLength = 0;
        int no = nSymbols[1];
        int nr = nSymbols[2];
        for (int i = 0; i < data[0].length; i++) {
            totalLength += data[0][i].length;
        }

        double[] score = new double[totalLength];

        int index = 0;
        for (Pair p : run(init,data)) {
            MultiPair mp = (MultiPair)p;
            int[][] counts = cMatrix.get(mp.toSingle());
            if (counts == null) {
                counts = new int[no][];
                cMatrix.put(mp.toSingle(),counts);
            }
            if (counts[mp.symbol(1)] == null) {
                counts[mp.symbol(1)] = new int[nr];
            }

            double totalCount = Util.sum(counts);
            score[(index++)] = ((counts[mp.symbol(1)][mp.symbol(2)] + beta / nr) / (totalCount + beta));
            counts[mp.symbol(1)][mp.symbol(2)]++;
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

    // Use with caution, I haven't checked this
    public void check() {
    	HashMap<int[], HashMap<Integer, MutableInteger>> dCustomerCounts = new HashMap<int[], HashMap<Integer, MutableInteger>>();

        for (MultiPair p : dMatrix.keySet()) {
            int[] context = p.symbol();

            HashMap<Integer, MutableInteger> typeCountMap = dCustomerCounts.get(context);
            if (typeCountMap == null) {
                typeCountMap = new HashMap<Integer, MutableInteger>();
                dCustomerCounts.put(context, typeCountMap);
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
        for (int i = 0; i < nSymbols[0]; i++) {
            for (int j = 0; j < nSymbols[1]; j++) {
                int[] context = new int[]{i,j};
                HashMap<Integer, MutableInteger> hm = dCustomerCounts.get(context);
                for (Integer type : hm.keySet()) {
                    rf.get(context).getCounts(type.intValue(), tCounts);
                    assert (tCounts[0] == hm.get(type).intVal());
                }
            }
        }
    }

    protected MultiPair[] randomPairArray() {
        Object[] oa = Util.randArray(dMatrix.keySet());
        MultiPair[] pa = new MultiPair[oa.length];
        System.arraycopy(oa, 0, pa, 0, oa.length);
        return pa;
    }




}

