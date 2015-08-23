/*
 * TemporalAxis.java
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

import com.sun.javafx.charts.ChartLayoutAnimator;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableLongProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Side;
import javafx.scene.chart.ValueAxis;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An axis for {@link Temporal} data.
 * Due to Java FX restrictions the data needs to be translated into Number representation. This class provides
 * conversion methods, to be invoked by Date/Time enabled chart.
 *
 * Exact temporal type and formatting rules to be specified in the subclasses.
 */
public abstract class TemporalAxis<T extends Temporal> extends ValueAxis<Long> {
// todo - a lot of code was borrowed as is from ValueAxis and NumberAxis
    private final T base;
    private final TemporalUnit unit;
    private Object currentAnimationID;
    private final ChartLayoutAnimator animator = new ChartLayoutAnimator(this);
    protected DefaultFormatter<T> defaultFormatter;
    private boolean inversed = false;
    double offset = 0;
    // hack to override JFX hardcoded mark label behavior
    private Field textNodeField = unlockedTextNodeField();

    // -------------- PUBLIC PROPERTIES --------------------------------------------------------------------------------

    /**
     * The value between each major tick mark in data units. This is automatically set if we are auto-ranging.
     */
    private LongProperty tickUnit = new StyleableLongProperty(5) {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }

        @Override
        public CssMetaData<TemporalAxis, Number> getCssMetaData() {
            return StyleableProperties.TICK_UNIT;
        }

        @Override
        public Object getBean() {
            return TemporalAxis.this;
        }

