package fdhpyp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class UniformRestaurant extends Restaurant
  implements Serializable
{
  public int alphabetSize;
  public int customers;
  public double lambda;
  private static final long serialVersionUID = 1L;

  public UniformRestaurant(int alphabetSize)
  {
    this.alphabetSize = alphabetSize;
    this.customers = 0;
  }

  public UniformRestaurant() {
    this.customers = 0;
    this.alphabetSize = -1;
    this.lambda = 0.001D;
  }

  public void set(int cust) {
    this.customers = cust;
  }

  public double predictiveProbability(int type)
  {
    if (this.alphabetSize > -1) {
      return 1.0D / this.alphabetSize;
    }
    return Math.exp(-1.0D * type * this.lambda) - Math.exp(-1.0D * (type + 1) * this.lambda);
  }

  public int[] seat(int type)
  {
    this.customers += 1;
    return null;
  }

  public void seatKN(int type)
  {
    this.customers += 1;
  }

  public void unseat(int type)
  {
    if (this.alphabetSize > -1) {
      this.customers -= 1;
    }
    assert (this.customers > -1);
  }

  public double logLik()
  {
    if (this.alphabetSize > -1) {
      return this.customers * Math.log(1.0D / this.alphabetSize);
    }
    return 0.0D;
  }

  public void predictiveProbability(double[] p)
  {
    if (this.alphabetSize > -1) {
      assert (p.length == this.alphabetSize);
      Arrays.fill(p, 1.0D / this.alphabetSize);
    } else {
      throw new RuntimeException("not supported in the case of infinite base distribution");
    }
  }

  public int generate()
  {
    double r = RNG.nextDouble();

    int ind = -1;
    double cuSum = 0.0D;
    while (cuSum <= r) {
      ind++;
      if (this.alphabetSize > -1) {
        cuSum += 1.0D / this.alphabetSize; continue;
      }
      cuSum += Math.exp(-1.0D * ind * this.lambda) - Math.exp(-1.0D * (ind + 1) * this.lambda);
    }

    return ind;
  }

  public int generateNewType(TreeMap<Integer, int[]> tblMap) {
    assert (this.alphabetSize == -1);

    double totalWeight = 1.0D;
    for (Integer type : tblMap.keySet()) {
      totalWeight -= Math.exp(-1.0D * type.intValue() * this.lambda) - Math.exp(-1.0D * (type.intValue() + 1) * this.lambda);
    }

    double r = RNG.nextDouble();
    int newType = -1;
    double cuSum = 0.0D;
    while (cuSum <= r) {
      newType++;
      if (tblMap.get(Integer.valueOf(newType)) == null) {
        double pp = predictiveProbability(newType);
        cuSum += pp / totalWeight;
      }
    }

    return newType;
  }
}