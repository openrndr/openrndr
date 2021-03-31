package io.lacuna.artifex;

import static io.lacuna.artifex.Vec.vec;
import static java.lang.Double.NaN;

/**
 * @author ztellman
 */
public class Box4 extends Box<Vec4, Box4> {

  public static final Box4 EMPTY = new Box4(vec(NaN, NaN, NaN, NaN), vec(NaN, NaN, NaN, NaN));

  private final double lx, ly, lz, lw, ux, uy, uz, uw;

  private Box4(double ax, double ay, double az, double aw, double bx, double by, double bz, double bw) {
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

    if (aw < bw) {
      this.lw = aw;
      this.uw = bw;
    } else {
      this.uw = aw;
      this.lw = bw;
    }
  }

  public Box4(Vec4 a, Vec4 b) {
    this(a.x, a.y, a.z, a.w, b.x, b.y, b.z, b.w);
  }

  public Box3 box3() {
    return new Box3(lx, ly, lz, ux, uy, uz);
  }

  public Box2 box2() {
    return new Box2(lx, ly, ux, uy);
  }

  @Override
  protected Box4 construct(Vec4 lower, Vec4 upper) {
    return new Box4(lower, upper);
  }

  @Override
  protected Box4 empty() {
    return EMPTY;
  }

  @Override
  public Vec4 lower() {
    return new Vec4(lx, ly, lz, lw);
  }

  @Override
  public Vec4 upper() {
    return new Vec4(ux, uy, uz, uw);
  }

  @Override
  public boolean isEmpty() {
    return this == EMPTY;
  }
}
