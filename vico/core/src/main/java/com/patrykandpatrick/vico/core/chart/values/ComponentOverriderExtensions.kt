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

import com.patrykandpatrick.vico.core.component.Component
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShader
import com.patrykandpatrick.vico.core.entry.ChartEntry

/**
 * Determines and returns the appropriate component to use for the given [chartEntry][ChartEntry].
 * When no override is necessary, returns the [defaultComponent].
 * When an override is necessary, it will be provided by [overrideBuilder].
 */
internal fun <T : Component> ComponentOverrider?.overrideComponentForEntry(
    chartEntry: ChartEntry,
    defaultComponent: T,
    overrideBuilder: (cacheKey: String, color: Int?, shader: DynamicShader?, strokeColor: Int?) -> T,
): T {
    return when {
        this == null -> defaultComponent
        shouldOverride(chartEntry) -> {
            val colorOverride = getColorOverride(chartEntry)
            val shaderOverride = getShaderOverride(chartEntry)
            val strokeColorOverride = getStrokeColorOverride(chartEntry)
            val cacheKey = "$colorOverride##${shaderOverride.hashCode()}##$strokeColorOverride"

            overrideBuilder(cacheKey, colorOverride, shaderOverride, strokeColorOverride)
        }

        else -> defaultComponent
    }
}
