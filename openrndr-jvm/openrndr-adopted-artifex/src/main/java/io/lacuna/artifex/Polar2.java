package io.lacuna.artifex;

/**
 * @author ztellman
 */
public class Polar2 {
  public final double theta;
  public final double r;

  public Polar2(double theta, double r) {
    this.theta = theta;
    this.r = r;
  }

  public Polar2 rotate(double theta) {
    return new Polar2(this.theta + theta, r);
  }

  public Vec2 vec2() {
    double x = Math.cos(theta);
    double y = Math.sin(theta);
    return new Vec2(x * r, y * r);
  }
}
