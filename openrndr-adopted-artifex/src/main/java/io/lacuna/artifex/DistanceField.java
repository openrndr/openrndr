package io.lacuna.artifex;

import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static io.lacuna.artifex.Vec.*;
import static io.lacuna.artifex.Vec2.cross;
import static io.lacuna.artifex.utils.Scalars.clamp;
import static java.lang.Math.*;

/**
 * @author ztellman
 */
public class DistanceField {

  private final float[][][] field;
  private final Box2 shapeBounds;
  private final Box2 fieldBounds;

  private DistanceField(float[][][] field, Box2 shapeBounds, Box2 fieldBounds) {
    this.field = field;
    this.shapeBounds = shapeBounds;
    this.fieldBounds = fieldBounds;
  }

  public int width() {
    return field.length;
  }

  public int height() {
    return field[0].length;
  }

  public Box2 shapeBounds() {
    return shapeBounds;
  }

  public Box2 fieldBounds() {
    return fieldBounds;
  }

  private static Vec3 normalizedPixel(Vec3 pixel, float range) {
    return pixel.div(range / 2).add(0.5).clamp(0, 1);
  }

  private Vec3 pixel(int x, int y) {
    float[] pixel = field[x][y];
    return new Vec3(pixel[0], pixel[1], pixel[2]);
  }

  public Vec3 get(double x, double y) {
    int x1 = (int) (x * (width() - 1));
    int x2 = min(width() - 1, x1 + 1);
    int y1 = (int) (y * (height() - 1));
    int y2 = min(height() - 1, y1 + 1);

    double xt = (x * width()) - x1;
    double yt = (y * height()) - y1;

    return lerp(
      lerp(pixel(x1, y1), pixel(x1, y2), yt),
      lerp(pixel(x2, y1), pixel(x2, y2), yt),
      xt);
  }

  public Vec3 normalized(double x, double y, double scale) {
    return get(x, y).div(scale / 2).add(0.5).clamp(0, 1);
  }

  public Vec3 test(double x, double y) {
    return get(x, y).map(n -> n < 0 ? 0 : 1);
  }

  public Vec3 rendered(double x, double y) {
    Vec3 pixel = get(x, y);
    return median(pixel.x, pixel.y, pixel.z) < 0 ? Vec3.ORIGIN : vec(1, 1, 1);
  }

  public Vec3 pixel(int x, int y, float scale) {
    float[] colors = field[x][y];
    return new Vec3(colors[0], colors[1], colors[2]).div(scale / 2).add(0.5).clamp(0, 1);
  }

  public static DistanceField from(Region2 region, double sampleFrequency) {
    return from(region, 4, sampleFrequency, Math.toRadians(3));
  }

  private static class FieldCurve {
    public final Curve2 curve;
    public final Box2 bounds;
    public final byte color;

    public FieldCurve(Curve2 curve, byte color) {
      this.curve = curve;
      this.bounds = curve.bounds();
      this.color = color;
    }
  }

