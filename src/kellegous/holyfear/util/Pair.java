package kellegous.holyfear.util;

public class Pair<U, V> {
  private final U u;
  private final V v;

  private Pair(U u, V v) {
    this.u = u;
    this.v = v;
  }

  public U getA() {
    return u;
  }

  public V getB() {
    return v;
  }

  public static <U, V> Pair<U, V> of(U a, V b) {
    return new Pair<>(a, b);
  }
}
