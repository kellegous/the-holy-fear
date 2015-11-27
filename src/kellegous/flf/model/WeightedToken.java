package kellegous.flf.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import kellegous.flf.Tokenizer;

import java.io.IOException;

public class WeightedToken {
  private final Tokenizer.Token token;
  private final double weight;

  WeightedToken(Tokenizer.Token token, double weight) {
    this.token = token;
    this.weight = weight;
  }

  public Tokenizer.Token token() {
    return token;
  }

  public double weight() {
    return weight;
  }

  public WeightedToken scaled(double f) {
    return new WeightedToken(token, weight * f);
  }

  static Tokenizer.Token readTokenFrom(JsonParser parser) throws IOException {
    String text = null;
    int flags = -1;
    String pos = null;

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String name = parser.getCurrentName();

      if (parser.nextToken() == JsonToken.VALUE_NULL) {
        continue;
      }

      if ("text".equals(name)) {
        text = parser.getText();
      } else if ("flags".equals(name)) {
        flags = parser.getIntValue();
      } else if ("pos".equals(name)) {
        pos = parser.getText();
      } else {
        parser.skipChildren();
      }
    }

    if (text == null) {
      throw new IOException("Token object is missing \"text\"");
    }

    if (flags < 0) {
      throw new IOException("Token object is missing \"flags\"");
    }

    if (pos == null) {
      throw new IOException("Token object is missing \"pos\"");
    }

    return new Tokenizer.Token(text, pos, flags);
  }

  static WeightedToken readFrom(JsonParser parser) throws IOException {
    Tokenizer.Token token = null;
    double weight = -1.0;

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String name = parser.getCurrentName();

      if (parser.nextToken() == JsonToken.VALUE_NULL) {
        continue;
      }

      if ("token".equals(name)) {
        token = readTokenFrom(parser);
      } else if ("weight".equals(name)) {
        weight = parser.getDoubleValue();
      } else {
        parser.skipChildren();
      }
    }

    if (token == null) {
      throw new IOException("WeightedToken object is missing \"token\"");
    }

    if (weight < 0) {
      throw new IOException("WeightedToken object is missing \"weight\"");
    }

    return new WeightedToken(token, weight);
  }
}
