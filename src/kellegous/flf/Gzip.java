package kellegous.flf;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Gzip {

  public static Writer newWriter(String filename) throws IOException {
    return newWriter(new File(filename));
  }

  public static Writer newWriter(File file) throws IOException {
    return new OutputStreamWriter(
        new GZIPOutputStream(new FileOutputStream(file)));
  }

  public static Reader newReader(String filename) throws IOException {
    return newReader(new File(filename));
  }

  public static Reader newReader(File file) throws IOException {
    return new InputStreamReader(
        new GZIPInputStream(new FileInputStream(file)));
  }
}
