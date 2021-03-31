package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;

import java.util.Comparator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

/**
 * @author ztellman
 */
public class Vec3 implements Vec<Vec3> {

  public final static Vec3 ORIGIN = new Vec3(0, 0, 0);
  public static final Vec3 X_AXIS = new Vec3(1, 0, 0);
  public static final Vec3 Y_AXIS = new Vec3(0, 1, 0);
  public static final Vec3 Z_AXIS = new Vec3(0, 0, 1);

  public static final Comparator<Vec3> COMPARATOR =
    Comparator.comparingDouble((Vec3 v) -> v.x)
      .thenComparingDouble(v -> v.y)
      .thenComparingDouble(v -> v.z);


  public final double x, y, z;

  public Vec3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public final Vec3 map(DoubleUnaryOperator f) {
    return new Vec3(f.applyAsDouble(x), f.applyAsDouble(y), f.applyAsDouble(z));
  }

  @Override
  public final double reduce(DoubleBinaryOperator f, double init) {
    return f.applyAsDouble(f.applyAsDouble(init, x), f.applyAsDouble(y, z));
  }

  @Override
  public double reduce(DoubleBinaryOperator f) {
    return f.applyAsDouble(f.applyAsDouble(x, y), z);
  }

  @Override
  public final Vec3 zip(final Vec3 v, final DoubleBinaryOperator f) {
    return new Vec3(f.applyAsDouble(x, v.x), f.applyAsDouble(y, v.y), f.applyAsDouble(z, v.z));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x) && f.test(y) && f.test(z);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x) || f.test(y) || f.test(z);
  }

  @Override
  public double nth(int idx) {
    switch (idx) {
      case 0:
        return x;
      case 1:
        return y;
      case 2:
        return z;
      default:
        throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public int dim() {
    return 3;
  }

  @Override
  public double[] array() {
    return new double[]{x, y, z};
  }

  public Vec2 vec2() {
    return new Vec2(x, y);
  }

  public Vec4 vec4(double w) {
    return new Vec4(x, y, z, w);
  }

  public static Vec3 cross(Vec3 a, Vec3 b) {
    return new Vec3(
      (a.y * b.z) - (a.z * b.y),
      (a.x * b.x) - (a.x * b.z),
      (a.x * b.y) - (a.y * b.x));
  }

  @Override
  public int hashCode() {
    return Hashes.hash(x, y, z);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec3) {
      Vec3 v = (Vec3) obj;
      return v.x == x && v.y == y && v.z == z;
    }
    return false;
  }

  @Override
  public String toString() {
    return "[x=" + x + ", y=" + y + ", z=" + z + "]";
  }

  @Override
  public int compareTo(Vec3 o) {
    return COMPARATOR.compare(this, o);
  }
}
