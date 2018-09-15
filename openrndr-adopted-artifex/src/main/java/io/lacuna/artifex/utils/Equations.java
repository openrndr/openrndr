package io.lacuna.artifex.utils;

import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.MACHINE_EPSILON;
import static java.lang.StrictMath.*;

/**
 * @author ztellman
 */
public class Equations {

  private static final double DISCRIMINANT_EPSILON = 1e-10;
  private static final double SOLUTION_EPSILON = 1e-8;

  // adapted from https://github.com/paperjs/paper.js/blob/develop/src/util/Numerical.js

  private static double[] trim(double[] acc, int len) {
    if (len == acc.length) {
      return acc;
    } else if (len == 0) {
      return new double[0];
    } else {
      double[] result = new double[len];
      System.arraycopy(acc, 0, result, 0, len);
      return result;
    }
  }

  private static double[] split(double n) {
    double
      x = n * 134217729,
      y = n - x,
      hi = y + x,
      lo = n - hi;

    return new double[]{hi, lo};
  }

  private static double discriminant(final double a, final double b, final double c) {
    double
      D = (b * b) - (a * c),
      E = (b * b) + (a * c);

    if (abs(D) * 3 < E) {
      double[]
        ad = split(a),
        bd = split(b),
        cd = split(c);

      double
        p = b * b,
        dp = ((bd[0] * bd[0]) - p + (2 * bd[0] * bd[1])) + (bd[1] * bd[1]),
        q = a * c,
        dq = ((ad[0] * cd[0]) - q + (ad[0] * cd[1]) + (ad[1] * cd[0])) + (ad[1] * cd[1]);

      D = (p - q) + (dp - dq);
    }

    return D;
  }

  public static int solveLinear(double a, double b, double[] acc) {
    if (abs(a) < EPSILON) {
      return 0;
    } else {
      acc[0] = -b / a;
      return 1;
    }
  }

  public static double[] solveLinear(double a, double b) {
    double[] acc = new double[1];
    return trim(acc, solveLinear(a, b, acc));
  }

  public static int solveQuadratic(double a, double b, double c, double[] acc) {

    if (abs(a) < EPSILON) {
      return solveLinear(b, c, acc);
    }

    b *= -0.5;

    double k = Scalars.normalizationFactor(a, b, c);
    a *= k;
    b *= k;
    c *= k;
    double D = discriminant(a, b, c);

    if (D >= -DISCRIMINANT_EPSILON) {
      double
        Q = D < 0 ? 0 : sqrt(D),
        R = b + (b < 0 ? -Q : Q);

      if (R == 0) {
        acc[0] = c / a;
        acc[1] = -c / a;
      } else {
        acc[0] = R / a;
        acc[1] = c / R;
      }

      int writeIdx = 0;
      for (int readIdx = 0; readIdx < 2; readIdx++) {
        double x = acc[readIdx];

        // since the tolerance for the discriminant is fairly large, we check our work
        double y = (a * x * x) + (-2 * b * x) + c;
        if (abs(y) < SOLUTION_EPSILON) {
          acc[writeIdx++] = x;
        }
      }
      return writeIdx;

    } else {
      return 0;
    }
  }

  public static double[] solveQuadratic(double a, double b, double c) {
    double[] acc = new double[2];
    return trim(acc, solveQuadratic(a, b, c, acc));
  }

  public static int solveCubic(double a, double b, double c, double d, double[] acc) {

    double k = Scalars.normalizationFactor(a, b, c, d);
    a *= k;
    b *= k;
    c *= k;
    d *= k;

    double x, b1, c2, qd, q;
    if (abs(a) < EPSILON) {
      return solveQuadratic(b, c, d, acc);

    } else if (abs(d) < EPSILON) {
      b1 = b;
      c2 = c;
      x = 0;

    } else {

      x = -(b / a) / 3;
      b1 = (a * x) + b;
      c2 = (b1 * x) + c;
      qd = (((a * x) + b1) * x) + c2;
      q = (c2 * x) + d;

      double
        t = q / a,
        r = pow(abs(t), 1 / 3.0),
        s = t < 0 ? -1 : 1,
        td = -qd / a,
        rd = td > 0 ? 1.324717957244746 * max(r, sqrt(td)) : r,
        x0 = x - (s * rd);

      if (x0 != x) {
        do {
          x = x0;
          b1 = (a * x) + b;
          c2 = (b1 * x) + c;
          qd = (((a * x) + b1) * x) + c2;
          q = (c2 * x) + d;

          x0 = qd == 0 ? x : x - (q / (qd / (1 + MACHINE_EPSILON)));
        } while (s * x0 > s * x);

        if (abs(a) * x * x > abs(d / x)) {
          c2 = -d / x;
          b1 = (c2 - c) / x;
        }
      }
    }

    int solutions = solveQuadratic(a, b1, c2, acc);

    for (int i = 0; i < solutions; i++) {
      if (acc[i] == x) {
        return solutions;
      }
    }

    double y = (a * x * x * x) + (b * x * x) + (c * x) + d;
    if (abs(y) < SOLUTION_EPSILON) {
      acc[solutions++] = x;
    }

    return solutions;
  }

  public static double[] solveCubic(double a, double b, double c, double d) {
    double[] acc = new double[3];
    return trim(acc, solveCubic(a, b, c, d, acc));
  }

}
