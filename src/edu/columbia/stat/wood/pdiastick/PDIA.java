/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdiastick;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.commons.math.special.Gamma;

/**
 *
 * @author davidpfau
 */
public class PDIA {

    private HashMap<Integer, int[]> cMatrix;
    private HashMap<Tuple, Integer> dMatrix = new HashMap<Tuple, Integer>();
    private int nsymb;
    public double alpha = 10; // Top level concentration
    public double gamma = 0.8; // Top level discount
    public double alpha0 = 10; // Lower level concentration
    public double gamma0 = 0.8; // Lower level discount
    public double beta = 1; // Emission distribution concentration
    private int[] states = new int[0];
    private int[][] nTables = new int[0][]; // number of tables serving a dish in a restaurant in HDP sampling scheme
    private int[][] nCustomers = new int[0][]; // number of customers in each restaurant eating a dish
    private double[] sticks = new double[0]; // explicit stick lengths for top-level DP
    private GenSym gensym = new GenSym();

    public PDIA(int nsymb) {
        this.nsymb = nsymb;
    }

    public Integer transitionAndAdd(Tuple tuple) {
        Integer state = dMatrix.get(tuple);
        if (state == null) {
            double[] probs = new double[states.length + 1];
            double b_u = 1 - Util.sum(sticks);
            for (int t = 0; t < states.length; t++) {
                probs[t] = nCustomers[t][tuple.get(1)] + alpha0 * sticks[t];
            }
            probs[states.length] = alpha0 * b_u;
            int idx = CategoricalDistribution.sample(probs);
            if (idx == states.length) {
                state = gensym.sample();
                addState(b_u, state, tuple.get(1));
            } else {
                state = states[idx];
            }
            dMatrix.put(tuple, state);
            nCustomers[idx][tuple.get(1)]++;
        }
        return state;
    }

    public void count(int[][] data) {
        cMatrix = new HashMap<Integer, int[]>();
        for (Tuple t : new PDIASequence(this, data)) {
            int[] cts = cMatrix.get(t.get(0));
            if (cts == null) {
                cts = new int[nsymb];
                cMatrix.put(t.get(0), cts);
            }
            cts[t.get(1)]++;
        }
    }

