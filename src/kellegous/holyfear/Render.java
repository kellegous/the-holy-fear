package kellegous.holyfear;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import kellegous.holyfear.util.Cli;
import kellegous.holyfear.util.StreamUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Render {

  private static class Opts {
    private static final String OPT_DEST_DIR = "dst";
    private static final String DEFAULT_DEST_DIR = "dst/www";

    private final File bibleFile;
    private final File textDir;
    private final File destDir;

    private Opts(File bibleFile, File textDir, File destDir) {
      this.bibleFile = bibleFile;
      this.textDir = textDir;
      this.destDir = destDir;
    }

    static Opts parse(String[] args) throws ParseException {

      Options opts = new Options();

      opts.addOption(Cli.newOptionWithArg(OPT_DEST_DIR,
          "Where to write the output files.",
          "DIR"));

      CommandLine cl = new DefaultParser().parse(opts, args);

      if (cl.getArgs().length != 2) {
        System.err.printf("usage: render [options] bible.json.gz text-dir\n");
        System.exit(1);
      }

      return new Opts(
          new File(cl.getArgs()[0]),
          new File(cl.getArgs()[1]),
          new File(cl.getOptionValue(OPT_DEST_DIR, DEFAULT_DEST_DIR)));
    }
  }

  private static class Page {
    private int[] view;
    private List<TextItem> text;
    private int number;

    private Page(int number, int[] view, List<TextItem> text) {
      this.number = number;
      this.view = view;
      this.text = text;
    }

    static Page fromJson(File src, int number) throws IOException {
      int[] view = new int[4];
      List<TextItem> text = new ArrayList<>();

      try (Reader r = new FileReader(src)) {
        JsonParser p = new JsonFactory().createParser(r);
        if (p.nextToken() != JsonToken.START_OBJECT) {
          throw new IOException("Expected {");
        }

        while (p.nextToken() != JsonToken.END_OBJECT) {
          String field = p.getCurrentName();

          if (p.nextToken() == JsonToken.VALUE_NULL) {
            continue;
          }

          if (field.equals("view")) {
            int i = 0;
            while (p.nextToken() != JsonToken.END_ARRAY) {
              view[i] = p.getIntValue();
              i++;
            }
          } else if (field.equals("text")) {
            while (p.nextToken() != JsonToken.END_ARRAY) {
              text.add(TextItem.fromJson(p));
            }
          } else {
            p.skipChildren();
          }
        }

        return new Page(number, view, text);
      }
    }
  }

  private static class TextItem {
    private int width;
    private int height;
    private int[] transform;
    private String str;
    private String fontFamily;
    private String fontName;

    private TextItem(
        int width,
        int height,
        int[] transform,
        String str,
        String fontFamily,
        String fontName) {
      this.width = width;
      this.height =height;
      this.transform = transform;
      this.str = str;
      this.fontFamily = fontFamily;
      this.fontName = fontName;
    }

    static TextItem fromJson(JsonParser p) throws IOException {
      int width = -1;
      int height = -1;
      int[] transform = new int[6];
      String str = null;
      String fontFamily = null;
      String fontName = null;

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String field = p.getCurrentName();

        if (p.nextToken() == JsonToken.VALUE_NULL) {
          continue;
        }

        if (field.equals("width")) {
          width = p.getIntValue();
        } else if (field.equals("height")) {
          height = p.getIntValue();
        } else if (field.equals("transform")) {
          int i = 0;
          while (p.nextToken() != JsonToken.END_ARRAY) {
            transform[i] = p.getIntValue();
            i++;
          }
        } else if (field.equals("str")) {
          str = p.getText();
        } else if (field.equals("fontFamily")) {
          fontFamily = p.getText();
        } else if (field.equals("fontName")) {
          fontName = p.getText();
        } else {
          p.skipChildren();
        }
      }

      return new TextItem(width, height, transform, str, fontFamily, fontName);
    }
  }

  private static class Text {
    static final int TYPE_VERSE = 0;
    static final int TYPE_CHAPTER_NUM = 1;
    static final int TYPE_VERSE_NUM = 2;
    static final int TYPE_BOOK_NAME = 3;
    static final int TYPE_BLANK = 4;

    private final int type;
    private final String tag;
    private final String value;
    private final double width;
    private final double height;
    private final double top;
    private final double left;

    Text(int type, String tag, String value, double width, double height, double top, double left) {
      this.type = type;
      this.tag = tag;
      this.value = value;
      this.width = width;
      this.height = height;
      this.top = top;
      this.left = left;
    }

    private void toJson(JsonGenerator g) throws IOException {
      g.writeStartObject();
      g.writeStringField("text", value);
      g.writeNumberField("type", type);
      g.writeNumberField("w", width);
      g.writeNumberField("h", height);
      g.writeNumberField("t", top);
      g.writeNumberField("l", left);
      if (tag != null) {
        g.writeStringField("tag", tag);
      }
      g.writeEndObject();
    }
  }

  private static class Converter {
    private final Iterator<Bible.Verse> verses;

    private final Set<String> names;

    private final Map<String, int[]> toc = new HashMap<>();

    private String text;
    private Bible.Verse verse;

    Converter(List<Bible.Book> books) {
      verses = Bible.allVerses(books)
          .iterator();

      names = books.stream()
          .map(b -> removeSpaces(b.name()))
          .collect(Collectors.toSet());

      next();
    }

    private boolean next() {
      text = null;
      verse = null;

      if (!verses.hasNext()) {
        return false;
      }

      verse = verses.next();
      text = removeSpaces(verse.toString());
      return true;
    }

    private static String removeSpaces(String s) {
      return s.replace(" ", "");
    }

    private static boolean isNumeric(String s) {
      for (int i = 0, n = s.length(); i < n; i++) {
        if (!Character.isDigit(s.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    private static boolean isAllSpaces(String s) {
      for (int i = 0, n = s.length(); i < n; i++) {
        if (!Character.isSpaceChar(s.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    private void updateToc(Page page, Bible.Verse verse) {
      int[] val = toc.computeIfAbsent(verse.chapter().tag(), x -> new int[]{
        page.number,
        page.number
      });
      val[1] = Math.max(page.number, val[1]);
    }

    private Text transform(TextItem item, Page page, double scale) {
      String noSpaces = removeSpaces(item.str);

      int type = classify(item, noSpaces);
      String tag = null;

      switch (type) {
      case Text.TYPE_VERSE:
        updateToc(page, this.verse);
        tag = this.verse.tag();
        text = text.substring(noSpaces.length());
        if (text.isEmpty()) {
          next();
        }
        break;
      case Text.TYPE_BLANK:
          return null;
      }

      return new Text(
          type,
          tag,
          item.str,
          item.width * scale,
          item.height * scale,
          (page.view[3] - item.transform[5] - item.transform[3]) * scale,
          item.transform[4] * scale);
    }

    int classify(TextItem item, String strNoSpaces) {
      if (text.startsWith(strNoSpaces)) {
        return Text.TYPE_VERSE;
      }

      if (names.contains(strNoSpaces)) {
        return Text.TYPE_BOOK_NAME;
      }

      boolean isNumber = isNumeric(strNoSpaces);
      if (isNumber && item.fontFamily.equals("monospace")) {
        return Text.TYPE_CHAPTER_NUM;
      } else if (isNumber) {
        return Text.TYPE_VERSE_NUM;
      }

      if (isAllSpaces(item.str)) {
        return Text.TYPE_BLANK;
      }

      throw new IllegalStateException();
    }

    Stream<Text> transform(Page page, int width) {
      double fx = (double) width / (double) page.view[2];
      
      return page.text.stream()
          .map(i -> transform(i, page, fx))
          .filter(i -> i != null);
    }
  }

  private static void toJson(File dst, Map<String, int[]> toc, int count) throws IOException {
    try (Writer w = new FileWriter(dst)) {
      try (JsonGenerator g = new JsonFactory().createGenerator(w)) {
        g.writeStartObject();
        g.writeNumberField("count", count);

        g.writeFieldName("chapters");
        g.writeStartObject();
        for (Map.Entry<String, int[]> e : toc.entrySet()) {
          g.writeFieldName(e.getKey());

          int[] vals = e.getValue();
          g.writeStartArray();
          g.writeNumber(vals[0]);
          g.writeNumber(vals[1]);
          g.writeEndArray();
        }
        g.writeEndObject();

        g.writeEndObject();
      }
    }
  }

  private static void toJson(File dst, Stream<Text> items) throws IOException {
    try (Writer w = new FileWriter(dst)) {
      try (JsonGenerator g = new JsonFactory().createGenerator(w)) {
        g.writeStartArray();
        for (Text text : StreamUtil.asIterable(items)) {
          text.toJson(g);
        }
        g.writeEndArray();
      }
    }
  }

  public static void main(String[] args) throws ParseException, IOException {
    Opts opts = Opts.parse(args);

    List<Bible.Book> books = Bible.fromJson(opts.bibleFile);

    Converter converter = new Converter(books);

    int count = 1;
    while (true) {
      File src = new File(opts.textDir, String.format("text-%04d.json", count));
      if (!src.exists()) {
        break;
      }

      FileUtils.forceMkdir(opts.destDir);
      toJson(new File(opts.destDir, String.format("%04d.json", count)),
          converter.transform(Page.fromJson(src, count), 900));
      count++;
    }

    toJson(new File(opts.destDir, "toc.json"), converter.toc, count - 1);
  }
}