  public static DistanceField from(Region2 region, int padding, double sampleFrequency, double cornerThreshold) {

    Box2 shapeBounds = region.bounds();
    int w = (int) Math.ceil(shapeBounds.size().x * sampleFrequency);
    int h = (int) Math.ceil(shapeBounds.size().y * sampleFrequency);
    Vec2 pixelSize = shapeBounds.size().div(vec(w, h));
    Box2 fieldBounds = shapeBounds.expand(pixelSize.mul(padding));

    // if our point isn't outside the curves, we've got the winding direction wrong
    /*if (insideRing2s(rings, fieldBounds.lower())) {
      rings = rings.stream().map(Path2::reverse).collect(Collectors.toList());
    }*/

    IMap<Curve2, Byte> curveMap = new LinearMap<>();

    for (Ring2 r : region.rings()) {
      curveMap = curveMap.union(edgeColors(r, cornerThreshold));
    }

    FieldCurve[] curves = curveMap.stream()
      .map(e -> new FieldCurve(e.key(), e.value()))
      .toArray(FieldCurve[]::new);

    float[][][] field = new float[w][h][3];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        SignedDistance r, g, b;
        r = g = b = null;
        Vec2 t = new Vec2((x + 0.5) / (w + 1), (y + 0.5) / (h + 1));
        Vec2 p = fieldBounds.lerp(t);

        for (FieldCurve c : curves) {

          double ds = c.bounds.distanceSquared(p);
          if (r != null && g != null && b != null && ds >= r.distSquared && ds >= g.distSquared && ds >= b.distSquared) {
            continue;
          }

          SignedDistance d = new SignedDistance(c.curve, p);

          if ((c.color & RED) > 0 && (r == null || r.compareTo(d) > 0)) {
            r = d;
          }
          if ((c.color & GREEN) > 0 && (g == null || g.compareTo(d) > 0)) {
            g = d;
          }
          if ((c.color & BLUE) > 0 && (b == null || b.compareTo(d) > 0)) {
            b = d;
          }
        }

        field[x][y][0] = r != null ? (float) r.distance() : 0f;
        field[x][y][1] = g != null ? (float) g.distance() : 0f;
        field[x][y][2] = b != null ? (float) b.distance() : 0f;
      }
    }

    fixClashes(field, vec(0, 0));

    return new DistanceField(field, shapeBounds, fieldBounds);
  }

  ////

  private static final byte BLACK = 0, RED = 1, GREEN = 2, YELLOW = 3, BLUE = 4, MAGENTA = 5, CYAN = 6, WHITE = 7;

  static class SignedDistance implements Comparable<SignedDistance> {

    private static final Comparator<SignedDistance> COMPARATOR =
      Comparator
        .comparing((SignedDistance d) -> d.distSquared)
        .thenComparing(d -> d.dot);

    public final double distSquared;
    public final double pseudoDistSquared;
    public final double dot;
    public boolean inside;

    public SignedDistance(Curve2 curve, Vec2 origin) {

      double param = curve.nearestPoint(origin);
      double clampedParam = clamp(0, param, 1);
      Vec2 pos = curve.position(clampedParam);
      Vec2 dir = curve.direction(clampedParam).norm();
      Vec2 po = origin.sub(pos);

      distSquared = po.lengthSquared();
      inside = cross(dir, po) > 0;

      if (param == clampedParam) {
        dot = 0;
        pseudoDistSquared = -1;
      } else {
        // calculate pseudo-distance
        double ts = dot(po, dir);
        dot = abs(dot(dir, po.norm()));

        if (signum(ts) == signum(param)) {
          double pseudoDistance = cross(po, dir);
          pseudoDistSquared = pseudoDistance * pseudoDistance;
        } else {
          pseudoDistSquared = -1;
        }
      }
    }

    public double distance() {
      return sqrt(distanceSquared()) * (inside ? 1 : -1);
    }

    public double distanceSquared() {
      return pseudoDistSquared > 0 && pseudoDistSquared < distSquared ? pseudoDistSquared : distSquared;
    }

    @Override
    public int compareTo(SignedDistance o) {
      return COMPARATOR.compare(this, o);
    }

  }

  // returns true if this should be treated as a "sharp" corner
  private static boolean isCorner(Curve2 a, Curve2 b, double crossThreshold) {
    Vec2 ta = a.direction(1).norm();
    Vec2 tb = b.direction(0).norm();

    return dot(ta, tb) <= 0 || abs(cross(ta, tb)) > crossThreshold;
  }

  private static List<Integer> cornerIndices(Ring2 ring, double angleThreshold) {
    List<Integer> corners = new ArrayList<>();
    Curve2[] curves = ring.curves;
    double crossThreshold = sin(angleThreshold);

    Curve2 prev = curves[curves.length - 1];
    for (int i = 0; i < curves.length; i++) {
      Curve2 curr = curves[i];
      if (isCorner(prev, curr, crossThreshold)) {
        corners.add(i);
      }
      prev = curr;
    }

    return corners;
  }

  private static Curve2[] splitIntoThirds(Curve2 c) {
    return c.split(new double[]{0.33, 0.66});
  }

  // TODO: how many of these cases are avoided by splitting on inflections?
  private static IMap<Curve2, Byte> edgeColors(Ring2 ring, double angleThreshold) {
    IMap<Curve2, Byte> edgeColors = new LinearMap<>();
    List<Integer> corners = cornerIndices(ring, angleThreshold);
    Curve2[] curves = ring.curves;

    if (corners.isEmpty()) {
      // smooth contour
      for (Curve2 c : curves) {
        edgeColors.put(c, WHITE);
      }
    } else if (corners.size() == 1) {
      // teardrop
      int offset = corners.get(0);
      byte[] colors = {MAGENTA, WHITE, YELLOW};
      int num = curves.length;

      if (num >= 3) {
        for (int i = 0; i < num; i++) {
          Curve2 c = curves[(i + offset) % num];
          int colorIdx = (int) (((3 + ((2.875 * i) / (num - 1))) - 1.4375) + .5) - 2;
          edgeColors.put(c, colors[colorIdx]);
        }
      } else if (num == 2) {
        Curve2[] a = splitIntoThirds(curves[0]);
        Curve2[] b = splitIntoThirds(curves[1]);
        for (int i = 0; i < 6; i++) {
          edgeColors.put(i < 3 ? a[i] : b[i - 3], colors[i / 2]);
        }
      } else {
        Curve2[] thirds = splitIntoThirds(curves[0]);
        for (int i = 0; i < 3; i++) {
          edgeColors.put(thirds[i], colors[i]);
        }
      }
    } else {
      // multi-corner
      int offset = corners.get(0);
      int cIdx = 0;
      byte[] colors = new byte[]{corners.size() % 3 == 1 ? YELLOW : CYAN, CYAN, MAGENTA, YELLOW};

      for (int i = 0; i < curves.length; i++) {
        int idx = (i + offset) % curves.length;
        if (cIdx + 1 < corners.size() && corners.get(cIdx + 1) == idx) {
          cIdx++;
        }
        edgeColors.put(curves[idx], colors[1 + (cIdx % 3) - (cIdx == 0 ? 1 : 0)]);
      }
    }

    return edgeColors;
  }

  public static double median(double a, double b, double c) {
    return max(min(a, b), min(max(a, b), c));
  }

  private static boolean clash(float[] a, float[] b, double threshold) {
    // Only consider pair where both are on the inside or both are on the outside
    boolean aIn = (a[0] > 0 ? 1 : 0) + (a[1] > 0 ? 1 : 0) + (a[2] > 0 ? 1 : 0) >= 2;
    boolean bIn = (b[0] > 0 ? 1 : 0) + (b[1] > 0 ? 1 : 0) + (b[2] > 0 ? 1 : 0) >= 2;
    if (aIn != bIn) return false;
    // If the change is 0 <-> 1 or 2 <-> 3 channels and not 1 <-> 1 or 2 <-> 2, it is not a clash
    if ((a[0] > 0 && a[1] > 0 && a[2] > 0) || (a[0] < 0 && a[1] < 0 && a[2] < 0)
      || (b[0] > 0 && b[1] > 0 && b[2] > 0) || (b[0] < 0 && b[1] < 0 && b[2] < 0))
      return false;
    // Find which color is which: _a, _b = the changing channels, _c = the remaining one
    float aa, ab, ba, bb, ac, bc;
    if ((a[0] > 0) != (b[0] > 0) && (a[0] < 0) != (b[0] < 0)) {
      aa = a[0];
      ba = b[0];
      if ((a[1] > 0) != (b[1] > 0) && (a[1] < 0) != (b[1] < 0)) {
        ab = a[1];
        bb = b[1];
        ac = a[2];
        bc = b[2];
      } else if ((a[2] > 0) != (b[2] > 0) && (a[2] < 0) != (b[2] < 0)) {
        ab = a[2];
        bb = b[2];
        ac = a[1];
        bc = b[1];
      } else
        return false; // this should never happen
    } else if ((a[1] > 0) != (b[1] > 0) && (a[1] < 0) != (b[1] < 0)
      && (a[2] > 0) != (b[2] > 0) && (a[2] < 0) != (b[2] < 0)) {
      aa = a[1];
      ba = b[1];
      ab = a[2];
      bb = b[2];
      ac = a[0];
      bc = b[0];
    } else
      return false;
    // Find if the channels are in fact discontinuous
    return (abs(aa - ba) >= threshold)
      && (abs(ab - bb) >= threshold)
      && abs(ac) >= abs(bc); // Out of the pair, only flag the pixel farther from a shape edge

  }

  /**
   * If there's potential for a clash between two texels which are both inside, just set all channels to the same value.
   */
  private static void fixClashes(float[][][] field, Vec2 threshold) {
    int width = field.length;
    int height = field[0].length;
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        float[] color = field[i][j];
        if ((i > 0 && clash(color, field[i - 1][j], threshold.x))
          || (i < (width - 1) && clash(color, field[i + 1][j], threshold.x))
          || (j > 0 && clash(color, field[i][j - 1], threshold.y))
          || (j < (height - 1) && clash(color, field[i][j + 1], threshold.y))) {
          float median = (float) median(color[0], color[1], color[2]);
          color[0] = color[1] = color[2] = median;
        }
      }
    }
  }

  private static boolean insideRing2s(List<Path2> rings, Vec2 point) {
    return rings.stream()
      .flatMap(rs -> Arrays.stream(rs.curves()))
      .map(c -> new SignedDistance(c, point))
      .sorted()
      .findFirst()
      .get()
      .inside;
  }
}
