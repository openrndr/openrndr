package io.lacuna.artifex.utils.regions;

import io.lacuna.artifex.*;
import io.lacuna.artifex.Ring2.Result;
import io.lacuna.artifex.utils.Combinatorics;
import io.lacuna.bifurcan.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class Clip {

  public static final BinaryOperator<Arc> SHORTEST_ARC = (x, y) -> x.length() < y.length() ? x : y;

  // for debug purposes

  private static final ISet<Vec2> VERTICES = new LinearSet<>();

  private static void describe(String prefix, IList<Vec2>... arcs) {
    for (IList<Vec2> arc : arcs) {
      arc.forEach(VERTICES::add);
      System.out.print(prefix + " ");
      arc.forEach(v -> System.out.print(VERTICES.indexOf(v) + " "));

      System.out.println();
    }
  }

  /**
   * an Arc is a list of curves, either describing a loop or an edge between two points of intersection
   */
  private final static class Arc extends LinearList<Curve2> {

    private double
      length = Double.NaN,
      area = Double.NaN;

    double length() {
      if (Double.isNaN(length)) {
        length = stream().mapToDouble(c -> c.end().sub(c.start()).length()).sum();
      }
      return length;
    }

    Vec2 head() {
      return first().start();
    }

    Vec2 tail() {
      return last().end();
    }

    Vec2 position(double t) {
      double length = length(),
        offset = 0,
        threshold = length * t;

      for (Curve2 c : this) {
        double l = c.end().sub(c.start()).length();
        Interval i = new Interval(offset, offset + l);
        if (i.contains(threshold)) {
          return c.position(i.normalize(threshold));
        }
        offset = i.hi;
      }

      throw new IllegalStateException();
    }

    Arc reverse() {
      Arc result = new Arc();
      forEach(c -> result.addFirst(c.reverse()));
      return result;
    }

    IList<Vec2> vertices() {
      IList<Vec2> result = new LinearList<Vec2>().addLast(head());
      forEach(c -> result.addLast(c.end()));
      return result;
    }

    double signedArea() {
      if (Double.isNaN(area)) {
        area = stream().mapToDouble(Curve2::signedArea).sum();
      }
      return area;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }
  }

  private static double area(IList<Arc> arcs) {
    return abs(arcs.stream().mapToDouble(Arc::signedArea).sum());
  }

  private static double length(IList<Arc> arcs) {
    return abs(arcs.stream().mapToDouble(Arc::length).sum());
  }

  private static Ring2 ring(IList<Arc> arcs) {
    IList<Curve2> acc = new LinearList<>();
    arcs.forEach(arc -> arc.forEach(acc::addLast));
    return new Ring2(acc);
  }

  private static Arc arc(Curve2... cs) {
    Arc result = new Arc();
    for (Curve2 c : cs) {
      result.addLast(c);
    }
    return result;
  }

  private static <U, V> IList<V> edges(IList<U> vertices, BiFunction<U, U, V> edge) {
    IList<V> result = new LinearList<>();
    for (int i = 0; i < vertices.size() - 1; i++) {
      result.addLast(edge.apply(vertices.nth(i), vertices.nth(i + 1)));
    }
    return result;
  }

  ///

  private enum Operation {
    UNION,
    INTERSECTION,
    DIFFERENCE
  }

  private enum Type {
    OUTSIDE,
    INSIDE,
    SAME_EDGE,
    DIFF_EDGE
  }

  private static boolean isTop(Curve2 c) {
    if (c == null) {
      return false;
    }

    double delta = c.end().x - c.start().x;
    if (delta == 0) {
      return c.end().y > c.start().y;
    }
    return delta < 0;
  }

  private static Type classify(Region2 region, Arc arc) {
    Result result = region.test(arc.position(0.5));
    if (!result.inside) {
      return Type.OUTSIDE;
    } else if (result.curve == null) {
      return Type.INSIDE;
    } else {
      return isTop(arc.first()) == isTop(result.curve) ? Type.SAME_EDGE : Type.DIFF_EDGE;
    }
  }

  /**
   * Cuts the rings of a region at the specified vertices, yielding a list of arcs that will serve as the edges of our
   * graph.
   */
  private static IList<Arc> partition(Region2 region, ISet<Vec2> vertices) {
    IList<Arc> result = new LinearList<>();

    for (Ring2 r : region.rings) {
      Curve2[] cs = r.curves;
      int offset = 0;
      for (; offset < cs.length; offset++) {
        if (vertices.contains(cs[offset].start())) {
          break;
        }
      }

      if (offset == cs.length) {
        result.addLast(arc(cs));
      } else {
        Arc acc = new Arc();
        for (int i = offset; i < cs.length; i++) {
          Curve2 c = cs[i];
          if (vertices.contains(c.start())) {
            if (acc.size() > 0) {
              result.addLast(acc);
            }
            acc = arc(c);
          } else {
            acc.addLast(c);
          }
        }

        for (int i = 0; i < offset; i++) {
          acc.addLast(cs[i]);
        }

        if (acc.size() > 0) {
          result.addLast(acc);
        }
      }
    }

    return result;
  }

  public static IList<IList<Arc>> greedyPairing(IGraph<Vec2, Arc> graph, IList<Vec2> out, ISet<Vec2> in) {
    IList<IList<Arc>> result = new LinearList<>();

    ISet<Vec2> remaining = LinearSet.from(in);
    for (Vec2 v : out) {
      IList<Vec2> path = Graphs.shortestPath(graph, v, remaining::contains, e -> e.value().length()).orElse(null);
      if (path == null) {
        return null;
      }

      remaining.remove(path.last());
      result.addLast(edges(path, graph::edge));
    }

    return result;
  }

  public static IList<IList<Arc>> repairGraph(IGraph<Vec2, ISet<Arc>> graph, Iterable<Arc> unused) {

    // create a graph of all the unused arcs
    IGraph<Vec2, Arc> search = new DirectedGraph<Vec2, Arc>().linear();
    for (Arc arc : unused) {
      search.link(arc.head(), arc.tail(), arc, SHORTEST_ARC);
    }

    // add in the existing arcs as reversed edges, so we can potentially retract them
    for (IEdge<Vec2, ISet<Arc>> e : graph.edges()) {
      Arc arc = e.value().stream().min(Comparator.comparingDouble(Arc::length)).get();
      search.link(arc.tail(), arc.head(), arc, SHORTEST_ARC);
    }

    //search.vertices().forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + search.out(v).stream().map(VERTICES::indexOf).collect(Lists.linearCollector())));

    ISet<Vec2>
      out = graph.vertices().stream().filter(v -> graph.out(v).size() == 0).collect(Sets.linearCollector()),
      in = graph.vertices().stream().filter(v -> graph.in(v).size() == 0).collect(Sets.linearCollector()),
      currOut = out.clone(),
      currIn = in.clone();

    //describe("out", out.elements());
    //describe("in", in.elements());

    // attempt to greedily pair our srcs and dsts
    IList<IList<Arc>> result = new LinearList<>();
    while (currIn.size() > 0 && currOut.size() > 0) {
      IList<Vec2> path = Graphs.shortestPath(search, currOut, in::contains, e -> e.value().length()).orElse(null);

      // if our search found a vertex that was previously claimed, we need something better than a greedy search
      if (path == null || !currIn.contains(path.last())) {
        break;

      } else {
        currOut.remove(path.first());
        currIn.remove(path.last());
        result.addLast(edges(path, search::edge));
      }
    }

    if (currIn.size() == 0 || currOut.size() == 0) {
      return result;
    }

    // do greedy pairings with every possible vertex ordering, and choose the one that results in the shortest aggregate
    // paths
    return Combinatorics.permutations(out.elements())
      .stream()
      .map(vs -> greedyPairing(search, vs, in))
      .filter(Objects::nonNull)
      .min(Comparator.comparingDouble(paths -> paths.stream().mapToDouble(Clip::length).sum()))
      .get();
  }

  public static Region2 operation(Region2 ra, Region2 rb, Operation operation, Predicate<Type> aPredicate, Predicate<Type> bPredicate) {

    Split.Result split = Split.split(ra, rb);
    Region2 a = split.a;
    Region2 b = split.b;

    // Partition rings into arcs separated at intersection points
    IList<Arc>
      pa = partition(a, split.splits),
      pb = partition(b, split.splits);

    if (operation == Operation.DIFFERENCE) {
      pb = pb.stream().map(Arc::reverse).collect(Lists.linearCollector());
    }

    // Filter out arcs which are to be ignored, per our operation
    ISet<Arc> arcs = new LinearSet<>();
    pa.stream()
      .filter(arc -> aPredicate.test(classify(b, arc)))
      .forEach(arcs::add);
    pb.stream()
      .filter(arc -> bPredicate.test(classify(a, arc)))
      .forEach(arcs::add);

    //describe("split", split.splits.elements());
    //describe("arcs", arcs.elements().stream().map(Arc::vertices).toArray(IList[]::new));
    //VERTICES.forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + v));

    IList<Ring2> result = new LinearList<>();
    ISet<Arc> consumed = new LinearSet<>();

    // First we're going to extract a cycle, and on the second go-around we'll try to "repair" the remaining edges, and
    // extract any additional cycles we create in the process.
    for (int i = 0; i < 2; i++) {

      // Construct a graph where the edges are the set of all arcs connecting the vertices
      IGraph<Vec2, ISet<Arc>> graph = new DirectedGraph<Vec2, ISet<Arc>>().linear();
      arcs.forEach(arc -> graph.link(arc.head(), arc.tail(), LinearSet.of(arc), ISet::union));

      //graph.vertices().forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + graph.out(v).stream().map(VERTICES::indexOf).collect(Lists.linearCollector())));

      if (i == 1) {
        for (IList<Arc> path : repairGraph(graph, LinearSet.from(pa.concat(pb)).difference(arcs).difference(consumed))) {
          for (Arc arc : path) {
            // if the graph currently contains the arc, remove it
            if (arcs.contains(arc)) {
              //describe("remove", arc.vertices());
              graph.unlink(arc.head(), arc.tail());
              arcs.remove(arc);

              // if the graph doesn't contain the arc, add it
            } else {
              //describe("add", arc.vertices());
              graph.link(arc.head(), arc.tail(), LinearSet.of(arc));
              arcs.add(arc);
            }
          }
        }
      }

      // find every cycle in the graph, and then expand those cycles into every possible arc combination, yielding a bunch
      // of rings ordered from largest to smallest
      IList<IList<Arc>> cycles = Graphs.cycles(graph)
        .stream()
        .map(cycle -> edges(cycle, (x, y) -> graph.edge(x, y).elements()))
        .map(Combinatorics::combinations)
        .flatMap(IList::stream)
        .sorted(Comparator.comparingDouble(Clip::area).reversed())
        .collect(Lists.linearCollector());

      // extract as many cycles as possible without using the same arc twice
      for (IList<Arc> cycle : cycles) {
        //describe("cycle", cycle.stream().map(Arc::vertices).toArray(IList[]::new));
        if (cycle.stream().anyMatch(consumed::contains)) {
          continue;
        }

        cycle.forEach(consumed::add);
        result.addLast(ring(cycle));
      }

      arcs = arcs.difference(consumed);

      if (arcs.size() == 0) {
        break;
      }
    }

    return new Region2(result);
  }

  ///

  public static Region2 union(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.UNION,
      t -> t == Type.OUTSIDE || t == Type.SAME_EDGE,
      t -> t == Type.OUTSIDE);
  }

  public static Region2 intersection(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.INTERSECTION,
      t -> t == Type.INSIDE || t == Type.SAME_EDGE,
      t -> t == Type.INSIDE);
  }

  public static Region2 difference(Region2 a, Region2 b) {
    return operation(a, b,
      Operation.DIFFERENCE,
      t -> t == Type.OUTSIDE || t == Type.DIFF_EDGE,
      t -> t == Type.INSIDE);
  }

}
