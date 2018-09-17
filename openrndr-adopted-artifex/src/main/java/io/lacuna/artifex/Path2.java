package io.lacuna.artifex;

import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;

/**
 * @author ztellman
 */
public class Path2 {

  private final Curve2[] curves;
  private final Box2 bounds;
  private final boolean isRing;

  Path2(Ring2 ring) {
    this.curves = ring.curves;
    this.bounds = ring.bounds;
    this.isRing = true;
  }

  public static Path2 of(Curve2... curves) {
    return new Path2(Arrays.asList(curves));
  }

  public static Path2 linear(Vec2... vertices) {
    ArrayList<Curve2> segments = new ArrayList<>();
    for (int i = 0; i < vertices.length - 1; i++) {
      Vec2 a = vertices[i];
      Vec2 b = vertices[i + 1];
      if (!Vec.equals(a, b, EPSILON)) {
        segments.add(Line2.line(vertices[i], vertices[i + 1]));
      }
    }
    return new Path2(segments);
  }

  public Path2(Iterable<Curve2> cs) {

    IList<Curve2> l = new LinearList<>();
    Box2 bounds = Box2.EMPTY;
    for (Curve2 a : cs) {
      for (Curve2 b : a.split(a.inflections())) {
        l.addLast(b);
        bounds = bounds.union(b.start()).union(b.end());
      }
    }

    this.bounds = bounds;
    this.isRing = Vec.equals(l.first().start(), l.last().end(), EPSILON);
    this.curves = l.toArray(Curve2[]::new);

    for (int i = 0; i < curves.length - 1; i++) {
      curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start());
    }

    if (isRing) {
      int lastIdx = curves.length - 1;
      curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start());
    }
  }

  public Path2 reverse() {
    return new Path2(Lists.reverse(Lists.lazyMap(Lists.from(curves), Curve2::reverse)));
  }

  public Curve2[] curves() {
    return curves;
  }

  public boolean isRing() {
    return isRing;
  }

  public Box2 bounds() {
    return bounds;
  }

  public Iterable<Vec2> vertices(double error) {
    List<Vec2> result = new ArrayList<>();
    for (Curve2 c : curves) {
      Vec2[] segments = c.subdivide(error);
      if (result.isEmpty()) {
        result.addAll(Arrays.asList(segments));
      } else {
        Vec2 t1 = result.get(result.size() - 1).sub(result.get(result.size() - 2)).norm();
        Vec2 t2 = segments[1].sub(segments[0]).norm();
        if (Vec.equals(t1, t2, EPSILON)) {
          result.remove(result.size() - 1);
        }

        for (int i = 1; i < segments.length; i++) {
          result.add(segments[i]);
        }
      }
    }

    return result;
  }
}
