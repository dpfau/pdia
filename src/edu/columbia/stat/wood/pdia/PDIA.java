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
        dMatrix = new HashMap();
        beta = 10.0;
    }

    public PDIASequence run(int[][]... data) {
        return new PDIASequence(this,0,data);
    }

    public PDIASequence run(int init, int[][]... data) {
        return new PDIASequence(this,init,data);
    }

    public static PDIASample sample(int[] nSymbols, int[][]... data) {
        return new PDIASample(nSymbols[0],data);
    }

    public static PDIA[] sample(int burnIn, int interval, int samples, int[] nSymbols, int[][]... data) {
        PDIA[] ps = new PDIA[samples];
        int i = 0;
        for (PDIA p : PDIA.sample(nSymbols,data)) {
            if (i >= burnIn && (i-burnIn) % interval == 0) {
                ps[(i-burnIn)/interval] = Util.copy(p);
            }
            i++;
            if (i == burnIn + interval*samples) break;
        }
        return ps;
    }

    public static double logLossPerToken(PDIA[] ps, int[][]... data) {
        return 0.0; 
    }

    public int states() {
        return cMatrix.size();
    }

    public void count(int[][]... data) {
        cMatrix = new HashMap();
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

    public void sampleD(int[][]... data) {
        for (Pair p : randomPairArray()) {
            if (dMatrix.get(p) != null) {
                sampleD(p,data);
            }
            fixDMatrix();
        }
    }

    public void sampleD(Pair p, int[][]... data) {
        int[] context = new int[]{p.symbol};
        double cLogLik = logLike;
        Integer currentType = dMatrix.get(p);
        assert (currentType != null);

        rf.unseat(currentType, context);
        Integer proposedType = rf.generate(context);
        dMatrix.put(p, proposedType);

        Object oldCounts = cMatrix.clone();
        count(data);
        double pLogLik = logLik();

        if (Math.log(RNG.nextDouble()) < pLogLik - cLogLik) { // accept
            rf.seat(proposedType, context);
        } else { // reject
            cMatrix = (HashMap<Integer,int[]>) oldCounts;
            logLike = cLogLik;
            rf.seat(currentType, context);
            dMatrix.put(p, currentType);
        }
    }

    public void fixDMatrix() {
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