    public double logLik() {
        double logLik = 0;
        double lgb = Gamma.logGamma(beta);
        double bn = beta / nsymb;
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

    // Log likelihood of the table arrangement for restaurant r
    public double logLikTables( int r ) {
        assert r >= 0 && r < nsymb : "Not a recognized restaurant!";
        double logLik = Gamma.logGamma( alpha0 ) - Gamma.logGamma( alpha0/gamma0 );
        int totalTables = 0;
        int totalCustomers = 0;
        for (int i = 0; i < nCustomers.length; i++) {
            totalTables += nTables[i][r];
            totalCustomers += nCustomers[i][r];
            logLik += Gamma.logGamma( nCustomers[i][r] - nTables[i][r] * gamma0 )
                    - Gamma.logGamma( nTables[i][r] * ( 1 - gamma0 ) );
        }
        return logLik + totalTables * Math.log( gamma0 )
                      + Gamma.logGamma( alpha0/gamma0 + totalTables )
                      - Gamma.logGamma( alpha0 + totalCustomers );
    }

    public void sample_tables(int j, int k) {
        double[] prob = new double[nCustomers[k][j]];
        for (int i = 1; i <= nCustomers[k][j]; i++) {
            prob[i - 1] = Math.exp(Gamma.logGamma(alpha0 * sticks[k])
                    - Gamma.logGamma(alpha0 * sticks[k] + nCustomers[k][j]))
                    * Util.stirling(nCustomers[k][j], i)
                    * Math.pow(alpha0 * sticks[k], i);
        }
        nTables[k][j] = CategoricalDistribution.sample(prob) + 1;
    }

    // Construct the posterior probability distribution of stick lengths given the number of tables serving each dish
    private DirichletDistribution stick_params() {
        double[] params = new double[nTables.length + 1];
        for (int i = 0; i < nTables.length; i++) {
            params[i] = Util.sum(nTables[i]) - gamma;
        }
        params[nTables.length] = alpha + nTables.length * gamma;
        return new DirichletDistribution( params );
    }

    public void sample_sticks() {
        DirichletDistribution d = stick_params();
        sticks = Util.delete(d.sample().parameters(), nTables.length, nTables.length + 1);
        for (int i = 0; i < sticks.length; i++) {
            if (sticks[i] == 0.0) {
                sticks = Util.delete(sticks, i, i + 1);
                states = Util.delete(states, i, i + 1);
                nTables = Util.delete(nTables, i, i + 1);
                nCustomers = Util.delete(nCustomers, i, i + 1);
                i--;
            }
        }
    }

    private interface Hyperparameter {
        double likelihood();
        double[] value();
        void change();
        void set( double[] value );
    }

    // Generic function for doing Metropolis sampling for hyperparameters
    private void metropolisUpdate(Hyperparameter hp) {
        double[] cValue = hp.value();
        double cLogLik = hp.likelihood(); // likelihood of current value
        hp.change();
        double pLogLik = hp.likelihood(); // likelhood of proposed value
        double r = Math.exp(pLogLik - cLogLik);
        if (Distribution.rng.nextDouble() >= r) {
            hp.set(cValue);
        }
    }

    protected void sampleBeta(final double var) {
        metropolisUpdate( new Hyperparameter() {
            public double likelihood() { return logLik() - beta; }
            public double[] value() { return new double[]{ beta }; }
            public void change() {
                double pBeta = beta + Distribution.rng.nextGaussian() * var;
                if ( pBeta > 0 ) beta = pBeta;
            }
            public void set( double[] value ) { beta = value[0]; }
        } );
    }

    protected void sampleTopHyperparams(final double var) {
        metropolisUpdate( new Hyperparameter() {
            public double likelihood() {
                return stick_params().logProbability( Util.append( sticks, 1 - Util.sum(sticks) ) )
                        - alpha + Math.log(alpha); // Likelihood, exponential prior, Hastings correction for lognormal proposal
            }

            public double[] value() { return new double[]{ alpha, gamma }; }

            public void change() {
                alpha = Math.exp( Math.log(alpha) + Distribution.rng.nextGaussian() * var ); // lognormal proposal
                double pGamma = gamma + 0.1 * Distribution.rng.nextGaussian() * var;
                if ( pGamma >= 0 && pGamma < 1 ) gamma = pGamma;
            }

            public void set( double[] value ) {
                alpha = value[0];
                gamma = value[1];
            }
        } );
    }

    protected void sampleBottomHyperparams(final double var) {
        metropolisUpdate( new Hyperparameter() {
            public double likelihood() {
                double logLik = 0;
                for ( int i = 0; i < nsymb; i++ ) {
                    logLik += logLikTables(i);
                }
                return logLik - alpha0 + Math.log(alpha0);
            }

            public double[] value() { return new double[]{ alpha0, gamma0 }; }

            public void change() {
                alpha0 = Math.exp( Math.log(alpha0) + Distribution.rng.nextGaussian() * var );
                double pGamma = gamma0 + 0.1 * Distribution.rng.nextGaussian() * var;
                if ( pGamma >= 0 && pGamma < 1 ) gamma0 = pGamma;
            }

            public void set( double[] value ) {
                alpha0 = value[0];
                gamma0 = value[1];
            }
        } );
    }

    public void sample(int[][] data, int[][] test) {
        Object[] os = Util.randArray(dMatrix.keySet());
        for (Object o : os) {
            // Gibbs sample a single transition
            Tuple t = (Tuple) o;
            if (dMatrix.containsKey(t)) {
                int state = dMatrix.get(t);
                int currIdx = -1;
                int newState = gensym.sample();
                double logProbNewState = newLogLik(t, newState, data);
                double[] logProb = new double[states.length + 1];
                int[][] n2 = Util.copy(nCustomers);
                int[][] m2 = Util.copy(nTables);
                int totalTables = 0;
                for (int i = 0; i < nTables.length; i++) {
                    if (states[i] == state) {
                        n2[i][t.get(1)]--;
                        if (n2[i][t.get(1)] < m2[i][t.get(1)]) {
                            m2[i][t.get(1)] = n2[i][t.get(1)];
                        }
                        currIdx = i;
                    }
                    totalTables += m2[i][t.get(1)];
                }
                for (int i = 0; i < logProb.length - 1; i++) {
                    int n_ij = n2[i][t.get(1)];
                    int m_ij = m2[i][t.get(1)];
                    logProb[i] = Math.log(n_ij - gamma0 * m_ij
                            + (alpha0 + gamma0 * totalTables) * sticks[i])
                            + newLogLik(t, states[i], data);
                }
                while (logProb.length < states.length + 1) { // Since calling newLogLike can add new states, continue assigning t to different states until we don't add any new ones.
                    double[] newProb = new double[states.length + 1];
                    System.arraycopy(logProb, 0, newProb, 0, logProb.length - 1);
                    for (int i = logProb.length - 1; i < newProb.length - 1; i++) {
                        newProb[i] = Math.log(alpha0 + gamma0 * totalTables) + Math.log(sticks[i]) + newLogLik(t, states[i], data);
                    }
                    logProb = newProb;
                }
                double b_u = 1 - Util.sum(sticks);
                logProb[logProb.length - 1] = Math.log(alpha0 + gamma0 * totalTables) + Math.log(b_u) + logProbNewState;

                // Avoid numerical underflow
                double max = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < logProb.length; i++) {
                    if (logProb[i] > max) {
                        max = logProb[i];
                    }
                }
                double[] prob = new double[logProb.length];
                for (int i = 0; i < logProb.length; i++) {
                    prob[i] = Math.exp(logProb[i] - max);
                }

                // Assign new state to transition
                int idx = CategoricalDistribution.sample(prob);
                nCustomers[currIdx][t.get(1)]--;
                if (idx == states.length) {
                    dMatrix.put(t, newState);
                    addState(b_u, newState, t.get(1));
                } else {
                    dMatrix.put(t, states[idx]);
                }
                nCustomers[idx][t.get(1)]++;

                // Clear unused states
                count(data);
                clearStates();

                // Sample number of tables in lower level restaurants
                for (int k = 0; k < nTables.length; k++) {
                    for (int j = 0; j < nsymb; j++) {
                        if (nCustomers[k][j] == 0) {
                            nTables[k][j] = 0;
                        } else if (nCustomers[k][j] == 1) {
                            nTables[k][j] = 1;
                        } else {
                            sample_tables(j, k);
                        }
                    }
                }
                if (Distribution.rng.nextDouble() < .01) {
                    // Sample stick lengths for top-level restaurant
                    sample_sticks();
                }
                if (Distribution.rng.nextDouble() < .1) {
                    sampleTopHyperparams(1.0);
                }
                if (Distribution.rng.nextDouble() < .1) {
                    sampleBottomHyperparams(0.5);
                }
                sampleBeta(1.0);
                count(data);
                clearStates();
                System.out.println(sticks.length + ": " + logLik() + ", " + PDIA.averageScore(score(test)) + ", b " + beta + " g " + gamma + " a " + alpha + " g0 " + gamma0 + " a0 " + alpha0 );
            }
        }
    }

