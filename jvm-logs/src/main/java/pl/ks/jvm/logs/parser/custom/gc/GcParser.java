package pl.ks.jvm.logs.parser.custom.gc;

import pl.ks.jvm.logs.model.gc.GcLog;
import pl.ks.jvm.logs.parser.JvmLogEntryProvider;
import pl.ks.jvm.logs.parser.LineByLineJvmLogParser;

class GcParser implements JvmLogEntryProvider<GcLog>, LineByLineJvmLogParser {
    @Override
    public void parseLine(String line) {

    }

    @Override
    public GcLog get() {
        return null;
    }
}
