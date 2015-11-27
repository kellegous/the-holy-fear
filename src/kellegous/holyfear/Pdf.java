package kellegous.holyfear;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Pdf {
  private static class Handler implements ContentHandler {

    private final StringBuilder buffer = new StringBuilder();

    private final List<String> paragraphs = new ArrayList<>();

    private final Function<String, String> tx;

    Handler(Function<String, String> tx) {
      this.tx = tx;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
      commitCurrentBuffer();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    private void commitCurrentBuffer() {
      if (buffer.length() > 0) {
        paragraphs.add(buffer.toString());
        buffer.setLength(0);
      }
    }

    /**
     * Indicates that the line is either an empty line or one with only digits (a page number).
     *
     * @param ch
     * @param start
     * @param length
     * @return
     */
    private static boolean isBreak(char[] ch, int start, int length) {
      for (int i = start; i < length; i++) {
        if (!Character.isWhitespace(ch[i]) && !Character.isDigit(ch[i])) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (isBreak(ch, start, length)) {
        commitCurrentBuffer();
        return;
      }

      String v = tx.apply(new String(ch, start, length));
      if (v != null) {
        buffer.append(v);
      }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    public void write(Writer writer) throws IOException {
      for (String para : paragraphs) {
        writer.write(para);
        writer.write('\n');
      }
    }
  }

  public static Reader extractToReader(InputStream src, Function<String, String> tx) throws IOException {
    StringBuilder buf = new StringBuilder();
    for (String para : extractParagraphs(src, tx)) {
      buf.append(para);
      buf.append('\n');
    }
    return new StringReader(buf.toString());
  }

  public static List<String> extractParagraphs(InputStream src, Function<String, String> tx) throws IOException {
    try {
      Handler handler = new Handler(tx);
      new PDFParser().parse(src, handler, new Metadata(), new ParseContext());
      return handler.paragraphs;
    } catch (SAXException | TikaException e) {
      throw new IOException(e);
    }
  }
}
