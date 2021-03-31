package io.lacuna.artifex;

import java.util.ArrayList;
import java.util.List;

import static io.lacuna.artifex.Vec.vec;
import static java.lang.Double.NaN;

/**
 * @author ztellman
 */
public class Box2 extends Box<Vec2, Box2> {

  public static final Box2 EMPTY = new Box2(vec(NaN, NaN), vec(NaN, NaN));

  public final double lx, ly, ux, uy;

  Box2(double ax, double ay, double bx, double by) {
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
  }

  public Box3 box3(double lz, double uz) {
    return new Box3(lx, ly, lz, ux, uy, uz);
  }

  public double width() {
    return ux - lx;
  }

  public double height() {
    return uy - ly;
  }

  public Box2(Vec2 a, Vec2 b) {
    this(a.x, a.y, b.x, b.y);
  }

  public Box2 scale(double k) {
    return scale(vec(k, k));
  }

  public Box2 scale(double x, double y) {
    return scale(vec(x, y));
  }

  public Box2 translate(double x, double y) {
    return translate(vec(x, y));
  }

  public Vec2[] vertices() {
    return new Vec2[] {vec(lx, ly), vec(ux, ly), vec(ux, uy), vec(lx, uy)};
  }

  public Ring2 outline() {
    List<Curve2> cs = new ArrayList<>();
    Vec2[] vs = vertices();
    for (int i = 0; i < vs.length; i++) {
      cs.add(Line2.line(vs[i], vs[(i + 1) % 4]));
    }
    return new Ring2(cs);
  }

  @Override
  public boolean intersects(Box2 b) {
    if (isEmpty() || b.isEmpty()) {
      return false;
    }

    return b.ux >= lx
      & ux >= b.lx
      & b.uy >= ly
      & uy >= b.ly;
  }

  @Override
  public Vec2 lower() {
    return new Vec2(lx, ly);
  }

  @Override
  public Vec2 upper() {
    return new Vec2(ux, uy);
  }

  @Override
  public boolean isEmpty() {
    return this == EMPTY;
  }

  @Override
  protected Box2 construct(Vec2 a, Vec2 b) {
    return new Box2(a.x, a.y, b.x, b.y);
  }

  @Override
  protected Box2 empty() {
    return EMPTY;
  }
}
