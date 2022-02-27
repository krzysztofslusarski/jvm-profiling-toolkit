package pl.ks.jvm.logs.parser;

import java.util.function.Supplier;
import pl.ks.jvm.logs.model.JvmLogEntry;

public interface JvmLogEntryProvider<E extends JvmLogEntry> extends Supplier<E> {
}
