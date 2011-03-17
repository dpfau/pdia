package edu.columbia.stat.wood.pdia;

import fdhpyp.MutableDouble;
import fdhpyp.MutableInteger;
import fdhpyp.Restaurant;
import fdhpyp.RestaurantFranchise;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.math.special.Gamma;

public class PDIA
  implements Serializable
{
  public RestaurantFranchise rf;
  public HashMap<StateSymbolPair, Integer> dMatrix;
  public HashMap<Integer, int[]> cMatrix;
  public int nSymbols;
  public MutableDouble beta;
  public int[][] symbols;
  public static Random RNG;

  public PDIA(int nSymbols, int[][] symbols)
  {
    this.symbols = symbols;
    this.rf = new RestaurantFranchise(1);
    this.nSymbols = nSymbols;
    this.dMatrix = new HashMap();
    this.beta = new MutableDouble(10.0D);
    fillCMatrix();
  }

  public int states() {
    return this.cMatrix.size();
  }

  public void fillCMatrix()
  {
    this.cMatrix = new HashMap();
    int[] context = new int[1];

    for (int i = 0; i < this.symbols.length; i++) {
      int state = 0;
      for (int j = 0; j < this.symbols[i].length; j++) {
        int[] counts = (int[])this.cMatrix.get(Integer.valueOf(state));

        if (counts == null) {
          counts = new int[this.nSymbols];
          this.cMatrix.put(Integer.valueOf(state), counts);
        }

        counts[this.symbols[i][j]] += 1;

        StateSymbolPair ssp = new StateSymbolPair(state, this.symbols[i][j]);
        Integer nextState = (Integer)this.dMatrix.get(ssp);

        if (nextState == null) {
          context[0] = this.symbols[i][j];
          nextState = Integer.valueOf(this.rf.generate(context));
          this.rf.seat(nextState.intValue(), context);
          this.dMatrix.put(ssp, nextState);
        }

        state = nextState.intValue();
      }
    }
  }

  public int nextState()
  {
    int state = 0;
    for (int j = 0; j < this.symbols[(this.symbols.length - 1)].length; j++) {
      StateSymbolPair ssp = new StateSymbolPair(state, this.symbols[(this.symbols.length - 1)][j]);
      Integer nextState = (Integer)this.dMatrix.get(ssp);

      assert (nextState != null);

      state = nextState.intValue();
    }

    return state;
  }

  public double jointScore() {
    return logLik() + this.rf.logLik();
  }

  public double logLik()
  {
    double logLik = 0.0D;
    double logGammaBeta = Gamma.logGamma(this.beta.doubleVal());
    double betaOverN = this.beta.doubleVal() / this.nSymbols;
    double logGammaBetaOverN = Gamma.logGamma(betaOverN);

    for (Integer state : this.cMatrix.keySet()) {
      int[] counts = (int[])this.cMatrix.get(state);

      logLik += logGammaBeta;

      int rowSum = 0;
      for (int count : counts) {
        if (count != 0) {
          logLik += Gamma.logGamma(betaOverN + count);
          logLik -= logGammaBetaOverN;
          rowSum += count;
        }
      }

      logLik -= Gamma.logGamma(this.beta.doubleVal() + rowSum);
    }

    return logLik;
  }

  public void sample() {
    sampleD();
    this.rf.sample();
    sampleBeta(1.0D);
  }

  public void sampleBeta(double proposalSTD)
  {
    double logLik = logLik();
    double currentBeta = this.beta.doubleVal();

    double proposal = currentBeta + RNG.nextGaussian() * proposalSTD;
    if (proposal <= 0.0D) {
      return;
    }
    this.beta.set(proposal);
    double pLogLik = logLik();
    double r = Math.exp(pLogLik - logLik - proposal + currentBeta);
    boolean accept = RNG.nextDouble() < r;
    if (!accept)
      this.beta.set(currentBeta);
  }

  public void sampleD()
  {
    StateSymbolPair[] randomSSPArray = randomSSPArray();
    for (StateSymbolPair ssp : randomSSPArray) {
      if (this.dMatrix.get(ssp) != null) {
        sampleD(ssp);
      }
      fixDMatrix();
    }
  }

  public void sampleD(StateSymbolPair ssp)
  {
    int[] context = new int[1];
    double logLik = logLik();
    Integer currentType = (Integer)this.dMatrix.get(ssp);
    assert (currentType != null);

    context[0] = ssp.symbol;
    this.rf.unseat(currentType.intValue(), context);
    Integer proposedType = Integer.valueOf(this.rf.generate(context));
    this.rf.seat(currentType.intValue(), context);
    this.dMatrix.put(ssp, proposedType);

    fillCMatrix();
    double pLogLik = logLik();

    double r = Math.exp(pLogLik - logLik);
    boolean accept = RNG.nextDouble() < r;

    if (accept) {
      this.rf.unseat(currentType.intValue(), context);
      this.rf.seat(proposedType.intValue(), context);
    } else {
      this.dMatrix.put(ssp, currentType);
    }
  }

  public void fixDMatrix()
  {
    fillCMatrix();
    HashSet<StateSymbolPair> keysToDiscard = new HashSet();

    for (StateSymbolPair ssp : this.dMatrix.keySet()) {
      int[] counts = (int[])this.cMatrix.get(Integer.valueOf(ssp.state));
      if ((counts == null) || (counts[ssp.symbol] == 0)) {
        keysToDiscard.add(ssp);
      }
    }

    int[] context = new int[1];
    for (StateSymbolPair ssp : keysToDiscard) {
      context[0] = ssp.symbol;
      this.rf.unseat(((Integer)this.dMatrix.get(ssp)).intValue(), context);
      this.dMatrix.remove(ssp);
    }
  }

  public double[] score(int[][] testSymbols, int initialState)
  {
    int totalLength = 0;
    for (int i = 0; i < testSymbols.length; i++) {
      totalLength += testSymbols[i].length;
    }

    double[] score = new double[totalLength];
    int[] context = new int[1];

    int index = 0;
    for (int i = 0; i < testSymbols.length; i++)
    {
      int state;
      if (i == 0)
        state = initialState;
      else {
        state = 0;
      }
      for (int j = 0; j < testSymbols[i].length; j++) {
        int[] counts = (int[])this.cMatrix.get(Integer.valueOf(state));
        if (counts == null) {
          counts = new int[this.nSymbols];
          this.cMatrix.put(Integer.valueOf(state), counts);
        }

        int totalCount = 0;
        for (int c : counts) {
          totalCount += c;
        }

        score[(index++)] = ((counts[testSymbols[i][j]] + this.beta.doubleVal() / this.nSymbols) / (totalCount + this.beta.doubleVal()));

        counts[testSymbols[i][j]] += 1;

        StateSymbolPair ssp = new StateSymbolPair(state, testSymbols[i][j]);
        Integer nextState = (Integer)this.dMatrix.get(ssp);

        if (nextState == null) {
          context[0] = testSymbols[i][j];
          nextState = Integer.valueOf(this.rf.generate(context));
          this.rf.seat(nextState.intValue(), context);
          this.dMatrix.put(ssp, nextState);
        }

        state = nextState.intValue();
      }
    }

    return score;
  }

  public void check()
  {
    HashMap dCustomerCounts = new HashMap();
    int[] context = new int[1];

    for (StateSymbolPair ssp : this.dMatrix.keySet()) {
      context[0] = ssp.symbol;

      HashMap typeCountMap = (HashMap)dCustomerCounts.get(Integer.valueOf(context[0]));
      if (typeCountMap == null) {
        typeCountMap = new HashMap();
        dCustomerCounts.put(Integer.valueOf(context[0]), typeCountMap);
      }

      Integer tKey = (Integer)this.dMatrix.get(ssp);
      MutableInteger count = (MutableInteger)typeCountMap.get(tKey);

      if (count == null) {
        count = new MutableInteger(0);
        typeCountMap.put(tKey, count);
      }

      count.increment();
    }

    int[] tCounts = new int[2];
    for (int i = 0; i < this.nSymbols; i++) {
      context[0] = i;

      for (Integer type : ((HashMap<Integer,Integer>)dCustomerCounts.get(Integer.valueOf(context[0]))).keySet()) {
        this.rf.get(context).getCounts(type.intValue(), tCounts);

        int c = tCounts[0];
        int cc = ((MutableInteger)((HashMap)dCustomerCounts.get(Integer.valueOf(context[0]))).get(type)).intVal();

        assert (tCounts[0] == ((MutableInteger)((HashMap)dCustomerCounts.get(Integer.valueOf(context[0]))).get(type)).intVal());
      }
    }
  }

  private StateSymbolPair[] randomSSPArray()
  {
    int[] randomOrder = sampleWOReplacement(this.dMatrix.size());
    StateSymbolPair[] randomSSPArray = new StateSymbolPair[this.dMatrix.size()];
    int ind = 0;

    for (StateSymbolPair c : this.dMatrix.keySet()) {
      randomSSPArray[randomOrder[(ind++)]] = new StateSymbolPair(c.state, c.symbol);
    }

    return randomSSPArray;
  }

  private int[] sampleWOReplacement(int n)
  {
    HashSet<Integer> set = new HashSet(n);

    for (int i = 0; i < n; i++) {
      set.add(Integer.valueOf(i));
    }

    int[] randomOrder = new int[n];
    int s = set.size();
    while (s > 0) {
      double rand = RNG.nextDouble();
      double cuSum = 0.0D;
      for (Integer i : set) {
        cuSum += 1.0D / s;
        if (cuSum > rand) {
          randomOrder[(n - s)] = i.intValue();
          set.remove(i);
          break;
        }
      }

      s = set.size();
    }

    return randomOrder;
  }

  static
  {
    RNG = new Random(0L);
  }
}