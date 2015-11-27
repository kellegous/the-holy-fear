package kellegous.holyfear.util;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
  private static class MergeIter<A, B> implements Iterator<Pair<A, B>> {
    private final Iterator<A> a;
    private final Iterator<B> b;

    MergeIter(Iterator<A> a, Iterator<B> b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public boolean hasNext() {
      return a.hasNext();
    }

    @Override
    public Pair<A, B> next() {
      return Pair.of(
          a.hasNext() ? a.next() : null,
          b.hasNext() ? b.next() : null);
    }
  }

  private static class Adapter<T> extends Spliterators.AbstractSpliterator<T> {
    private final Iterator<T> iter;

    Adapter(Iterator<T> iter) {
      super(Long.MAX_VALUE, 0);
      this.iter = iter;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      if (!iter.hasNext()) {
        return false;
      }

      action.accept(iter.next());
      return true;
    }
  }

  private static class SlidingIter<T> implements Iterator<List<T>> {
    private final Iterator<T> iter;
    private final int n;
    private final ArrayDeque<T> win = new ArrayDeque<>();

    SlidingIter(Iterator<T> iter, int n) {
      this.iter = iter;
      this.n = n;
      while (iter.hasNext() && win.size() < n) {
        win.addLast(iter.next());
      }
    }

    private void advance() {
      if (iter.hasNext()) {
        win.removeFirst();
        win.addLast(iter.next());
      } else {
        win.clear();
      }
    }

    @Override
    public boolean hasNext() {
      return !win.isEmpty();
    }

    @Override
    public List<T> next() {
      advance();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      // that's a bummer right?
      return new ArrayList<>(win);
    }
  }

  public static <T> Iterable<T> asIterable(Stream<T> s) {
    return s::iterator;
  }

  public static <A, B> Stream<Pair<A, B>> zip(Stream<A> a, Stream<B> b) {
    return StreamSupport.stream(
        new Adapter<>(new MergeIter<>(a.iterator(), b.iterator())),
        false);
  }

  public static <T> Stream<List<T>> sliding(Stream<T> stream, int n) {
    return StreamSupport.stream(
        new Adapter<>(new SlidingIter<T>(stream.iterator(), n)),
        false);
  }
}
