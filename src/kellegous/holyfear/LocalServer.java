package kellegous.holyfear;

import kellegous.holyfear.util.Cli;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class LocalServer {
  private static ResourceHandler resourceHandlerFor(File file) {
    ResourceHandler handler = new ResourceHandler() {
      @Override
      protected void doResponseHeaders(HttpServletResponse response, Resource resource, String mimeType) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        super.doResponseHeaders(response, resource, mimeType);
      }
    };
    handler.setDirectoriesListed(true);
    handler.setWelcomeFiles(new String[]{"index.html"});
    handler.setResourceBase(file.getAbsolutePath());
    return handler;
  }

  private static HandlerList handlersOf(Handler... handlers) {
    HandlerList hl = new HandlerList();
    hl.setHandlers(handlers);
    return hl;
  }

  private static class Opts {
    private static final String OPT_DAT_DIR = "data-dir";
    private static final String DEFAULT_DAT_DIR = "dst/www";

    private static final String OPT_PORT = "port";
    private static final int DEFAULT_PORT = 8081;

    private final File datDir;
    private final int port;

    private Opts(File datDir, int port) {
      this.datDir = datDir;
      this.port = port;
    }

    private static Opts parse(String[] args) throws ParseException {
      Options options = new Options();

      options.addOption(Cli.newOptionWithArg(OPT_DAT_DIR,
          "Data directory where some JSON shit is found.",
          "DIR"));
      options.addOption(Cli.newOptionWithArg(OPT_PORT,
          "Port where the HTTP motherfucker listens.",
          "NUM"));

      CommandLine cl = new DefaultParser().parse(options, args);

      return new Opts(
          new File(cl.getOptionValue(OPT_DAT_DIR, DEFAULT_DAT_DIR)),
          Cli.getOptionInt(cl, OPT_PORT, DEFAULT_PORT, Integer::parseInt));
    }
  }

  public static void main(String[] args) throws Exception {
    Opts opts = Opts.parse(args);

    Server server = new Server();
    ServerConnector c = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
    c.setPort(opts.port);

    server.addConnector(c);
    server.setHandler(handlersOf(
        resourceHandlerFor(opts.datDir),
        resourceHandlerFor(new File("pub")),
        new DefaultHandler()
    ));
    server.start();
    server.join();
  }
}
