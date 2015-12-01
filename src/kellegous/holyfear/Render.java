package kellegous.holyfear;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import kellegous.holyfear.util.StreamUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Render {

  private static class Opts {
    private final File bibleFile;
    private final File textDir;

    private Opts(File bibleFile, File textDir) {
      this.bibleFile = bibleFile;
      this.textDir = textDir;
    }

    static Opts parse(String[] args) throws ParseException {

      Options opts = new Options();

      CommandLine cl = new DefaultParser().parse(opts, args);

      if (cl.getArgs().length != 2) {
        System.err.printf("usage: render [options] bible.json.gz text-dir\n");
        System.exit(1);
      }

      return new Opts(
          new File(cl.getArgs()[0]),
          new File(cl.getArgs()[1]));
    }
  }

  private static class Page {
    private int[] view;
    private List<TextItem> text;

    private Page(int[] view, List<TextItem> text) {
      this.view = view;
      this.text = text;
    }

    static Page fromJson(File src) throws IOException {
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

        return new Page(view, text);
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

    private String text;
    private String tag;

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
      tag = null;

      if (!verses.hasNext()) {
        return false;
      }

      Bible.Verse verse = verses.next();
      text = removeSpaces(verse.toString());
      tag = verse.tag();
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

    private Text transform(TextItem item, double scale, int viewHeight) {
      String noSpaces = removeSpaces(item.str);
      int type = classify(item, noSpaces);
      String tag = null;

      switch (type) {
      case Text.TYPE_VERSE:
        tag = this.tag;
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
          (viewHeight - item.transform[5] - item.transform[3]) * scale,
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

      throw new IllegalStateException(tag);
    }

    Stream<Text> transform(Page page, int width) {
      double fx = (double) width / (double) page.view[2];
      return page.text.stream()
          .map(i -> transform(i, fx, page.view[3]))
          .filter(i -> i != null);
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

    for (int i = 1;; i++) {
      File src = new File(opts.textDir, String.format("text-%04d.json", i));
      if (!src.exists()) {
        break;
      }

      toJson(new File(opts.textDir, String.format("t%04d.json", i)),
          converter.transform(Page.fromJson(src), 900));
    }
  }
}
