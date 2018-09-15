package io.lacuna.artifex;

import io.lacuna.artifex.utils.Intersections;

import java.util.Arrays;

import static io.lacuna.artifex.Box.box;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static java.lang.Math.abs;

/**
 * @author ztellman
 */
public interface Curve2 {

  /**
   * @param t a parametric point on the curve, not necessarily within [0, 1]
   * @return the interpolated position on the curve
   */
  Vec2 position(double t);

  /**
   * Given a ring of curves, the sum of area() will be the are enclosed by that ring. For clockwise rings, the sum will
   * be negative, for counter-clockwise rings it will be positive.
   *
   * @return the signed area of the curve
   */
  double signedArea();

  double length();

  default Vec2 start() {
    return position(0);
  }

  default Vec2 end() {
    return position(1);
  }

  /**
   * @return an updated curve with the specified endpoints.
   */
  Curve2 endpoints(Vec2 start, Vec2 end);

  /**
   * @param t a parametric point on the curve, not necessarily within [0, 1]
   * @return the tangent at the interpolated position on the curve, which is not normalized
   */
  Vec2 direction(double t);

  /**
   * @param t a parametric point within the curve, which must be within (0, 1)
   * @return an array representing the lower and upper regions of the curve, split at {@code t}
   */
  Curve2[] split(double t);

  boolean isFlat(double epsilon);

  /**
   * @param interval the parametric range
   * @return the curve within [interval.lo, interval.hi]
   */
  default Curve2 range(Interval interval) {
    return range(interval.lo, interval.hi);
  }

  /**
   * @param tMin the lower parametric bound
   * @param tMax the upper parametric bound
   * @return the curve within [tMin, tMax]
   */
  default Curve2 range(double tMin, double tMax) {
    if (tMin == tMax) {
      throw new IllegalArgumentException("range must be non-zero");
    } else if (tMax < tMin) {
      throw new IllegalArgumentException("tMin must be less than tMax");
    }

    Curve2 c;
    if (tMin == 0 && tMax == 1) {
      return this;
    } else if (tMin == 0) {
      return split(tMax)[0];
    } else if (tMax == 1) {
      return split(tMin)[1];
    } else {
      return split(tMin)[1].split((tMax - tMin) / (1 - tMin))[0].endpoints(position(tMin), position(tMax));
    }
  }

  /**
   * @param ts an array of parametric split points
   * @return an array of curves, split at the specified points.
   */
  default Curve2[] split(double[] ts) {

    if (ts.length == 0) {
      return new Curve2[] {this};
    }

    ts = ts.clone();
    Arrays.sort(ts);

    int offset = ts[0] == 0 ? 1 : 0;
    int len = (ts.length - offset) - (ts[ts.length - 1] == 1 ? 1 : 0);
    System.arraycopy(ts, offset, ts, 0, len);

    if (len == 0) {
      return new Curve2[] {this};
    } else if (len == 1) {
      return split(ts[0]);
    }

    Curve2[] result = new Curve2[len + 1];

    result[0] = range(0, ts[0]);
    for (int i = 0; i < len - 1; i++) {
      result[i + 1] = range(ts[i], ts[i + 1]);
    }
    result[len] = range(ts[len - 1], 1);

    return result;
  }

  /**
   * @param p a point in 2D space
   * @return the {@code t} parameter representing the closest point on the curve, not necessarily within [0,1]
   */
  double nearestPoint(Vec2 p);

  default Box2 bounds() {
    Box2 bounds = box(start(), end());
    for (double t : inflections()) {
      bounds = bounds.union(position(t));
    }
    return bounds;
  }

  Vec2[] subdivide(double error);

  Curve2 transform(Matrix3 m);

  Curve2 reverse();

  double[] inflections();

  default Vec2[] intersections(Curve2 c) {
    return Intersections.intersections(this, c);
  }
}
