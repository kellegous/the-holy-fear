package kellegous.holyfear;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A model object for the King James Bible.
 */
public class Bible {

  /**
   * A map from abbr to original bible book name.
   */
  private static final Map<String, String> nameByAbbr = createAbbrToNameMap();

  /**
   * Create the mappings from abbr to original book name.
   */
  private static Map<String, String> createAbbrToNameMap() {
    Map<String, String> m = new HashMap<>();
    m.put("col", "Colossians");
    m.put("luk", "Luke");
    m.put("ecc", "Ecclesiastes");
    m.put("heb", "Hebrews");
    m.put("num", "Numbers");
    m.put("est", "Esther");
    m.put("hab", "Habakkuk");
    m.put("bel", "Bel and the Dragon");
    m.put("jo2", "2 John");
    m.put("jo1", "1 John");
    m.put("hag", "Haggai");
    m.put("jo3", "3 John");
    m.put("rut", "Ruth");
    m.put("neh", "Nehemiah");
    m.put("dan", "Daniel");
    m.put("bet", "Bel and the Dragon Th");
    m.put("sol", "Song of Solomon");
    m.put("bar", "Baruch");
    m.put("act", "Acts");
    m.put("nah", "Nahum");
    m.put("deu", "Deuteronomy");
    m.put("jer", "Jeremiah");
    m.put("dat", "Daniel Th");
    m.put("jam", "James");
    m.put("lam", "Lamentations");
    m.put("lao", "Laodiceans");
    m.put("lev", "Leviticus");
    m.put("ma1", "1 Macabees");
    m.put("eph", "Ephesians");
    m.put("exo", "Exodus");
    m.put("zac", "Zechariah");
    m.put("epj", "Epistle of Jeremiah");
    m.put("ma3", "3 Macabees");
    m.put("tit", "Titus");
    m.put("ma2", "2 Macabees");
    m.put("ma4", "4 Macabees");
    m.put("sa1", "1 Samuel");
    m.put("rev", "Revelation");
    m.put("zep", "Zephaniah");
    m.put("sa2", "2 Samuel");
    m.put("pro", "Proverbs");
    m.put("ode", "Odes");
    m.put("jsb", "Joshua B");
    m.put("jsa", "Joshua A");
    m.put("job", "Job");
    m.put("th1", "1 Thessalonians");
    m.put("psa", "Psalms");
    m.put("th2", "2 Thessalonians");
    m.put("joe", "Joel");
    m.put("amo", "Amos");
    m.put("es1", "1 Esdras");
    m.put("joh", "John");
    m.put("co1", "1 Corinthians");
    m.put("es2", "2 Esdras");
    m.put("mic", "Micah");
    m.put("tob", "Tobias");
    m.put("toa", "Tobit BA");
    m.put("co2", "2 Corinthians");
    m.put("jon", "Jonah");
    m.put("aza", "Prayer of Azariah");
    m.put("hos", "Hosea");
    m.put("aes", "Additions to Esther");
    m.put("jos", "Joshua");
    m.put("sus", "Susanna");
    m.put("gen", "Genesis");
    m.put("eze", "Ezekiel");
    m.put("pss", "Psalms of Solomon");
    m.put("rom", "Romans");
    m.put("sut", "Susanna Th");
    m.put("gal", "Galatians");
    m.put("kg1", "1 Kings");
    m.put("kg2", "2 Kings");
    m.put("mal", "Malachi");
    m.put("tos", "Tobit S");
    m.put("man", "Prayer of Manasseh");
    m.put("ezr", "Ezra");
    m.put("ti2", "2 Timothy");
    m.put("pe2", "2 Peter");
    m.put("sir", "Sirach");
    m.put("ti1", "1 Timothy");
    m.put("pe1", "1 Peter");
    m.put("mar", "Mark");
    m.put("wis", "Wisdom");
    m.put("mat", "Matthew");
    m.put("jda", "Judges A");
    m.put("jdb", "Judges B");
    m.put("jde", "Jude");
    m.put("jdg", "Judges");
    m.put("ch2", "2 Chronicles");
    m.put("ch1", "1 Chronicles");
    m.put("oba", "Obadiah");
    m.put("phi", "Philippians");
    m.put("plm", "Philemon");
    m.put("isa", "Isaiah");
    m.put("jdt", "Judith");
    return m;
  }

  /**
   * Represents a book in the bible.
   */
  public static class Book {

    private final String abbr;

    private final String name;

    private final List<Chapter> chapters;

    Book(String abbr, String name) {
      this.abbr = abbr;
      this.name = name;
      chapters = new ArrayList<>();
    }

    private Book(String abbr, String name, List<Chapter> chapters) {
      this.abbr = abbr;
      this.name = name;
      this.chapters = chapters;
    }

    public Book(String abbr, String name, Stream<Chapter> chapters) {
      this.abbr = abbr;
      this.name = name;
      this.chapters = chapters
          .map(c -> c.inBook(this))
          .collect(Collectors.toList());
    }

    /**
     * The full name of the book.
     */
    public String name() {
      return name;
    }

