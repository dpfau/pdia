
package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.MutableDouble;
import edu.columbia.stat.wood.hpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.apache.commons.math.special.Gamma;
/**
 * An implementation of PDIA for reinforcement learning that uses *two*
 * infinite transition matrices, one for the state following an action, one for
 * the state following an observation (and reward).  Note that this means there
 * are two classes of states, which we refer to as a-states, for those preceding
 * an action, and o-states, for those preceding an observation.  The typical
 * states of a POMDP, which emit an observation and reward, correspond to
 * o-states.
 * @author David Pfau, 2011
 */
public class BipartitePDIA implements Serializable, PDIA {

    protected RestaurantFranchise[] RFs; // RFs[0]: customers are a-states, restaurants are actions, dishes are o-states
                                         // RFs[1]: customers are o-states, restaurants are observations, dishes are a-states

    public HashMap<SinglePair, Integer>[] transitions; // transitions[0]: key is length 2 array with a-state and action, value is o-state
                                                       // transitions[1]: key is length 2 array with o-state and observation, value is a-state

    public HashMap<SinglePair, int[]> rMatrix; // the number of times a given reward is observed following a given a-state and action
    public HashMap<Integer, int[]> oMatrix; // the number of times a given observaton is seen following a given o-state, used to construct an MDP
    public int[] nSymbols; // nSymbols[0] = nActions, nSymbols[1] = nObservation, nSymbols[2] = nRewards
    public double beta; // hyperparameter for reward distributions given a state
    //protected double gamma; // only used for observation likelihood
    protected double logLike;
    protected static Random RNG = new Random(0L);
    private static final long serialVersionUID = 1L;

    public BipartitePDIA( int[] n ) {
        assert n.length == 3 : "Need size of action, observation, and reward space.";
        RFs = new RestaurantFranchise[2];
        RFs[0] = new RestaurantFranchise(1);
        RFs[1] = new RestaurantFranchise(1);
        nSymbols = n;
        transitions = new HashMap[2];
        transitions[0] = new HashMap<SinglePair, Integer>();
        transitions[1] = new HashMap<SinglePair, Integer>();
        beta = 10.0;
    }

    public double beta() {
        return beta;
    }

    public double[] concentrations(int i) {
        return RFs[i].concentrations.get();
    }

    public double[] discounts(int i) {
        return RFs[i].discounts.get();
    }

    public void setBeta(double b) {
        beta = b;
    }

    public void setConcentration(int i, int j, double c) {
        RFs[i].concentrations.get(j).set(c);
    }

    public void setDiscount(int i, int j, double d) {
        RFs[i].discounts.get(j).set(d);
    }

    public PDIASequence run( int[][]... data ) {
        assert data.length == 3 : "Need actions, observations and rewards.";
        return new PDIASequence(this,0,data);
    }

