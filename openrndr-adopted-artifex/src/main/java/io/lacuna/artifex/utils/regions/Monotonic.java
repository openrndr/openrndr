package io.lacuna.artifex.utils.regions;

/**
 * @author ztellman
 */
public class Monotonic {

  /*

  private static final Comparator<Vec2> COMPARATOR = Comparator
    .comparingDouble((Vec2 a) -> -a.y)
    .thenComparingDouble(a -> a.x);

  private static class State {
    private final LinearMap<HalfEdge, Vec2> helper = new LinearMap<>();
    private final FloatMap<HalfEdge> map = new FloatMap<HalfEdge>().linear();

    public State() {
    }

    private static double key(HalfEdge e) {
      return Math.min(e.origin.x, e.twin.origin.x);
    }

    public void add(HalfEdge e) {
      map.put(key(e), e);
      helper.put(e, e.origin);
    }

    public void helper(HalfEdge a, Vec2 v) {
      helper.put(a, v);
    }

    public Vec2 helper(HalfEdge a) {
      return helper.get(a).get();
    }

    public void remove(HalfEdge e) {
      HalfEdge x = map.get(key(e), null);
      if (x == e) {
        map.remove(key(e));
      }
      helper.remove(e);
    }

    public HalfEdge search(Vec2 v) {
      return map.floor(v.x).value();
    }

    @Override
    public String toString() {
      return map.keys().toString();
    }
  }

  enum VertexType {
    REGULAR,
    SPLIT,
    MERGE,
    START,
    END
  }

  private static boolean above(Vec2 a, Vec2 b) {
    return COMPARATOR.compare(a, b) < 0;
  }

  private static VertexType vertexType(HalfEdge e) {
    Vec2 a = e.prev.origin;
    Vec2 b = e.origin;
    Vec2 c = e.twin.origin;

    if (above(a, b) && above(c, b)) {
      return e.interiorAngle() < Math.PI ? VertexType.END : VertexType.MERGE;
    } else if (above(b, a) && above(b, c)) {
      return e.interiorAngle() < Math.PI ? VertexType.START : VertexType.SPLIT;
    } else {
      return VertexType.REGULAR;
    }
  }

  public static void monotonize(EdgeList edges) {

    // state
    PriorityQueue<HalfEdge> heap = new PriorityQueue<>((a, b) -> COMPARATOR.compare(a.origin, b. origin));
    IMap<Vec2, VertexType> type = new LinearMap<>();
    State state = new State();

    for (Vec2 v : edges.vertices()) {

      HalfEdge e = edges.edge(v, INSIDE);
      heap.add(e);
      type.put(e.start(), vertexType(e));
    }

    // helpers
    BiConsumer<HalfEdge, HalfEdge> connectMergeHelper = (a, b) -> {
      Vec2 v = state.helper(b);
      if (type.get(v).get() == VertexType.MERGE) {
        edges.add(a.start(), v, INSIDE, INSIDE);
      }
    };

    Consumer<HalfEdge> connectLeftHelper = e -> {
      HalfEdge left = state.search(e.origin);
      connectMergeHelper.accept(e, left);
      state.helper(left, e.origin);
    };

    // add diagonals
    while (!heap.isEmpty()) {
      HalfEdge curr = heap.poll();
      HalfEdge prev = curr.prev;
      switch (type.get(curr.start()).get()) {
        case START:
          state.add(curr);
          break;

        case END:
          connectMergeHelper.accept(curr, prev);
          state.remove(prev);
          break;

        case SPLIT:
          HalfEdge left = state.search(curr.origin);
          Vec2 v = state.helper(left);
          edges.add(curr.origin, v);

          state.helper(left, curr.origin);
          state.add(curr);
          break;

        case MERGE:
          connectMergeHelper.accept(curr, prev);

          state.remove(prev);
          connectLeftHelper.accept(curr);
          break;

        case REGULAR:
          if (above(prev.start(), curr.start())) {
            connectMergeHelper.accept(curr, prev);
            state.remove(prev);
            state.add(curr);
          } else {
            connectLeftHelper.accept(curr);
          }
          break;
      }
    }
  }

  */
}
