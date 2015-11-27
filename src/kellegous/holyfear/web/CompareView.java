package kellegous.holyfear.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import kellegous.holyfear.Bible;
import kellegous.holyfear.Tokenizer;
import kellegous.holyfear.util.StreamUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompareView {
  private static class Book {
    public final String rName;
    public final String lName;
    public final List<Verse> verses;

    Book(String lName, String rName, Stream<Verse> verses) {
      this.lName = lName;
      this.rName = rName;
      this.verses = verses.collect(Collectors.toList());
    }
  }

  private static class Verse {
    public final String number;
    public final List<Token> rVerse;
    public final List<Token> lVerse;

    Verse(String number, List<Token> lVerse, List<Token> rVerse) {
      this.number = number;
      this.lVerse = lVerse;
      this.rVerse = rVerse;
    }
  }

  private static class Token {
    public final int id;
    public final String text;
    public final String pos;

    Token(int id, String text, String pos) {
      this.id = id;
      this.text = text;
      this.pos = pos;
    }

    static String toText(Tokenizer.Token t, int ix) {
      if (ix != 0 || t.needsLeadingSpace()) {
        return " " + t.text();
      }
      return t.text();
    }

    static List<Token> from(List<List<Tokenizer.Token>> text) {
      return StreamUtil.zip(text.stream(), Stream.iterate(0, i -> i + 1))
          .flatMap(p -> p.getA().stream().map(
              t -> new Token(p.getB(), toText(t, p.getB()), t.pos())))
          .collect(Collectors.toList());
    }
  }

  private static class Ctx {
    public List<Book> books;

    Ctx(Stream<Book> books) {
      this.books = books.collect(Collectors.toList());
    }
  }

  private static Stream<Verse> toVerseData(List<Bible.Chapter> a, List<Bible.Chapter> b) {
    return StreamUtil.zip(a.stream(), b.stream())
        .flatMap(pc -> StreamUtil.zip(pc.getA().verses().stream(), pc.getB().verses().stream())
            .map(pv -> new Verse(
                String.format("%d.%d", pc.getA().number(), pv.getA().number()),
                Token.from(pv.getA().text()),
                Token.from(pv.getB().text()))));
  }

  private static Stream<Book> toData(List<Bible.Book> l, List<Bible.Book> r) {
    return StreamUtil.zip(l.stream(), r.stream())
        .map(p -> new Book(
            p.getA().name(),
            p.getB().name(),
            toVerseData(p.getA().chapters(), p.getB().chapters())));
  }

  private static String resourcePathFor(String name) throws IOException {
    String path = CompareView.class.getPackage().getName().replace('.', '/');
    return path + "/" + name;
  }

  private static void copyResource(String name, File dir) throws IOException {
    try (Reader r = new InputStreamReader(CompareView.class.getResourceAsStream(name));
         Writer w = new FileWriter(new File(dir, name))) {
      IOUtils.copy(r, w);
    }
  }

  public static void render(File dst, List<Bible.Book> a, List<Bible.Book> b) throws IOException {
    a = a.subList(0, 1);
    b = b.subList(0, 1);

    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache m = mf.compile(resourcePathFor("compare-view.mustache"));

    FileUtils.forceMkdir(dst);

    try (Writer w = new FileWriter(new File(dst, "index.html"))) {
      m.execute(w, new Ctx(toData(a, b)));
    }

    copyResource("index.css", dst);
  }
}
