package io.lacuna.artifex.utils;

import io.lacuna.artifex.Bezier2.CubicBezier2;
import io.lacuna.artifex.Bezier2.QuadraticBezier2;
import io.lacuna.artifex.*;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Interval.interval;
import static io.lacuna.artifex.Line2.line;
import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class Intersections {

  // utilities

  public static final double FAT_LINE_PARAMETRIC_RESOLUTION = 1e-7;
  public static final double FAT_LINE_SPATIAL_EPSILON = 1e-6;

  public static final double PARAMETRIC_EPSILON = 1e-6;
  public static final double SPATIAL_EPSILON = 1e-10;

  public static final int MAX_CUBIC_CUBIC_INTERSECTIONS = 9;
  
  public static final Box2 PARAMETRIC_BOUNDS = box(vec(0, 0), vec(1, 1));

  // utilities

  // shockingly, the extra work done by Math.min/max to handle the weirder corners of the floating point spec adds
  // noticeable overhead
  public static double min(double a, double b) {
    return a < b ? a : b;
  }

  public static double max(double a, double b) {
    return a > b ? a : b;
  }

  // subdivision (slow, but as close to a reference implementation as exists)

  public static class CurveInterval {
    public final Curve2 curve;
    public final boolean isFlat;
    public final double tLo, tHi;
    public final Vec2 pLo, pHi;

    public CurveInterval(Curve2 curve, double tLo, double tHi, Vec2 pLo, Vec2 pHi) {
      this.curve = curve;
      this.tLo = tLo;
      this.tHi = tHi;
      this.pLo = pLo;
      this.pHi = pHi;
      this.isFlat = Vec.equals(pLo, pHi, SPATIAL_EPSILON)
        || (tHi - tLo) < PARAMETRIC_EPSILON
        || curve.range(tLo, tHi).isFlat(SPATIAL_EPSILON);
    }

    public Box2 bounds() {
      return box(pLo, pHi);
    }

    public boolean intersects(CurveInterval c) {
      return bounds().expand(SPATIAL_EPSILON).intersects(c.bounds());
    }

    public CurveInterval[] split() {
      if (isFlat) {
        return new CurveInterval[]{this};
      } else {
        double tMid = (tLo + tHi) / 2;
        Vec2 pMid = curve.position(tMid);
        return new CurveInterval[]{
          new CurveInterval(curve, tLo, tMid, pLo, pMid),
          new CurveInterval(curve, tMid, tHi, pMid, pHi)
        };
      }
    }

    public static CurveInterval[] from(Curve2 c) {
      double[] ts = c.inflections();
      Arrays.sort(ts);

      if (ts.length == 0) {
        return new CurveInterval[]{new CurveInterval(c, 0, 1, c.start(), c.end())};
      } else {
        CurveInterval[] ls = new CurveInterval[ts.length + 1];
        for (int i = 0; i < ls.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == ls.length - 1 ? 1 : ts[i];
          ls[i] = new CurveInterval(c, lo, hi, c.position(lo), c.position(hi));
        }

        return ls;
      }
    }

    public void intersections(CurveInterval c, IList<Vec2> acc) {
      for (Vec2 i : lineLine(line(pLo, pHi), line(c.pLo, c.pHi))) {
        if (PARAMETRIC_BOUNDS.expand(PARAMETRIC_EPSILON).contains(i)) {
          acc.addLast(Vec.lerp(vec(tLo, c.tLo), vec(tHi, c.tHi), i));
        }
      }
    }

    @Override
    public String toString() {
      return "[" + tLo + ", " + tHi + "]";
    }
  }

  public static Vec2[] subdivisionCurveCurve(Curve2 a, Curve2 b) {

    LinearList<CurveInterval> queue = new LinearList<>();
    CurveInterval[] as = CurveInterval.from(a);
    CurveInterval[] bs = CurveInterval.from(b);
    for (CurveInterval ap : as) {
      for (CurveInterval bp : bs) {
        queue.addLast(ap).addLast(bp);
      }
    }

    boolean collinearCheck = false;
    int iterations = 0;
    LinearList<Vec2> acc = new LinearList<>();
    while (queue.size() > 0) {

      if (iterations > 32 && !collinearCheck) {
        collinearCheck = true;
        Vec2[] is = collinearIntersection(a, b);
        if (isCollinear(a, b, is)) {
          return is;
        }
      }

      iterations++;
      CurveInterval cb = queue.popLast();
      CurveInterval ca = queue.popLast();

      if (!ca.intersects(cb)) {
        continue;
      }

      if (ca.isFlat && cb.isFlat) {
        ca.intersections(cb, acc);
      } else {
        for (CurveInterval ap : ca.split()) {
          for (CurveInterval bp : cb.split()) {
            queue.addLast(ap).addLast(bp);
          }
        }
      }
    }

    return normalize(acc.toArray(Vec2[]::new));
  }

  // fat lines (faster, but more temperamental)

  // This is adapted from Sederberg's "Curve Intersection Using Bezier Clipping", but the algorithm as described
  // gets unstable when one curve is clipped small enough, causing it to over-clip the other curve, causing us to miss
  // intersection points.  To address this, we quantize the curve sub-ranges using FAT_LINE_PARAMETRIC_RESOLUTION,
  // preventing them from getting too small, and expand the width of our clipping regions by FAT_LINE_SPATIAL_EPSILON.

  public static double signedDistance(Vec2 p, Vec2 a, Vec2 b) {
    Vec2 d = b.sub(a);
    return (cross(p, d) + cross(b, a)) / d.length();
  }

  public static Interval fatLineWidth(Curve2 c) {
    if (c instanceof Line2) {
      return interval(0, 0);

    } else if (c instanceof QuadraticBezier2) {
      QuadraticBezier2 b = (QuadraticBezier2) c;
      return interval(0, signedDistance(b.p1, b.p0, b.p2) / 2);

    } else if (c instanceof CubicBezier2) {
      CubicBezier2 b = (CubicBezier2) c;
      double
        d1 = signedDistance(b.p1, b.p0, b.p3),
        d2 = signedDistance(b.p2, b.p0, b.p3),
        k = d1 * d2 < 0 ? 4 / 9.0 : 3 / 4.0;
      return interval(min(0, min(d1, d2)) * k, max(0, max(d1, d2)) * k);

    } else {
      throw new IllegalStateException();
    }
  }

  public static Vec2[] convexHull(Vec2 a, Vec2 b, QuadraticBezier2 c) {
    Vec2
      p0 = vec(0, signedDistance(c.p0, a, b)),
      p1 = vec(1 / 2.0, signedDistance(c.p1, a, b)),
      p2 = vec(1, signedDistance(c.p2, a, b));

    return new Vec2[]{p0, p1, p2, p0};
  }

  public static Vec2[] convexHull(Vec2 a, Vec2 b, CubicBezier2 c) {
    Vec2
      p0 = vec(0, signedDistance(c.p0, a, b)),
      p1 = vec(1 / 3.0, signedDistance(c.p1, a, b)),
      p2 = vec(2 / 3.0, signedDistance(c.p2, a, b)),
      p3 = vec(1, signedDistance(c.p3, a, b));

    double d1 = signedDistance(p1, p0, p3);
    double d2 = signedDistance(p2, p0, p3);
    if (d1 * d2 < 0) {
      return new Vec2[]{p0, p1, p3, p2, p0};
    } else {
      double k = d1 / d2;
      if (k >= 2) {
        return new Vec2[]{p0, p1, p3, p0};
      } else if (k <= 0.5) {
        return new Vec2[]{p0, p2, p3, p0};
      } else {
        return new Vec2[]{p0, p1, p2, p3, p0};
      }
    }
  }

  public static Vec2[] convexHull(Vec2 a, Vec2 b, Curve2 c) {
    if (c instanceof QuadraticBezier2) {
      return convexHull(a, b, (QuadraticBezier2) c);
    } else if (c instanceof CubicBezier2) {
      return convexHull(a, b, (CubicBezier2) c);
    } else {
      throw new IllegalStateException();
    }
  }

  public static Interval clipHull(Interval fatLine, Vec2[] hull) {
    double
      lo = Double.POSITIVE_INFINITY,
      hi = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < hull.length - 1; i++) {
      if (fatLine.contains(hull[i].y)) {
        lo = min(lo, hull[i].x);
        hi = max(hi, hull[i].x);
      }
    }

    for (double y : new double[]{fatLine.lo, fatLine.hi}) {
      for (int i = 0; i < hull.length - 1; i++) {
        Vec2 a = hull[i];
        Vec2 b = hull[i + 1];
        if (interval(a.y, b.y).contains(y)) {
          if (a.y == b.y) {
            lo = min(lo, min(a.x, b.x));
            hi = max(lo, max(a.x, b.x));
          } else {
            double t = Scalars.lerp(a.x, b.x, (y - a.y) / (b.y - a.y));
            lo = min(lo, t);
            hi = max(hi, t);
          }
        }
      }
    }

    return hi < lo
      ? Interval.EMPTY
      : interval(lo, hi);
  }

  public static Interval quantize(Interval t) {
    double resolution = FAT_LINE_PARAMETRIC_RESOLUTION;
    double lo = min(1 - resolution, Math.floor(t.lo / resolution) * resolution);
    double hi = max(lo + resolution, Math.ceil(t.hi / resolution) * resolution);

    return interval(lo, hi);
  }

  public static void addIntersections(FatLine a, FatLine b, IList<Vec2> acc) {
    Line2
      la = a.line(),
      lb = b.line();

    Vec2
      av = la.end().sub(la.start()),
      bv = lb.end().sub(lb.start()),
      asb = la.start().sub(lb.start());

    double d = cross(av, bv);

    Vec2 i = vec(cross(bv, asb) / d, cross(av, asb) / d);

    if (PARAMETRIC_BOUNDS.expand(0.1).contains(i)) {
      acc.addLast(box(a.t, b.t).lerp(i));
    }
  }

  public static FatLine clip(FatLine subject, FatLine clipper) {
    Vec2[] hull = convexHull(clipper.range.start(), clipper.range.end(), subject.range);
    Interval normalized = clipHull(clipper.line.expand(FAT_LINE_SPATIAL_EPSILON), hull);
    return normalized.isEmpty()
      ? null :
      new FatLine(subject.curve, subject.t.lerp(normalized));
  }

  public static class FatLine {
    public final Curve2 curve, range;
    public final Interval t, line;

    FatLine(Curve2 curve, Interval t) {
      this.curve = curve;
      this.t = quantize(t);
      this.range = curve.range(this.t);
      this.line = fatLineWidth(range);
    }

    public static FatLine[] from(Curve2 c) {
      double[] ts = c.inflections();
      Arrays.sort(ts);

      if (ts.length == 0) {
        return new FatLine[]{new FatLine(c, interval(0, 1))};
      } else {
        FatLine[] result = new FatLine[ts.length + 1];
        for (int i = 0; i < result.length; i++) {
          double lo = i == 0 ? 0 : ts[i - 1];
          double hi = i == result.length - 1 ? 1 : ts[i];
          result[i] = new FatLine(c, interval(lo, hi));
        }
        return result;
      }
    }

    public double mid() {
      return t.lerp(0.5);
    }

    public boolean isFlat() {
      return t.size() < PARAMETRIC_EPSILON || line.size() <= SPATIAL_EPSILON;
    }

    public Box2 bounds() {
      return box(range.start(), range.end());
    }

    public boolean intersects(FatLine l) {
      return bounds().expand(SPATIAL_EPSILON * 10).intersects(l.bounds());
    }

    public FatLine[] split() {
      if (isFlat()) {
        return new FatLine[]{this};
      }

      return new FatLine[]{
        new FatLine(curve, interval(t.lo, mid())),
        new FatLine(curve, interval(mid(), t.hi))
      };
    }

    public Line2 line() {
      return Line2.line(range.start(), range.end());
    }
  }

  public static Vec2[] fatLineCurveCurve(Curve2 a, Curve2 b) {

    LinearList<FatLine> queue = new LinearList<>();
    FatLine[] as = FatLine.from(a);
    FatLine[] bs = FatLine.from(b);
    for (FatLine ap : as) {
      for (FatLine bp : bs) {
        queue.addLast(ap).addLast(bp);
      }
    }

    int iterations = 0;
    boolean collinearCheck = false;
    LinearList<Vec2> acc = new LinearList<>();
    while (queue.size() > 0) {

      // if it's taking a while, check once (and only once) if they're collinear
      if (iterations > 32 && !collinearCheck) {
        collinearCheck = true;
        Vec2[] is = collinearIntersection(a, b);
        if (isCollinear(a, b, is)) {
          return is;
        }
      }

      FatLine lb = queue.popLast();
      FatLine la = queue.popLast();

      for (; ; ) {
        iterations++;

        if (!la.intersects(lb)) {
          break;
        }

        if (la.isFlat() && lb.isFlat()) {
          addIntersections(la, lb, acc);
          break;
        }

        double aSize = la.t.size();
        double bSize = lb.t.size();

        // use a to clip b
        FatLine lbPrime = clip(lb, la);
        if (lbPrime == null) {
          break;
        }
        lb = lbPrime;

        // use b to clip a
        FatLine laPrime = clip(la, lb);
        if (laPrime == null) {
          break;
        }
        la = laPrime;

        double
          ka = la.t.size() / aSize,
          kb = lb.t.size() / bSize;
        if (max(ka, kb) > 0.8) {
          // TODO: switch over to subdivision at some point?
          for (FatLine ap : la.split()) {
            for (FatLine bp : lb.split()) {
              queue.addLast(ap).addLast(bp);
            }
          }
          break;
        }
      }
    }

    return normalize(acc.toArray(Vec2[]::new));
  }

  // post-processing

  public static double round(double n, double epsilon) {
    if (Scalars.equals(n, 0, epsilon)) {
      return 0;
    } else if (Scalars.equals(n, 1, epsilon)) {
      return 1;
    } else {
      return n;
    }
  }

  private static boolean isCollinear(Curve2 a, Curve2 b, Vec2[] is) {
    if (is.length != 2) {
      return false;
    }

    for (int i = 0; i < MAX_CUBIC_CUBIC_INTERSECTIONS + 1; i++) {
      double t = (double) i / MAX_CUBIC_CUBIC_INTERSECTIONS;
      Vec2 pa = a.position(Scalars.lerp(is[0].x, is[1].x, t));
      Vec2 pb = b.position(Scalars.lerp(is[0].y, is[1].y, t));
      if (!Vec.equals(pa, pb, SPATIAL_EPSILON)) {
        return false;
      }
    }

    return true;
  }

  public static Vec2[] normalize(Vec2[] intersections) {

    int limit = intersections.length;
    if (limit == 0) {
      return intersections;
    }

    int readIdx, writeIdx;

    // round and filter within [0, 1]
    for (readIdx = 0, writeIdx = 0; readIdx < limit; readIdx++) {
      Vec2 i = intersections[readIdx].map(n -> round(n, PARAMETRIC_EPSILON));
      if (PARAMETRIC_BOUNDS.contains(i)) {
        intersections[writeIdx++] = i;
      }
    }
    limit = writeIdx;

    if (limit > 1) {
      // dedupe intersections on b
      Arrays.sort(intersections, 0, limit, Comparator.comparingDouble(v -> v.y));
      for (readIdx = 0, writeIdx = -1; readIdx < limit; readIdx++) {
        Vec2 i = intersections[readIdx];
        if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].y, i.y, EPSILON)) {
          intersections[++writeIdx] = i;
        }
      }
      limit = writeIdx + 1;
    }

    if (limit > 1) {
      // dedupe intersections on a
      Arrays.sort(intersections, 0, limit, Comparator.comparingDouble(v -> v.x));
      for (readIdx = 0, writeIdx = -1; readIdx < limit; readIdx++) {
        Vec2 i = intersections[readIdx];
        if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].x, i.x, EPSILON)) {
          intersections[++writeIdx] = i;
        }
      }
      limit = writeIdx + 1;
    }

    Vec2[] result = new Vec2[limit];
    System.arraycopy(intersections, 0, result, 0, limit);
    return result;
  }

  // analytical methods

  public static Vec2[] collinearIntersection(Curve2 a, Curve2 b) {
    LinearList<Vec2> result = new LinearList<>();

    for (int i = 0; i < 2; i++) {
      double tb = b.nearestPoint(a.position(i));

      // a overhangs the start of b
      if (tb <= 0) {
        double s = round(a.nearestPoint(b.start()), PARAMETRIC_EPSILON);
        if (0 <= s && s <= 1) {
          result.addLast(vec(s, 0));
        }

        // a overhangs the end of b
      } else if (tb >= 1) {
        double s = round(a.nearestPoint(b.end()), PARAMETRIC_EPSILON);
        if (0 <= s && s <= 1) {
          result.addLast(vec(s, 1));
        }

        // a is contained in b
      } else {
        result.addLast(vec(i, tb));
      }
    }

    if (result.size() == 2 && Vec.equals(result.nth(0), result.nth(1), PARAMETRIC_EPSILON)) {
      result.popLast();
    }

    return result.toArray(Vec2[]::new);
  }

  public static Vec2[] lineCurve(Line2 a, Curve2 b) {
    if (b instanceof Line2) {
      return lineLine(a, (Line2) b);
    } else if (b.isFlat(SPATIAL_EPSILON)) {
      return lineLine(a, line(b.start(), b.end()));
    } else if (b instanceof QuadraticBezier2) {
      return lineQuadratic(a, (QuadraticBezier2) b);
    } else {
      return lineCubic(a, (CubicBezier2) b);
    }
  }

  public static Vec2[] lineLine(Line2 a, Line2 b) {

    Vec2 av = a.end().sub(a.start());
    Vec2 bv = b.end().sub(b.start());

    double d = cross(av, bv);
    if (abs(d) < 1e-6) {
      Vec2[] is = collinearIntersection(a, b);
      if (Arrays.stream(is).allMatch(v -> Vec.equals(a.position(v.x), b.position(v.y), SPATIAL_EPSILON))) {
        return is;
      } else if (abs(d) == 0) {
        return new Vec2[0];
      }
    }

    Vec2 asb = a.start().sub(b.start());
    double s = cross(bv, asb) / d;
    double t = cross(av, asb) / d;
    return new Vec2[]{vec(s, t)};
  }

  public static Vec2[] lineQuadratic(Line2 p, QuadraticBezier2 q) {

    // (p0 - 2p1 + p2) t^2 + (-2p0 + 2p1) t + p0
    Vec2 a = q.p0.add(q.p1.mul(-2)).add(q.p2);
    Vec2 b = q.p0.mul(-2).add(q.p1.mul(2));
    Vec2 c = q.p0;

    Vec2 dir = p.end().sub(p.start());
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveQuadratic(
      dot(n, a),
      dot(n, b),
      dot(n, c) + cross(p.start(), p.end()));

    Vec2[] result = new Vec2[roots.length];
    if (Scalars.equals(dir.x, 0, EPSILON)) {
      double y0 = p.start().y;
      for (int i = 0; i < roots.length; i++) {
        double t = roots[i];
        double y1 = q.position(t).y;
        result[i] = vec((y1 - y0) / dir.y, t);
      }
    } else {
      double x0 = p.start().x;
      for (int i = 0; i < roots.length; i++) {
        double t = roots[i];
        double x1 = q.position(t).x;
        result[i] = vec((x1 - x0) / dir.x, t);
      }
    }

    return result;
  }

  public static Vec2[] lineCubic(Line2 p, CubicBezier2 q) {

    // (-p0 + 3p1 - 3p2 + p3) t^3 + (3p0 - 6p1 + 3p2) t^2 + (-3p0 + 3p1) t + p0
    Vec2 a = q.p0.mul(-1).add(q.p1.mul(3)).add(q.p2.mul(-3)).add(q.p3);
    Vec2 b = q.p0.mul(3).add(q.p1.mul(-6)).add(q.p2.mul(3));
    Vec2 c = q.p0.mul(-3).add(q.p1.mul(3));
    Vec2 d = q.p0;

    Vec2 dir = p.end().sub(p.start());
    double dLen = dir.length();
    Vec2 n = vec(-dir.y, dir.x);

    double[] roots = Equations.solveCubic(
      dot(n, a),
      dot(n, b),
      dot(n, c),
      dot(n, d) + cross(p.start(), p.end()));

    Vec2[] result = new Vec2[roots.length];
    for (int i = 0; i < roots.length; i++) {
      double t = roots[i];
      Vec2 v = q.position(t).sub(p.start());
      double vLen = v.length();
      double s = (vLen / dLen) * signum(dot(dir, v));

      result[i] = vec(s, t);
    }

    return result;
  }

  //

  public static Vec2[] intersections(Curve2 a, Curve2 b) {
    if (!a.bounds().expand(SPATIAL_EPSILON).intersects(b.bounds())) {
      return new Vec2[0];
    }

    if (a instanceof Line2) {
      return normalize(lineCurve((Line2) a, b));
    } else if (b instanceof Line2) {
      Vec2[] result = normalize(lineCurve((Line2) b, a));
      for (int i = 0; i < result.length; i++) {
        result[i] = result[i].swap();
      }
      return result;
    } else {
      //return subdivisionCurveCurve(a, b);
      return fatLineCurveCurve(a, b);
    }
  }


}