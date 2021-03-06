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
package pl.ks.viewer.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class PageCreatorHelper {
    public static String numToString(BigDecimal bigDecimal, DecimalFormat decimalFormat) {
        if (bigDecimal == null) {
            return null;
        }
        return decimalFormat.format(bigDecimal.setScale(2, RoundingMode.HALF_EVEN)).replaceAll(",", " ");
    }

    public static String numToString(Double number, DecimalFormat decimalFormat) {
        if (number == null) {
            return null;
        }
        return decimalFormat.format(number).replaceAll(",", " ");
    }
}
