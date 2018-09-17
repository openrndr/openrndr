package io.lacuna.artifex.utils;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.getExponent;
import static java.lang.StrictMath.max;

/**
 * @author ztellman
 */
public class Scalars {

  public static final double MACHINE_EPSILON = Math.ulp(1.0);
  public static final double EPSILON = 1e-14;

  public static boolean equals(double a, double b, double epsilon) {
    return Math.abs(a - b) < epsilon;
  }

  public static boolean angleEquals(double t0, double t1, double epsilon) {
    if (t1 < t0) {
      double tmp = t1;
      t1 = t0;
      t0 = tmp;
    }

    boolean result = (t1 - t0) < epsilon;
    if (!result) {
      t1 -= Math.PI * 2;
      result = (t0 - t1) < epsilon;
    }
    return result;
  }

  public static double normalize(double a, double b, double n) {
    return (n - a) / (b - a);
  }

  public static double lerp(double a, double b, double t) {
    return a + ((b - a) * t);
  }

  public static boolean inside(double min, double n, double max) {
    return min < n && n < max;
  }

  public static double clamp(double min, double n, double max) {
    if (n <= min) {
      return min;
    } else if (n >= max) {
      return max;
    } else {
      return n;
    }
  }

  public static double normalizationFactor(double a, double b, double c, double d) {
    double exponent = getExponent(max(max(a, b), max(c, d)));
    return (exponent < -8 || exponent > 8) ? Math.pow(2, -exponent) : 1;
  }

  public static double normalizationFactor(double a, double b, double c) {
    double exponent = getExponent(max(max(a, b), c));
    return (exponent < -8 || exponent > 8) ? Math.pow(2, -exponent) : 1;
  }

  public static double normalizationFactor(double a, double b) {
    double exponent = getExponent(max(a, b));
    return (exponent < -8 || exponent > 8) ? Math.pow(2, -exponent) : 1;
  }

  public static double max(double a, double b) {
    return a < b ? b : a;
  }

  public static double max(double a, double b, double c) {
    return max(a, max(b, c));
  }
}
