package pl.ks.jfr.parser.filter;

import org.openjdk.jmc.common.item.IItem;
import pl.ks.jfr.parser.JfrAccessors;

public interface PreStackFilter {
    boolean shouldInclude(JfrAccessors accessors, IItem event);
}
