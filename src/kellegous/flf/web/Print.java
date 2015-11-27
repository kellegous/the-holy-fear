package kellegous.flf.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import kellegous.flf.Bible;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class Print {
  private static String resourcePathFor(String name) {
    String path = Print.class.getPackage().getName().replace('.', '/');
    return path + "/" + name;
  }

  private static Mustache templateFrom(String name) {
    return new DefaultMustacheFactory().compile(resourcePathFor(name));
  }

  private static class Ctx {
    public final String title;
    public final List<Bible.Book> books;

    Ctx(String title, List<Bible.Book> books) {
      this.title = title;
      this.books = books;
    }

    static Ctx build(String title, List<Bible.Book> books) {
      return new Ctx(title, books);
    }
  }

  private static JsonGenerator gen(File dst) throws IOException {
    return new JsonFactory().createGenerator(new FileWriter(dst));
  }

  private static void toJson(JsonGenerator g, Bible.Verse verse) throws IOException {
    g.writeStartObject();
    g.writeNumberField("num", verse.number());
    g.writeStringField("txt", verse.toString());
    g.writeEndObject();
  }

  private static void toJson(JsonGenerator g, Bible.Chapter ch) throws IOException {
    g.writeStartObject();

    g.writeNumberField("num", ch.number());

    g.writeFieldName("verses");
    g.writeStartArray();
    for (Bible.Verse verse : ch.verses()) {
      toJson(g, verse);
    }
    g.writeEndArray();

    g.writeEndObject();
  }

  private static void toJson(JsonGenerator g, Bible.Book book) throws IOException {
    g.writeStartObject();

    g.writeStringField("abbr", book.abbr());

    g.writeStringField("name", book.name());

    g.writeFieldName("chapters");
    g.writeStartArray();
    for (Bible.Chapter ch : book.chapters()) {
      toJson(g, ch);
    }
    g.writeEndArray();

    g.writeEndObject();
  }

  public static void toJson(File dst, List<Bible.Book> books) throws IOException {
    FileUtils.forceMkdir(dst);

    try (JsonGenerator g = gen(new File(dst, "print.json"))) {
      g.writeStartArray();

      for (Bible.Book book : books) {
        toJson(g, book);
      }

      g.writeEndArray();
    }
  }

  public static void toHtml(File dst, List<Bible.Book> books) throws IOException {
    FileUtils.forceMkdir(dst);

    Ctx ctx = Ctx.build("The Holy Fear", books);
    try (Writer w = new FileWriter(new File(dst, "index.html"))) {
      templateFrom("print.mustache").execute(w, ctx);
    }
  }
}
