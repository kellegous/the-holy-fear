package kellegous.holyfear;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import kellegous.holyfear.model.Context;
import kellegous.holyfear.model.Model;
import kellegous.holyfear.util.Cli;
import kellegous.holyfear.util.Pair;
import kellegous.holyfear.web.Export;
import kellegous.holyfear.web.Print;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The command for generating the text of The Holy Fear.
 */
public class CreateFear {

  private static final String TITLE = "The Holy Fear";

  private static final String DESC = "The sacred union of the King James Version of The Holy Bible and Hunter S. Thompson's Fear and Loathing in Las Vegas";

  /**
   * The part of speeches that are replaceable in the text.
   */
  private static Set<String> rewritablePoses = Sets.newHashSet(
      "NNP",
      "NNS",
      "VBD",
      "VBG",
      "VBN",
      "VBP",
      "NNPS",
      "VB",
      "VBZ",
      "RB",
      "JJ",
      "NN"
  );

  /**
   * Words that cannot be used as took names.
   */
  private static Set<String> bannedBookNames = Sets.newHashSet(
      "niggers"
  );

  /**
   * Logic for renaming books.
   */
  private static class Books {

    /**
     * Words that should not be rewritten in book names.
     */
    private static final Set<String> STOPS = Sets.newHashSet("of", "the");

    /**
     * Select uniformly among the contents of the stream using the given random
     * number generator.
     */
    private static String select(Stream<String> opts, Random rng) {
      List<String> vals = opts.collect(Collectors.toList());
      return vals.get(rng.nextInt(vals.size()));
    }

