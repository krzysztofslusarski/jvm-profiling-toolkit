package pl.ks.jfr.parser.tuning;

import org.openjdk.jmc.common.item.IItem;
import pl.ks.jfr.parser.JfrAccessors;

public interface PreStackFilter {
    boolean shouldInclude(JfrAccessors accessors, IItem event);
}
