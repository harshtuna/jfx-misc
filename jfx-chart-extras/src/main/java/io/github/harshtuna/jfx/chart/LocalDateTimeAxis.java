/*
 * LocalDateTimeAxis.java
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
import javafx.util.converter.LocalDateTimeStringConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;

public class LocalDateTimeAxis extends TemporalAxis<LocalDateTime> {
    public LocalDateTimeAxis(LocalDateTime base, TemporalUnit unit) {
        super(base, unit);
        defaultFormatter = new LdtFormatter(this);
    }

    public LocalDateTimeAxis(LocalDateTime lowerBound, LocalDateTime upperBound, long tickUnit, TemporalUnit unit) {
        super(lowerBound, upperBound, tickUnit, unit);
        defaultFormatter = new LdtFormatter(this);
    }

    public LocalDateTimeAxis(String axisLabel, LocalDateTime lowerBound, LocalDateTime upperBound, long tickUnit, TemporalUnit unit) {
        super(axisLabel, lowerBound, upperBound, tickUnit, unit);
        defaultFormatter = new LdtFormatter(this);
    }

    @Override
    protected String getTickMarkLabel(LocalDateTime temporal) {
        StringConverter<LocalDateTime> formatter = getTemporalLabelFormatter();
        if (formatter == null) formatter = defaultFormatter;
        return formatter.toString(temporal);
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * Default formatter for TimeAxis, this stays in sync with auto-ranging and formats values appropriately.
     * You can wrap this formatter to add prefixes or suffixes;
     */
    public static class LdtFormatter extends DefaultFormatter<LocalDateTime> {
        /**
         * Construct a DefaultFormatter for the given TimeAxis
         *
         * @param axis The axis to format tick marks for
         */
        public LdtFormatter(final LocalDateTimeAxis axis) {
            super(axis);
        }

        @Override
        protected StringConverter<LocalDateTime> getFormatter() {
            return new LocalDateTimeStringConverter(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
