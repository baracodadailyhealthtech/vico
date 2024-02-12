/*
 * Copyright 2023 by Patryk Goworowski and Patrick Michalik.
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

package com.patrykandpatrick.vico.core.chart.column

import com.patrykandpatrick.vico.core.DefaultDimens
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.AxisRenderer
import com.patrykandpatrick.vico.core.chart.BaseChart
import com.patrykandpatrick.vico.core.chart.Chart
import com.patrykandpatrick.vico.core.chart.composed.ComposedChart
import com.patrykandpatrick.vico.core.chart.dimensions.MutableHorizontalDimensions
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.chart.forEachInIndexed
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.put
import com.patrykandpatrick.vico.core.chart.values.ChartValues
import com.patrykandpatrick.vico.core.chart.values.ChartValuesManager
import com.patrykandpatrick.vico.core.chart.values.ChartValuesProvider
import com.patrykandpatrick.vico.core.chart.values.ComponentOverrider
import com.patrykandpatrick.vico.core.chart.values.overrideComponentForEntry
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShader
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.component.text.VerticalPosition
import com.patrykandpatrick.vico.core.component.text.inBounds
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.diff.DefaultDrawingModelInterpolator
import com.patrykandpatrick.vico.core.entry.diff.DrawingModelInterpolator
import com.patrykandpatrick.vico.core.entry.diff.ExtraStore
import com.patrykandpatrick.vico.core.entry.diff.MutableExtraStore
import com.patrykandpatrick.vico.core.extension.doubled
import com.patrykandpatrick.vico.core.extension.getRepeating
import com.patrykandpatrick.vico.core.extension.getStart
import com.patrykandpatrick.vico.core.extension.half
import com.patrykandpatrick.vico.core.extension.setAll
import com.patrykandpatrick.vico.core.formatter.DecimalFormatValueFormatter
import com.patrykandpatrick.vico.core.formatter.ValueFormatter
import com.patrykandpatrick.vico.core.marker.Marker
import kotlin.math.abs

/**
 * [ColumnChart] displays data as vertical bars. It can group and stack columns.
 *
 * @param columns the [LineComponent] instances to use for columns. This list is iterated through as many times
 * as necessary for each column collection. If the list contains a single element, all columns have the same appearance.
 * @param spacingDp the distance between neighboring column collections.
 * @param innerSpacingDp the distance between neighboring grouped columns.
 * @param mergeMode defines how columns should be drawn in column collections.
 * @param targetVerticalAxisPosition if this is set, any [AxisRenderer] with an [AxisPosition] equal to the provided
 * value will use the [ChartValues] provided by this chart. This is meant to be used with [ComposedChart].
 * @param dataLabel an optional [TextComponent] to use for data labels.
 * @param dataLabelVerticalPosition the vertical position of data labels relative to the top of their
 * respective columns.
 * @param dataLabelValueFormatter the [ValueFormatter] to use for data labels.
 * @param dataLabelRotationDegrees the rotation of data labels (in degrees).
 * @param drawingModelInterpolator interpolates the [ColumnChart]’s [ColumnChartDrawingModel]s.
 * @param columnLineComponentOverrider allows overriding some parameters of the default [LineComponent] for a specific
 * [ChartEntryModel]
 */
