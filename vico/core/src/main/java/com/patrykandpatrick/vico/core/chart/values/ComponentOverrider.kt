/*
 * Copyright 2024 by Patryk Goworowski and Patrick Michalik.
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

package com.patrykandpatrick.vico.core.chart.values

import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShader
import com.patrykandpatrick.vico.core.entry.ChartEntry

/**
 * Overrider for Column [line components][LineComponent].
 */
public interface ComponentOverrider {
    /**
     * @return true when the component associated to the provided [entry][ChartEntry] requires runtime customisation.
     */
    public fun shouldOverride(chartEntry: ChartEntry): Boolean = false

    /**
     * @return the provided [entry][ChartEntry] override color, null when no changes are requested.
     */
    public fun getColorOverride(chartEntry: ChartEntry): Int? = null

    /**
     * @return the provided [entry][ChartEntry] override shader, null when no changes are requested.
     */
    public fun getShaderOverride(chartEntry: ChartEntry): DynamicShader? = null

    /**
     * @return the provided [entry][ChartEntry] override stroke color, null when no changes are requested.
     */
    public fun getStrokeColorOverride(chartEntry: ChartEntry): Int? = null
}
