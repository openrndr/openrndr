package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

public class Vec1 implements Vec<Vec1> {

  public final double x;

  public Vec1(double x) {
    this.x = x;
  }

  public Vec2 vec2(double y) {
    return new Vec2(x, y);
  }

  public Vec3 vec3(double y, double z) {
    return new Vec3(x, y, z);
  }

  public Vec3 vec3(Vec2 v) {
    return new Vec3(x, v.x, v.y);
  }

  public Vec4 vec4(double y, double z, double w) {
    return new Vec4(x, y, z, w);
  }

  public Vec4 vec4(Vec3 v) {
    return new Vec4(x, v.x, v.y, v.z);
  }

  @Override
  public Vec1 map(DoubleUnaryOperator f) {
    return new Vec1(f.applyAsDouble(x));
  }

  @Override
  public double reduce(DoubleBinaryOperator f, double init) {
    return f.applyAsDouble(init, x);
  }

  @Override
  public double reduce(DoubleBinaryOperator f) {
    throw new IllegalStateException();
  }

  @Override
  public Vec1 zip(Vec1 v, DoubleBinaryOperator f) {
    return new Vec1(f.applyAsDouble(x, v.x));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x);
  }

  @Override
  public double nth(int idx) {
    if (idx == 0) {
      return x;
    } else {
      throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public int dim() {
    return 1;
  }

  @Override
  public double[] array() {
    return new double[]{x};
  }

  @Override
  public int hashCode() {
    return Hashes.hash(x);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec1) {
      Vec1 v = (Vec1) obj;
      return x == v.x;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[x=%f]", x);
  }

  @Override
  public int compareTo(Vec1 o) {
    return Double.compare(x, o.x);
  }

}
