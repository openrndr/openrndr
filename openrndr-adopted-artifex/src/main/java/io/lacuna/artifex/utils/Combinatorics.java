package io.lacuna.artifex.utils;

import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.Lists;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Combinatorics {

  public static final int MAX_RESULTS = 32;

  public static <V> void swap(V[] ary, int i, int j) {
    V tmp = ary[i];
    ary[i] = ary[j];
    ary[j] = tmp;
  }

  public static <V> V randNth(IList<V> list) {
    return list.nth(ThreadLocalRandom.current().nextInt((int) list.size()));
  }

  public static <V> IList<V> shuffle(IList<V> values) {
    Object[] ary = values.toArray();
    ThreadLocalRandom gen = ThreadLocalRandom.current();
    for (int i = ary.length - 1; i > 0; i--) {
      swap(ary, gen.nextInt(i + 1), i);
    }
    return (IList<V>) Lists.from(ary);
  }

  public static <V> IList<IList<V>> permutations(IList<V> values) {

    // if exhaustive searching is out of the question, put your trust in the RNG
    if (values.size() > 4) {
      return IntStream.range(0, MAX_RESULTS)
        .mapToObj(i -> shuffle(values))
        .collect(Lists.linearCollector());
    }

    IList<IList<V>> result = new LinearList<>();

    Object[] ary = values.toArray();
    int[] c = new int[ary.length];
    int i = 0;

    result.addLast((IList<V>) Lists.from(ary.clone()));

    while (i < ary.length) {
      if (c[i] < i) {
        swap(ary, i % 2 == 0 ? 0 : c[i], i);
        result.addLast((IList<V>) Lists.from(ary.clone()));
        c[i]++;
        i = 0;
      } else {
        c[i] = 0;
        i++;
      }
    }

    return result;
  }

  /**
   * Given a list of potential values at each index in a list, returns all possible combinations of those values.
   */
  public static <V> IList<IList<V>> combinations(IList<IList<V>> paths) {
    long count = paths.stream().mapToLong(IList::size).reduce(1, (a, b) -> a * b);
    if (count == 0) {
      return Lists.EMPTY;

    } else if (count == 1) {
      return LinearList.of(
        paths.stream()
          .map(IList::first)
          .collect(Lists.linearCollector()));

    } else if (count > MAX_RESULTS) {
      return IntStream.range(0, MAX_RESULTS)
        .mapToObj(i -> paths.stream()
          .map(Combinatorics::randNth)
          .collect(Lists.linearCollector()))
        .collect(Lists.linearCollector());
    }

    int[] indices = new int[(int) paths.size()];
    IList<IList<V>> result = new LinearList<>();

    while (indices[0] < paths.first().size()) {
      IList<V> path = new LinearList<>(indices.length);
      for (int i = 0; i < indices.length; i++) {
        path.addLast(paths.nth(i).nth(indices[i]));
      }
      result.addLast(path);

      for (int i = indices.length - 1; i >= 0; i--) {
        if (++indices[i] < paths.nth(i).size()) {
          break;
        } else if (i > 0) {
          indices[i] = 0;
        }
      }
    }

    return result;
  }

}
