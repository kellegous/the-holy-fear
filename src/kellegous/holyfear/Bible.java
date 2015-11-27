package kellegous.holyfear;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Bible {
  public static final String NAME = "King James Bible";

  private static final Map<String, String> nameByAbbr = createAbbrToNameMap();

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

  public static class Book {
    private final String abbr;
    private final String name;
    private final List<Chapter> chapters;

    Book(String abbr, String name) {
      this.abbr = abbr;
      this.name = name;
      chapters = new ArrayList<>();
    }
    public Book(String abbr, String name, List<Chapter> chapters) {
      this.abbr = abbr;
      this.name = name;
      this.chapters = chapters;
    }

    public Book(String abbr, String name, Stream<Chapter> chapters) {
      this.abbr = abbr;
      this.name = name;
      this.chapters = chapters.collect(Collectors.toList());

    }

    public String name() {
      return name;
    }

    public String abbr() {
      return abbr;
    }

    public List<Chapter> chapters() {
      return chapters;
    }

    private void add(int chapter, int number, List<List<Tokenizer.Token>> text) {
      Chapter ch = !chapters.isEmpty()
          ? chapters.get(chapters.size() - 1)
          : null;

      if (ch == null || ch.number() != chapter) {
        ch = new Chapter(chapter);
        chapters.add(ch);
      }

      ch.add(number, text);
    }
  }

  public static class Chapter {
    private final int number;
    private final List<Verse> verses;

    Chapter(int number) {
      this.number = number;
      this.verses = new ArrayList<>();
    }

    public Chapter(int number, List<Verse> verses) {
      this.number = number;
      this.verses = verses;
    }

    public Chapter(int number, Stream<Verse> verses) {
      this.number = number;
      this.verses = verses.collect(Collectors.toList());
    }

    public List<Verse> verses() {
      return verses;
    }

    public int number() {
      return number;
    }

    private void add(int number, List<List<Tokenizer.Token>> text) {
      verses.add(new Verse(number, text));
    }
  }

  public static class Verse {
    private final int number;
    private final List<List<Tokenizer.Token>> text;

    public Verse(int number, List<List<Tokenizer.Token>> text) {
      this.number = number;
      this.text = text;
    }

    public int number() {
      return number;
    }

    public List<List<Tokenizer.Token>> text() {
      return text;
    }

    public StringBuilder toString(StringBuilder buffer) {
      for (int i = 0, n = text.size(); i < n; i++) {
        if (i != 0) {
          buffer.append(' ');
        }
        Tokenizer.Token.format(text.get(i), buffer);
      }
      return buffer;
    }

    @Override
    public String toString() {
      return toString(new StringBuilder()).toString();
    }
  }

  private Bible() {
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

  public static List<Book> readFrom(File file, Tokenizer tokenizer) throws IOException {
    try (FileReader r = new FileReader(file)) {
      return readFrom(r, tokenizer);
    }
  }

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
}
