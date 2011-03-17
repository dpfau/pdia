package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;

public class Concentrations
  implements Serializable
{
  private MutableDouble[] concentrations;
  private static final long serialVersionUID = 1L;

  public Concentrations(double[] cs)
  {
    this.concentrations = new MutableDouble[cs.length];
    for (int i = 0; i < cs.length; i++)
      this.concentrations[i] = new MutableDouble(cs[i]);
  }

  public Concentrations()
  {
    this(new double[] { 0.001D });
  }

  public MutableDouble get(int d) {
    if (d < this.concentrations.length) {
      return this.concentrations[d];
    }
    return this.concentrations[(this.concentrations.length - 1)];
  }

  public int length()
  {
    return this.concentrations.length;
  }

  public void print() {
    System.out.print("[" + this.concentrations[0].doubleVal());
    for (int i = 1; i < this.concentrations.length; i++) {
      System.out.print(", " + this.concentrations[i].doubleVal());
    }
    System.out.println("]");
  }
}