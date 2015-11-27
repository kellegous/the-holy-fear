package kellegous.holyfear.model;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import kellegous.holyfear.Tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

public class Model {

  private static boolean useEvenWeightedSelection = false;

  public static class Builder {

    private final Context context;

    private final Map<String, Map<Tokenizer.Token, Integer>> options = new HashMap<>();

    private final String name;

    public Builder(String name, int n) {
      this.name = name;
      this.context = new Context(n);
    }

    public void add(List<Tokenizer.Token> sentence) {
      sentence.forEach(t -> add(t));
      context.clear();
    }

    private void add(Tokenizer.Token token) {
      context.keys().forEach(ctx -> {
        Map<Tokenizer.Token, Integer> a = options.computeIfAbsent(
            keyFor(token.pos(), ctx),
            s -> new HashMap<>());
        a.put(token, a.getOrDefault(token, 0) + 1);
      });
      Map<Tokenizer.Token, Integer> b = options.computeIfAbsent(
          token.pos(),
          s -> new HashMap<>());
      b.put(token, b.getOrDefault(token, 0) + 1);
      context.add(token);
    }

    private static List<WeightedToken> toSortedWeightedList(Map<Tokenizer.Token, Integer> options) {
      List<WeightedToken> tokens = new ArrayList<>(options.size());

      double total = 0.0;
      for (Integer count : options.values()) {
        total += count;
      }

      for (Map.Entry<Tokenizer.Token, Integer> e : options.entrySet()) {
        tokens.add(new WeightedToken(e.getKey(), e.getValue() / total));
      }

      tokens.sort((a, b) -> Double.compare(b.weight(), a.weight()));

      return tokens;
    }

    public Model build() {
      Map<String, List<WeightedToken>> edges = new HashMap<>();
      for (Map.Entry<String, Map<Tokenizer.Token, Integer>> e : options.entrySet()) {
        edges.put(e.getKey(), toSortedWeightedList(e.getValue()));
      }
      return new Model(name, context.size(), edges);
    }
  }

  private static String keyFor(String pos, String pfx) {
    return String.format("%s-%s", pos, pfx);
  }

  private final Map<String, List<WeightedToken>> edges;
  private final int size;
  private final String name;

  private Model(
      String name,
      int size,
      Map<String, List<WeightedToken>> edges) {
    this.name = name;
    this.size = size;
    this.edges = edges;
  }

  public int size() {
    return size;
  }

  public String name() {
    return name;
  }

  private static WeightedToken select(List<WeightedToken> tokens, Random rng) {
    if (useEvenWeightedSelection) {
      return tokens.get(rng.nextInt(tokens.size()));
    }

    double v = rng.nextDouble();
    double s = 0.0;
    for (int i = 0, n = tokens.size(); i < n; i++) {
      WeightedToken t = tokens.get(i);
      s += t.weight();
      if (v <= s) {
        return t;
      }
    }
    throw new RuntimeException(String.format("impossible %f %f", v, s));
  }

  public Tokenizer.Token selectToken(Context context, String pos, Random rng) {
    for (String ctx : context.keys()) {
      List<WeightedToken> selections = edges.get(keyFor(pos, ctx));
      if (selections == null) {
        continue;
      }

      return select(selections, rng).token();
    }

    // return select(edges.get(pos), rng).token();
    return null;
  }

  public Set<String> allTokens() {
    Set<String> tokens = new HashSet<>();
    edges.values()
        .stream()
        .forEach(v -> {
          v.stream().forEach(t -> tokens.add(t.token().text()));
        });
    return tokens;
  }

  void writeTo(JsonGenerator gen) throws IOException {
    gen.writeStartObject();

    gen.writeNumberField("size", size);
    gen.writeStringField("name", name);

    gen.writeFieldName("edges");
    gen.writeStartObject();
    for (Map.Entry<String, List<WeightedToken>> e : edges.entrySet()) {
      gen.writeFieldName(e.getKey());
      writeTo(gen, e.getValue());
    }
    gen.writeEndObject();

    gen.writeEndObject();
  }

  public void writeTo(Writer w) throws IOException {
    JsonFactory jf = new JsonFactory();
    try (JsonGenerator gen = jf.createGenerator(w)) {
      writeTo(gen);
    }
  }

  private static void writeTo(JsonGenerator gen, Tokenizer.Token token) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("text", token.text());
    gen.writeNumberField("flags", token.flags());
    gen.writeStringField("pos", token.pos());
    gen.writeEndObject();
  }

  private static void writeTo(JsonGenerator gen, WeightedToken token) throws IOException {
    gen.writeStartObject();
    gen.writeFieldName("token");
    writeTo(gen, token.token());
    gen.writeNumberField("weight", token.weight());
    gen.writeEndObject();
  }

  private static void writeTo(JsonGenerator gen, List<WeightedToken> tokens) throws IOException {
    gen.writeStartArray();
    for (WeightedToken token : tokens) {
      writeTo(gen, token);
    }
    gen.writeEndArray();
  }

  private static List<WeightedToken> readTokensFrom(
      JsonParser parser,
      List<WeightedToken> tokens) throws IOException {
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      tokens.add(WeightedToken.readFrom(parser));
    }
    return tokens;
  }

  private static Map<String, List<WeightedToken>> readEdgesFrom(
      JsonParser parser,
      Map<String, List<WeightedToken>> edges) throws IOException {
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String name = parser.getCurrentName();

      if (parser.nextToken() == JsonToken.VALUE_NULL) {
        continue;
      }

      edges.put(name, readTokensFrom(parser, new ArrayList<>()));
    }

    return edges;
  }

  static Model readFrom(JsonParser parser) throws IOException {
    String name = null;
    int size = 0;
    Map<String, List<WeightedToken>> edges = new HashMap<>();

    while (parser.nextToken() != JsonToken.END_OBJECT) {
      String field = parser.getCurrentName();

      if (parser.nextToken() == JsonToken.VALUE_NULL) {
        continue;
      }

      if ("edges".equals(field)) {
        readEdgesFrom(parser, edges);
      } else if ("size".equals(field)) {
        size = parser.getIntValue();
      } else if ("name".equals(field)) {
        name = parser.getText();
      } else {
        parser.skipChildren();
      }
    }

    return new Model(name, size, edges);
  }

  public static Model readFrom(Reader r) throws IOException {
    JsonFactory jf = new JsonFactory();
    JsonParser parser = jf.createParser(r);
    if (parser.nextToken() != JsonToken.START_OBJECT) {
      throw new IOException("Model object not found in JSON.");
    }

    return readFrom(parser);
  }

  public static Model build(
      String name,
      int size,
      List<List<Tokenizer.Token>> sentences) {
    Builder builder = new Builder(name, size);
    sentences.forEach(s -> builder.add(s));
    return builder.build();
  }
}
