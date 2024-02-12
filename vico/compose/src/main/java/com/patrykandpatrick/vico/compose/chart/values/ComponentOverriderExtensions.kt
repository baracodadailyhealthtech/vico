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

package com.patrykandpatrick.vico.compose.chart.values

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.core.chart.values.ComponentOverrider
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShader
import com.patrykandpatrick.vico.core.entry.ChartEntry


/**
 * Creates and remembers a [ComponentOverrider] instance returning predefined values.
 *
 * @param entryMatcher a matcher used to determine if the line component should be overridden or not.
 * @param color the line component color override provider.
 */
@Composable
public fun rememberOverrideComponent(
    color: ((ChartEntry) -> Color)? = null,
    dynamicShader: ((ChartEntry) -> DynamicShader)? = null,
    strokeColor: ((ChartEntry) -> Color)? = null,
    entryMatcher: (ChartEntry) -> Boolean,
): ComponentOverrider {
    require((color != null) || (dynamicShader != null) || (strokeColor != null)) {
        "At least one of color, dynamicShader or strokeColor parameters needs to be non null"
    }

    return remember(entryMatcher, color, strokeColor, dynamicShader) {
        object : ComponentOverrider {
            override fun shouldOverride(chartEntry: ChartEntry): Boolean = entryMatcher(chartEntry)
            override fun getColorOverride(chartEntry: ChartEntry): Int? = color?.invoke(chartEntry)?.toArgb()
            override fun getShaderOverride(chartEntry: ChartEntry): DynamicShader? = dynamicShader?.invoke(chartEntry)
            override fun getStrokeColorOverride(chartEntry: ChartEntry): Int? =
                strokeColor?.invoke(chartEntry)?.toArgb()
        }
    }
}

/**
 * Creates and remembers a [ComponentOverrider] instance returning predefined values.
 *
 * @param entryMatcher a matcher used to determine if the line component should be overridden or not.
 * @param color the line component color override.
 */
@Composable
public fun rememberOverrideComponent(
    color: Color? = null,
    dynamicShader: DynamicShader? = null,
    strokeColor: Color? = null,
    entryMatcher: (ChartEntry) -> Boolean,
): ComponentOverrider {
    require((color != null) || (dynamicShader != null) || (strokeColor != null)) {
        "At least one of color, dynamicShader or strokeColor parameters needs to be non null"
    }

    return remember(entryMatcher, color, strokeColor, dynamicShader) {
        object : ComponentOverrider {
            override fun shouldOverride(chartEntry: ChartEntry): Boolean = entryMatcher(chartEntry)
            override fun getColorOverride(chartEntry: ChartEntry): Int? = color?.toArgb()
            override fun getShaderOverride(chartEntry: ChartEntry): DynamicShader? = dynamicShader
            override fun getStrokeColorOverride(chartEntry: ChartEntry): Int? = strokeColor?.toArgb()
        }
    }
}

/**
 * Creates and remembers a [ComponentOverrider] instance returning predefined values.
 *
 * @param entry the specific entry for which the line component should be overridden.
 * @param color the line component color override.
 */
@Composable
public fun rememberOverrideComponentForEntry(
    entry: ChartEntry,
    color: Color? = null,
    dynamicShader: DynamicShader? = null,
    strokeColor: Color? = null,
): ComponentOverrider {
    return rememberOverrideComponent(
        entryMatcher = { it == entry },
        color = color,
        dynamicShader = dynamicShader,
        strokeColor = strokeColor,
    )
}

/**
 * Creates and remembers a [ComponentOverrider] instance returning predefined values.
 *
 * @param entryIndex the specific entry [index][ChartEntry.x] for which the line component should be overridden.
 * @param color the line component color override.
 */
@Composable
public fun rememberOverrideComponentForEntryIndex(
    entryIndex: Float,
    color: Color? = null,
    dynamicShader: DynamicShader? = null,
    strokeColor: Color? = null,
): ComponentOverrider {
    return rememberOverrideComponent(
        entryMatcher = { it.x == entryIndex },
        color = color,
        dynamicShader = dynamicShader,
        strokeColor = strokeColor,
    )
}

/**
 * Creates and remembers a [ComponentOverrider] instance returning predefined values.
 *
 * @param entryValue the specific entry [value][ChartEntry.y] for which the line component should be overridden.
 * @param color the line component color override.
 */
@Composable
public fun rememberOverrideComponentForEntryValue(
    entryValue: Float,
    color: Color? = null,
    dynamicShader: DynamicShader? = null,
    strokeColor: Color? = null,
): ComponentOverrider {
    return rememberOverrideComponent(
        entryMatcher = { it.y == entryValue },
        color = color,
        dynamicShader = dynamicShader,
        strokeColor = strokeColor,
    )
}
