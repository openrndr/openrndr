package io.lacuna.artifex;

import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.bifurcan.LinearList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.lerp;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Equations.solveCubic;
import static io.lacuna.artifex.utils.Equations.solveQuadratic;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.inside;
import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class Bezier2 {

  public static Curve2 curve(Vec2 p0, Vec2 p1) {
    return Line2.line(p0, p1);
  }

  public static Curve2 curve(Vec2 p0, Vec2 p1, Vec2 p2) {
    return new QuadraticBezier2(p0, p1, p2);
  }

  public static Curve2 curve(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
    return new CubicBezier2(p0, p1, p2, p3);
  }

  private static double sign(double n) {
    double s = Math.signum(n);
    return s == 0 ? -1 : s;
  }

  private static <V extends Curve2> void subdivide(List<Vec2> result, V c, ToDoubleFunction<V> error, double maxError) {
    if (error.applyAsDouble(c) <= maxError) {
      result.add(c.start());
    } else {
      Curve2[] split = c.split(0.5);
      subdivide(result, (V) split[0], error, maxError);
      subdivide(result, (V) split[1], error, maxError);
    }
  }

  public static double signedDistance(Vec2 p, Vec2 a, Vec2 b) {
    Vec2 d = b.sub(a);
    return (cross(p, d) + cross(b, a)) / d.length();
  }

  public static class QuadraticBezier2 implements Curve2 {

    public final Vec2 p0, p1, p2;

    private boolean noInflections = false;

    private QuadraticBezier2(Vec2 p0, Vec2 p1, Vec2 p2, boolean noInflections) {
      this(p0, p1, p2);
      this.noInflections = noInflections;
    }

    QuadraticBezier2(Vec2 p0, Vec2 p1, Vec2 p2) {
      this.p0 = p0;
      this.p1 = p1;
      this.p2 = p2;
    }

    @Override
    public Vec2 start() {
      return p0;
    }

    @Override
    public Vec2 end() {
      return p2;
    }

    @Override
    public boolean isFlat(double epsilon) {
      return abs(signedDistance(p1, p0, p2) / 2) < epsilon;
    }

    @Override
    public double length() {
      return 0;
    }

    @Override
    public double signedArea() {
      return ((p2.x * (p0.y - (2 * p1.y)))
        + (2 * p1.x * (p2.y - p0.y))
        + (p0.x * ((2 * p1.y) + p2.y))) / 6;
    }

    @Override
    public Vec2 position(double t) {
      if (t == 0) {
        return start();
      } else if (t == 1) {
        return end();
      }

      double mt = 1 - t;

      // (1 - t)^2 * p0 + 2t(1 - t) * p1 + t^2 * p2;
      return p0.mul(mt * mt)
        .add(p1.mul(2 * t * mt))
        .add(p2.mul(t * t));
    }

    @Override
    public Vec2 direction(double t) {
      double mt = 1 - t;

      // 2(1 - t) * (p1 - p0) + 2t * (p2 - p1)
      return p1.sub(p0).mul(2 * mt)
        .add(p2.sub(p1).mul(2 * t));
    }

    @Override
    public QuadraticBezier2 endpoints(Vec2 start, Vec2 end) {
      Vec2 ad = p1.sub(p0);
      Vec2 bd = p1.sub(p2);

      double dx = end.x - start.x;
      double dy = end.y - start.y;
      double det = bd.x * ad.y - bd.y * ad.x;
      double u = (dy * bd.x - dx * bd.y) / det;

      return new QuadraticBezier2(start, start.add(ad.mul(u)), end, noInflections);
    }

    @Override
    public Curve2[] split(double t) {
      if (t <= 0 || t >= 1) {
        throw new IllegalArgumentException("t must be within (0,1)");
      }

      Vec2
        e = lerp(p0, p1, t),
        f = lerp(p1, p2, t),
        g = position(t);

      return new Curve2[]{
        new QuadraticBezier2(p0, e, g, noInflections),
        new QuadraticBezier2(g, f, p2, noInflections)};
    }

    @Override
    public Vec2[] subdivide(double error) {
      ArrayList<Vec2> points = new ArrayList<>();
      Bezier2.subdivide(points, this, b -> Vec.lerp(b.p0, b.p2, 0.5).sub(b.p1).lengthSquared(), error * error);
      points.add(end());

      return points.toArray(new Vec2[points.size()]);
    }

    @Override
    public double nearestPoint(Vec2 p) {

      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 qc = p2.sub(p);
      Vec2 ac = p2.sub(p0);
      Vec2 br = p0.add(p2).sub(p1).sub(p1);

      double minDistance = sign(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = sign(cross(bc, qc)) * qc.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = max(1, dot(p.sub(p1), bc) / dot(bc, bc));
      }

      double a = dot(br, br);
      double b = 3 * dot(ab, br);
      double c = (2 * dot(ab, ab)) + dot(qa, br);
      double d = dot(qa, ab);
      double[] ts = solveCubic(a, b, c, d);

      for (double t : ts) {
        if (t > 0 && t < 1) {
          Vec2 endpoint = position(t);
          distance = sign(cross(ac, endpoint.sub(p))) * endpoint.sub(p).length();
          if (abs(distance) < abs(minDistance)) {
            minDistance = distance;
            param = t;
          }
        }
      }

      return param;
    }

    @Override
    public Curve2 transform(Matrix3 m) {
      return new QuadraticBezier2(p0.transform(m), p1.transform(m), p2.transform(m));
    }

    @Override
    public QuadraticBezier2 reverse() {
      return new QuadraticBezier2(p2, p1, p0, noInflections);
    }

    @Override
    public Box2 bounds() {
      if (noInflections) {
        return box(p0, p2);
      } else {
        return Curve2.super.bounds();
      }
    }

    @Override
    public double[] inflections() {
      if (noInflections) {
        return new double[0];
      }

      final double epsilon = 1e-10;

      Vec2 div = p0.sub(p1.mul(2)).add(p2);
      if (div.equals(Vec2.ORIGIN)) {
        noInflections = true;
        return new double[0];
      } else {
        Vec2 v = p0.sub(p1).div(div);
        boolean x = inside(epsilon, v.x, 1 - epsilon);
        boolean y = inside(epsilon, v.y, 1 - epsilon);
        if (x && y) {
          return new double[]{v.x, v.y};
        } else if (x ^ y) {
          return new double[]{x ? v.x : v.y};
        } else {
          noInflections = true;
          return new double[0];
        }
      }
    }

    @Override
    public String toString() {
      return "p0=" + p0 + ", p1=" + p1 + ", p2=" + p2;
    }

  }

  public static class CubicBezier2 implements Curve2 {

    private static final int SEARCH_STARTS = 4;
    private static final int SEARCH_STEPS = 8;

    public final Vec2 p0, p1, p2, p3;

    private boolean noInflections = false;
    private Box2 bounds;

    private CubicBezier2(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3, boolean noInflections) {
      this(p0, p1, p2, p3);
      this.noInflections = noInflections;
    }

    CubicBezier2(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
      this.p0 = p0;
      this.p1 = p1;
      this.p2 = p2;
      this.p3 = p3;
    }

    @Override
    public Vec2 position(double t) {
      if (t == 0) {
        return start();
      } else if (t == 1) {
        return end();
      }

      double mt = 1 - t;
      double mt2 = mt * mt;
      double t2 = t * t;

      // (1 - t)^3 * p0 + 3t(1 - t)^2 * p1 + 3(1 - t)t^2 * p2 + t^3 * p3;
      return p0.mul(mt2 * mt)
        .add(p1.mul(3 * mt2 * t))
        .add(p2.mul(3 * mt * t2))
        .add(p3.mul(t2 * t));
    }

    @Override
    public Vec2 direction(double t) {
      double mt = 1 - t;

      // 3(1 - t)^2 * (p1 - p0) + 6(1 - t)t * (p2 - p1) + 3t^2 * (p3 - p2)
      return p1.sub(p0).mul(3 * mt * mt)
        .add(p2.sub(p1).mul(6 * mt * t))
        .add(p3.sub(p2).mul(3 * t * t));
    }

    @Override
    public double signedArea() {
      return ((p3.x * (-p0.y - (3 * p1.y) - (6 * p2.y)))
        - (3 * p2.x * (p0.y + p1.y - (2 * p3.y)))
        + (3 * p1.x * ((-2 * p0.y) + p2.y + p3.y))
        + (p0.x * ((6 * p1.y) + (3 * p2.y) + p3.y))) / 20;
    }

    @Override
    public double length() {
      return 0;
    }

    @Override
    public boolean isFlat(double epsilon) {
      double d1 = signedDistance(p1, p0, p3);
      double d2 = signedDistance(p2, p0, p3);

      // from Sederberg 1990
      double k = d1 * d2 < 0 ? 4 / 9.0 : 3 / 4.0;
      return abs(d1 * k) < epsilon && abs(d2 * k) < epsilon;
    }

    @Override
    public CubicBezier2 endpoints(Vec2 start, Vec2 end) {
      return new CubicBezier2(start, p1.add(start.sub(p0)), p2.add(end.sub(p3)), end, noInflections);
    }

    @Override
    public Vec2 start() {
      return p0;
    }

    @Override
    public Vec2 end() {
      return p3;
    }

    @Override
    public Curve2[] split(double t) {
      if (t <= 0 || t >= 1) {
        throw new IllegalArgumentException("t must be within (0,1)");
      }

      Vec2
        e = lerp(p0, p1, t),
        f = lerp(p1, p2, t),
        g = lerp(p2, p3, t),
        h = lerp(e, f, t),
        j = lerp(f, g, t),
        k = position(t);

      return new Curve2[]{
        new CubicBezier2(p0, e, h, k, noInflections),
        new CubicBezier2(k, j, g, p3, noInflections)};
    }

    @Override
    public Vec2[] subdivide(double error) {
      List<Vec2> points = new ArrayList<>();
      Bezier2.subdivide(points, this,
        b -> Math.max(
          Vec.lerp(b.p0, b.p3, 1.0 / 3).sub(b.p1).lengthSquared(),
          Vec.lerp(b.p0, b.p3, 2.0 / 3).sub(b.p2).lengthSquared()),
        error * error);
      points.add(end());

      return points.toArray(new Vec2[points.size()]);
    }

    @Override
    /**
     * This quintic solver is adapted from https://github.com/Chlumsky/msdfgen, which is available under the MIT
     * license.
     */
    public double nearestPoint(Vec2 p) {
      Vec2 qa = p0.sub(p);
      Vec2 ab = p1.sub(p0);
      Vec2 bc = p2.sub(p1);
      Vec2 cd = p3.sub(p2);
      Vec2 qd = p3.sub(p);
      Vec2 br = bc.sub(ab);
      Vec2 as = cd.sub(bc).sub(br);

      double minDistance = sign(cross(ab, qa)) * qa.length();
      double param = -dot(qa, ab) / dot(ab, ab);

      double distance = sign(cross(cd, qd)) * qd.length();
      if (abs(distance) < abs(minDistance)) {
        minDistance = distance;
        param = max(1, dot(p.sub(p2), cd) / dot(cd, cd));
      }

      for (int i = 0; i < SEARCH_STARTS; i++) {
        double t = (double) i / (SEARCH_STARTS - 1);
        for (int step = 0; ; step++) {
          Vec2 qpt = position(t).sub(p);
          distance = sign(cross(direction(t), qpt)) * qpt.length();
          if (abs(distance) < abs(minDistance)) {
            minDistance = distance;
            param = t;
          }

          if (step == SEARCH_STEPS) {
            break;
          }

          Vec2 d1 = as.mul(3 * t * t).add(br.mul(6 * t)).add(ab.mul(3));
          Vec2 d2 = as.mul(6 * t).add(br.mul(6));
          double dt = dot(qpt, d1) / (dot(d1, d1) + dot(qpt, d2));
          if (abs(dt) < EPSILON) {
            break;
          }

          t -= dt;
          if (t < 0 || t > 1) {
            break;
          }
        }
      }

      return param;
    }

    @Override
    public Curve2 transform(Matrix3 m) {
      return new CubicBezier2(p0.transform(m), p1.transform(m), p2.transform(m), p3.transform(m));
    }

    @Override
    public CubicBezier2 reverse() {
      return new CubicBezier2(p3, p2, p1, p0, noInflections);
    }

    @Override
    public Box2 bounds() {
      if (noInflections) {
        return box(p0, p3);
      } else {
        return Curve2.super.bounds();
      }
    }

    @Override
    public double[] inflections() {

      if (noInflections) {
        return new double[0];
      }

      // there are pathological shapes that require less precision here
      final double epsilon = 1e-7;

      Vec2 a0 = p1.sub(p0);
      Vec2 a1 = p2.sub(p1).sub(a0).mul(2);
      Vec2 a2 = p3.sub(p2.mul(3)).add(p1.mul(3)).sub(p0);

      double[] s1 = solveQuadratic(a2.x, a1.x, a0.x);
      double[] s2 = solveQuadratic(a2.y, a1.y, a0.y);

      DoubleAccumulator acc = new DoubleAccumulator();
      for (double n : s1) if (inside(epsilon, n, 1 - epsilon)) acc.add(n);
      for (double n : s2) if (inside(epsilon, n, 1 - epsilon)) acc.add(n);

      noInflections = acc.size() == 0;

      return acc.toArray();
    }

    @Override
    public String toString() {
      return "p0=" + p0 + ", p1=" + p1 + ", p2=" + p2 + ", p3=" + p3;
    }

    /// approximate as quadratic

    private double error() {
      return p3.sub(p2.mul(3)).add(p2.mul(3)).sub(p0).lengthSquared() / 4;
    }

    private CubicBezier2 subdivide(double t0, double t1) {
      Vec2 p0 = position(t0),
        p3 = position(t1),
        p1 = p0.add(direction(t0)),
        p2 = p3.sub(direction(t1));

      return new CubicBezier2(p0, p1, p2, p3);
    }

    private QuadraticBezier2 approximate() {
      return new QuadraticBezier2(p0, p1.mul(0.75).add(p2.mul(0.75)).sub(p0.mul(-0.25)).sub(p3.mul(-0.25)), p3);
    }

    /**
     * @param error the maximum distance between the reference cubic curve and the returned quadratic curves
     * @return an array of one or more quadratic bezier curves
     */
    public QuadraticBezier2[] approximate(double error) {
      double threshold = error * error;

      LinearList<QuadraticBezier2> result = new LinearList<>();
      LinearList<Vec2> intervals = new LinearList<Vec2>().addLast(vec(0, 1));

      while (intervals.size() > 0) {
        Vec2 i = intervals.popLast();
        CubicBezier2 c = subdivide(i.x, i.y);
        if (c.error() <= threshold) {
          result.addLast(c.approximate());
        } else {
          double midpoint = (i.x + i.y) / 2;
          intervals
            .addLast(vec(i.x, midpoint))
            .addLast(vec(midpoint, i.y));
        }
      }

      return result.toArray(QuadraticBezier2[]::new);
    }

  }
}
