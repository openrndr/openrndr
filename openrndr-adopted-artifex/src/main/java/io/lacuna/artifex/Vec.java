package io.lacuna.artifex;

import java.awt.geom.Point2D;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.*;
import java.util.stream.DoubleStream;
import java.util.stream.StreamSupport;

/**
 * @author ztellman
 */
@SuppressWarnings("unchecked")
public interface Vec<T extends Vec<T>> extends Comparable<T> {

  DoubleUnaryOperator NEGATE = n -> -n;
  DoubleBinaryOperator ADD = (a, b) -> a + b;
  DoubleBinaryOperator MUL = (a, b) -> a * b;
  DoubleBinaryOperator SUB = (a, b) -> a - b;
  DoubleBinaryOperator DIV = (a, b) -> a / b;
  DoubleBinaryOperator DELTA = (a, b) -> Math.abs(a - b);

  static Vec2 from(Point2D p) {
    return new Vec2(p.getX(), p.getY());
  }

  static Vec1 vec(double x) {
    return new Vec1(x);
  }

  static Vec2 vec(double x, double y) {
    return new Vec2(x, y);
  }

  static Vec3 vec(double x, double y, double z) {
    return new Vec3(x, y, z);
  }

  static Vec4 vec(double x, double y, double z, double w) {
    return new Vec4(x, y, z, w);
  }

  static Vec from(double[] ary) {
    switch (ary.length) {
      case 2:
        return new Vec2(ary[0], ary[1]);
      case 3:
        return new Vec3(ary[0], ary[1], ary[2]);
      case 4:
        return new Vec4(ary[0], ary[1], ary[2], ary[3]);
      default:
        throw new IllegalArgumentException("ary must have a length in [1,4]");
    }
  }

  static <T extends Vec<T>> double dot(T a, T b) {
    return a.mul(b).reduce(ADD);
  }

  static double dot(Vec2 a, Vec2 b) {
    return (a.x * b.x) + (a.y * b.y);
  }

  static <T extends Vec<T>> T lerp(T a, T b, double t) {
    return a.add(b.sub(a).mul(t));
  }

  static Vec2 lerp(Vec2 a, Vec2 b, double t) {
    return new Vec2(a.x + ((b.x - a.x) * t), a.y + ((b.y - a.y) * t));
  }

  static <T extends Vec<T>> T lerp(T a, T b, T t) {
    return a.add(b.sub(a).mul(t));
  }

  static Vec2 lerp(Vec2 a, Vec2 b, Vec2 t) {
    return new Vec2(a.x + ((b.x - a.x) * t.x), a.y + ((b.y - a.y) * t.y));
  }

  static <T extends Vec<T>> boolean equals(T a, T b, double tolerance) {
    return a.zip(b, DELTA).every(i -> i <= tolerance);
  }

  T map(DoubleUnaryOperator f);

  double reduce(DoubleBinaryOperator f, double init);

  double reduce(DoubleBinaryOperator f);

  T zip(T v, DoubleBinaryOperator f);

  boolean every(DoublePredicate f);

  boolean any(DoublePredicate f);

  double nth(int idx);

  int dim();

  double[] array();

  default T negate() {
    return map(NEGATE);
  }

  default T add(T v) {
    return zip(v, ADD);
  }

  default T add(final double n) {
    return map(i -> i + n);
  }

  default T sub(T v) {
    return zip(v, SUB);
  }

  default T sub(final double n) {
    return map(i -> i - n);
  }

  default T mul(T v) {
    return zip(v, MUL);
  }

  default T mul(final double k) {
    return map(i -> i * k);
  }

  default T div(T v) {
    return zip(v, DIV);
  }

  default T div(double k) {
    return mul(1.0 / k);
  }

  default T abs() {
    return map(Math::abs);
  }

  default double lengthSquared() {
    return dot((T) this, (T) this);
  }

  default double length() {
    return Math.sqrt(lengthSquared());
  }

  default T norm() {
    double l = lengthSquared();
    if (l == 1.0) {
      return (T) this;
    } else {
      return div(Math.sqrt(l));
    }
  }

  default T pseudoNorm() {
    int exponent = Math.getExponent(reduce(Math::max));
    return (exponent < -8 | exponent > 8)
      ? mul(Math.pow(2, -exponent))
      : (T) this;
  }

  default PrimitiveIterator.OfDouble iterator() {
    return stream().iterator();
  }

  default DoubleStream stream() {
    return DoubleStream.of(array());
  }

  default T clamp(double min, double max) {
    return map(i -> Math.max(min, Math.min(max, i)));
  }

  default T clamp(T min, T max) {
    return zip(min, Math::max).zip(max, Math::min);
  }

}