    private void clearStates() {
        HashSet<Tuple> keysToDiscard = new HashSet();
        for (Tuple key : dMatrix.keySet()) {
            int[] counts = cMatrix.get(key.get(0));
            if (counts == null || counts[key.get(1)] == 0) {
                keysToDiscard.add(key);
            }
        }

        HashMap<Integer, Integer> stateIndex = new HashMap<Integer, Integer>();
        for (int i = 0; i < states.length; i++) {
            stateIndex.put(states[i], i);
        }
        for (Tuple t : keysToDiscard) {
            Integer next = dMatrix.get(t);
            Integer idx = stateIndex.get(next);
            nCustomers[idx][t.get(1)]--;
            dMatrix.remove(t);
        }

        for (int idx = 0; idx < states.length; idx++) {
            if (Util.sum(nCustomers[idx]) == 0) {
                states = Util.delete(states, idx, idx + 1);
                sticks = Util.delete(sticks, idx, idx + 1);
                nTables = Util.delete(nTables, idx, idx + 1);
                nCustomers = Util.delete(nCustomers, idx, idx + 1);
                idx--;
            }
        }
    }

    public double newLogLik(Tuple t, Integer state, int[][] data) {
        dMatrix.put(t, state);
        count(data);
        return logLik();
    }

    private void addState(double b_u, int state, int symbol) {
        DirichletDistribution betaDist = new DirichletDistribution(new double[]{1 - gamma, alpha + gamma * states.length});
        double b = betaDist.sample().probability(0);
        states = Util.append(states, state);
        sticks = Util.append(sticks, b_u * b);
        nTables = Util.append(nTables, new int[nsymb]);
        nCustomers = Util.append(nCustomers, new int[nsymb]);
        nTables[states.length - 1][symbol]++;
    }

