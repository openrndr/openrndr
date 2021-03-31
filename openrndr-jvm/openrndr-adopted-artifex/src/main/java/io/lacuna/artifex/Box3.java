package io.lacuna.artifex;

import static io.lacuna.artifex.Vec.vec;
import static java.lang.Double.NaN;

/**
 * @author ztellman
 */
public class Box3 extends Box<Vec3, Box3> {

  public static final Box3 EMPTY = new Box3(vec(NaN, NaN, NaN), vec(NaN, NaN, NaN));
  
  public final double lx, ly, lz, ux, uy, uz;
  
  Box3(double ax, double ay, double az, double bx, double by, double bz) {
    if (ax < bx) {
      this.lx = ax;
      this.ux = bx;
    } else {
      this.ux = ax;
      this.lx = bx;
    }

    if (ay < by) {
      this.ly = ay;
      this.uy = by;
    } else {
      this.uy = ay;
      this.ly = by;
    }

    if (az < bz) {
      this.lz = az;
      this.uz = bz;
    } else {
      this.uz = az;
      this.lz = bz;
    }
  }

  public Box3(Vec3 a, Vec3 b) {
    this(a.x, a.y, a.z, b.x, b.y, b.z);
  }

  public Box2 box2() {
    return new Box2(lx, ly, ux, uz);
  }

  @Override
  protected Box3 construct(Vec3 a, Vec3 b) {
    return new Box3(a, b);
  }

  @Override
  protected Box3 empty() {
    return EMPTY;
  }

  @Override
  public Vec3 lower() {
    return new Vec3(lx, ly, lz);
  }

  @Override
  public Vec3 upper() {
    return new Vec3(ux, uy, uz);
  }

  @Override
  public boolean isEmpty() {
    return this == EMPTY;
  }
}
