/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdiastick;

import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.math.special.Gamma;

/**
 *
 * @author davidpfau
 */
public class PDIA {

    private HashMap<Integer, int[]> cMatrix;
    private HashMap<Tuple, Integer> dMatrix = new HashMap<Tuple, Integer>();
    private int nsymb;
    public double gamma = 1;
    public double alpha0 = 1;
    public double beta = 1;
    private int[] states = new int[0];
    private int[][] nTables = new int[0][]; // number of tables serving a dish in a restaurant in HDP sampling scheme
    private int[][] nCustomers = new int[0][]; // number of customers in each restaurant eating a dish
    private double[] sticks = new double[0]; // explicit stick lengths for top-level DP
    private GenSym gensym = new GenSym();
    private DirichletDistribution betaDist = new DirichletDistribution(new double[]{1, gamma});

    public PDIA(int nsymb) {
        this.nsymb = nsymb;
    }

    public void count(int[][] data) {
        cMatrix = new HashMap<Integer, int[]>();
        for (int i = 0; i < data.length; i++) {
            Integer state = 0;
            for (int j = 0; j < data[i].length; j++) {
                int[] cts = cMatrix.get(state);
                if (cts == null) {
                    cts = new int[nsymb];
                    cMatrix.put(state, cts);
                }
                cts[data[i][j]]++;

                Tuple tuple = new Tuple(state, data[i][j]);
                state = dMatrix.get(tuple);
                if (state == null) {
                    double[] probs = new double[states.length + 1];
                    double b_u = 1 - Util.sum(sticks);
                    for (int t = 0; t < states.length; t++) {
                        probs[t] = nCustomers[t][data[i][j]] + alpha0 * sticks[t];
                    }
                    probs[states.length] = alpha0 * b_u;
                    int idx = CategoricalDistribution.sample(probs);
                    if (idx == states.length) {
                        state = gensym.sample();
                        addState(b_u, state, data[i][j]);
                    } else {
                        state = states[idx];
                    }
                    dMatrix.put(tuple, state);
                    nCustomers[idx][data[i][j]]++;
                }
            }
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

    public void sample_sticks() {
        double[] params = new double[nTables.length + 1];
        for (int i = 0; i < nTables.length; i++) {
            params[i] = Util.sum(nTables[i]);
        }
        params[nTables.length] = gamma;
        DirichletDistribution d = new DirichletDistribution(params);
        sticks = Util.delete(d.sample().parameters(), params.length - 1, params.length);
    }

    public void sample(int[][] data) {
        Object[] os = Util.randArray(dMatrix.keySet());
        for (Object o : os) {
            Tuple t = (Tuple) o;
            // Gibbs sample a single transition
            if (dMatrix.containsKey(t)) {
                int state = dMatrix.get(t);
                int currIdx = -1;
                double[] logProb = new double[states.length + 1];
                int[][] n2 = Util.copy(nCustomers);
                for (int i = 0; i < logProb.length - 1; i++) {
                    int n_ij = n2[i][t.get(1)];
                    if (states[i] == state) {
                        n_ij--;
                        currIdx = i;
                    }
                    logProb[i] = Math.log(n_ij + alpha0 * sticks[i]) + newLogLik(t, states[i], data);
                }
                while (logProb.length < states.length + 1) { // Since calling newLogLike can add new states, continue assigning t to different states until we don't add any new ones.
                    double[] newProb = new double[states.length + 1];
                    System.arraycopy(logProb, 0, newProb, 0, logProb.length - 1);
                    for (int i = logProb.length; i < newProb.length - 1; i++) {
                        newProb[i] = Math.log(alpha0 * sticks[i]) + newLogLik(t, states[i], data);
                    }
                    logProb = newProb;
                }
                double b_u = 1 - Util.sum(sticks);
                logProb[logProb.length - 1] = Math.log(alpha0 * b_u) + newLogLik(t, gensym.sample(), data);

                for (int i = 0; i < logProb.length; i++ ) { // Avoid numerical underflow
                    logProb[i] -= logProb[0];
                }
                int idx = CategoricalDistribution.sample(Util.exp(logProb));
                nCustomers[currIdx][t.get(1)]--;
                if (idx == states.length) {
                    addState(b_u, dMatrix.get(t), t.get(1));
                } else {
                    dMatrix.put(t, states[idx]);
                }
                nCustomers[idx][t.get(1)]++;

                // Clear unused states
                if (idx != states.length - 1) {
                    count(data);
                }
                clearStates();
            }
            System.out.println(sticks.length);
        }

        // Sample number of tables in lower level restaurants
        for (int k = 0; k < nTables.length; k++) {
            for (int j = 0; j < nsymb; j++) {
                if (nCustomers[k][j] > 0) {
                    assert nTables[k][j] <= nCustomers[k][j] : "More tables than customers!";
                    sample_tables(j, k);
                }
            }
        }
        // Sample stick lengths for top-level restaurant
        sample_sticks();
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
            int idx = stateIndex.get(next);
            nCustomers[idx][t.get(1)]--;
            if (nCustomers[idx][t.get(1)] == 0) {
                nTables[idx][t.get(1)] = 0;
            }
            dMatrix.remove(t);
        }

        for (int idx = 0; idx < states.length; idx++) {
            if (Util.sum(nTables[idx]) == 0) {
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
        double b = betaDist.sample().probability(0);
        states = Util.append(states, state);
        sticks = Util.append(sticks, b_u * b);
        nTables = Util.append(nTables, new int[nsymb]);
        nCustomers = Util.append(nCustomers, new int[nsymb]);
        nTables[states.length - 1][symbol]++;
    }

    public static void main(String[] args) {
        HashMap<Integer, Integer> alphabet = new HashMap<Integer, Integer>();
        try {
            int[][] data = Util.loadText("/Users/davidpfau/Documents/Wood Group/pdia_git/data/aiw_full.train", alphabet);
            PDIA p = new PDIA(alphabet.size());
            p.count(data);
            for (int i = 0; i < p.sticks.length; i++) {
                System.out.println(p.sticks[i] + ", " + Util.sum(p.nTables[i]));
            }
            for (int i = 0; i < p.states.length; i++) {
                System.out.println(p.states[i]);
            }
            p.sample(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
