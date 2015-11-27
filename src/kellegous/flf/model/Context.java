package kellegous.flf.model;

import kellegous.flf.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class Context {
  private final Tokenizer.Token[] prefix;

  public Context(int n) {
    prefix = new Tokenizer.Token[n];
  }

  int size() {
    return prefix.length;
  }

  public void clear() {
    for (int i = 0, n = prefix.length; i < n; i++) {
      prefix[i] = null;
    }
  }

  public void add(Tokenizer.Token token) {
    int n = prefix.length;
    for (int i = 1; i < n; i++) {
      prefix[i - 1] = prefix[i];
    }
    prefix[n - 1] = token;
  }

  StringBuilder asString(StringBuilder buffer) {
    boolean hasOne = false;
    for (int i = 0, n = prefix.length; i < n; i++) {
      Tokenizer.Token token = prefix[i];

      if (token == null) {
        continue;
      }

      if (hasOne && token.needsLeadingSpace()) {
        buffer.append(' ');
      }
      buffer.append(token.text());
      hasOne = true;
    }
    return buffer;
  }

  String asString() {
    return asString(new StringBuilder()).toString();
  }

  StringBuilder asKey(StringBuilder buffer, int n) {
    boolean hasOne = false;
    for (int i = prefix.length - n, m = prefix.length; i < m; i++) {
      Tokenizer.Token token = prefix[i];

      if (token == null) {
        continue;
      }

      if (hasOne && token.needsLeadingSpace()) {
        buffer.append(' ');
      }
      buffer.append(token.text());
      hasOne = true;
    }
    return buffer;
  }

  String asKey(int n) {
    return asKey(new StringBuilder(), n).toString();
  }

  public List<String> keys() {
    List<String> keys = new ArrayList<>(prefix.length);
    for (int i = 0, n = prefix.length; i < n; i++) {
      if (prefix[i] == null) {
        continue;
      }

      StringBuilder buf = new StringBuilder();
      for (int j = i, m = n; j < m; j++) {
        Tokenizer.Token token = prefix[j];
        if (j != i && token.needsLeadingSpace()) {
          buf.append(' ');
        }
        buf.append(token.text());
      }
      keys.add(buf.toString());
    }

    if (keys.isEmpty()) {
      keys.add("");
    }

    return keys;
  }

}