    /**
     * A 3-letter abbreviation of the book.
     */
    public String abbr() {
      return abbr;
    }

    /**
     * All the chapters that make up this book.
     */
    public List<Chapter> chapters() {
      return chapters;
    }

    private void add(int chapter, int number, List<List<Tokenizer.Token>> text) {
      Chapter ch = !chapters.isEmpty()
          ? chapters.get(chapters.size() - 1)
          : null;

      if (ch == null || ch.number() != chapter) {
        ch = new Chapter(this, chapter);
        chapters.add(ch);
      }

      ch.add(number, text);
    }

    private void toJson(JsonGenerator g) throws IOException {
      g.writeStartObject();

      g.writeStringField("abbr", abbr);
      g.writeStringField("name", name);

      g.writeFieldName("chapters");
      g.writeStartArray();
      for (Chapter chapter : chapters) {
        chapter.toJson(g);
      }
      g.writeEndArray();

      g.writeEndObject();
    }

    private static Book fromJson(JsonParser p) throws IOException {
      String abbr = null;
      String name = null;
      List<Chapter> chapters = new ArrayList<>();

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String field = p.getCurrentName();

        if (p.nextToken() == JsonToken.VALUE_NULL) {
          continue;
        }

        if (field.equals("abbr")) {
          abbr = p.getText();
        } else if (field.equals("name")) {
          name = p.getText();
        } else if (field.equals("chapters")) {
          while (p.nextToken() != JsonToken.END_ARRAY) {
            chapters.add(Chapter.fromJson(p));
          }
        } else {
          p.skipChildren();
        }
      }

