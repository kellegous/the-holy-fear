package kellegous.holyfear;

import java.io.*;

public class FearAndLoathing {
  public static final String NAME = "Fear and Loathing in Las Vegas";

  private static String filterOutStupidTitle(String text) {
    return text.startsWith("Microsoft Word") ? null : text;
  }

  private FearAndLoathing() {
  }

  public static Reader readFrom(File f) throws IOException {
    try (InputStream src = new FileInputStream(f)) {
      return Pdf.extractToReader(src, FearAndLoathing::filterOutStupidTitle);
    }
  }
}
