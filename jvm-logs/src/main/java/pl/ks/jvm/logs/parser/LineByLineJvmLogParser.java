
package pl.ks.jvm.logs.parser;

public interface LineByLineJvmLogParser extends JvmLogParser {
    void parseLine(String line);
}
