package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

public class RestaurantFranchise
  implements Serializable
{
  public Restaurant root;
  public Discounts discounts;
  public Concentrations concentrations;
  private int depth;
  private int alphabetSize;
  private int[] currentContext;
  private static final long serialVersionUID = 1L;
  private double[] dLogLik;
  private double[] cLogLik;
  private int num = 0;

  public RestaurantFranchise(int depth, int alphabetSize)
  {
    this.alphabetSize = alphabetSize;
    this.depth = depth;

    this.discounts = new Discounts(new double[] { 0.0D });
    this.concentrations = new Concentrations(new double[] { 10.0D, 10.0D });
    this.root = new Restaurant(new UniformRestaurant(alphabetSize), this.discounts.get(0), this.concentrations.get(0));
  }

  public RestaurantFranchise(int depth) {
    this.depth = depth;

    this.discounts = new Discounts(new double[] { 0.5D, 0.5D });

    this.concentrations = new Concentrations(new double[] { 20.0D, 15.0D });
    this.root = new Restaurant(new UniformRestaurant(), this.discounts.get(0), this.concentrations.get(0));
    this.alphabetSize = -1;
  }

  public double continueSequence(int type)
  {
    double p = seat(type, this.currentContext);
    updateContext(type);
    return Math.log(p);
  }

  public int depth() {
    return this.depth;
  }

  public double seat(int type, int[] context)
  {
    Restaurant r = get(context);
    double p = r.predictiveProbability(type);
    r.seat(type);

    return p;
  }

  public int generate(int[] context) {
    return get(context).generate();
  }

  public void unseat(int type, int[] context) {
    get(context).unseat(type);
  }

  public double predictiveProbability(int type, int[] context) {
    return getDontAdd(context).predictiveProbability(type);
  }

  public HashMap<Integer, MutableDouble> predictiveProbabilityExisingTypes(int[] context)
  {
    HashMap returnMap = new HashMap(this.root.tableMap.size() + 1);
    Restaurant r = getDontAdd(context);
    double cuSum = 0.0D;

    for (Integer type : this.root.tableMap.keySet()) {
      double p = r.predictiveProbability(type.intValue());
      cuSum += p;
      returnMap.put(Integer.valueOf(type.intValue()), new MutableDouble(p));
    }

    returnMap.put(Integer.valueOf(-1), new MutableDouble(1.0D - cuSum));

    return returnMap;
  }

  public int generateNewType()
  {
    UniformRestaurant r = (UniformRestaurant)this.root.getParent();
    return r.generateNewType(this.root.tableMap);
  }

  public double[] predictiveProbability(int[] context) {
    double[] p = new double[this.alphabetSize];
    get(context).predictiveProbability(p);
    return p;
  }

  public void sample() {
    sampleSeating(this.root);
    sampleDiscounts(0.07000000000000001D);
    sampleConcentrations(1.5D);
  }

  private void sampleSeating(Restaurant r)
  {
    for (Restaurant child : r.values()) {
      sampleSeating(child);
    }

    HashSet<Integer> toRemove = new HashSet();

    for (Integer key : r.keySet()) {
      Restaurant c = (Restaurant)r.get(key);
      if ((c.isEmpty()) && (c.tables() == 0)) {
        toRemove.add(key);
      }
    }

    for (Integer key : toRemove) {
      r.remove(key);
    }

    r.sampleSeatingArrangements();
  }

  public void sampleDiscounts(double proposalSTD)
  {
    double[] currentDiscounts = new double[this.discounts.length()];

    logLik();

    double[] currentLogLik = new double[this.discounts.length()];
    System.arraycopy(this.dLogLik, 0, currentLogLik, 0, this.discounts.length());

    for (int i = 0; i < this.discounts.length(); i++) {
      MutableDouble d = this.discounts.get(i);
      currentDiscounts[i] = d.doubleVal();
      double proposal = currentDiscounts[i] + Restaurant.RNG.nextGaussian() * proposalSTD;
      proposal = (proposal >= 1.0D) || (proposal <= 0.0D) ? currentDiscounts[i] : proposal;
      d.set(proposal);
    }

    logLik();

    for (int i = 0; i < this.discounts.length(); i++) {
      double r = Math.exp(this.dLogLik[i] - currentLogLik[i]);
      boolean accept = Restaurant.RNG.nextDouble() < r;
      if (!accept)
        this.discounts.get(i).set(currentDiscounts[i]);
    }
  }

  public void sampleConcentrations(double proposalSTD)
  {
    double[] currentConcentrations = new double[this.concentrations.length()];

    logLik();

    double[] currentLogLik = new double[this.concentrations.length()];
    System.arraycopy(this.cLogLik, 0, currentLogLik, 0, this.concentrations.length());

    for (int i = 0; i < this.concentrations.length(); i++) {
      MutableDouble c = this.concentrations.get(i);
      currentConcentrations[i] = c.doubleVal();
      double proposal = currentConcentrations[i] + Restaurant.RNG.nextGaussian() * proposalSTD;
      proposal = proposal <= 0.0D ? currentConcentrations[i] : proposal;
      c.set(proposal);
    }

    logLik();

    for (int i = 0; i < this.concentrations.length(); i++) {
      double r = Math.exp(this.cLogLik[i] - this.concentrations.get(i).doubleVal() - currentLogLik[i] + currentConcentrations[i]);
      boolean accept = Restaurant.RNG.nextDouble() < r;
      if (!accept)
        this.concentrations.get(i).set(currentConcentrations[i]);
    }
  }

  public double logLik()
  {
    double logLik = this.root.getParent().logLik();
    this.dLogLik = new double[this.discounts.length()];
    this.cLogLik = new double[this.concentrations.length()];

    logLik(this.root, 0);
    for (double d : this.dLogLik) {
      logLik += d;
    }

    return logLik;
  }

  public void logLik(Restaurant r, int d)
  {
    int dIndex = d < this.discounts.length() ? d : this.discounts.length() - 1;
    int cIndex = d < this.concentrations.length() ? d : this.concentrations.length() - 1;

    double logLik = r.logLik();

    this.dLogLik[dIndex] += logLik;
    this.cLogLik[cIndex] += logLik;

    for (Restaurant child : r.values())
      logLik(child, d + 1);
  }

  public Restaurant getDontAdd(int[] context)
  {
    if ((context == null) || (context.length == 0)) {
      return this.root;
    }

    int ci = context.length - 1;
    int d = 0;
    Restaurant current = this.root;
    Restaurant child = null;

    while ((d < this.depth) && (ci > -1)) {
      child = (Restaurant)current.get(Integer.valueOf(context[ci]));
      if (child == null) {
        break;
      }
      current = child;
      ci--;
      d++;
    }

    return current;
  }

  public Restaurant get(int[] context)
  {
    if ((context == null) || (context.length == 0)) {
      return this.root;
    }

    int ci = context.length - 1;
    int d = 0;
    Restaurant current = this.root;
    Restaurant child = null;

    while ((d < this.depth) && (ci > -1)) {
      child = (Restaurant)current.get(Integer.valueOf(context[ci]));
      if (child == null) {
        child = new Restaurant(current, this.discounts.get(d + 1), this.concentrations.get(d + 1));
        current.put(Integer.valueOf(context[ci]), child);
      }
      current = child;
      ci--;
      d++;
    }

    return current;
  }

  private void updateContext(int obs) {
    if (this.depth != 0) if (this.currentContext == null) {
        this.currentContext = new int[] { obs };
      } else if (this.currentContext.length < this.depth) {
        int[] newContext = new int[this.currentContext.length + 1];
        System.arraycopy(this.currentContext, 0, newContext, 0, this.currentContext.length);
        newContext[this.currentContext.length] = obs;
        this.currentContext = newContext;
      } else {
        for (int i = 0; i < this.depth - 1; i++) {
          this.currentContext[i] = this.currentContext[(i + 1)];
        }
        this.currentContext[(this.depth - 1)] = obs;
      }
  }

  public void checkConsistency()
  {
    checkConsistency(this.root);
  }

  public void checkConsistency(Restaurant r)
  {
    if (!r.isEmpty())
    {
      int childrenTables = 0;

      for (Restaurant child : r.values()) {
        childrenTables += child.tables();
        checkConsistency(child);
      }

      if (r.customers != childrenTables)
        System.out.println(this.num++);
    }
  }

  public void print()
  {
    print(this.root, new int[0]);
  }

  public void print(Restaurant r, int[] rKey)
  {
    for (Integer k : r.keySet()) {
      int[] cKey = new int[rKey.length + 1];
      System.arraycopy(rKey, 0, cKey, 1, rKey.length);
      cKey[0] = k.intValue();
      print((Restaurant)r.get(k), cKey);
    }

    System.out.print("[");
    for (int keyVar : rKey) {
      System.out.print(keyVar + " ");
    }
    System.out.println("]");

    r.printTableMap();
    System.out.println();
  }
}