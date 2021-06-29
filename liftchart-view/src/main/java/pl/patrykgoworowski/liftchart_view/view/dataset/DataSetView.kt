package pl.patrykgoworowski.liftchart_view.view.dataset

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import pl.patrykgoworowski.liftchart_common.axis.*
import pl.patrykgoworowski.liftchart_common.constants.DEF_CHART_WIDTH
import pl.patrykgoworowski.liftchart_common.extension.dpInt
import pl.patrykgoworowski.liftchart_common.extension.set
import pl.patrykgoworowski.liftchart_common.marker.Marker
import pl.patrykgoworowski.liftchart_common.motion_event.ChartMotionEventHandler
import pl.patrykgoworowski.liftchart_view.common.UpdateRequestListener
import pl.patrykgoworowski.liftchart_view.data_set.DataSetRendererWithModel
import pl.patrykgoworowski.liftchart_view.data_set.layout.ViewVirtualLayout
import pl.patrykgoworowski.liftchart_view.extension.*
import java.util.*
import kotlin.properties.Delegates.observable

class DataSetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val contentBounds = RectF()

    private val updateRequestListener: UpdateRequestListener = {
        if (ViewCompat.isAttachedToWindow(this@DataSetView)) {
            updateBounds()
            invalidate()
        }
    }

    private val virtualLayout = ViewVirtualLayout(isLTR)
    private val motionEventHandler = ChartMotionEventHandler(this::handleEvent)

    private var touchPoint: PointF? = null

    var dataSet: DataSetRendererWithModel<*>? by observable(null) { _, oldValue, newValue ->
        oldValue?.removeListener(updateRequestListener)
        newValue?.addListener(updateRequestListener)
        updateBounds()
    }

    var axisManager = AxisManager()
    var marker: Marker? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (marker != null && motionEventHandler.handleTouchPoint(event)) true
        else super.onTouchEvent(event)
    }

    private fun handleEvent(pointF: PointF?) {
        touchPoint = pointF
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val dataSet = dataSet ?: return
        val segmentProperties = dataSet.getSegmentProperties()
        axisManager.draw(canvas, dataSet.getEntriesModel(), segmentProperties)
        dataSet.draw(canvas, touchPoint, marker)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureDimension(widthMeasureSpec.specSize, widthMeasureSpec)

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> DEF_CHART_WIDTH.dpInt + verticalPadding
            MeasureSpec.AT_MOST -> minOf(
                DEF_CHART_WIDTH.dpInt + verticalPadding,
                heightMeasureSpec.specSize
            )
            else -> measureDimension(heightMeasureSpec.specSize, heightMeasureSpec)
        }
        setMeasuredDimension(width, height)

        contentBounds.set(
            paddingLeft,
            paddingTop,
            width - paddingRight,
            height - paddingBottom
        )
        updateBounds()
    }

    private fun updateBounds() {
        val dataSet = dataSet ?: return
        virtualLayout.setBounds(contentBounds, dataSet, axisManager, marker)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        val isLTR = layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR
        virtualLayout.isLTR = isLTR
    }
}