    public double[][] score(int[][] data) {
        double[][] scores = new double[data.length][];
        int line = -1;
        int pos = 0;
        for (Tuple t : new PDIASequence(this, data)) {
            if (t.get(0) == 0) {
                line ++;
                pos = 0;
                scores[line] = new double[data[line].length];
            }
            int[] counts = cMatrix.get(t.get(0));
            if (counts == null) {
                counts = new int[nsymb];
                cMatrix.put(t.get(0),counts);
            }

            int totalCount = Util.sum(counts);
            scores[line][pos++] = ((counts[t.get(1)] + beta / nsymb) / (totalCount + beta));
            counts[t.get(1)]++;
        }
        return scores;
    }

    public static double averageScore(double[][] score) {
        double totalScore = 0.0;
        int totalLength = 0;
        for (int i = 0; i < score.length; i++) {
            totalScore  += Util.sum(Util.log(score[i]));
            totalLength += score[i].length;
        }
        return totalScore / totalLength / Math.log(2);
    }

    public boolean check() {
        HashMap<Integer,Integer> stateIndex = new HashMap<Integer,Integer>();
        for (int i = 0; i < states.length; i++) {
            stateIndex.put(states[i],i);
        }
        int[][] n = new int[states.length][nsymb];
        for( Tuple t : dMatrix.keySet() ) {
            int state = dMatrix.get(t);
            int idx = stateIndex.get(state);
            int symb = t.get(1);
            n[ idx ][ symb ] ++ ;
        }
        for (int i = 0; i < states.length; i++) {
            if (!Arrays.equals(nCustomers[i], n[i])) {
                return false;
            }
        }
        return true;
    }

    private class PDIASequence implements Iterable<Tuple>, Iterator<Tuple> {
        private int[][] data;
        private PDIA p;
        private int line;
        private int pos;
        private int state;

        private PDIASequence(PDIA p, int[][] data) {
            this.p = p;
            this.data = data;
            line = 0;
            pos = 0;
            state = 0;
        }

        @Override
        public Iterator<Tuple> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return data.length != 0 && (line < data.length - 1 || (line == data.length - 1 && pos < data[line].length));
        }

        @Override
        public Tuple next() {
            Tuple t = new Tuple(state, data[line][pos]);
            if (pos == data[line].length - 1) {
                pos = 0;
                line++;
                state = 0;
            } else {
                pos++;
                state = p.transitionAndAdd(t);
            }
            return t;
        }

        @Override
        public void remove() {
        }
    }

    public static void main(String[] args) {
        HashMap<Integer, Integer> alphabet = new HashMap<Integer, Integer>();
        try {
            //int[][] train = Util.loadText("/Users/davidpfau/Documents/Wood Group/pdia_git/data/aiw.train", alphabet);
            //int[][] test  = Util.loadText("/Users/davidpfau/Documents/Wood Group/pdia_git/data/aiw.test",  alphabet);
            int[][] train = Util.loadText("/hpc/stats/users/dbp2112/PDIA/2011-06/data/aiw.train", alphabet);
            int[][] test  = Util.loadText("/hpc/stats/users/dbp2112/PDIA/2011-06/data/aiw.test",  alphabet);
            //int[][] data = Util.loadText("/Users/davidpfau/Documents/Wood Group/pdia_git/data/even.dat",alphabet);
            //int[][] train = {data[0]};
            //int[][] test  = {data[1]};
            PDIA p = new PDIA(alphabet.size());
            p.count(train);
            for (int i = 0; i < 1000; i++) {
                p.sample(train,test);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}