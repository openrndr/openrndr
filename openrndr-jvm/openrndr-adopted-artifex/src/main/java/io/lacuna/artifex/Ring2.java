package io.lacuna.artifex;

import io.lacuna.artifex.utils.Intersections;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Arrays;
import java.util.Comparator;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Intersections.*;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;
import static java.lang.Math.signum;

public class Ring2 {

  public static class Result {
    public static final Result INSIDE = new Result(true, null);
    public static final Result OUTSIDE = new Result(false, null);

    public final boolean inside;
    public final Curve2 curve;

    private Result(boolean inside, Curve2 curve) {
      this.inside = inside;
      this.curve = curve;
    }

    public Result(Curve2 curve) {
      this(true, curve);
    }

    @Override
    public String toString() {
      if (inside) {
        return curve == null ? "INSIDE" : "EDGE: " + curve;
      } else {
        return "OUTSIDE";
      }
    }
  }

  public final Curve2[] curves;
  public final Box2 bounds;
  public final boolean isClockwise;
  public final double area;

  private Ring2(Curve2[] curves, Box2 bounds, boolean isClockwise, double area) {
    this.curves = curves;
    this.bounds = bounds;
    this.isClockwise = isClockwise;
    this.area = area;
  }

  public Ring2(Iterable<Curve2> cs) {

    // TODO: dedupe collinear adjacent lines
    Box2 bounds = Box2.EMPTY;
    double signedArea = 0;
    LinearList<Curve2> list = new LinearList<>();
    for (Curve2 a : cs) {
      for (Curve2 b : a.split(a.inflections())) {

        list.addLast(b);
        bounds = bounds.union(b.start()).union(b.end());
        signedArea += b.signedArea();
      }
    }

    this.isClockwise = signedArea < 0;
    this.area = abs(signedArea);
    this.bounds = bounds;

    curves = list.toArray(Curve2[]::new);
    for (int i = 0; i < curves.length - 1; i++) {
      curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start());
    }
    int lastIdx = curves.length - 1;
    curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start());
  }

  public static Ring2 of(Curve2... cs) {
    return new Ring2(Lists.from(cs));
  }

  public Region2 region() {
    return new Region2(LinearList.of(this));
  }

  /**
   * @return a unit square from [0, 0] to [1, 1]
   */
  public static Ring2 square() {
    return Box.box(vec(0, 0), vec(1, 1)).outline();
  }

  /**
   * @return a unit circle with radius of 1, centered at [0, 0]
   */
  public static Ring2 circle() {
    // taken from http://whizkidtech.redprince.net/bezier/circle/kappa/
    double k = 4.0 / 3.0 * (StrictMath.sqrt(2) - 1);
    return Ring2.of(
      Bezier2.curve(vec(1, 0), vec(1, k), vec(k, 1), vec(0, 1)),
      Bezier2.curve(vec(0, 1), vec(-k, 1), vec(-1, k), vec(-1, 0)),
      Bezier2.curve(vec(-1, 0), vec(-1, -k), vec(-k, -1), vec(0, -1)),
      Bezier2.curve(vec(0, -1), vec(k, -1), vec(1, -k), vec(1, 0)));
  }

  public Path2 path() {
    return new Path2(this);
  }

  public Ring2 reverse() {
    return new Ring2(
      LinearList.from(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse))).toArray(Curve2[]::new),
      bounds,
      !isClockwise,
      area);
  }

  public Result test(Vec2 p) {

    if (!bounds.expand(SPATIAL_EPSILON).contains(p)) {
      return Result.OUTSIDE;
    }

    Line2 ray = Line2.line(p, vec(bounds.ux + 1, p.y));
    int count = 0;

    // since our curves have been split at inflection points, there can only
    // be a single ray/curve intersection unless the curve is collinear
    for (Curve2 c : curves) {
      Box2 b = c.bounds();
      boolean flat = b.height() == 0;

      //System.out.println(p + " " + b + " " + c);

      // it's to our right
      if (p.x < b.lx) {
        // check if we intersect within [bottom, top)
        if (p.y >= b.ly && p.y < b.uy) {
          //System.out.println("right, incrementing");
          count++;
        }

        // we're inside the bounding box
      } else if (b.expand(vec(SPATIAL_EPSILON, 0)).contains(p)) {
        Vec2 i = Arrays.stream(lineCurve(ray, c))
          .map(v -> v.map(n -> Intersections.round(n, Intersections.PARAMETRIC_EPSILON)))
          .filter(PARAMETRIC_BOUNDS::contains)
          .min(Comparator.comparingDouble(v -> v.x))
          .orElse(null);

        if (i != null) {
          //System.out.println(i);
          if (i.x == 0) {
            return new Result(c);
          } else if (!flat && p.y < b.uy) {
            //System.out.println("intersected, incrementing");
            count++;
          }
        } else {
          //System.out.println("no intersection");
        }
      }
    }

    //System.out.println(count);

    return count % 2 == 1 ? Result.INSIDE : Result.OUTSIDE;
  }

  public Ring2 transform(Matrix3 m) {
    return new Ring2(() -> Arrays.stream(curves).map(c -> c.transform(m)).iterator());
  }
}