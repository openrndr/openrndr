package io.lacuna.artifex;

import io.lacuna.artifex.utils.Hashes;
import io.lacuna.artifex.utils.Scalars;

import java.util.Comparator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

import static io.lacuna.artifex.Vec.dot;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.normalizationFactor;
import static java.lang.Math.acos;
import static java.lang.Math.atan2;

/**
 * @author ztellman
 */
public class Vec2 implements Vec<Vec2> {

  public final static Vec2 ORIGIN = new Vec2(0, 0);
  public static final Vec2 X_AXIS = new Vec2(1, 0);
  public static final Vec2 Y_AXIS = new Vec2(0, 1);

  public static final Comparator<Vec2> COMPARATOR =
    Comparator.comparingDouble((Vec2 v) -> v.x)
      .thenComparingDouble(v -> v.y);

  public final double x, y;

  public Vec2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public final Vec2 map(DoubleUnaryOperator f) {
    return new Vec2(f.applyAsDouble(x), f.applyAsDouble(y));
  }

  @Override
  public final double reduce(DoubleBinaryOperator f, double init) {
    return f.applyAsDouble(f.applyAsDouble(x, y), init);
  }

  @Override
  public double reduce(DoubleBinaryOperator f) {
    return f.applyAsDouble(x, y);
  }

  @Override
  public final Vec2 zip(final Vec2 v, final DoubleBinaryOperator f) {
    return new Vec2(f.applyAsDouble(x, v.x), f.applyAsDouble(y, v.y));
  }

  @Override
  public boolean every(DoublePredicate f) {
    return f.test(x) && f.test(y);
  }

  @Override
  public boolean any(DoublePredicate f) {
    return f.test(x) || f.test(y);
  }

  @Override
  public double nth(int idx) {
    switch (idx) {
      case 0:
        return x;
      case 1:
        return y;
      default:
        throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public int dim() {
    return 2;
  }

  @Override
  public double[] array() {
    return new double[]{x, y};
  }

  public Vec2 add(double x, double y) {
    return new Vec2(this.x + x, this.y + y);
  }

  public Vec2 sub(double x, double y) {
    return new Vec2(this.x - x, this.y - y);
  }

  public Vec2 swap() {
    return new Vec2(y, x);
  }

  public Vec3 vec3(double z) {
    return new Vec3(x, y, z);
  }

  public Vec4 vec4(double z, double w) {
    return new Vec4(x, y, z, w);
  }

  public Vec4 vec4(Vec2 v) {
    return new Vec4(x, y, v.x, v.y);
  }

  public Vec2 transform(Matrix3 m) {
    return m.transform(this);
  }

  /**
   * @return a rotated vector
   */
  public Vec2 rotate(double radians) {
    double s = Math.sin(radians);
    double c = Math.cos(radians);
    return new Vec2((c * x) + (-s * y), (s * x) + (c * y));
  }

  public static double cross(Vec2 a, Vec2 b) {
    return (a.x * b.y) - (a.y * b.x);
  }

  /**
   * @return the clockwise angle between the two vectors
   */
  public static double angleBetween(Vec2 a, Vec2 b) {
    a = a.pseudoNorm();
    b = b.pseudoNorm();

    // from section 12 of https://people.eecs.berkeley.edu/~wkahan/Mindless.pdf
    double theta = StrictMath.atan2(cross(a, b), dot(a, b));

    if (theta > 0) {
      theta -= Math.PI * 2;
    }
    return theta;
  }

  /**
   * @return whether {@code b} sits between the vectors {@code a} and {@code b}
   */
  public static boolean between(Vec2 a, Vec2 b, Vec2 c) {
    return (cross(a, b) * cross(c, b)) < 0;
  }

  public Polar2 polar2() {
    return new Polar2(atan2(y, x), length());
  }

  @Override
  public int hashCode() {
    return Hashes.hash(x, y);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Vec2) {
      Vec2 v = (Vec2) obj;
      return v.x == x && v.y == y;
    }
    return false;
  }

  @Override
  public String toString() {
    return "[x=" + x + ", y=" + y +"]";
  }

  @Override
  public int compareTo(Vec2 o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public Vec2 add(Vec2 v) {
    return new Vec2(x + v.x, y + v.y);
  }

  @Override
  public Vec2 add(double n) {
    return new Vec2(x + n, y + n);
  }

  @Override
  public Vec2 negate() {
    return new Vec2(-x, -y);
  }

  @Override
  public Vec2 sub(Vec2 v) {
    return new Vec2(x - v.x, y - v.y);
  }

  @Override
  public Vec2 sub(double n) {
    return new Vec2(x - n, y - n);
  }

  @Override
  public Vec2 mul(Vec2 v) {
    return new Vec2(x * v.x, y * v.y);
  }

  @Override
  public Vec2 mul(double k) {
    return new Vec2(x * k, y * k);
  }

  @Override
  public Vec2 div(Vec2 v) {
    return new Vec2(x / v.x, y / v.y);
  }
}