    /**
     * Does the given String only contain digits?
     */
    private static boolean isAllDigits(String word) {
      for (int i = 0, n = word.length(); i < n; i++) {
        if (!Character.isDigit(word.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    /**
     * Is the given word replaceable?
     */
    private static boolean canReplace(String word) {
      return !isAllDigits(word) && !STOPS.contains(word);
    }

    /**
     * Associates a soundex-like encoding for each of the given string
     * tokens.
     */
    private static List<Pair<String, String>> tokensForBookNames(
        Set<String> tokens,
        StringEncoder encoder) throws EncoderException {
      List<Pair<String, String>> entries = new ArrayList<>();
      for (String token : tokens) {
        if (!Character.isLetter(token.charAt(0))) {
          continue;
        }

        String t = token.toLowerCase();
        entries.add(Pair.of(t, encoder.encode(t)));
      }
      return entries;
    }

    /**
     * For a given word in a book title, get a list of candidate replacement terms.
     */
    private static Stream<String> getCandidateTerms(
        String word,
        int limit,
        StringEncoder encoder,
        List<Pair<String, String>> terms) throws EncoderException {
      String encoded = encoder.encode(word);
      String lower = word.toLowerCase();
      return terms.parallelStream()
          .filter(e -> !bannedBookNames.contains(e.getA().toLowerCase()))
          .map(e -> Pair.of(e.getA(), StringUtils.getLevenshteinDistance(encoded, e.getB())))
          .sorted((a, b) -> {
            int c = Integer.compare(a.getB(), b.getB());
            if (c != 0) {
              return c;
            }
            int len = word.length();
            return Integer.compare(
                Math.abs(a.getA().length() - len),
                Math.abs(b.getA().length() - len));
          })
          .filter(e -> {
            String b = e.getA().toLowerCase();
            return lower.indexOf(b) == -1 && b.indexOf(lower) == -1;
          })
          .limit(limit)
          .map(e -> e.getA());
    }

    /**
     * Rename this book.
     */
    static String rename(
        String name,
        Random rng,
        Map<String, String> knownTerms,
        StringEncoder encoder,
        List<Pair<String, String>> terms) throws EncoderException {
      String[] words = name.split("\\s+");
      List<String> res = new ArrayList<>(words.length);

      for (String word : words) {
        if (!canReplace(word)) {
          res.add(word);
          continue;
        }

        String val = knownTerms.get(word);
        if (val != null) {
          res.add(val);
          continue;
        }

        val = WordUtils.capitalizeFully(select(getCandidateTerms(word, 3, encoder, terms), rng));
        knownTerms.put(word, val);
        res.add(val);
      }

      return Joiner.on(' ').join(res);
    }

    /**
     * Rename all the book in this bible, returning a map from book abbreviation to
     * replacement name.
     */
    static Map<String, String> renameBooks(
        List<Bible.Book> bible,
        Model src,
        Random rng,
        StringEncoder encoder) throws EncoderException {
      List<Pair<String, String>> tokens = tokensForBookNames(src.allTokens(), encoder);

      Map<String, String> knownTerms = new HashMap<>();

      Map<String, String> res = new HashMap<>();

      for (Bible.Book book : bible) {
        res.put(
            book.abbr(),
            rename(book.name(), rng, knownTerms, encoder, tokens));
      }

      return res;
    }
  }

  /**
   * A {@link java.util.function.Supplier} that requires IO in order to supply the value. FML, checked exceptions.
   */
  @FunctionalInterface
  private interface SupplierWithIo<V> {
    V supply() throws IOException;
  }

  /**
   * Avoids rebuilding a model by caching it into a given file.
   */
  private static Model usingCacheFile(
      File dst,
      SupplierWithIo<Model> fn) throws IOException {
    try (Reader r = Gzip.newReader(dst)) {
      return Model.readFrom(r);
    } catch (FileNotFoundException e) {
      // fall through
    }

    Model m = fn.supply();
    try (Writer w = Gzip.newWriter(dst)) {
      m.writeTo(w);
    }

    return m;
  }

  /**
   * The command line options for this command.
   */
  private static class Opts {
    private static final String OPT_FEAR_AND_LOATHING_FILE = "fear-and-loathing-file";
    private static final String DEFAULT_FEAR_AND_LOATHING_FILE = "dat/fearandloathing.pdf";

    private static final String OPT_BIBLE_FILE = "bible-file";
    private static final String DEFAULT_BIBLE_FILE = "dat/kjvdat.txt";

    private static final String OPT_SIZE = "size";
    private static final int DEFAULT_SIZE = 2;

    private static final String OPT_MODEL_CACHE_FILE ="model-cache-file";
    private static final String DEFAULT_MODEL_CACHE_FILE = "dst/cached-model.json.gz";

    private static final String OPT_POS_MODEL_FILE = "pos-model-file";
    private static final String DEFAULT_POS_MODEL_FILE = "dst/stanford-postagger-2015-04-20/models/english-left3words-distsim.tagger";

    private static final String OPT_DEST_DIR = "dest-dir";
    private static final String DEFAULT_DEST_DIR = "dst/www";

    private static final String OPT_SEED = "seed";
    private static final long DEFAULT_SEED = 0x420l;

    private final File fearAndLoathingFile;
    private final File bibleFile;
    private final int size;
    private final File modelCacheFile;
    private final File destDir;
    private final File postModelFile;
    private final long seed;

    Opts(File fearAndLoathingFile,
         File bibleFile,
         int size,
         File fearAndLoathingCacheFile,
         File postModelFile,
         File destDir,
         long seed) {
      this.fearAndLoathingFile = fearAndLoathingFile;
      this.bibleFile = bibleFile;
      this.size = size;
      this.modelCacheFile = fearAndLoathingCacheFile;
      this.postModelFile = postModelFile;
      this.destDir = destDir;
      this.seed = seed;
    }

    private static Opts parse(String[] args) throws ParseException {
      Options options = new Options();

      options.addOption(Cli.newOptionWithArg(OPT_FEAR_AND_LOATHING_FILE,
          "Fear and Loathing in Las Vegas data file",
          "FILE"));
      options.addOption(Cli.newOptionWithArg(OPT_BIBLE_FILE,
          "Bible data file",
          "FILE"));
      options.addOption(Cli.newOptionWithArg(OPT_SIZE,
          "Context size in building markov model.",
          "NUM"));
      options.addOption(Cli.newOptionWithArg(OPT_MODEL_CACHE_FILE,
          "Path to write cache file for fear and loathing model",
          "FILE"));
      options.addOption(Cli.newOptionWithArg(OPT_POS_MODEL_FILE,
          "Path to POS Tagger model file",
          "FILE"));
      options.addOption(Cli.newOptionWithArg(OPT_DEST_DIR,
          "Destination directory for output files",
          "DIR"));
      options.addOption(Cli.newOptionWithArg(OPT_SEED,
          "The seed to the PRNG",
          "NUM"));

      CommandLine cl = new DefaultParser().parse(options, args);

      return new Opts(
          new File(cl.getOptionValue(OPT_FEAR_AND_LOATHING_FILE, DEFAULT_FEAR_AND_LOATHING_FILE)),
          new File(cl.getOptionValue(OPT_BIBLE_FILE, DEFAULT_BIBLE_FILE)),
          Cli.getOptionInt(cl, OPT_SIZE, DEFAULT_SIZE, Integer::parseInt),
          new File(cl.getOptionValue(OPT_MODEL_CACHE_FILE, DEFAULT_MODEL_CACHE_FILE)),
          new File(cl.getOptionValue(OPT_POS_MODEL_FILE, DEFAULT_POS_MODEL_FILE)),
          new File(cl.getOptionValue(OPT_DEST_DIR, DEFAULT_DEST_DIR)),
          Cli.getOptionInt(cl, OPT_SEED, DEFAULT_SEED, Long::parseLong));
    }
  }

  /**
   * Select an individual token from a statistical model.
   */
  private static Tokenizer.Token selectToken(Model model, Context ctx, Tokenizer.Token t, Random rng) {
    if (!rewritablePoses.contains(t.pos())) {
      return t;
    }

    Tokenizer.Token ts = model.selectToken(ctx, t.pos(), rng);

    return ts != null ? ts : t;
  }

  /**
   * Rewrite the text of a verse using the given statistical model.
   */
  private static List<List<Tokenizer.Token>> rewrite(
      List<List<Tokenizer.Token>> text,
      Model model,
      Random rng) {
    Context ctx = new Context(model.size());
    return text.stream().map(s -> {
      ctx.clear();
      return s.stream().map(t -> {
        Tokenizer.Token ts = selectToken(model, ctx, t, rng);
        ctx.add(ts);
        return ts;
      }).collect(Collectors.toList());
    }).collect(Collectors.toList());
  }

  /**
   * Rewrite the bible using the given statistical model.
   */
  private static List<Bible.Book> rewrite(
      List<Bible.Book> books,
      Model model,
      Map<String, String> names,
      Random rng) {
    return books.stream()
        .map(b -> new Bible.Book(
            b.abbr(),
            names.get(b.abbr()),
            b.chapters().stream().map(
                c -> new Bible.Chapter(
                    c.number(),
                    c.verses().stream().map(
                        v -> new Bible.Verse(
                            v.number(),
                            rewrite(v.text(), model, rng)))))))
        .collect(Collectors.toList());
  }

  public static void main(String[] args) throws IOException, EncoderException, ParseException {

    Opts opts = Opts.parse(args);

    Tokenizer tokenizer = Tokenizer.create(opts.postModelFile);

    List<Bible.Book> bible = Bible.readFrom(opts.bibleFile, tokenizer);

    Model model = usingCacheFile(
        opts.modelCacheFile,
        () -> Model.build(
            FearAndLoathing.NAME,
            opts.size,
            tokenizer.tokenize(FearAndLoathing.readFrom(opts.fearAndLoathingFile))));

    Random rng = new Random(opts.seed);

    Map<String, String> names = Books.renameBooks(
        bible,
        model,
        rng,
        new DoubleMetaphone());


    List<Bible.Book> books = rewrite(bible, model, names, rng);

    Bible.toJson(new File(opts.destDir, "fear.json.gz"), books);
    Print.toJson(opts.destDir, TITLE, DESC, opts.seed, books);
    Print.toHtml(new File(opts.destDir, "print"), TITLE, DESC, opts.seed, books);
    Export.toJson(opts.destDir, bible, books);
  }
}
