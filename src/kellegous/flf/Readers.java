package kellegous.flf;

import java.io.*;
import java.util.function.Function;

public class Readers {

  private  Readers() {
    // nope
  }

  private static String parseContentFromLine(String s) throws IOException {
    if (!s.endsWith("~")) {
      throw new IOException(String.format("invalid line (missing ~): %s", s));
    }

    int ix = s.indexOf("| ");
    if (ix < 0) {
      throw new IOException(String.format("invalid line (missing \"| \"): %s", s));
    }

    return s.substring(ix+2, s.length()-1);
  }

  private static StringBuilder readAllBibleLines(FileReader r) throws IOException {
    StringBuilder buf = new StringBuilder();

    BufferedReader br = new BufferedReader(r);
    String line;
    while ((line = br.readLine()) != null) {
      buf.append(parseContentFromLine(line));
      buf.append('\n');
    }
    return buf;
  }

  public static Reader readerForBible(File f) throws IOException {
    try (FileReader r = new FileReader(f)) {
      return new StringReader(readAllBibleLines(r).toString());
    }
  }

  public static Reader readerForFearAndLoathing(File f) throws IOException {
    return readerForFearAndLoathing(f, (String s) -> s);
  }

  public static Reader readerForFearAndLoathing(File f, Function<String, String> tx) throws IOException {
    try (InputStream src = new FileInputStream(f)) {
      return Pdf.extractToReader(src, tx);
    }
  }
}
