package io.lacuna.artifex.utils;

import io.lacuna.artifex.Curve2;
import io.lacuna.artifex.Path2;
import io.lacuna.artifex.Region2;
import io.lacuna.artifex.Vec2;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;

import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.Vec.vec;

/**
 * @author ztellman
 */
public class PathSweep {

  private static Vec2 transpose(Vec2 v) {
    return vec(-v.y, v.x);
  }

  private static Vec2 miter(Curve2 a, Curve2 b) {
    Vec2 l1 = a.end().norm();
    Vec2 l2 = b.start().norm();
    Vec2 tangent = l1.add(l2).div(2);
    Vec2 miter = transpose(tangent);
    Vec2 normal = transpose(l1);

    return miter.div(dot(miter, normal));
  }

  public static Region2 sweep(Path2 path, double width) {

    Curve2[] curves = path.curves();
    Vec2[] miters = new Vec2[curves.length + 1];
    for (int i = 1; i < curves.length; i++) {
      miters[i] = miter(curves[i - 1], curves[i]);
    }

    IList<Curve2> above = new LinearList<>();
    IList<Curve2> below = new LinearList<>();


    IList<Curve2> result = new LinearList<>();
    if (path.isRing()) {
      miters[0] = miter(curves[curves.length - 1], curves[0]);
      miters[miters.length - 1] = miters[0];
    } else {
      miters[0] = transpose(curves[0].start()).norm();
      miters[miters.length - 1] = transpose(curves[curves.length - 1].end()).norm();
    }

    return null;
  }

}