      Book book = new Book(abbr, name, chapters);
      chapters.forEach(c -> c.inBook(book));
      return book;
    }
  }

  /**
   * Represents a chapter in a book of the bible.
   */
  public static class Chapter {
    private final int number;
    private final List<Verse> verses;
    private Book book;

    private Chapter(Book book, int number) {
      this.book = book;
      this.number = number;
      this.verses = new ArrayList<>();
    }

    private Chapter(int number, List<Verse> verses) {
      this.number = number;
      this.verses = verses;
    }

    public Chapter(int number, Stream<Verse> verses) {
      this.number = number;
      this.verses = verses
          .map(v -> v.inChapter(this))
          .collect(Collectors.toList());
    }

    private Chapter inBook(Book book) {
      this.book = book;
      return this;
    }

    public Book book() {
      return book;
    }

    /**
     * The versions that make up this chapter.
     */
    public List<Verse> verses() {
      return verses;
    }

    /**
     * The number of this chapter.
     */
    public int number() {
      return number;
    }

    private void add(int number, List<List<Tokenizer.Token>> text) {
      verses.add(new Verse(this, number, text));
    }

    private void toJson(JsonGenerator g) throws IOException {
      g.writeStartObject();

      g.writeNumberField("number", number);

      g.writeFieldName("verses");
      g.writeStartArray();
      for (Verse verse : verses) {
        verse.toJson(g);
      }
      g.writeEndArray();

      g.writeEndObject();
    }

    private static Chapter fromJson(JsonParser p) throws IOException {
      int number = -1;
      List<Verse> verses = new ArrayList<>();

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String field = p.getCurrentName();

        if (p.nextToken() == JsonToken.VALUE_NULL) {
          continue;
        }

        if (field.equals("number")) {
          number = p.getIntValue();
        } else if (field.equals("verses")) {
          while (p.nextToken() != JsonToken.END_ARRAY) {
            verses.add(Verse.fromJson(p));
          }
        } else {
          p.skipChildren();
        }
      }

      Chapter chapter = new Chapter(number, verses);
      verses.forEach(v -> v.inChapter(chapter));
      return chapter;
    }
  }

  /**
   * Represents a biblical verse.
   */
  public static class Verse {
    private final int number;
    private final List<List<Tokenizer.Token>> text;
    private Chapter chapter;

    private Verse(Chapter chapter, int number, List<List<Tokenizer.Token>> text) {
      this.chapter = chapter;
      this.number = number;
      this.text = text;
    }

    public Verse(int number, List<List<Tokenizer.Token>> text) {
      this.number = number;
      this.text = text;
    }

    private Verse inChapter(Chapter chapter) {
      this.chapter = chapter;
      return this;
    }

    public Chapter chapter() {
      return chapter;
    }

    public String tag() {
      return String.format("%s-%d-%d",
          chapter.book().abbr(),
          chapter.number(),
          number);
    }
    /**
     * The number of the verse.
     */
    public int number() {
      return number;
    }

    /**
     * A tokenized representation of the verse's text.
     */
    public List<List<Tokenizer.Token>> text() {
      return text;
    }

    /**
     * Turn the text of this verse into string data and store it in the given StringBuilder.
     * @see #toString()
     */
    public StringBuilder toString(StringBuilder buffer) {
      for (int i = 0, n = text.size(); i < n; i++) {
        if (i != 0) {
          buffer.append(' ');
        }
        Tokenizer.Token.format(text.get(i), buffer);
      }
      return buffer;
    }

    /**
     * Turn the text of this verse into a string.
     */
    @Override
    public String toString() {
      return toString(new StringBuilder()).toString();
    }

    private static void toJson(JsonGenerator g, Tokenizer.Token token) throws IOException {
      g.writeStartObject();
      g.writeStringField("text", token.text());
      g.writeNumberField("flags", token.flags());
      g.writeStringField("pos", token.pos());
      g.writeEndObject();
    }

    private void toJson(JsonGenerator g) throws IOException {
      g.writeStartObject();

      g.writeNumberField("number", number);

      g.writeFieldName("text");
      g.writeStartArray();
      for (List<Tokenizer.Token> sentence : text) {
        g.writeStartArray();
        for (Tokenizer.Token token : sentence) {
          toJson(g, token);
        }
        g.writeEndArray();
      }
      g.writeEndArray();

      g.writeEndObject();
    }

    private static Tokenizer.Token tokenFromJson(JsonParser p) throws IOException {
      String text = null;
      int flags = -1;
      String pos = null;

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String field = p.getCurrentName();

        if (p.nextToken() == JsonToken.VALUE_NULL) {
          continue;
        }

        if (field.equals("text")) {
          text = p.getText();
        } else if (field.equals("flags")) {
          flags = p.getIntValue();
        } else if (field.equals("pos")) {
          pos = p.getText();
        } else {
          p.skipChildren();
        }
      }

      return new Tokenizer.Token(text, pos, flags);
    }

    private static void textFromJson(JsonParser p, List<List<Tokenizer.Token>> text) throws IOException {
      while (p.nextToken() != JsonToken.END_ARRAY) {
        List<Tokenizer.Token> tokens = new ArrayList<>();
        text.add(tokens);
        while (p.nextToken() != JsonToken.END_ARRAY) {
          tokens.add(tokenFromJson(p));
        }
      }
    }

    private static Verse fromJson(JsonParser p) throws IOException {
      int number = -1;
      List<List<Tokenizer.Token>> text = new ArrayList<>();

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String field = p.getCurrentName();

        if (p.nextToken() == JsonToken.VALUE_NULL) {
          continue;
        }

        if (field.equals("number")) {
          number = p.getIntValue();
        } else if (field.equals("text")) {
          textFromJson(p, text);
        } else {
          p.skipChildren();
        }
      }

      return new Verse(number, text);
    }
  }

  private Bible() {
    // uninstantiable
  }

  private static void add(
      List<Book> books,
      String abbr,
      String name,
      int chapter,
      int number,
      List<List<Tokenizer.Token>> text) {
    Book book = !books.isEmpty()
        ? books.get(books.size() - 1)
        : null;
    if (book == null || !abbr.equals(book.abbr())) {
      book = new Book(abbr, name);
      books.add(book);
    }

    book.add(chapter, number, text);
  }

  public static Stream<Verse> allVerses(List<Book> books) {
    return books.stream()
        .flatMap(b -> b.chapters().stream()
            .flatMap(c -> c.verses().stream()));
  }

  /**
   * Read a bible from the given file using the given tokenizer.
   */
  public static List<Book> readFrom(File file, Tokenizer tokenizer) throws IOException {
    try (FileReader r = new FileReader(file)) {
      return readFrom(r, tokenizer);
    }
  }

  /**
   * Read a bible from the given reader using the given tokenizer.
   */
  public static List<Book> readFrom(Reader r, Tokenizer tokenizer) throws IOException {
    try (BufferedReader br = new BufferedReader(r)) {

      List<Book> books = new ArrayList<>();

      String line;
      while ((line = br.readLine()) != null) {
        String[] p = line.split("\\|");
        if (p.length != 4 || !line.endsWith("~")) {
          throw new IOException(String.format("invalid line: %s", line));
        }

        List<List<Tokenizer.Token>> text = tokenizer.tokenize(
            new StringReader(p[3].substring(0, p[3].length() - 1)));

        String abbr = p[0].toLowerCase();

        add(books,
            abbr,
            nameByAbbr.get(abbr),
            Integer.parseInt(p[1]),
            Integer.parseInt(p[2]),
            text);
      }

      return books;
    }
  }

  public static void toJson(File dest, List<Book> books) throws IOException {
    try (Writer w = Gzip.newWriter(dest)) {
      toJson(w, books);
    }
  }

  private static void toJson(Writer w, List<Book> books) throws IOException {
    try (JsonGenerator g = new JsonFactory().createGenerator(w)) {
      g.writeStartArray();
      for (Book book : books) {
        book.toJson(g);
      }
      g.writeEndArray();
    }
  }

  public static List<Book> fromJson(File src) throws IOException {
    try (Reader r = Gzip.newReader(src)) {
      return fromJson(r);
    }
  }

  private static List<Book> fromJson(Reader r) throws IOException {
    try (JsonParser p = new JsonFactory().createParser(r)) {
      if (p.nextToken() != JsonToken.START_ARRAY) {
        throw new IOException("Expected beginning of array.");
      }

      List<Book> books = new ArrayList<>();
      while (p.nextToken() != JsonToken.END_ARRAY) {
        books.add(Book.fromJson(p));
      }
      return books;
    }
  }
}
