package kellegous.flf.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import kellegous.flf.Bible;
import kellegous.flf.Tokenizer;
import kellegous.flf.util.Pair;
import kellegous.flf.util.StreamUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class Export {
  private static JsonGenerator gen(File dst) throws IOException {
    return new JsonFactory().createGenerator(new FileWriter(dst));
  }

  private static void writeIndex(File dst, List<Bible.Book> a, List<Bible.Book> b) throws IOException {
    try (JsonGenerator g = gen(dst)) {
      g.writeStartArray();
      for (int i = 0, n = a.size(); i < n; i++) {
        Bible.Book ab = a.get(i);
        Bible.Book bb = b.get(i);
        g.writeStartObject();
        g.writeStringField("abbr", ab.abbr());
        g.writeStringField("name_a", ab.name());
        g.writeStringField("name_b", bb.name());
        g.writeEndObject();
      }
      g.writeEndArray();
    }
  }

  private static String tokenToString(Tokenizer.Token t, int ix) {
    if (ix != 0 || t.needsLeadingSpace()) {
      return " " + t.text();
    }
    return t.text();
  }

  private static void write(JsonGenerator g, List<List<Tokenizer.Token>> text) throws IOException {
    Stream<Pair<Tokenizer.Token, Integer>> s = StreamUtil.zip(text.stream(), Stream.iterate(0, i -> i + 1))
        .flatMap(p -> p.getA().stream().map(
            t -> Pair.of(t, p.getB())));

    g.writeStartArray();
    for (Pair<Tokenizer.Token, Integer> p : StreamUtil.asIterable(s)) {
      Tokenizer.Token t = p.getA();
      g.writeStartObject();
      g.writeStringField("pos", t.pos());
      g.writeStringField("text", tokenToString(t, p.getB()));
      g.writeEndObject();
    }
    g.writeEndArray();
  }

  private static void write(JsonGenerator g, String number, Bible.Verse a, Bible.Verse b) throws IOException {
    g.writeStartObject();
    g.writeStringField("num", number);

    g.writeFieldName("text_a");
    write(g, a.text());

    g.writeFieldName("text_b");
    write(g, b.text());

    g.writeEndObject();
  }

  private static void writeBook(File dst, Bible.Book a, Bible.Book b) throws IOException {
    try (JsonGenerator g = gen(dst)) {
      g.writeStartObject();
      g.writeStringField("name_a", a.name());
      g.writeStringField("name_b", b.name());

      g.writeFieldName("verses");
      g.writeStartArray();
      List<Bible.Chapter> acs = a.chapters();
      List<Bible.Chapter> bcs = b.chapters();
      for (int i = 0, n = acs.size(); i < n; i++) {
        Bible.Chapter ac = acs.get(i);
        Bible.Chapter bc = bcs.get(i);

        List<Bible.Verse> av = ac.verses();
        List<Bible.Verse> bv = bc.verses();
        for (int j = 0, m = av.size(); j < m; j++) {
          write(g, String.format("%d.%d", ac.number(), av.get(j).number()), av.get(j), bv.get(j));
        }
      }
      g.writeEndArray();
    }
  }

  public static void toJson(
      File dst,
      List<Bible.Book> a,
      List<Bible.Book> b) throws IOException {
    writeIndex(new File(dst, "index.json"), a, b);

    for (int i = 0, n = a.size(); i < n; i++) {
      writeBook(new File(dst, String.format("%s.json", a.get(i).abbr())), a.get(i), b.get(i));
    }
  }
}
