package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;

public class Discounts
  implements Serializable
{
  private MutableDouble[] discounts;
  private static final long serialVersionUID = 1L;

  public Discounts(double[] ds)
  {
    this.discounts = new MutableDouble[ds.length];

    for (int i = 0; i < ds.length; i++)
      this.discounts[i] = new MutableDouble(ds[i]);
  }

  public Discounts()
  {
    this(new double[] { 0.5D });
  }

  public MutableDouble get(int d) {
    if (d < this.discounts.length) {
      return this.discounts[d];
    }
    return this.discounts[(this.discounts.length - 1)];
  }

  public int length()
  {
    return this.discounts.length;
  }

  public void print() {
    System.out.print("[" + this.discounts[0].doubleVal());
    for (int i = 1; i < this.discounts.length; i++) {
      System.out.print(", " + this.discounts[i].doubleVal());
    }
    System.out.println("]");
  }
}