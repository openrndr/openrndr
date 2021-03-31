package io.lacuna.artifex.utils;

import io.lacuna.artifex.*;
import io.lacuna.bifurcan.*;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.IntPredicate;

import static io.lacuna.artifex.Vec.vec;
import static io.lacuna.artifex.utils.Scalars.EPSILON;
import static io.lacuna.artifex.utils.Scalars.angleEquals;

/**
 * An implementation of a doubly-connected edge list.  Since this is an inherently mutable data structure, it is
 * not exposed at the top-level, and instead only used to transform between immutable geometric representations.
 *
 * @author ztellman
 */
public class EdgeList {

  public static class HalfEdge {
    public HalfEdge prev, next, twin;
    public final Vec2 origin;

    private HalfEdge(Vec2 origin, HalfEdge twin) {
      this.origin = origin;
      this.twin = twin;
    }

    public HalfEdge(Vec2 a, Vec2 b) {
      this.origin = a;
      this.twin = new HalfEdge(b, this);
    }

    public boolean visible(Vec2 p) {
      double t0 = Vec2.angleBetween(prev.origin.sub(origin), p.sub(origin));
      double t1 = interiorAngle();

      return 0 < t0 && t0 <= t1;
    }

    public double interiorAngle() {
      return -Vec2.angleBetween(prev.origin.sub(origin), twin.origin.sub(origin));
    }

    public void link(HalfEdge e) {
      assert twin.origin.equals(e.origin);

      this.next = e;
      e.prev = this;
    }

    public Iterable<HalfEdge> face() {
      return () -> new Iterator<HalfEdge>() {
        HalfEdge curr = HalfEdge.this;
        boolean started = false;

        @Override
        public boolean hasNext() {
          return curr != null && (!started || curr != HalfEdge.this);
        }

        @Override
        public HalfEdge next() {
          HalfEdge result = curr;
          started = true;
          curr = curr.next;
          return result;
        }
      };
    }

    @Override
    public String toString() {
      return origin + " -> " + twin.origin;
    }
  }

  ///

  private final LinearMap<Vec2, HalfEdge> vertices = new LinearMap<>();

  // a potentially redundant list of edges on different faces, which will be cleaned up if we ever iterate over them
  private final LinearSet<HalfEdge> pseudoFaces = new LinearSet<>();
  private boolean invalidated = false;

  public EdgeList() {
  }

  /// accessors

  public ISet<Vec2> vertices() {
    return vertices.keys();
  }

  public IList<HalfEdge> faces() {
    if (invalidated) {
      for (int i = 0; i < pseudoFaces.size(); i++) {
        HalfEdge e = pseudoFaces.nth(i);

        int j = 0;
        HalfEdge curr = e.next;
        while (curr != null && curr != e) {
          if (j++ > 1_000_000) {
            throw new IllegalStateException(e.toString());
          }
          pseudoFaces.remove(curr);
          curr = curr.next;
        }
      }
      invalidated = false;
    }

    return LinearList.from(pseudoFaces.elements());
  }

  /// modifiers

  private void registerFace(HalfEdge e) {
    pseudoFaces.add(e).add(e.twin);
    invalidated = true;
  }

  private void insert(HalfEdge src, HalfEdge e) {
    src.prev.link(e);
    e.twin.link(src);
    registerFace(e);
  }

  private void insert(HalfEdge e) {
    HalfEdge src = vertices.get(e.origin, null);
    if (src == null) {
      vertices.put(e.origin, e);
      registerFace(e);

    } else if (src.prev == null) {
      e.twin.link(src);
      src.twin.link(e);

    } else {
      HalfEdge curr = src;
      for (; ; ) {
        if (curr.visible(e.twin.origin)) {
          insert(curr, e);
          break;
        } else {
          curr = curr.twin.next;
        }

        assert curr != src;
      }
    }
  }

  public HalfEdge add(Vec2 a, Vec2 b) {
    HalfEdge e = new HalfEdge(a, b);
    insert(e);
    insert(e.twin);
    return e;
  }

  ///


  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (HalfEdge e : faces()) {
      IList<HalfEdge> l = LinearList.from(e.face());
      l.forEach(x -> b.append(x.origin).append(" -> "));
      b.append(l.last().next == null ? "null" : "cycle").append("\n");
    }
    return b.toString();
  }
}
