package io.lacuna.artifex.utils.regions;

/**
 * @author ztellman
 */
public class Triangles {

  /*
  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

  public static void triangulate(EdgeList edges) {
    for (HalfEdge e : edges.faces()) {
      if (e.flag == INSIDE) {
        triangulate(edges, e);
      }
    }
  }

  public static void triangulate(EdgeList edges, HalfEdge init) {

    IList<HalfEdge> halfEdges = new LinearList<>();

    HalfEdge curr = init;
    do {
      halfEdges.addLast(curr);
      curr = curr.next;
    } while (curr != init);

    if (halfEdges.size() <= 3) {
      return;
    }
    halfEdges = Lists.sort(halfEdges, (a, b) -> COMPARATOR.compare(a.start(), b.start()));

    ISet<Vec2> left = new LinearSet<>();
    curr = halfEdges.first();
    while (curr != halfEdges.last()) {
      left.add(curr.start());
      curr = curr.next;
    }

    IList<Vec2> vertices = halfEdges.stream().map(HalfEdge::start).collect(Lists.linearCollector());

    LinearList<Vec2> stack = new LinearList<Vec2>()
      .addLast(vertices.nth(0))
      .addLast(vertices.nth(1));

    for (int i = 2; i < vertices.size() - 1; i++) {
      Vec2 u = vertices.nth(i);
      boolean isLeft = left.contains(u);

      if (isLeft == left.contains(stack.last())) {
        Vec2 popped = stack.popLast();
        while (stack.size() > 0) {
          Vec2 v = stack.last();

          double theta = -angleBetween(popped.sub(u), v.sub(u));
          boolean isVisible = isLeft
            ? Scalars.inside(1e-3, theta, Math.PI)
            : Scalars.inside(1e-3, (Math.PI * 2) - theta, Math.PI);

          if (isVisible) {
            popped = stack.popLast();
            edges.add(u, v, INSIDE, INSIDE);
          } else {
            break;
          }
        }

        stack
          .addLast(popped)
          .addLast(u);
      } else {
        while (stack.size() > 1) {
          Vec2 v = stack.popLast();
          edges.add(u, v, INSIDE, INSIDE);
        }
        stack
          .removeLast()
          .addLast(vertices.nth(i - 1))
          .addLast(u);
      }
    }

    stack.removeLast();
    while (stack.size() > 1) {
      edges.add(vertices.last(), stack.popLast(), INSIDE, INSIDE);
    }
  }

  /*public static IList<Fan2> fans(EdgeList edges) {

    IList<Fan2> result = new LinearList<>();

    for (HalfEdge init : edges.faces()) {

      if (init.flag == Hulls.HULL) {
        HalfEdge curr = init;
        while (curr.curve instanceof Line2) {
          curr = curr.next;
        }

        result.addLast(Fan2.curve(curr.curve));

      } else if (init.flag == Hulls.INSIDE) {

        HalfEdge a = init;
        HalfEdge b = a.next;
        HalfEdge c = b.next;

        assert c.next == a;

        boolean xa = a.twin.flag == OUTSIDE;
        boolean xb = b.twin.flag == OUTSIDE;
        boolean xc = c.twin.flag == OUTSIDE;

        int flag = (xa ? 1 : 0) | (xb ? 2 : 0) | (xc ? 4 : 0);
        if (flag == 0) {
          result.addLast(Fan2.internal(a.start(), b.start(), c.start()));
        } else {
          Vec2 centroid = null;
          switch (flag) {
            case 1:
              centroid = c.start();
              break;
            case 2:
              centroid = a.start();
              break;
            case 3:
              centroid = c.curve.position(0.5);
              break;
            case 4:
              centroid = b.start();
              break;
            case 5:
              centroid = b.curve.position(0.5);
              break;
            case 6:
              centroid = a.curve.position(0.5);
              break;
            case 7:
              centroid = a.start().add(b.start()).add(c.start()).div(3);
              break;
          }

          if (xa) result.addLast(Fan2.external(centroid, (Line2) a.curve));
          if (xb) result.addLast(Fan2.external(centroid, (Line2) b.curve));
          if (xc) result.addLast(Fan2.external(centroid, (Line2) c.curve));
        }
      }
    }

    return result;
  }*/

}
