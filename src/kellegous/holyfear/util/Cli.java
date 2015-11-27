package kellegous.holyfear.util;

import com.google.common.base.Function;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class Cli {
  public static <T> T getOptionInt(
      CommandLine cl,
      String name,
      T def,
      Function<String, T> parse) throws ParseException {
    if (!cl.hasOption(name)) {
      return def;
    }

    try {
      return parse.apply(cl.getOptionValue(name));
    } catch (NumberFormatException e) {
      throw new ParseException(String.format("%s should be an integer.", name));
    }
  }

  public static Option newOptionWithArg(String name, String desc, String argName) {
    Option opt = new Option(null, name, true, desc);
    opt.setArgName(argName);
    return opt;
  }

  private Cli() {
  }
}
