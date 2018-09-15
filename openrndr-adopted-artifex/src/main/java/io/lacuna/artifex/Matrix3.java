package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import static io.lacuna.artifex.Vec.vec;

/**
 * @author ztellman
 */
public class Matrix3 {

  public static final Matrix3 IDENTITY = new Matrix3(1, 0, 0, 0, 1, 0, 0, 0, 1);

  private final double[] elements;

  private Matrix3(double m00, double m01, double m02,
                  double m10, double m11, double m12,
                  double m20, double m21, double m22) {
    elements = new double[]{m00, m01, m02, m10, m11, m12, m20, m21, m22};
  }

  private Matrix3(double[] elements) {
    this.elements = elements;
  }

  public static Matrix3 from(Vec2 a, Vec2 b) {
    return new Matrix3(a.x, b.x, 0, a.y, b.y, 0, 0, 0, 1);
  }

  public static Matrix3 from(Vec3 a, Vec3 b) {
    return new Matrix3(a.x, a.x, 0, a.y, b.y, 0, a.z, b.z, 1);
  }

  public static Matrix3 from(Vec3 a, Vec3 b, Vec3 c) {
    return new Matrix3(a.x, b.x, c.x, a.y, b.y, c.y, a.z, b.z, c.z);
  }

  public static Matrix3 translate(double x, double y) {
    return new Matrix3(
      1, 0, x,
      0, 1, y,
      0, 0, 1);
  }

  public static Matrix3 translate(Vec2 v) {
    return translate(v.x, v.y);
  }

  public static Matrix3 scale(double x, double y) {
    return new Matrix3(x, 0, 0, 0, y, 0, 0, 0, 1);
  }

  public static Matrix3 scale(Vec2 v) {
    return scale(v.x, v.y);
  }

  public static Matrix3 scale(double k) {
    return scale(k, k);
  }

  public static Matrix3 rotate(double radians) {
    double c = Math.cos(radians);
    double s = Math.sin(radians);
    return new Matrix3(c, -s, 0, s, c, 0, 0, 0, 1);
  }

  public static Matrix3 mul(Matrix3... matrices) {
    Matrix3 m = matrices[0];
    for (int i = 1; i < matrices.length; i++) {
      m = m.mul(matrices[i]);
    }
    return m;
  }

  public Matrix3 mul(double k) {
    double[] es = new double[9];
    for (int i = 0; i < 9; i++) {
      es[i] = elements[i] * k;
    }
    return new Matrix3(es);
  }

  public double get(int row, int column) {
    return elements[(row * 3) + column];
  }

  public Vec3 row(int row) {
    int idx = row * 2;
    return vec(elements[idx], elements[idx + 1], elements[idx + 2]);
  }

  public Vec3 column(int column) {
    int idx = column;
    return vec(elements[idx], elements[idx + 3], elements[idx + 6]);
  }

  public Matrix3 mul(Matrix3 b) {
    double[] es = new double[9];
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        double n = 0;
        for (int k = 0; k < 3; k++) {
          n += b.get(k, j) * get(i, k);
        }
        es[(i * 3) + j] = n;
      }
    }
    return new Matrix3(es);
  }

  public Matrix3 add(Matrix3 b) {
    double[] es = new double[9];
    for (int i = 0; i < 9; i++) {
      es[i] = elements[i] + b.elements[i];
    }
    return new Matrix3(es);
  }

  public Matrix4 matrix4() {
    return new Matrix4(
      elements[0], elements[1], 0, elements[2],
      elements[3], elements[4], 0, elements[5],
      elements[6], elements[7], elements[8], 0,
      0, 0, 0, 1);
  }

  public Matrix3 transpose() {
    return new Matrix3(
      elements[0], elements[3], elements[6],
      elements[1], elements[4], elements[7],
      elements[2], elements[5], elements[8]);
  }

  public Vec2 transform(Vec2 v) {
    return new Vec2(
      (v.x * elements[0]) + (v.y * elements[1]) + elements[2],
      (v.x * elements[3]) + (v.y * elements[4]) + elements[5]);
  }

  public PrimitiveIterator.OfDouble rowMajor() {
    return new PrimitiveIterator.OfDouble() {
      int idx = 0;

      public boolean hasNext() {
        return idx < 9;
      }

      public double nextDouble() {
        if (idx < 9) {
          return elements[idx++];
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  public PrimitiveIterator.OfDouble columnMajor() {
    return transpose().rowMajor();
  }

  @Override
  public int hashCode() {
    int hash = 0;
    for (double n : elements) {
      hash = (hash * 31) + Hashes.hash(n);
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof Matrix3) {
      Matrix3 m = (Matrix3) obj;
      return Arrays.equals(elements, m.elements);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer();
    rowMajor().forEachRemaining((double n) -> s.append(n).append(", "));
    return s.toString();
  }
}
