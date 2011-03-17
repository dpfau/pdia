package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

public class Restaurant extends TreeMap<Integer, Restaurant>
  implements Serializable
{
  public MutableDouble discount;
  public MutableDouble concentration;
  public TreeMap<Integer, int[]> tableMap;
  public int customers;
  public int tables;
  public Restaurant parent;
  public static int restCount;
  public static final Random RNG;
  private static final long serialVersionUID = 1L;

  public Restaurant(Restaurant parent, MutableDouble discount, MutableDouble concentration)
  {
    this.parent = parent;
    this.discount = discount;
    this.concentration = concentration;
    this.tableMap = new TreeMap();
    this.customers = 0;
    this.tables = 0;
    restCount += 1;
  }

  public Restaurant() {
  }

  public Restaurant getParent() {
    return this.parent;
  }

  public void getCounts(int type, int[] counts)
  {
    int cust = 0;
    int tbls = 0;
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));

    if (tsa != null) {
      for (int c : tsa) {
        cust += c;
      }
      tbls = tsa.length;
    }

    counts[0] = cust;
    counts[1] = tbls;
  }

  public int tables() {
    return this.tables;
  }

  public double predictiveProbability(int type)
  {
    double d = this.discount.doubleVal();
    double c = this.concentration.doubleVal();
    double pp = this.parent.predictiveProbability(type);
    int[] tCounts;
    getCounts(type, tCounts = new int[2]);
    double denom = this.customers + c;

    return (tCounts[0] - tCounts[1] * d + (this.tables * d + c) * pp) / denom;
  }

  public void predictiveProbability(double[] p)
  {
    this.parent.predictiveProbability(p);

    double d = this.discount.doubleVal();
    double c = this.concentration.doubleVal();
    int[] tCounts = new int[2];

    for (int i = 0; i < p.length; i++) {
      p[i] *= (this.tables * d + c) / (this.customers + c);
    }

    for (Integer type : this.tableMap.keySet()) {
      getCounts(type.intValue(), tCounts);
      p[type.intValue()] += (tCounts[0] - tCounts[1] * d) / (this.customers + c);
    }
  }

  public int generate()
  {
    int generatedValue = -1;
    double d = this.discount.doubleVal();
    double c = this.concentration.doubleVal();
    double r = RNG.nextDouble();
    double thisRestCutoff = (this.customers - this.tables * d) / (this.customers + c);
    double cuSum;
    int[] tc;
    if (r < thisRestCutoff) {
      cuSum = 0.0D;
      tc = new int[2];
      for (Integer type : this.tableMap.keySet()) {
        getCounts(type.intValue(), tc);
        cuSum += (tc[0] - tc[1] * d) / (this.customers + c);
        if (cuSum > r) {
          generatedValue = type.intValue();
          break;
        }
      }
    } else {
      generatedValue = this.parent.generate();
    }

    assert (generatedValue > -1);
    return generatedValue;
  }

  public int[] seat(int type)
  {
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));
    boolean seatInParent = true;

    if (tsa == null) {
      this.tableMap.put(Integer.valueOf(type), tsa = new int[] { 1 });
    }
    else
    {
      double d = this.discount.doubleVal();
      double c = this.concentration.doubleVal();
      double pp = this.parent.predictiveProbability(type);
      double rand = RNG.nextDouble();
      double cuSum = 0.0D;
      int[] tCounts;
      getCounts(type, tCounts = new int[2]);
      double totalWeight = tCounts[0] - tCounts[1] * d + (tables() * d + c) * pp;

      for (int i = 0; i < tsa.length; i++) {
        cuSum += (tsa[i] - d) / totalWeight;
        if (cuSum > rand) {
          tsa[i] += 1;
          seatInParent = false;
          break;
        }
      }

      if (cuSum <= rand) {
        int[] nSeatingArrangement = new int[tsa.length + 1];
        System.arraycopy(tsa, 0, nSeatingArrangement, 0, tsa.length);
        nSeatingArrangement[tsa.length] = 1;
        this.tableMap.put(Integer.valueOf(type), nSeatingArrangement);
        tsa = nSeatingArrangement;
      }
    }

    this.customers += 1;
    if (seatInParent) {
      this.tables += 1;
      this.parent.seat(type);
    }

    return tsa;
  }

  public void seatKN(int type)
  {
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));
    boolean seatInParent = true;

    this.customers += 1;
    if (tsa == null) {
      this.tableMap.put(Integer.valueOf(type), new int[] { 1 });
      this.tables += 1;
    } else {
      tsa[0] += 1;
      seatInParent = false;
    }

    if (seatInParent)
      this.parent.seatKN(type);
  }

  public void unseat(int type)
  {
    boolean unseatInParent = true;
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));
    double random = RNG.nextDouble();
    int tc = 0;
    int ind = -1;
    double cuSum = 0.0D;

    for (int tableSize : tsa) {
      tc += tableSize;
    }

    for (int i = 0; i < tsa.length; i++) {
      cuSum += (double)tsa[i] / tc;
      if (cuSum > random) {
        tsa[i] -= 1;
        ind = i;
        break;
      }
    }

    if (ind == -1) {
        System.out.println("hm");
    }

    if (tsa[ind] == 0) {
      if (tsa.length == 1) {
        this.tableMap.remove(Integer.valueOf(type));
      } else {
        int[] nSeatingArrangement = new int[tsa.length - 1];
        System.arraycopy(tsa, 0, nSeatingArrangement, 0, ind);
        System.arraycopy(tsa, ind + 1, nSeatingArrangement, ind, tsa.length - 1 - ind);
        this.tableMap.put(Integer.valueOf(type), nSeatingArrangement);
      }
    }
    else unseatInParent = false;

    this.customers -= 1;
    if (unseatInParent) {
      this.tables -= 1;
      this.parent.unseat(type);
    }
  }

  public void sampleTable(int type, int table)
  {
    double d = this.discount.doubleVal();
    double c = this.concentration.doubleVal();
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));
    int tt = 0;
    int tc = -1;

    for (int i = 0; i < tsa.length; i++) {
      tc += tsa[i];
      if (tsa[i] > 0) {
        tt++;
      }
    }

    int customersToSample = tsa[table];
    for (int iter = 0; iter < customersToSample; iter++) {
      int zeroInd = -1;

      tsa[table] -= 1;
      if (tsa[table] == 0) {
        zeroInd = table;
        this.parent.unseat(type);
        this.tables -= 1;
        tt--;
      }

      double r = RNG.nextDouble();
      double cuSum = 0.0D;
      double pp = this.parent.predictiveProbability(type);
      double totalWeight = tc - tt * d + (this.tables * d + c) * pp;

      for (int i = 0; i < tsa.length; i++) {
        if (tsa[i] > 0)
          cuSum += (tsa[i] - d) / totalWeight;
        else {
          zeroInd = i;
        }
        if (cuSum > r) {
          tsa[i] += 1;
          break;
        }
      }

      if (cuSum <= r) {
        this.tables += 1;
        tt++;
        this.parent.seat(type);
        if (zeroInd > -1) {
          tsa[zeroInd] += 1;
        } else {
          int[] ntsa = new int[tsa.length + 1];
          System.arraycopy(tsa, 0, ntsa, 0, tsa.length);
          ntsa[tsa.length] = 1;
          this.tableMap.put(Integer.valueOf(type), ntsa);
          tsa = ntsa;
        }
      }
    }
  }

  public void sampleCustomer(int type, int table)
  {
    double d = this.discount.doubleVal();
    double c = this.concentration.doubleVal();
    int[] tsa = (int[])this.tableMap.get(Integer.valueOf(type));
    int tt = 0;
    int tc = -1;

    int zeroInd = -1;
    for (int i = 0; i < tsa.length; i++) {
      tc += tsa[i];
      if (tsa[i] > 0)
        tt++;
      else {
        zeroInd = i;
      }
    }

    tsa[table] -= 1;
    if (tsa[table] == 0) {
      this.parent.unseat(type);
      this.tables -= 1;
      tt--;
      zeroInd = table;
    }

    double r = RNG.nextDouble();
    double cuSum = 0.0D;
    double pp = this.parent.predictiveProbability(type);
    double totalWeight = tc - tt * d + (this.tables * d + c) * pp;

    for (int i = 0; i < tsa.length; i++) {
      if (tsa[i] > 0) {
        cuSum += (tsa[i] - d) / totalWeight;
      }
      if (cuSum > r) {
        tsa[i] += 1;
        break;
      }
    }

    if (cuSum <= r) {
      this.tables += 1;
      if (zeroInd > -1) {
        tsa[zeroInd] += 1;
      } else {
        int[] ntsa = new int[tsa.length + 1];
        System.arraycopy(tsa, 0, ntsa, 0, tsa.length);
        ntsa[tsa.length] = 1;
        this.tableMap.put(Integer.valueOf(type), ntsa);
      }
      this.parent.seat(type);
    }
  }

  public void sampleSeatingArrangements()
  {
    int[] typeTable = new int[2];
    RandomCustomer randomCustomer = new RandomCustomer(this.tableMap);
    int b;
    while ((b = randomCustomer.nextCustomer(typeTable)) > -1) {
      sampleCustomer(typeTable[0], typeTable[1]);
    }

    fixZeros();
  }

  private void fixZeros()
  {
    for (Integer type : this.tableMap.keySet()) {
      int[] tsa = (int[])this.tableMap.get(type);

      int numZeros = 0;
      for (int tableSize : tsa) {
        assert (tableSize >= 0);
        if (tableSize == 0) {
          numZeros++;
        }
      }

      if (numZeros > 0) {
        int[] ntsa = new int[tsa.length - numZeros];
        int ind = 0;
        for (int tableSize : tsa) {
          if (tableSize > 0) {
            ntsa[(ind++)] = tableSize;
          }
        }
        this.tableMap.put(type, ntsa);
      }
    }
  }

  public double logLik()
  {
    int c = 0;
    int t = 0;
    double logLik = 0.0D;

    for (int[] tsa : this.tableMap.values()) {
      for (int custs : tsa) {
        logLik += logLikTable(custs, c, t);
        t++;
        c += custs;
      }
    }

    return logLik;
  }

  private double logLikTable(int tableSize, int existingCust, int existingTables)
  {
    double disc = this.discount.doubleVal();
    double conc = this.concentration.doubleVal();
    double logLik = 0.0D;
    double p = (existingTables * disc + conc) / (existingCust + conc);

    logLik += Math.log(p);
    existingCust++;

    for (int i = 1; i < tableSize; i++) {
      p = (i - disc) / (existingCust + conc);
      logLik += Math.log(p);
      existingCust++;
    }

    return logLik;
  }

  public void printTableMap()
  {
    for (Integer i : this.tableMap.keySet()) {
      int[] tsa = (int[])this.tableMap.get(i);
      System.out.print(i + " : [" + tsa[0]);
      for (int j = 1; j < tsa.length; j++) {
        System.out.print(", " + tsa[j]);
      }
      System.out.println("]");
    }
  }

  static
  {
    restCount = 0;
    RNG = new Random(0L);
  }

  private class RandomCustomer
  {
    private int[] randomType;
    private int[] randomTable;
    private int index;

    public RandomCustomer(TreeMap<Integer, int[]> tblMap)
    {
      this.index = 0;
      this.randomType = new int[Restaurant.this.customers];
      this.randomTable = new int[Restaurant.this.customers];

      int[] randomOrder = sampleWOReplacement(Restaurant.this.customers);

      int ind = 0;

      for (Integer type : tblMap.keySet()) {
        int[] tsa = (int[])tblMap.get(type);
        for (int table = 0; table < tsa.length; table++)
          for (int customer = 0; customer < tsa[table]; customer++) {
            this.randomType[randomOrder[ind]] = type.intValue();
            this.randomTable[randomOrder[(ind++)]] = table;
          }
      }
    }

    public int nextCustomer(int[] typeAndTable)
    {
      if (this.index == Restaurant.this.customers) {
        return -1;
      }
      typeAndTable[0] = this.randomType[this.index];
      typeAndTable[1] = this.randomTable[(this.index++)];
      return 0;
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
        double rand = Restaurant.RNG.nextDouble();
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
  }

  /*private class RandomTable
  {
    private int tablesRemaining;
    private HashMap<Integer, int[]> map;

    public RandomTable()
    {
      this.map = new HashMap(tblMap.size());
      this.tablesRemaining = 0;
      for (Integer type : tblMap.keySet()) {
        int[] tsa = (int[])tblMap.get(type);

        if ((tsa.length == 1) && (tsa[0] == 1))
        {
          continue;
        }
        int[] ctsa = new int[tsa.length];
        Arrays.fill(ctsa, 1);

        this.tablesRemaining += tsa.length;
        this.map.put(type, ctsa);
      }
    }

    public int nextTable(int[] typeAndTable) {
      if (this.tablesRemaining == 0) {
        return -1;
      }

      int type = -1;
      int table = -1;
      double cuSum = 0.0D;
      double random = Restaurant.RNG.nextDouble();

      for (Integer t : this.map.keySet()) {
        int[] tables = (int[])this.map.get(t);
        for (table = 0; table < tables.length; table++) {
          cuSum += tables[table] / this.tablesRemaining;
          if (cuSum > random) {
            tables[table] -= 1;
            this.tablesRemaining -= 1;
            type = t.intValue();
            break label146;
          }
        }
      }

      label146: typeAndTable[0] = type;
      typeAndTable[1] = table;

      return 0;
    }
  }*/
}