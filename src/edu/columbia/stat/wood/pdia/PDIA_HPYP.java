package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.Concentrations;
import edu.columbia.stat.wood.hpyp.Discounts;
import edu.columbia.stat.wood.hpyp.Restaurant;
import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.util.HashSet;

/**
 * A modified version of PDIA where the emission distributions are sampled from an HPYP instead of iid Dirichlet
 * @author davidpfau
 */
public class PDIA_HPYP extends PDIA_Dirichlet {

    private RestaurantFranchise emitRF;
    private Discounts emitD;
    private Concentrations emitC;
    private static final long serialVersionUID = 1L;

    public PDIA_HPYP(int n) {
        super(n);
        emitRF = new RestaurantFranchise(n);
        emitD = emitRF.discounts;
        emitC = emitRF.concentrations;
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
        for (Pair p : new PDIAContinuation(Util.copy(this),init,length)) {
            line[i] = p.symbol;
            i++;
        }
        return line;
    }*/

    /**
     * Given data, returns an iterator over PDIA that forms an MCMC sampler
     * @param nSymbols The size of the alphabet for each data type
     * @param data
     * @return The iterator over PDIA
     */
    public static PDIASample sample(int[] nSymbols, int[][]... data) {
        assert nSymbols.length == data.length : "Number of data types is inconsistent!";
        return new PDIASample(new PDIA_HPYP(nSymbols[0]),data);
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
    public static PDIA_HPYP[] sample(int burnIn, int interval, int samples, int[] nSymbols, int[][]... data) {
        PDIA_HPYP[] ps = new PDIA_HPYP[samples];
        int i = 0;
        for (PDIA p : PDIA_HPYP.sample(nSymbols,data)) {
            if (i < burnIn) {
                System.out.println("Burn In Sample " + i + " of " + burnIn);
            }
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = (PDIA_HPYP)Util.copy(p);
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
    @Override
    public int states() { return emitRF.get(null).size(); }

    /**
     * Runs over all the data, filling in an HPYP where each restaurant
     * corresponds to one state
     * @param data
     */
    @Override
    public void count(int[][]... data) {
        emitRF = new RestaurantFranchise(nSymbols);
        emitRF.concentrations = emitC;
        emitRF.discounts      = emitD;
        for (SinglePair p : run(data)) {
            emitRF.seat(p.symbol(0), new int[]{p.state()});
        }
        logLike = logLik();
    }

    /**
     * @return The log likelihood of the data (for which emitRF is a sufficient statistic)
     */
    @Override
    public double logLik() {
        return emitRF.logLik();
    }

    @Override
    public void sampleOnce(int[][]... data) {
        sampleD(data);
        rf.sample();
        emitRF.sample();
        fixDMatrix();
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
        int[] context = new int[]{p.symbol(0)};
        double cLogLik = logLike;
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        rf.seat(proposedType, context);
        dMatrix.put(p, proposedType);

        RestaurantFranchise oldEmitRF = Util.<RestaurantFranchise>copy(emitRF); // probably slow, should replace with its own deep copy method if significat
        count(data);
        double pLogLik = logLik();

        if (Math.log(RNG.nextDouble()) < pLogLik - cLogLik) { // accept
        } else { // reject
            rf.unseat(proposedType, context);
            rf.seat(currentType, context);
            emitRF = oldEmitRF;
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
            Restaurant r = emitRF.getDontAdd(new int[]{p.state()});
            if (r == null) {
                keysToDiscard.add(p);
            } else {
                int[] custAndTbl = new int[2];
                r.getCounts(p.symbol(0),custAndTbl);
                if (custAndTbl[0] == 0) {
                    keysToDiscard.add(p);
                }
            }
        }

        int[] context = new int[1];
        for (SinglePair p : keysToDiscard) {
            context[0] = p.symbol(0);
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
    @Override
    public double[] score(int init, int[][]... data) {
        int totalLength = 0;
        for (int i = 0; i < data[0].length; i++) {
            totalLength += data[0][i].length;
        }

        double[] score = new double[totalLength];

        int index = 0;
        for (SinglePair p : run(init,data)) {
            score[(index++)] = emitRF.predictiveProbability(p.symbol(0), new int[]{p.state()});
            emitRF.seat(p.symbol(0), new int[]{p.state()});
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
    public static double[] score(PDIA_HPYP[] ps, int init, int[][]... data) {
        int n = 10;
        double[] score = new double[Util.totalLen(data[0])];
        for (PDIA_HPYP pdia : ps) {
            for (int i = 0; i < n; i++) {
                PDIA_HPYP copy = Util.<PDIA_HPYP>copy(pdia);
                Util.addArrays(score, copy.score(init, data));
            }
        }

        for (int i = 0; i < score.length; i++) {
            score[i] /= (n*ps.length);
        }
        return score;
    }
}
