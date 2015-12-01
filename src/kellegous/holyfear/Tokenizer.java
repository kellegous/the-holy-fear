package kellegous.holyfear;

import com.google.common.collect.Lists;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.StringUtils;
import kellegous.holyfear.util.Pair;

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {
  public static final int FLAG_NONE = 0;
  public static final int FLAG_HEAD = 1;
  public static final int FLAG_TERMINAL = 1<<1;
  public static final int FLAG_NO_LEADING_SPACE = 1<<2;

  private static final String TOKENIZER_OPTIONS = "invertible=true";

  private static final List<Pair<Pattern, Pattern>> bigramPatterns = Lists.newArrayList(
      Pair.of(Pattern.compile("Las"), Pattern.compile("Vegas")),
      Pair.of(Pattern.compile("Las"), Pattern.compile("Angeles")),
      Pair.of(Pattern.compile("San"), Pattern.compile("Francisco")),
      Pair.of(Pattern.compile("Dr\\."), Pattern.compile("^[A-Z].*"))
  );

  public static class Token {

    private final String text;

    private final int flags;

    private final String pos;

    public Token(String text, String pos, int flags) {
      this.text = text;
      this.pos = pos;
      this.flags = flags;
    }

    private boolean hasFlag(int mask) {
      return (flags&mask) != 0;
    }

    public boolean isTerminal() {
      return hasFlag(FLAG_TERMINAL);
    }

    public boolean isHead() {
      return hasFlag(FLAG_HEAD);
    }

    public boolean needsLeadingSpace() {
      return !hasFlag(FLAG_NO_LEADING_SPACE);
    }

    public String text() {
      return text;
    }

    public int flags() {
      return flags;
    }

    public String pos() {
      return pos;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof Token)) {
        return false;
      }

      return equals(this, (Token) obj);
    }

    @Override
    public int hashCode() {
      return  text.hashCode() ^ Integer.hashCode(flags) ^ pos.hashCode();
    }

    public static boolean equals(Token a, Token b) {
      if (a == null && b == null) {
        return true;
      }

      return a.text.equals(b.text) && a.flags == b.flags && a.pos.equals(b.pos);
    }

    public static StringBuilder format(List<Token> tokens, StringBuilder buffer) {
      for (int i = 0, n = tokens.size(); i < n; i++) {
        Token token = tokens.get(i);
        if (i > 0 && token.needsLeadingSpace()) {
          buffer.append(" ");
        }

        buffer.append(token.text());
      }

      return buffer;
    }

    private String flagAsString() {
      String f = "";
      if ((flags & FLAG_HEAD) != 0) {
        f += "h";
      }

      if ((flags & FLAG_TERMINAL) != 0) {
        f += "t";
      }

      if ((flags & FLAG_NO_LEADING_SPACE) != 0) {
        f += "s";
      }

      return f;
    }

    @Override
    public String toString() {
      return String.format("%s (%s, %s)", text, flagAsString(), pos);
    }
  }

  private static Pattern terminalPattern = Pattern.compile("\\.|[!?]+");

  private static boolean isTerminal(String word) {
    return terminalPattern.matcher(word).matches();
  }

  private static <T extends HasWord> List<List<T>> wordsToSentences(List<T> words) {
    List<List<T>> sentences = new ArrayList<>();
    List<T> sentence = new ArrayList<>();
    sentences.add(sentence);
    T prev = null;

    int quoteLevel = 0;
    Iterator<T> iter = words.iterator();

    while (iter.hasNext()) {
      T curr = iter.next();

      sentence.add(curr);
      if (quoteLevel > 0) {
        if (curr.word().equals("``")) {
          quoteLevel++;
        } else if (curr.word().equals("''") || curr.word().equals("'''")) {
          quoteLevel--;
          if (quoteLevel == 0) {
            if (isTerminal(prev.word())) {
              sentence = new ArrayList<>();
              sentences.add(sentence);
            }
          }
        }
      } else {
        if (curr.word().equals("``")) {
          quoteLevel++;
        } else if (isTerminal(curr.word())) {
          sentence = new ArrayList<>();
          sentences.add(sentence);
        }
      }

      prev = curr;
    }

    return sentences;
  }

  private static List<List<CoreLabel>> segmentIntoSentences(Reader r) {
    List<CoreLabel> tokens = Lists.newArrayList(
        new PTBTokenizer<>(r, new CoreLabelTokenFactory(), TOKENIZER_OPTIONS));
    return wordsToSentences(tokens);
  }

  private static int flagsFor(CoreLabel label, boolean isHead, boolean isTerm) {
    int flags = 0;

    if (label.before().isEmpty()) {
      flags |= FLAG_NO_LEADING_SPACE;
    }

    if (isHead) {
      flags |= FLAG_HEAD;
    } else if (isTerm) {
      flags |= FLAG_TERMINAL;
    }

    return flags;
  }

  private final MaxentTagger tagger;

  private Tokenizer(MaxentTagger tagger) {
    this.tagger = tagger;
  }

  public static Tokenizer create(File model) {
    String path = model.toString();
    MaxentTagger tagger = new MaxentTagger(
        path,
        StringUtils.argsToProperties(new String[]{"-model", path}),
        false);
    return new Tokenizer(tagger);
  }

  private static List<Token> reifyBigrams(List<Token> tokens) {
    List<Token> update = new ArrayList<>(tokens.size());

    for (int i = 1, n = tokens.size(); i <= n; i++) {
      Token a = tokens.get(i-1);

      if (i == n) {
        update.add(a);
        return update;
      }

      Token b = tokens.get(i);
      boolean isBigram = false;
      for (Pair<Pattern, Pattern> bigram : bigramPatterns) {
        if (bigram.getA().matcher(a.text()).matches() && bigram.getB().matcher(b.text()).matches()) {
          update.add(new Token(String.format("%s %s", a.text(), b.text()), a.pos(), a.flags()));
          i++;
          isBigram = true;
          break;
        }
      }

      if (!isBigram) {
        update.add(a);
      }
    }

    return update;
  }

  public List<List<Token>> tokenize(Reader r) {
    return segmentIntoSentences(r).stream()
        .map(s -> {
          List<TaggedWord> tags = tagger.tagSentence(s);
          List<Token> tokens = new ArrayList<>(s.size());
          for (int i = 0, n = s.size(), t = n - 1; i < n; i++) {
            CoreLabel label = s.get(i);
            tokens.add(new Token(
                label.originalText(),
                tags.get(i).tag(),
                flagsFor(label, i == 0, i == t)));
          }
          return reifyBigrams(tokens);
        })
        .collect(Collectors.toList());
  }
}