    public PDIASequence run( int init, int[][]... data ) {
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
    public static PDIASample sample( int[] nSymbols, int[][]... data ) {
        assert nSymbols.length == data.length : "Number of data types is inconsistent!";
        return new PDIASample(new BipartitePDIA(nSymbols),data);
    }

    //Hacked version of the above to make Matlab scripts work
    public static PDIASample sample( BipartitePDIA p, Object[] action, Object[] observation, Object[] reward ) {
        int[][] castAct = new int[action.length][];
        for (int i = 0; i < action.length; i++) {
            castAct[i] = (int[])action[i];
        }
        int[][] castObs = new int[observation.length][];
        for (int i = 0; i < action.length; i++) {
            castObs[i] = (int[])observation[i];
        }
        int[][] castRew = new int[reward.length][];
        for (int i = 0; i < action.length; i++) {
            castRew[i] = (int[])reward[i];
        }
        return new PDIASample(p,castAct,castObs,castRew);
    }

    public static PDIASample sample( int[] nSymbols, Object[] action, Object[] observation, Object[] reward ) {
        return BipartitePDIA.sample(new BipartitePDIA(nSymbols), action, observation, reward);
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
    public static BipartitePDIA[] sample( int burnIn, int interval, int samples, int[] nSymbols, int[][]... data ) {
        BipartitePDIA[] ps = new BipartitePDIA[samples];
        int i = 0;
        for (PDIA p : BipartitePDIA.sample(nSymbols,data)) {
            if (i < burnIn) {
                System.out.println("Burn In Sample " + i + " of " + burnIn);
            }
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = (BipartitePDIA)Util.copy(p);
                System.out.println("Wrote sample " + Integer.toString((i-burnIn)/interval) + " of " + samples);
            }
            i++;
            if (i == burnIn + interval*samples) break;
        }
        return ps;
    }

    /**
     * Returns the set of states that precede an action, that is the a-states,
     * as these are the states used for planning.
     * @return
     */
    public Set<Integer> states() {
        HashSet<Integer> states = new HashSet<Integer>();
        states.add(0);
        for (Integer i : transitions[1].values()) {
            states.add(i);
        }
        return states;
    }


    public Integer transition( int state, int action, int observation ) {
        if ( halfTransition( state, action ) == null ) {
            return null;
        } else {
            return transitions[1].get( new SinglePair( halfTransition( state, action ), observation ) );
        }
    }

    // Transition from an a-state to an o-state, rather than a-state to another a-state
    public Integer halfTransition( int state, int action ) {
        return transitions[0].get( new SinglePair( state, action ) );
    }

    /**
     * Given a state and action, returns an array of counts for observations seen
     * @param state
     * @param action
     * @return
     */
    public int[] observation( int state, int action ) {
        return oMatrix.get( halfTransition( state, action ) );
    }

    /**
     * Given a state and action, returns an array of counts for rewards seen
     * @param state
     * @param action
     * @return
     */
    public int[] reward( int state, int action ) {
        return rMatrix.get( new SinglePair( state, action ) );
    }

    public Integer transition( Pair p ) {
        return transition( p.state(), p.symbol(0), p.symbol(1) );
    }

    public Integer transitionAndAdd( Pair p ) {
        Integer aState = transition(p);
        if ( aState == null ) {
            Integer oState = halfTransition( p.state(), p.symbol(0) );
            if ( oState == null ) {
                int[] aContext = new int[]{ p.symbol(0) };
                oState = RFs[0].generate( aContext );
                RFs[0].seat( oState, aContext );
                transitions[0].put( new SinglePair( p.state(), p.symbol(0) ), oState );
            }
            int[] oContext = new int[]{ p.symbol(1) };
            aState = RFs[1].generate( oContext );
            RFs[1].seat( aState, oContext );
            transitions[1].put( new SinglePair( oState, p.symbol(1) ), aState );
        }
        return aState;
    }

    public Integer transitionAndAdd( int state, int action, int observation ) {
        return transitionAndAdd( new MultiPair( state, new int[]{ action, observation } ) );
    }

    private double[][] aoTransitionProbability( int[] context ) {
        HashMap<Integer,MutableDouble> map = RFs[0].predictiveProbabilityExistingTypes( context );
        double[][] probs = new double[map.size()][2];
        int i = 0;
        for (int key : map.keySet()) {
            probs[i][0] = key;
            probs[i][1] = map.get(key).doubleVal();
            i++;
        }
        return probs;
    }

    /**
     * Returns a two-column matrix of next o-states in the first column (with -1
     * for an unused state) and the probability of transitioning to that state
     * from an unknown state without any assumption about the observed action or
     * observation associated with that transition
     */
    public double[][] aoTransitionProbability() {
        return aoTransitionProbability( null );
    }

    /**
     * Returns a two-column matrix with the same format as above, but for a
     * transition with a known observation
     * @param observation
     * @return
     */
    public double[][] aoTransitionProbability( int action ) {
        return aoTransitionProbability( new int[]{action} );
    }

    private double[][] oaTransitionProbability( int[] context ) {
        HashMap<Integer,MutableDouble> map = RFs[1].predictiveProbabilityExistingTypes( context );
        double[][] probs = new double[map.size()][2];
        int i = 0;
        for (int key : map.keySet()) {
            probs[i][0] = key;
            probs[i][1] = map.get(key).doubleVal();
            i++;
        }
        return probs;
    }

    /**
     * Returns a two-column matrix of next a-states in the first column (with -1
     * for an unused state) and the probability of transitioning to that state
     * from an unknown state without any assumption about the observed action or
     * observation associated with that transition
     */
    public double[][] oaTransitionProbability() {
        return oaTransitionProbability( null );
    }

    /**
     * Returns a two-column matrix with the same format as above, but for a
     * transition with a known observation
     * @param observation
     * @return
     */
    public double[][] oaTransitionProbability( int observation ) {
        return oaTransitionProbability( new int[]{ observation } );
    }

    public void count(int[][]... data) {
        oMatrix = new HashMap<Integer, int[]>();
        rMatrix = new HashMap<SinglePair, int[]>();
        for (Pair p : run(data)) {
            SinglePair sa = new SinglePair( p.state(), p.symbol(0) ); // a-state and action
            Integer oState = transitions[0].get( sa );

            int[] oCounts = oMatrix.get( oState );
            if ( oCounts == null ) {
                oCounts = new int[nSymbols[1]];
                oMatrix.put( oState, oCounts );
            }

            int[] rCounts = rMatrix.get( sa );
            if ( rCounts == null ) {
                rCounts = new int[nSymbols[2]];
                rMatrix.put( sa, rCounts );
            }

            oCounts[p.symbol(1)] ++;
            rCounts[p.symbol(2)] ++;
        }
        logLike = logLik();
    }

    public double jointScore() {
        return logLike + RFs[0].logLik() + RFs[1].logLik();
    }

    public double logLik() {
        double logLik = 0;
        double lgb = Gamma.logGamma(beta);
        double bn = beta / nSymbols[nSymbols.length - 1];
        double lgbn = Gamma.logGamma(bn);

        for (int[] counts : rMatrix.values()) {
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] != 0) {
                    logLik += Gamma.logGamma(counts[i] + bn) - lgbn;
                }
            }
            logLik -= Gamma.logGamma(Util.sum(counts) + beta) - lgb;
        }

        /*
        double lgg = Gamma.logGamma(gamma);
        double gn = gamma / nSymbols[1];
        double lggn = Gamma.logGamma(gn);

        for (int[] counts : oMatrix.values()) {
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] != 0) {
                    logLik += Gamma.logGamma(counts[i] + gn) - lggn;
                }
            }
            logLik -= Gamma.logGamma(Util.sum(counts) + gamma) - lgg;
        }
        */

        return logLik;
    }

    /**
     * Samples the hyperparameter over the emission distribution
     * @param proposalSTD
     */
    protected void sampleBeta(double proposalSTD) {
        double currentBeta = beta;
        //double currentGamma = gamma;

        double proposal = currentBeta + RNG.nextGaussian() * proposalSTD;
        //double propGam  = currentGamma + RNG.nextGaussian() * proposalSTD;
        if (proposal <= 0) {
            return;
        }
        /*if (propGam <= 0) {
            return;
        }*/
        beta = proposal;
        //gamma = propGam;
        double pLogLik = logLik();
        //double r = Math.exp(pLogLik - logLike - proposal - propGam + currentBeta + currentGamma);
        double r = Math.exp(pLogLik - logLike - proposal + currentBeta);
        if (RNG.nextDouble() >= r) {
            beta = currentBeta;
            //gamma = currentGamma;
        } else {
            logLike = pLogLik;
        }
    }

    public void sampleOnce(int[][]... data) {
        sampleTransitions(0,data);
        RFs[0].sample();
        sampleTransitions(1,data);
        RFs[1].sample();
        sampleBeta(1.0);
    }

    /**
     * One sweep of sampling over the a-state to o-state transition matrix.
     * @param data
     */
    private void sampleTransitions(int i, int[][]... data) {
        for (SinglePair p : randomPairArray(transitions[i].keySet())) {
            if (transitions[i].get(p) != null) {
                sampleTransitions(i,p,data);
            }
            fixTransitions();
        }
    }


    /**
     * Samples a single entry of a transition matrix
     * @param i 0 = sample a-state to o-state transitions, 1 = sample o-state to a-state transitions
     * @param p The state/symbol pair to be sampled
     * @param data
     */
    private void sampleTransitions(int i, SinglePair p, int[][]... data) {
        int[] context = p.symbol();
        double cLogLik = logLike;
        Integer currentType = transitions[i].get(p);
        assert (currentType != null);

        RFs[i].unseat(currentType, context);
        Integer proposedType = RFs[i].generate(context);
        RFs[i].seat(currentType, context);
        transitions[i].put(p, proposedType);

        HashMap<Integer, int[]>    oMatOld = (HashMap<Integer,int[]>)   Util.intArrayMapCopy(oMatrix);
        HashMap<SinglePair, int[]> rMatOld = (HashMap<SinglePair,int[]>)Util.intArrayMapCopy(rMatrix);
        count(data);
        double pLogLik = logLik();

        if (Math.log(RNG.nextDouble()) < pLogLik - cLogLik) { // accept
            RFs[0].unseat(currentType, context);
            RFs[0].seat(proposedType, context);
        } else { // reject
            oMatrix = oMatOld;
            rMatrix = rMatOld;
            logLike = cLogLik;
            transitions[i].put(p, currentType);
        }
    }

    /**
     * After sampling, clears out state/action and state/observation pairs for which there are no data
     */
    private void fixTransitions() {
        HashSet<SinglePair> actToDiscard = new HashSet<SinglePair>();
        HashSet<SinglePair> obsToDiscard = new HashSet<SinglePair>();

        for ( SinglePair sa : transitions[0].keySet() ) {
            if ( rMatrix.get( sa ) == null ) {
                actToDiscard.add( sa );
            }
        }

        for ( SinglePair sa : actToDiscard ) {
            RFs[0].unseat( sa.state(), sa.symbol() );
            transitions[0].remove( sa );
        }

        for ( SinglePair so : transitions[1].keySet() ) {
            int[] oCounts = oMatrix.get( so.state() );
            if ( oCounts == null || oCounts[so.symbol(0)] == 0 ) {
                obsToDiscard.add( so );
            }
        }

        for ( SinglePair so : obsToDiscard ) {
            RFs[1].unseat( so.state(), so.symbol() );
            transitions[1].remove( so );
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
            Integer oState = halfTransition( mp.state(), mp.symbol(0) );
            int[] oCounts = oMatrix.get( oState );
            if (oCounts == null) {
                oCounts = new int[no];
                oMatrix.put( oState, oCounts );
            }

            int[] rCounts = rMatrix.get( mp.toSingle() );
            if (rCounts == null) {
                rCounts = new int[nr];
                rMatrix.put( mp.toSingle(), rCounts );
            }

            double totalCount = Util.sum(rCounts);
            score[(index++)] = ((rCounts[mp.symbol(2)] + beta / nr) / (totalCount + beta));
            oCounts[mp.symbol(1)] ++;
            rCounts[mp.symbol(2)] ++;
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
    public static double[] score(BipartitePDIA[] ps, int init, int[][]... data) {
        int n = 10;
        double[] score = new double[Util.totalLen(data[0])];
        for (BipartitePDIA pdia : ps) {
            for (int i = 0; i < n; i++) {
                BipartitePDIA copy = Util.copy(pdia);
                Util.addArrays(score, copy.score(init, data));
            }
        }

        for (int i = 0; i < score.length; i++) {
            score[i] /= (n*ps.length);
        }
        return score;
    }

    /**
     * Does nothing.  Don't waste your time.
     */
    public void check() {
    	
    }

    protected SinglePair[] randomPairArray(Set s) {
        Object[] oa = Util.randArray(s);
        SinglePair[] pa = new SinglePair[oa.length];
        System.arraycopy(oa, 0, pa, 0, oa.length);
        return pa;
    }
}

