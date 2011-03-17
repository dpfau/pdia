package edu.columbia.stat.wood.pdia;

import java.io.Serializable;

public class StateSymbolPair
  implements Serializable
{
  public int state;
  public int symbol;
  private static final long serialVersionUID = 1L;

  public StateSymbolPair(int state, int symbol)
  {
    this.state = state;
    this.symbol = symbol;
  }

  public boolean equals(Object obj)
  {
    if (getClass() != obj.getClass())
      return false;
    if (obj == null) {
      return false;
    }
    return (((StateSymbolPair)obj).state == this.state) && (((StateSymbolPair)obj).symbol == this.symbol);
  }

  public int hashCode()
  {
    int hash = 7;
    hash = 29 * hash + this.state;
    hash = 29 * hash + this.symbol;
    return hash;
  }
}