package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.*;
import io.lacuna.artifex.utils.DoubleAccumulator;
import io.lacuna.artifex.utils.Scalars;
import io.lacuna.artifex.utils.SweepQueue;
import io.lacuna.bifurcan.*;

import java.util.Arrays;

import static io.lacuna.artifex.utils.Intersections.PARAMETRIC_EPSILON;
import static io.lacuna.artifex.utils.Intersections.SPATIAL_EPSILON;
import static java.lang.Math.max;

public class Split {

  static class VertexUnion {

    private final IMap<Vec2, Vec2> parent = new LinearMap<>();
    private final ISet<Vec2> roots = new LinearSet<>();

    public void join(Vec2 a, Vec2 b) {
      a = adjust(a);
      b = adjust(b);
      int cmp = a.compareTo(b);
      if (cmp < 0) {
        parent.put(b, a);
        roots.add(a);
      } else if (cmp > 0) {
        parent.put(a, b);
        roots.add(b);
      } else {
        roots.add(b);
      }
    }

    public Vec2 adjust(Vec2 p) {
      Vec2 curr = p;
      for (; ; ) {
        Vec2 next = parent.get(curr, null);
        if (next == null) {
          if (!curr.equals(p)) {
            parent.put(p, curr);
          }
          return curr;
        }
        curr = next;
      }
    }

    public Curve2 adjust(Curve2 c) {
      Vec2 start = adjust(c.start());
      Vec2 end = adjust(c.end());

      return start.equals(end)
        ? null
        : c.endpoints(start, end);
    }

    public ISet<Vec2> roots() {
      return roots.difference(parent.keys());
    }

  }

  public static class Result {
    public final Region2 a, b;
    public final ISet<Vec2> splits;

    public Result(Region2 a, Region2 b, ISet<Vec2> splits) {
      this.a = a;
      this.b = b;
      this.splits = splits;
    }
  }

  public static Result split(Region2 a, Region2 b) {

    SweepQueue<Curve2>[] queues = new SweepQueue[]{new SweepQueue(), new SweepQueue()};

    add(a, queues[0]);
    add(b, queues[1]);

    VertexUnion union = new VertexUnion();
    IMap<Curve2, DoubleAccumulator> intersections = new LinearMap<>();

    Curve2[] cs = new Curve2[2];
    for (; ; ) {
      int idx = SweepQueue.next(queues);
      cs[idx] = queues[idx].take();

      if (cs[idx] == null) {
        break;
      }

      intersections.put(cs[idx], new DoubleAccumulator());

      for (Curve2 c : queues[1 - idx].active()) {
        cs[1 - idx] = c;
        Vec2[] ts = cs[0].intersections(cs[1]);

        for (int i = 0; i < ts.length; i++) {
          //System.out.println(ts.length + " " + ts[i] + " " + cs[0].position(ts[i].x) + " " + cs[0].position(ts[i].x).sub(cs[1].position(ts[i].y)).length() + " " + ts[i].sub(ts[max(0, i - 1)]));
          double t0 = ts[i].x;
          double t1 = ts[i].y;

          intersections.get(cs[0]).get().add(t0);
          intersections.get(cs[1]).get().add(t1);

          Vec2 p0 = cs[0].position(t0);
          Vec2 p1 = cs[1].position(t1);
          union.join(p0, p1);
        }
      }
    }

    IMap<Curve2, DoubleAccumulator> deduped = intersections.mapValues((c, acc) -> dedupe(c, acc, union));

    return new Result(
      split(a, deduped, union),
      split(b, deduped, union),
      union.roots());
  }

  private static Region2 split(Region2 region, IMap<Curve2, DoubleAccumulator> splits, VertexUnion union) {
    return new Region2(
      Arrays.stream(region.rings)
        .map(ring -> split(ring, splits, union))
        .toArray(Ring2[]::new));
  }

  private static DoubleAccumulator dedupe(Curve2 c, DoubleAccumulator acc, VertexUnion union) {

    double[] ts = acc.toArray();
    Arrays.sort(ts);

    DoubleAccumulator result = new DoubleAccumulator();
    for (int i = 0; i < ts.length; i++) {
      double t0 = result.size() == 0 ? 0 : result.last();
      double t1 = ts[i];
      if (Scalars.equals(t0, t1, PARAMETRIC_EPSILON)
        || Vec.equals(c.position(t0), c.position(t1), SPATIAL_EPSILON)) {
        union.join(c.position(t0), c.position(t1));
      } else if (Scalars.equals(t1, 1, PARAMETRIC_EPSILON)
        || Vec.equals(c.position(t1), c.end(), SPATIAL_EPSILON)) {
        union.join(c.position(t1), c.end());
      } else {
        result.add(t1);
      }
    }

    return result;
  }

  private static Ring2 split(Ring2 r, IMap<Curve2, DoubleAccumulator> splits, VertexUnion union) {
    IList<Curve2> curves = new LinearList<>();
    for (Curve2 c : r.curves) {
      DoubleAccumulator acc = splits.get(c).get();
      for (Curve2 cp : c.split(acc.toArray())) {
        cp = union.adjust(cp);
        if (cp != null) {
          curves.addLast(cp);
        }
      }
    }
    return new Ring2(curves);
  }

  private static void add(Region2 region, SweepQueue<Curve2> queue) {
    for (Ring2 r : region.rings()) {
      for (Curve2 c : r.curves) {
        queue.add(c, c.start().x, c.end().x);
      }
    }
  }


}