        @Override
        public String getName() {
            return "tickUnit";
        }
    };


    public final long getTickUnit() {
        return tickUnit.get();
    }

    public final void setTickUnit(long value) {
        tickUnit.set(value);
    }

    public final LongProperty tickUnitProperty() {
        return tickUnit;
    }

    /**
     * StringConverter used to format tick mark labels. If null a default will be used
     */
    private final ObjectProperty<StringConverter<T>> temporalLabelFormatter =
            new ObjectPropertyBase<StringConverter<T>>(null) {
                @Override
                protected void invalidated() {
                    invalidateRange();
                    requestAxisLayout();
                }

                @Override
                public Object getBean() {
                    return TemporalAxis.this;
                }

                @Override
                public String getName() {
                    return "temporalLabelFormatter";
                }
            };

    public final StringConverter<T> getTemporalLabelFormatter() {
        return temporalLabelFormatter.getValue();
    }

    public final void setTemporalLabelFormatter(StringConverter<T> value) {
        temporalLabelFormatter.setValue(value);
    }

    public final ObjectProperty<StringConverter<T>> temporalLabelFormatterProperty() {
        return temporalLabelFormatter;
    }

    // -------------- CONSTRUCTORS -------------------------------------------------------------------------------------

    /**
     * Create an auto-ranging TemporalAxis
     */
    public TemporalAxis(T base, TemporalUnit unit) {
        super();
        this.base = base;
        this.unit = unit;
    }

    /**
     * Create a non-auto-ranging TemporalAxis with the given upper bound, lower bound and tick unit
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit   The tick unit, ie space between tickmarks in tempUnit-s
     */
    public TemporalAxis(T lowerBound, T upperBound, long tickUnit, TemporalUnit unit) {
        super(lowerBound.until(lowerBound, unit), lowerBound.until(upperBound, unit)); //toLong() inlined
        this.base = lowerBound;
        this.unit = unit;
        setTickUnit(tickUnit);
    }

    /**
     * Create a non-auto-ranging TemporalAxis with the given upper bound, lower bound and tick unit
     *
     * @param axisLabel  The name to display for this axis
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     * @param tickUnit   The tick unit, ie space between tickmarks
     */
    public TemporalAxis(String axisLabel, T lowerBound, T upperBound, long tickUnit, TemporalUnit unit) {
        this(lowerBound, upperBound, tickUnit, unit);
        setLabel(axisLabel);
    }

    // -------------- PROTECTED METHODS --------------------------------------------------------------------------------

    @Override
    protected String getTickMarkLabel(Long value) {
        return getTickMarkLabel(toTemporal(value));
    }

    protected abstract String getTickMarkLabel(T temporal);

    /**
     * Called to get the current axis range.
     *
     * @return A range object that can be passed to setRange() and calculateTickValues()
     */
    @Override
    protected Range getRange() {
        return new Range(
                (long) getLowerBound(),
                (long) getUpperBound(),
                getTickUnit(),
                getScale()
        );
    }

    /**
     * Called to set the current axis range to the given range. If isAnimating() is true then this method should
     * animate the range to the new range.
     *
     * @param range   A range object returned from autoRange()
     * @param animate If true animate the change in range
     */
    @Override
    protected void setRange(Object range, boolean animate) {
        final Range r = (Range) range;
        final long oldLowerBound = (long) getLowerBound();
        setLowerBound(r.lowerBound());
        setUpperBound(r.upperBound());
        setTickUnit(r.tickUnit());
        if (animate) {
            animator.stop(currentAnimationID);
            final WritableScale writableScale = new WritableScale();
            currentAnimationID = animator.animate(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(currentLowerBound, oldLowerBound),
                            new KeyValue(writableScale, getScale())
                    ),
                    new KeyFrame(Duration.millis(700),
                            new KeyValue(currentLowerBound, r.lowerBound()),
                            new KeyValue(writableScale, r.scale())
                    )
            );
        } else {
            currentLowerBound.set(r.lowerBound());
            setScale(r.scale());
        }
    }

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param length The length of the axis in display units
     * @param range  A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    @Override
    protected List<Long> calculateTickValues(double length, Object range) {
        final Range r = (Range) range;
        List<Long> tickValues = new ArrayList<>();
        if (r.lowerBound() == r.upperBound()) {
            tickValues.add(r.lowerBound());
        } else if (r.tickUnit() <= 0) {
            tickValues.add(r.lowerBound());
            tickValues.add(r.upperBound());
        } else if (r.tickUnit() > 0) {
            if (((r.upperBound() - r.lowerBound()) / r.tickUnit()) > 2000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 2000 major tick marks on a TemporalAxis. " +
                        "Lower Bound=" + r.lowerBound() + ", Upper Bound=" + r.upperBound() + ", Tick Unit=" + r.tickUnit());
            } else {
                if (r.lowerBound() + r.tickUnit() < r.upperBound()) {
                    for (long major = r.lowerBound(); major < r.upperBound(); major += r.tickUnit()) {
                        tickValues.add(major);
                    }
                }
            }
            tickValues.add(r.upperBound());
        }
        return tickValues;
    }

    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */
    protected List<Long> calculateMinorTickMarks() {
        final List<Long> minorTickMarks = new ArrayList<>();
        final double lowerBound = getLowerBound();
        final double upperBound = getUpperBound();
        final double tickUnit = getTickUnit();
        final double minorUnit = tickUnit / Math.max(1, getMinorTickCount());
        if (tickUnit > 0) {
            if (((upperBound - lowerBound) / minorUnit) > 10000) {
                // This is a ridiculous amount of major tick marks, something has probably gone wrong
                System.err.println("Warning we tried to create more than 10000 minor tick marks on a TemporalAxis. " +
                        "Lower Bound=" + getLowerBound() + ", Upper Bound=" + getUpperBound() + ", Tick Unit=" + tickUnit);
                return minorTickMarks;
            }
            for (double minor = Math.floor(lowerBound) + minorUnit; minor < Math.ceil(lowerBound); minor += minorUnit) {
                if (minor > lowerBound) {
                    minorTickMarks.add((long) minor);
                }
            }
            for (double major = Math.ceil(lowerBound); major < upperBound; major += tickUnit) {
                final double next = Math.min(major + tickUnit, upperBound);
                for (double minor = major + minorUnit; minor < next; minor += minorUnit) {
                    minorTickMarks.add((long) minor);
                }
            }
        }
        return minorTickMarks;
    }

    /**
     * Called to set the upper and lower bound and anything else that needs to be auto-ranged
     *
     * @param minValue  The min data value that needs to be plotted on this axis
     * @param maxValue  The max data value that needs to be plotted on this axis
     * @param length    The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     */
    // todo - extend symmetrically maybe
    @Override
    protected Object autoRange(double minValue, double maxValue, double length, double labelSize) {
        final long range = (long) (maxValue - minValue);
        // pad min and max by 2%, checking if the range is zero
        final long padding = (range < 100) ? 1 : (long) (range * 0.01);
        final long paddedRange = range + padding * 2;
        final long paddedMin = (long) minValue - padding;
        final long paddedMax = (long) maxValue + padding;
        // calculate the number of tick-marks we can fit in the given length
        int numOfTickMarks = (int) Math.floor(length / labelSize);
        // can never have less than 2 tick marks one for each end
        numOfTickMarks = Math.max(numOfTickMarks, 2);
        // calculate tick unit for the number of ticks can have in the given data range
        // search for the best tick unit that fits
        double tickUnitRounded = Math.max(paddedRange / numOfTickMarks, 1);
        double tickUnitPrev = 0;
        long minRounded = 0;
        long maxRounded = 0;
        int count = 0;
        double reqLength = Double.MAX_VALUE;
        // loop till we find a set of ticks that fit length and result in a total of less than 20 tick marks
        final Side side = getSide();
        while (reqLength > length || count > 20) {
//            System.out.println(minRounded + " " + maxRounded + " " + tickUnitRounded);
            tickUnitPrev = tickUnitRounded;
            // find a user friendly match from our default tick units to match calculated tick unit
            // todo
            // move min and max to nearest tick mark
            minRounded = (long) (Math.floor(paddedMin / tickUnitRounded) * tickUnitRounded);
            maxRounded = (long) (Math.ceil(paddedMax / tickUnitRounded) * tickUnitRounded);
            // calculate the required length to display the chosen tick marks for real, this will handle if there are
            // huge numbers involved etc or special formatting of the tick mark label text
            double maxReqTickGap = 0;
            count = 0;
            for (long tick = minRounded; tick <= maxRounded; tick += tickUnitRounded, count++) {
                final Dimension2D tickMarkArea = measureTickMarkSize(tick, getTickLabelRotation());
                double size = side.isVertical() ? tickMarkArea.getHeight() : tickMarkArea.getWidth();
                maxReqTickGap = (long) Math.max(maxReqTickGap, size + getTickLabelGap());
            }
            reqLength = (count - 1) * maxReqTickGap;
            // check if we already found max tick unit
            // todo
            if (reqLength > length || count > 20) {
                tickUnitRounded = (long) (tickUnitRounded * reqLength / length) + 1;
                if (tickUnitRounded == tickUnitPrev) tickUnitRounded++;
            }
        }
        // calculate new scale
        final double newScale = calculateNewScale(length, minRounded, maxRounded);
        // return new range
        return new Range(minRounded, maxRounded, (long) tickUnitRounded, newScale);
    }

    protected Dimension2D measureTickMarkSize(long tick, double rotation) {
        return measureTickMarkLabelSize(getTickMarkLabel(tick), rotation);
    }

    void calculateNewScale(double length) {
        this.offset = length;
        setScale(super.calculateNewScale(length, getLowerBound(), getUpperBound()));
    }

    /**
     * Overridden to compensate JFX hardcoded axis direction and mark label behavior for inversion
     */
    //todo - JavaFX pull request for axis direction cleanup and protected access
    @Override
    protected void layoutChildren() {
        final Side side = getSide();
        boolean isHorisontal = null == side || side.isHorizontal();
        this.offset = isHorisontal ? getWidth() : getHeight();
        super.layoutChildren();
        if (inversed) {
            double prevEnd = isHorisontal ? offset + getTickLabelGap() : -getTickLabelGap();
            for (TickMark m : getTickMarks()) {
                double position = m.getPosition();
                try {
                    final Text textNode = (Text) textNodeField.get(m);
                    final Bounds bounds = textNode.getLayoutBounds();
                    if (0 <= position && position <= offset)
                        if (isHorisontal) {
                            textNode.setVisible(position < prevEnd);
                            prevEnd = position - (bounds.getWidth() + getTickLabelGap());
                        } else {
                            textNode.setVisible(position > prevEnd);
                            prevEnd = position + (bounds.getHeight() + getTickLabelGap());
                        }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    private Field unlockedTextNodeField() {
        Field f = null;
        try {
            f = TickMark.class.getDeclaredField("textNode");
            f.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
        return f;
    }

    @Override
    public Long getValueForDisplay(double displayPosition) {
        if (inversed)
            return super.getValueForDisplay(offset - displayPosition);
        else
            return super.getValueForDisplay(displayPosition);
    }

    @Override
    public double getDisplayPosition(Long value) {
        if (inversed)
            return offset - super.getDisplayPosition(value);
        else
            return super.getDisplayPosition(value);
    }

    @Override
    public double getZeroPosition() {
        if (0 < getLowerBound() || 0 > getUpperBound()) return Double.NaN;
        return getDisplayPosition(0L);
    }

    @Override
    public Long toRealValue(double value) {
        return (long) value;
    }

    /**
     * Inverts axis direction - using some ugly hacks.
     * Use with caution.
     * @deprecated
     */
    public void inverse() {
        inversed = !inversed; // boolean property
        invalidateRange();
        requestAxisLayout();
    }

    long parse(String string) {
        return toLong(defaultFormatter.fromString(string));
    }

    public long toLong(T temporal) {
        return base.until(temporal, unit);
    }

    T toTemporal(long val) {
        return (T) base.plus(val, unit);
    }

    public boolean isInversed() {
        return inversed;
    }

    // -------------- INNER CLASSES ------------------------------------------------------------------------------------

    /**
     * Default formatter for TemporalAxis, this stays in sync with auto-ranging and formats values appropriately.
     */
    protected static abstract class DefaultFormatter<TF extends Temporal> extends StringConverter<TF> {
        private StringConverter<TF> formatter;

        /**
         * Construct a DefaultFormatter for the given TimeAxis
         *
         * @param axis The axis to format tick marks for
         */
        public DefaultFormatter(final TemporalAxis<TF> axis) {
            formatter = getFormatter();
//            final ChangeListener axisListener = new ChangeListener() {
//                @Override
//                public void changed(ObservableValue observable, Object oldValue, Object newValue) {
//                    formatter = getFormatter();
//                }
//            };
//            axis.autoRangingProperty().addListener(axisListener);
        }

        protected abstract StringConverter<TF> getFormatter();

        @Override
        public String toString(TF object) {
            return formatter.toString(object);
        }

        @Override
        public TF fromString(String string) {
            return formatter.fromString(string);
        }
    }

    protected static class Range {
        public static final double SCALE_PRECISION = 0.000001;

        private final double[] rangeProps;

        public Range(long lowerBound, long upperBound, long tickUnit, double scale) {
            rangeProps = new double[]{lowerBound, upperBound, tickUnit, scale};
        }

        public long lowerBound() {
            return (long) rangeProps[0];
        }

        public long upperBound() {
            return (long) rangeProps[1];
        }

        public long tickUnit() {
            return (long) rangeProps[2];
        }

        public double scale() {
            return rangeProps[3];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Range)) return false;

            Range r = (Range) o;

            return rangeProps.length == r.rangeProps.length &&
                    rangeProps[0] == (r.rangeProps[0]) &&
                    rangeProps[1] == (r.rangeProps[1]) &&
                    rangeProps[2] == (r.rangeProps[2]) &&
                    Math.abs(scale() - r.scale()) < SCALE_PRECISION;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "rangeProps=" + Arrays.toString(rangeProps) +
                    '}';
        }
    }

    // -------------- STYLESHEET HANDLING ------------------------------------------------------------------------------

    /**
     * @treatAsPrivate implementation detail
     */
    private static class StyleableProperties {
        private static final CssMetaData<TemporalAxis, Number> TICK_UNIT =
                new CssMetaData<TemporalAxis, Number>("-fx-tick-unit",
                        SizeConverter.getInstance(), 5.0) {

                    @Override
                    public boolean isSettable(TemporalAxis n) {
                        return n.tickUnit == null || !n.tickUnit.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(TemporalAxis n) {
                        return (StyleableProperty<Number>) n.tickUnitProperty();
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(ValueAxis.getClassCssMetaData());
            styleables.add(TICK_UNIT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }


    private class WritableScale implements WritableValue<Double> {
        @Override
        public Double getValue() {
            return getScale();
        }

        @Override
        public void setValue(Double value) {
            setScale(value);
        }
    }


}
