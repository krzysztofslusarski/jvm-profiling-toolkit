/*
 * Copyright 2022 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.cli.tui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Formats {
    private static final ThreadLocal<DecimalFormat> GROUPED = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0", symbols);
    });

    private Formats() {
    }

    static String number(long value) {
        return GROUPED.get().format(value);
    }

    static String pad(String text, int width, boolean rightAligned) {
        if (text.length() > width) {
            return width <= 2 ? text.substring(0, width) : text.substring(0, width - 2) + "..";
        }
        String padding = " ".repeat(width - text.length());
        return rightAligned ? padding + text : text + padding;
    }
}
