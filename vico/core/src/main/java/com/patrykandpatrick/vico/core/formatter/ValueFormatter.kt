/*
 * Copyright 2022 by Patryk Goworowski and Patrick Michalik.
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

package com.patrykandpatrick.vico.core.formatter

import com.patrykandpatrick.vico.core.chart.values.ChartValues

/**
 * Formats values for display.
 */
public interface ValueFormatter {

    /**
     * Called to format axis labels and data labels.
     *
     * @param value the value to be formatted.
     * @param chartValues the [ChartValues] used by the chart.
     *
     * @see ChartValues
     *
     * @return a formatted value.
     */
    public fun formatValue(
        value: Float,
        chartValues: ChartValues,
    ): CharSequence
}