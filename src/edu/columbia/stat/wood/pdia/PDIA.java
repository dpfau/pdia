package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.Util;
import edu.columbia.stat.wood.hpyp.MutableDouble;
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
    public int[][] data;
    public static Random RNG = new Random(0L);

    public PDIA(int nSymbols, int[][] data) {
        this.data = data;
        rf = new RestaurantFranchise(1);
        this.nSymbols = nSymbols;
        dMatrix = new HashMap();
        beta = 10.0;
        count();
    }

    public int states() {
        return cMatrix.size();
    }

    public void count() {
        cMatrix = new HashMap();
        for (Pair p : new PDIASequence(this,data)) {
            int[] counts = cMatrix.get(p.state);
            if (counts == null) {
                counts = new int[nSymbols];
                cMatrix.put(p.state, counts);
            }
            counts[p.symbol] ++;
        }
    }

    public double jointScore() {
        return logLik() + rf.logLik();
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

    public void sample() {
        sampleD();
        rf.sample();
        sampleBeta(1.0);
    }

    public void sampleBeta(double proposalSTD) {
        double logLik = logLik();
        double currentBeta = beta;

        double proposal = currentBeta + RNG.nextGaussian() * proposalSTD;
        if (proposal <= 0) {
            return;
        }
        beta = proposal;
        double pLogLik = logLik();
        double r = Math.exp(pLogLik - logLik - proposal + currentBeta);
        if (RNG.nextDouble() >= r) {
            beta = currentBeta;
        }
    }

    public void sampleD() {
        Pair[] randomSSPArray = randomPairArray();
        for (Pair p : randomSSPArray) {
            if (dMatrix.get(p) != null) {
                sampleD(p);
            }
            fixDMatrix();
        }
    }

    public void sampleD(Pair p) {
        int[] context = new int[]{p.symbol};
        double logLik = logLik();
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        rf.seat(currentType, context);
        dMatrix.put(p, proposedType);

        count();
        double pLogLik = logLik();

        if (RNG.nextDouble() < Math.exp(pLogLik - logLik)) {
            rf.unseat(currentType, context);
            rf.seat(proposedType, context);
        } else {
            dMatrix.put(p, currentType);
        }
    }

    public void fixDMatrix() {
        count();
        HashSet<Pair> keysToDiscard = new HashSet();

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

    public double[] score(int[][] testSymbols, int initialState) {
        int totalLength = 0;
        for (int i = 0; i < testSymbols.length; i++) {
            totalLength += testSymbols[i].length;
        }

        double[] score = new double[totalLength];

        int index = 0;
        for (Pair p : new PDIASequence(this, initialState, testSymbols)) {
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

    public void check() {
        HashMap dCustomerCounts = new HashMap();
        int[] context = new int[1];

        for (Pair p : dMatrix.keySet()) {
            context[0] = p.symbol;

            HashMap typeCountMap = (HashMap) dCustomerCounts.get(p.symbol);
            if (typeCountMap == null) {
                typeCountMap = new HashMap();
                dCustomerCounts.put(p.symbol, typeCountMap);
            }

            Integer tKey = dMatrix.get(p);
            MutableInteger count = (MutableInteger) typeCountMap.get(tKey);

            if (count == null) {
                count = new MutableInteger(0);
                typeCountMap.put(tKey, count);
            }

            count.increment();
        }

        int[] tCounts = new int[2];
        for (int i = 0; i < nSymbols; i++) {
            HashMap<Integer, Integer> hm = (HashMap<Integer,Integer>) dCustomerCounts.get(i);
            for (Integer type : hm.keySet()) {
                rf.get(context).getCounts(type.intValue(), tCounts);
                assert (tCounts[0] == ((MutableInteger) ((HashMap) hm).get(type)).intVal());
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
