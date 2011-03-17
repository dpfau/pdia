package fdhpyp;

import java.io.PrintStream;
import java.io.Serializable;

public class MutableInteger
  implements Serializable
{
  private int i = 0;
  private static final long serialVersionUID = 1L;

  public MutableInteger(int i)
  {
    this.i = i;
  }

  public int set(int newI) {
    return this.i = newI;
  }

  public int intVal() {
    return this.i;
  }

  public int increment() {
    return ++this.i;
  }

  public int decrement() {
    return --this.i;
  }

  public void print() {
    System.out.println(this.i);
  }

  public static void main(String[] args) {
    MutableInteger mi = new MutableInteger(10);
    mi.print();

    System.out.println(mi.increment());
    mi.print();

    System.out.println(mi.decrement());
    mi.print();

    System.out.println(mi.set(15));
    mi.print();
  }
}