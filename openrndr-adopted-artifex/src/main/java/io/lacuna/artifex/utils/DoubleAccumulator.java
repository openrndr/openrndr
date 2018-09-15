package io.lacuna.artifex.utils;

import static java.lang.System.arraycopy;

/**
 * @author ztellman
 */
public class DoubleAccumulator {

  private double[] values = new double[2];
  private int index = 0;

  private void expand() {
    double[] values = new double[this.values.length << 1];
    arraycopy(this.values, 0, values, 0, this.values.length);
    this.values = values;
  }

  public DoubleAccumulator add(double[] ns) {
    while (index > this.values.length - ns.length) {
      expand();
    }

    System.arraycopy(ns, 0, values, index, ns.length);
    index += ns.length;

    return this;
  }

  public DoubleAccumulator add(double n) {
    if (index > this.values.length - 1) {
      expand();
    }

    values[index++] = n;

    return this;
  }

  public DoubleAccumulator add(double a, double b) {
    if (index > this.values.length - 2) {
      expand();
    }

    values[index++] = a;
    values[index++] = b;

    return this;
  }

  public void pop(int num) {
    index -= num;
  }

  public void clear() {
    index = 0;
  }

  public int size() {
    return index;
  }

  public double get(int index) {
    return values[index];
  }

  public double last() {
    return values[index - 1];
  }

  public void set(int index, double n) {
    values[index] = n;
  }

  public double[] toArray() {
    double[] result = new double[size()];
    arraycopy(values, 0, result, 0, size());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("[");
    for (int i = 0; i < index; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(values[i]);
    }
    b.append("]");

    return b.toString();
  }
}
