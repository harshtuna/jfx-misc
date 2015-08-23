/*
 * BiTemporalChart.groovy
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

package io.github.harshtuna.jfx.chart

import javafx.geometry.Side
import javafx.scene.chart.XYChart

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import static groovyx.javafx.GroovyFX.start


start {
    def lby = LocalDateTime.parse('2015-07-05T00:00:00')
    def uby = LocalDateTime.parse('2015-08-15T00:00:00')
    //static axis, major unit: 5 days
    final yAxis = new LocalDateTimeAxis(lby, uby, 5, ChronoUnit.DAYS)

    def lbx = LocalDate.parse('2015-07-05')
    //dynamic axis, major unit in days (auto-calculated)
    final xAxis = new LocalDateAxis(lbx, ChronoUnit.DAYS)

    def d1y = yAxis.parse('2015-07-07T00:00:00')
    def d1x = xAxis.parse('2015-07-07')
    def d2y = yAxis.parse('2015-08-10T00:00:00')
    def d2x = xAxis.parse('2015-07-15')
    def d3y = yAxis.parse('2015-07-15T00:00:00')
    def d3x = xAxis.parse('2015-07-27')
    def series1data = [d1x, d1y, d2x, d2y, d3x, d3y]

    def series2data = [d1x, d3y, d3x, d1y, d2x, d3y]

    stage(title: "BiTemporalChart", visible: true) {
        scene {
            vbox {
                yAxis.label = "Y Axis"
                xAxis.label = "X Axis"

                final XYChart chart = lineChart(xAxis: xAxis, yAxis: yAxis) {
                    series(name: 'First Series', data: series1data)
                    series(name: 'Second Series', data: series2data)
                }

                button(text: 'inverse Y') {
                    onAction {
                        yAxis.inverse()
                        if (yAxis.inversed)
                            xAxis.side = Side.TOP
                        else
                            xAxis.side = Side.BOTTOM
                    }
                }
                button(text: 'inverse X') {
                    onAction {
                        xAxis.inverse()
                        if (xAxis.inversed)
                            yAxis.side = Side.RIGHT
                        else
                            yAxis.side = Side.LEFT
                    }
                }
            }
        }
    }
}

