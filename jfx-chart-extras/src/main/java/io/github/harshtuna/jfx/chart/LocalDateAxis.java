/*
 * LocalDateAxis.java
 *
 * Copyright 2015 Alexey Egorov
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

package io.github.harshtuna.jfx.chart;

import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;

public class LocalDateAxis extends TemporalAxis<LocalDate> {
    public LocalDateAxis(LocalDate base, TemporalUnit unit) {
        super(base, unit);
        defaultFormatter = new LdFormatter(this);
    }

    public LocalDateAxis(LocalDate lowerBound, LocalDate upperBound, long tickUnit, TemporalUnit unit) {
        super(lowerBound, upperBound, tickUnit, unit);
        defaultFormatter = new LdFormatter(this);
    }

    public LocalDateAxis(String axisLabel, LocalDate lowerBound, LocalDate upperBound, long tickUnit, TemporalUnit unit) {
        super(axisLabel, lowerBound, upperBound, tickUnit, unit);
        defaultFormatter = new LdFormatter(this);
    }

    @Override
    protected String getTickMarkLabel(LocalDate temporal) {
        StringConverter<LocalDate> formatter = getTemporalLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        return formatter.toString(temporal);
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * Default formatter for TimeAxis, this stays in sync with auto-ranging and formats values appropriately.
     * You can wrap this formatter to add prefixes or suffixes;
     */
    public static class LdFormatter extends DefaultFormatter<LocalDate> {
        /**
         * Construct a DefaultFormatter for the given TimeAxis
         *
         * @param axis The axis to format tick marks for
         */
        public LdFormatter(final LocalDateAxis axis) {
            super(axis);
        }

        @Override
        protected StringConverter<LocalDate> getFormatter() {
            return new LocalDateStringConverter(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ISO_LOCAL_DATE
            );
        }
    }
}
