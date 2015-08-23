/*
 * LocalDateTimeAxisTest.java
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

import de.saxsys.javafx.test.JfxRunner;
import javafx.geometry.Side;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JfxRunner.class)
public class LocalDateTimeAxisTest {
    public static final double DEFAULT_PRECISION = 0.00001;
    AxisInspector axis;
    LocalDateTime lowerBound;
    private ChronoUnit chronoUnit;
    private LocalDateTime upperBound;

    @Before
    public void setUp() {
        chronoUnit = ChronoUnit.MINUTES;
        lowerBound = LocalDateTime.of(2015, 8, 15, 18, 23, 1, 0);
        upperBound = lowerBound.plus(20, chronoUnit);

        axis = new AxisInspector(lowerBound, upperBound, 10, chronoUnit);
        axis.calculateNewScale(100);
    }

    @Test
    public void testGetZeroPosition() throws Exception {
        assertEquals(0, axis.getZeroPosition(), .1);

        axis.setRange(new TemporalAxis.Range(1L, 40L, 5L, 1.2), false);
        assertEquals(Double.NaN, axis.getZeroPosition(), .1);

        axis.setRange(new TemporalAxis.Range(-40L, 40L, 5L, 1.2), false);
        assertEquals(48, axis.getZeroPosition(), .1); //todo verify
    }

    @Test
    public void testCalculateTickValues() {
        final List<Long> tickValues = axis.calculateTickValues(100, axis.getRange());
//        System.out.println(tickValues);

        List<Long> expected = Arrays.asList(0L, 10L, 20L);

        assertEquals(3, tickValues.size());
        assertEquals(expected, tickValues);
    }

    @Ignore //fixme
    @Test
    public void testAutoRangeVertical() throws Exception {
        axis = new AxisInspector(lowerBound, chronoUnit);
        axis.setSide(Side.RIGHT);

        long minValue = -49L;
        long maxValue = 55L;
        axis.invalidateRange(Arrays.asList(minValue, 10L, 15L, maxValue));

        int labelSize = 5;
        Object expected = new TemporalAxis.Range(-54L, 60L, 6L, -4.38596);
        assertEquals(expected, axis.autoRange(minValue, maxValue, 500, labelSize));
    }

    @Test
    public void testAutoRangeHorizontal() throws Exception {
        axis = new AxisInspector(lowerBound, chronoUnit);
        axis.setSide(Side.BOTTOM);

        long minValue = -49L;
        long maxValue = 55L;
        axis.invalidateRange(Arrays.asList(minValue, maxValue));

        int labelSize = 5;
        Object expected = new TemporalAxis.Range(-58L, 58L, 29L, 4.310344);
        assertEquals(expected, axis.autoRange(minValue, maxValue, 500, labelSize));
    }

    @Test
    public void testSetRangeNoAnimation() throws Exception {
        final TemporalAxis.Range range = new TemporalAxis.Range(0L, 40L, 5L, 1.2);

        axis.setRange(range, false);

        assertEquals(range, axis.getRange());
    }

    @Test
    public void testSetRangeWithAnimation() throws Exception {
        final TemporalAxis.Range range = new TemporalAxis.Range(0L, 40L, 5L, 4.0);

        axis.setRange(range, true);
        Thread.sleep(800); //wait for animation

        assertEquals(range, axis.getRange());
    }


    @Test
    public void testGetRange() throws Exception {

        assertEquals(new TemporalAxis.Range(0L, 20L, 10L, 5.0), axis.getRange());
    }


    @Test
    public void testGetDisplayPosition() throws Exception {
        double v = axis.getDisplayPosition(5L);

        assertEquals(25, v, .1);
    }

    @Test
    public void testGetValueForDisplay() throws Exception {
        long v = axis.getValueForDisplay(75);

        assertEquals(15, v);
    }

    @Test
    public void testIsValueOnAxis() throws Exception {
        assertEquals(true, axis.isValueOnAxis(axis.toLong(lowerBound)));
        assertEquals(true, axis.isValueOnAxis(axis.toLong(upperBound)));

        assertEquals(false, axis.isValueOnAxis(axis.toLong(lowerBound.minus(1, chronoUnit))));
        assertEquals(false, axis.isValueOnAxis(axis.toLong(upperBound.plus(1, chronoUnit))));
    }

    @Test
    public void testToNumericValue() throws Exception {
        assertEquals(15, axis.toNumericValue(15L), .1);
    }

    @Test
    public void testToRealValue() throws Exception {
        assertEquals(15, axis.toNumericValue(15L), .1);
    }

    @Test
    public void testGetTickMarkLabel() throws Exception {
        assertEquals("2015-08-15T18:23:01", axis.getTickMarkLabel(lowerBound));
    }

    @Test
    public void testParse() {
        assertEquals(0, axis.parse(lowerBound.toString()));
    }

    @Test
    public void testInverseHorizontal() {
        axis.setSide(Side.BOTTOM);
        axis.calculateNewScale(100);

        long lb = (long) axis.getLowerBound(); //0
        long ub = (long) axis.getUpperBound(); //20

        assertEquals(0, axis.getDisplayPosition(lb), DEFAULT_PRECISION);
        assertEquals(5, axis.getDisplayPosition(1L), DEFAULT_PRECISION);
        assertEquals(100, axis.getDisplayPosition(ub), DEFAULT_PRECISION);

        axis.inverse();

        assertEquals(100, axis.getDisplayPosition(lb), DEFAULT_PRECISION);
        assertEquals(95, axis.getDisplayPosition(1L), DEFAULT_PRECISION);
        assertEquals(0, axis.getDisplayPosition(ub), DEFAULT_PRECISION);
    }

    @Test
    public void testInverseVertical() {
        axis.setSide(Side.LEFT);
        axis.calculateNewScale(100);

        long lb = (long) axis.getLowerBound(); //0
        // vertical axis is inversed internally in JavaFX
        assertEquals(100, axis.getDisplayPosition(lb), DEFAULT_PRECISION);
        axis.inverse();
        assertEquals(0, axis.getDisplayPosition(lb), DEFAULT_PRECISION);
    }

    private static class AxisInspector extends LocalDateTimeAxis {
        public AxisInspector(LocalDateTime lowerBound, LocalDateTime upperBound, long tickUnit, TemporalUnit tempUnit) {
            super(lowerBound, upperBound, tickUnit, tempUnit);
        }

        public AxisInspector(LocalDateTime base, TemporalUnit tempUnit) {
            super(base, tempUnit);
        }
    }

}