public open class ColumnChart(
    public var columns: List<LineComponent>,
    public var spacingDp: Float = DefaultDimens.COLUMN_OUTSIDE_SPACING,
    public var innerSpacingDp: Float = DefaultDimens.COLUMN_INSIDE_SPACING,
    public var mergeMode: MergeMode = MergeMode.Grouped,
    public var targetVerticalAxisPosition: AxisPosition.Vertical? = null,
    public var dataLabel: TextComponent? = null,
    public var dataLabelVerticalPosition: VerticalPosition = VerticalPosition.Top,
    public var dataLabelValueFormatter: ValueFormatter = DecimalFormatValueFormatter(),
    public var dataLabelRotationDegrees: Float = 0f,
    public var drawingModelInterpolator: DrawingModelInterpolator<
        ColumnChartDrawingModel.ColumnInfo,
        ColumnChartDrawingModel,
        > = DefaultDrawingModelInterpolator(),
    public var columnLineComponentOverrider: ComponentOverrider? = null,
) : BaseChart<ChartEntryModel>() {

    /**
     * Creates a [ColumnChart] with a common style for all columns.
     *
     * @param column a [LineComponent] defining the appearance of the columns.
     * @param spacingDp the distance between neighboring column collections.
     * @param targetVerticalAxisPosition if this is set, any [AxisRenderer] with an [AxisPosition] equal to the provided
     * value will use the [ChartValues] provided by this chart. This is meant to be used with [ComposedChart].
     */
    public constructor(
        column: LineComponent,
        spacingDp: Float = DefaultDimens.COLUMN_OUTSIDE_SPACING,
        targetVerticalAxisPosition: AxisPosition.Vertical? = null,
    ) : this(columns = listOf(column), spacingDp = spacingDp, targetVerticalAxisPosition = targetVerticalAxisPosition)

    /**
     * Creates a [ColumnChart] instance with [columns] set to an empty list. The list must be populated before the chart
     * is drawn.
     */
    public constructor() : this(emptyList())

    /**
     * When [mergeMode] is set to [MergeMode.Stack], this maps the x-axis value of every column collection to a pair
     * containing the bottom coordinate of the collection’s bottommost column and the top coordinate of the collection’s
     * topmost column. This hash map is used by [drawChart] and [drawChartInternal].
     */
    protected val heightMap: HashMap<Float, Pair<Float, Float>> = HashMap()

    /**
     * Holds information on the [ColumnChart]’s horizontal dimensions.
     */
    protected val horizontalDimensions: MutableHorizontalDimensions = MutableHorizontalDimensions()

    protected val drawingModelKey: ExtraStore.Key<ColumnChartDrawingModel> = ExtraStore.Key()

    override val entryLocationMap: HashMap<Float, MutableList<Marker.EntryModel>> = HashMap()

    private val columnLineComponentOverridesMap: HashMap<String, LineComponent> = HashMap()
    private val unusedColumnLineComponentOverrides: MutableList<String> = mutableListOf()

    override fun drawChart(
        context: ChartDrawContext,
        model: ChartEntryModel,
    ): Unit = with(context) {
        entryLocationMap.clear()
        unusedColumnLineComponentOverrides.setAll(columnLineComponentOverridesMap.keys)
        drawChartInternal(
            chartValues = chartValuesProvider.getChartValues(axisPosition = targetVerticalAxisPosition),
            model = model,
            drawingModel = model.extraStore.getOrNull(drawingModelKey),
        )
        unusedColumnLineComponentOverrides.forEach(columnLineComponentOverridesMap::remove)
        unusedColumnLineComponentOverrides.clear()
        heightMap.clear()
    }

    protected open fun ChartDrawContext.drawChartInternal(
        chartValues: ChartValues,
        model: ChartEntryModel,
        drawingModel: ColumnChartDrawingModel?,
    ) {
        val yRange = (chartValues.maxY - chartValues.minY).takeIf { it != 0f } ?: return
        val heightMultiplier = contentBounds.height() / yRange

        var drawingStart: Float
        var height: Float
        var columnCenterX: Float
        var defaultColumn: LineComponent
        var columnTop: Float
        var columnBottom: Float
        val zeroLinePosition = contentBounds.bottom + chartValues.minY / yRange * contentBounds.height()

        model.entries.forEachIndexed { index, entryCollection ->

            defaultColumn = columns.getRepeating(index)
            drawingStart = getDrawingStart(index, model.entries.size) - horizontalScroll

            entryCollection.forEachInIndexed(chartValues.minX..chartValues.maxX) { entryIndex, entry, _ ->
                val column = getAppropriateColumnLineComponent(entry, defaultColumn)
                val columnInfo = drawingModel?.getOrNull(index)?.get(entry.x)
                height = (columnInfo?.height ?: (abs(entry.y) / chartValues.lengthY)) * contentBounds.height()
                val xSpacingMultiplier = (entry.x - chartValues.minX) / chartValues.xStep
                check(xSpacingMultiplier % 1f == 0f) { "Each entry’s x value must be a multiple of the x step." }
                columnCenterX = drawingStart +
                    (horizontalDimensions.xSpacing * xSpacingMultiplier + column.thicknessDp.half.pixels * zoom) *
                    layoutDirectionMultiplier

                when (mergeMode) {
                    MergeMode.Stack -> {
                        val (stackedNegHeight, stackedPosHeight) = heightMap.getOrElse(entry.x) { 0f to 0f }
                        columnBottom = zeroLinePosition +
                            if (entry.y < 0f) {
                                height + stackedNegHeight
                            } else {
                                -stackedPosHeight
                            }

                        columnTop = (columnBottom - height).coerceAtMost(columnBottom)
                        heightMap[entry.x] =
                            if (entry.y < 0f) {
                                stackedNegHeight + height to stackedPosHeight
                            } else {
                                stackedNegHeight to stackedPosHeight + height
                            }
                    }

                    MergeMode.Grouped -> {
                        columnBottom = zeroLinePosition + if (entry.y < 0f) height else 0f
                        columnTop = columnBottom - height
                    }
                }

                val columnSignificantY = if (entry.y < 0f) columnBottom else columnTop

                if (column.intersectsVertical(
                        context = this,
                        top = columnTop,
                        bottom = columnBottom,
                        centerX = columnCenterX,
                        boundingBox = contentBounds,
                        thicknessScale = zoom,
                    )
                ) {
                    updateMarkerLocationMap(entry, columnSignificantY, columnCenterX, column, entryIndex)
                    column.drawVertical(this, columnTop, columnBottom, columnCenterX, zoom, drawingModel?.opacity ?: 1f)
                }

                if (mergeMode == MergeMode.Grouped) {
                    drawDataLabel(
                        modelEntriesSize = model.entries.size,
                        columnThicknessDp = column.thicknessDp,
                        dataLabelValue = entry.y,
                        x = columnCenterX,
                        y = columnSignificantY,
                        isFirst = index == 0 && entry.x == chartValues.minX,
                        isLast = index == model.entries.lastIndex && entry.x == chartValues.maxX,
                    )
                } else if (index == model.entries.lastIndex) {
                    val yValues = heightMap[entry.x]
                    drawStackedDataLabel(
                        modelEntriesSize = model.entries.size,
                        columnThicknessDp = column.thicknessDp,
                        negativeY = yValues?.first,
                        positiveY = yValues?.second,
                        x = columnCenterX,
                        zeroLinePosition = zeroLinePosition,
                        heightMultiplier = heightMultiplier,
                        isFirst = entry.x == chartValues.minX,
                        isLast = entry.x == chartValues.maxX,
                    )
                }
            }
        }
    }

    protected open fun ChartDrawContext.drawStackedDataLabel(
        modelEntriesSize: Int,
        columnThicknessDp: Float,
        negativeY: Float?,
        positiveY: Float?,
        x: Float,
        zeroLinePosition: Float,
        heightMultiplier: Float,
        isFirst: Boolean,
        isLast: Boolean,
    ) {
        if (positiveY != null && positiveY > 0f) {
            val y = zeroLinePosition - positiveY * heightMultiplier
            drawDataLabel(modelEntriesSize, columnThicknessDp, positiveY, x, y, isFirst, isLast)
        }
        if (negativeY != null && negativeY < 0f) {
            val y = zeroLinePosition + abs(negativeY) * heightMultiplier
            drawDataLabel(modelEntriesSize, columnThicknessDp, negativeY, x, y, isFirst, isLast)
        }
    }

    protected open fun ChartDrawContext.drawDataLabel(
        modelEntriesSize: Int,
        columnThicknessDp: Float,
        dataLabelValue: Float,
        x: Float,
        y: Float,
        isFirst: Boolean,
        isLast: Boolean,
    ) {
        dataLabel?.let { textComponent ->

            val canUseXSpacing =
                mergeMode == MergeMode.Stack ||
                    mergeMode == MergeMode.Grouped && modelEntriesSize == 1
            var maxWidth = when {
                canUseXSpacing -> horizontalDimensions.xSpacing
                mergeMode == MergeMode.Grouped ->
                    (columnThicknessDp + minOf(spacingDp, innerSpacingDp).half).pixels * zoom

                else -> error(message = "Encountered an unexpected `MergeMode`.")
            }
            if (isFirst && horizontalLayout is HorizontalLayout.FullWidth) {
                maxWidth = maxWidth.coerceAtMost(horizontalDimensions.startPadding.doubled)
            }
            if (isLast && horizontalLayout is HorizontalLayout.FullWidth) {
                maxWidth = maxWidth.coerceAtMost(horizontalDimensions.endPadding.doubled)
            }
            val text = dataLabelValueFormatter.formatValue(
                value = dataLabelValue,
                chartValues = chartValuesProvider.getChartValues(axisPosition = targetVerticalAxisPosition),
            )
            val dataLabelWidth = textComponent.getWidth(
                context = this,
                text = text,
                rotationDegrees = dataLabelRotationDegrees,
            ).coerceAtMost(maximumValue = maxWidth)

            if (x - dataLabelWidth.half > contentBounds.right || x + dataLabelWidth.half < contentBounds.left) return

            val labelVerticalPosition =
                if (dataLabelValue < 0f) dataLabelVerticalPosition.negative() else dataLabelVerticalPosition

            val verticalPosition = labelVerticalPosition.inBounds(
                y = y,
                bounds = contentBounds,
                componentHeight = textComponent.getHeight(
                    context = this,
                    text = text,
                    width = maxWidth.toInt(),
                    rotationDegrees = dataLabelRotationDegrees,
                ),
            )
            textComponent.drawText(
                context = this,
                text = text,
                textX = x,
                textY = y,
                verticalPosition = verticalPosition,
                maxTextWidth = maxWidth.toInt(),
                rotationDegrees = dataLabelRotationDegrees,
            )
        }
    }

    private fun getAppropriateColumnLineComponent(
        chartEntry: ChartEntry,
        lineComponent: LineComponent,
    ): LineComponent {
        return columnLineComponentOverrider.overrideComponentForEntry(
            chartEntry = chartEntry,
            defaultComponent = lineComponent,
        ) { cacheKey: String, color: Int?, shader: DynamicShader?, strokeColor: Int? ->
            unusedColumnLineComponentOverrides.remove(cacheKey)
            columnLineComponentOverridesMap.getOrPut(key = cacheKey) {
                LineComponent(
                    color = color ?: lineComponent.color,
                    thicknessDp = lineComponent.thicknessDp,
                    shape = lineComponent.shape,
                    dynamicShader = shader ?: lineComponent.dynamicShader,
                    margins = lineComponent.margins,
                    strokeWidthDp = lineComponent.strokeWidthDp,
                    strokeColor = strokeColor ?: lineComponent.strokeColor,
                )
            }
        }
    }

    protected open fun updateMarkerLocationMap(
        entry: ChartEntry,
        columnTop: Float,
        columnCenterX: Float,
        column: LineComponent,
        index: Int,
    ) {
        if (columnCenterX > contentBounds.left - 1 && columnCenterX < contentBounds.right + 1) {
            entryLocationMap.put(
                x = columnCenterX,
                y = columnTop.coerceIn(contentBounds.top, contentBounds.bottom),
                entry = entry,
                color = column.color,
                index = index,
            )
        }
    }

    override fun updateChartValues(chartValuesManager: ChartValuesManager, model: ChartEntryModel, xStep: Float?) {
        @Suppress("DEPRECATION_ERROR")
        chartValuesManager.tryUpdate(
            minX = axisValuesOverrider?.getMinX(model) ?: minX ?: model.minX,
            maxX = axisValuesOverrider?.getMaxX(model) ?: maxX ?: model.maxX,
            minY = axisValuesOverrider?.getMinY(model) ?: minY ?: mergeMode.getMinY(model).coerceAtMost(0f),
            maxY = axisValuesOverrider?.getMaxY(model)
                ?: maxY
                ?: if (model.minY == 0f && model.maxY == 0f) 1f else mergeMode.getMaxY(model).coerceAtLeast(0f),
            xStep = xStep ?: model.xGcd,
            chartEntryModel = model,
            axisPosition = targetVerticalAxisPosition,
        )
    }

    override fun updateHorizontalDimensions(
        context: MeasureContext,
        horizontalDimensions: MutableHorizontalDimensions,
        model: ChartEntryModel,
    ) {
        with(context) {
            val columnCollectionWidth =
                getColumnCollectionWidth(if (model.entries.isNotEmpty()) model.entries.size else 1)
            val xSpacing = columnCollectionWidth + spacingDp.pixels
            when (val horizontalLayout = horizontalLayout) {
                is HorizontalLayout.Segmented -> {
                    horizontalDimensions.ensureValuesAtLeast(
                        xSpacing = xSpacing,
                        scalableStartPadding = xSpacing.half,
                        scalableEndPadding = xSpacing.half,
                    )
                }
                is HorizontalLayout.FullWidth -> {
                    horizontalDimensions.ensureValuesAtLeast(
                        xSpacing = xSpacing,
                        scalableStartPadding = columnCollectionWidth.half +
                            horizontalLayout.scalableStartPaddingDp.pixels,
                        scalableEndPadding = columnCollectionWidth.half + horizontalLayout.scalableEndPaddingDp.pixels,
                        unscalableStartPadding = horizontalLayout.unscalableStartPaddingDp.pixels,
                        unscalableEndPadding = horizontalLayout.unscalableEndPaddingDp.pixels,
                    )
                }
            }
        }
    }

    override val modelTransformerProvider: Chart.ModelTransformerProvider = object : Chart.ModelTransformerProvider {
        private val modelTransformer =
            ColumnChartModelTransformer(drawingModelKey, { targetVerticalAxisPosition }, { drawingModelInterpolator })

        override fun <T : ChartEntryModel> getModelTransformer(): Chart.ModelTransformer<T> = modelTransformer
    }

    protected open fun MeasureContext.getColumnCollectionWidth(
        entryCollectionSize: Int,
    ): Float = when (mergeMode) {
        MergeMode.Stack ->
            columns.take(entryCollectionSize).maxOf { it.thicknessDp.pixels }

        MergeMode.Grouped ->
            getCumulatedThickness(entryCollectionSize) + innerSpacingDp.pixels * (entryCollectionSize - 1)
    }

    protected open fun ChartDrawContext.getDrawingStart(entryCollectionIndex: Int, entryCollectionCount: Int): Float {
        val mergeModeComponent = when (mergeMode) {
            MergeMode.Grouped ->
                getCumulatedThickness(entryCollectionIndex) + innerSpacingDp.pixels * entryCollectionIndex

            MergeMode.Stack -> 0f
        }
        return contentBounds.getStart(isLtr) + (
            horizontalDimensions.startPadding +
                (mergeModeComponent - getColumnCollectionWidth(entryCollectionCount).half) * zoom
            ) * layoutDirectionMultiplier
    }

    protected open fun MeasureContext.getCumulatedThickness(count: Int): Float {
        var thickness = 0f
        for (i in 0..<count) {
            thickness += columns.getRepeating(i).thicknessDp * density
        }
        return thickness
    }

    /**
     * Defines how a [ColumnChart] should draw columns in column collections.
     */
    public enum class MergeMode {

        /**
         * Columns with the same x-axis values will be placed next to each other in groups.
         */
        Grouped,

        /**
         * Columns with the same x-axis values will be placed on top of each other.
         */
        Stack,

        ;

        /**
         * Returns the minimum y-axis value, taking into account the current [MergeMode].
         */
        public fun getMinY(model: ChartEntryModel): Float = when (this) {
            Grouped -> model.minY
            Stack -> model.stackedNegativeY
        }

        /**
         * Returns the maximum y-axis value, taking into account the current [MergeMode].
         */
        public fun getMaxY(model: ChartEntryModel): Float = when (this) {
            Grouped -> model.maxY
            Stack -> model.stackedPositiveY
        }
    }

    protected class ColumnChartModelTransformer(
        override val key: ExtraStore.Key<ColumnChartDrawingModel>,
        private val getTargetVerticalAxisPosition: () -> AxisPosition.Vertical?,
        private val getDrawingModelInterpolator: () -> DrawingModelInterpolator<
            ColumnChartDrawingModel.ColumnInfo,
            ColumnChartDrawingModel,
            >,
    ) : Chart.ModelTransformer<ChartEntryModel>() {

        override fun prepareForTransformation(
            oldModel: ChartEntryModel?,
            newModel: ChartEntryModel?,
            extraStore: MutableExtraStore,
            chartValuesProvider: ChartValuesProvider,
        ) {
            getDrawingModelInterpolator().setModels(
                extraStore.getOrNull(key),
                newModel?.toDrawingModel(chartValuesProvider.getChartValues(getTargetVerticalAxisPosition())),
            )
        }

        override suspend fun transform(extraStore: MutableExtraStore, fraction: Float) {
            getDrawingModelInterpolator()
                .transform(fraction)
                ?.let { extraStore[key] = it }
                ?: extraStore.remove(key)
        }

        private fun ChartEntryModel.toDrawingModel(chartValues: ChartValues): ColumnChartDrawingModel = entries
            .map { series ->
                series.associate { entry ->
                    entry.x to ColumnChartDrawingModel.ColumnInfo(abs(entry.y) / chartValues.lengthY)
                }
            }
            .let(::ColumnChartDrawingModel)
    }
}
