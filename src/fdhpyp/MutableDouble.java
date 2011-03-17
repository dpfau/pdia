package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;

public class MutableDouble
  implements Serializable
{
  private double d = 0.0D;
  private static final long serialVersionUID = 1L;

  public MutableDouble(double d)
  {
    this.d = d;
  }

  public double set(double newD) {
    return this.d = newD;
  }

  public double doubleVal() {
    return this.d;
  }

  public double times(double dd) {
    return this.d *= dd;
  }

  public void print() {
    System.out.println(this.d);
  }

  public static void main(String[] args) {
    MutableDouble md = new MutableDouble(1.0D);
    md.print();

    md.set(30.0D);
    md.print();
  }
}