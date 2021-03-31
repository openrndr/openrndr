package io.lacuna.artifex;

import io.lacuna.artifex.Ring2.Result;
import io.lacuna.artifex.utils.EdgeList;
import io.lacuna.artifex.utils.regions.Clip;
import io.lacuna.artifex.utils.regions.Hulls;
import io.lacuna.artifex.utils.regions.Monotonic;
import io.lacuna.artifex.utils.regions.Triangles;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author ztellman
 */
public class Region2 {

  public final Ring2[] rings;
  public final Box2 bounds;

  public Region2(Iterable<Ring2> rings) {
    this(LinearList.from(rings).toArray(Ring2[]::new));
  }

  public Region2(Ring2[] rings) {
    this.rings = rings.clone();
    Arrays.sort(this.rings, Comparator.comparingDouble(r -> r.area));

    this.bounds = Arrays.stream(this.rings)
      .map(r -> r.bounds)
      .reduce(Box2.EMPTY, Box2::union);
  }

  ///

  public Ring2[] rings() {
    return rings;
  }

  public static Region2 of(Ring2... rings) {
    return new Region2(Lists.from(rings));
  }

  public Box2 bounds() {
    return bounds;
  }

  public Result test(Vec2 p) {
    for (Ring2 r : rings) {
      Result result = r.test(p);
      if (result.inside) {
        return result.curve == null && r.isClockwise
          ? Result.OUTSIDE
          : result;
      }
    }

    return Result.OUTSIDE;
  }

  public boolean contains(Vec2 p) {
    return test(p).inside;
  }

  /// transforms and set operations

  public Region2 transform(Matrix3 m) {
    return new Region2(Arrays.stream(rings).map(r -> r.transform(m)).toArray(Ring2[]::new));
  }

  public Region2 intersection(Region2 region) {
    return Clip.intersection(this, region);
  }

  public Region2 union(Region2 region) {
    return Clip.union(this, region);
  }

  public Region2 difference(Region2 region) {
    return Clip.difference(this, region);
  }

}
