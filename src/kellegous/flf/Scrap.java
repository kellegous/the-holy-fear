package kellegous.flf;

import java.io.IOException;
import java.util.*;

public class Scrap {
  private static <T> Map<T, Set<T>> merge(Map<T, Set<T>> a, Map<T, Set<T>> b) {
    Map<T, Set<T>> m = new HashMap<>();
    a.entrySet().forEach(e -> m.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue()));
    b.entrySet().forEach(e -> m.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue()));
    return m;
  }

  private static <T> List<T> firstN(Set<T> s, int n) {
    List<T> list = new ArrayList<>(s);

    if (list.size() < n) {
      return list;
    }

    return list.subList(0, n);
  }

  public static void main(String[] args) throws IOException {
//    Reader r = new StringReader("“At least I think it was them...” “Well, anyway, here I am. And I tell you that was one hell of a long night, man! Seven hours on that goddamn bus! But when I woke up it was dawn and here I was in downtown Vegas and for a minute I didn’t know what the hell I was doin’ here. All I could think was, ‘O Jesus, here we go again: Who’s divorced me this time?’” He accepted a cigarette from somebody in the crowd, still grinning as he lit up. “But then I remembered, by God! I was here for the Mint 400… and, man, that’s all I needed to know. I tell you it’s wonderful to be here, man. I don’t give a damn who wins or loses. It’s just wonderful to be here with you people. Nobody argued with him. We all understood. In some circles, the “Mint 400” is a far, far better thing than the Super Bowl, the Kentucky Derby and the Lower Oakland Roller Derby Finals all rolled into one,” he said. Right?");
//
//    Bible bible = Bible.readFrom(new File("kjvdat.txt"));
//    Tokenizer tokenizer = Tokenizer.create(new File("dst"));
//
//    Map<String, Set<String>> allPos = bible.allVerses().<Map<String, Set<String>>>reduce(
//        new HashMap<>(),
//        (m, v) -> {
//          List<List<Tokenizer.Token>> tokens = tokenizer.tokenize(new StringReader(v.text()));
//          tokens.forEach(x -> x.forEach(t -> m.computeIfAbsent(t.pos(), k -> new HashSet<>()).add(t.text())));
//          return m;
//        },
//        (a, b) -> merge(a, b));
//
//    allPos.entrySet().forEach(
//        e -> System.out.printf(
//            "%s (%d): %s\n",
//            e.getKey(),
//            e.getValue().size(),
//            Joiner.on(", ").join(firstN(e.getValue(), 5))));
  }
}
