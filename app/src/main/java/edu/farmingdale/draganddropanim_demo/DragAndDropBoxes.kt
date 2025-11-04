@file:OptIn(ExperimentalFoundationApi::class)

package edu.farmingdale.draganddropanim_demo

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DragAndDropBoxes(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Long-press the rectangle to drag. Drop on TOP to spin, BOTTOM to bounce.",
                fontWeight = FontWeight.SemiBold
            )
        }

        // Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = LocalDensity.current

            var canvasWpx by remember { mutableStateOf(0f) }
            var canvasHpx by remember { mutableStateOf(0f) }

            val rectSizeDp: Dp = 120.dp
            val rectSizePx = with(density) { rectSizeDp.toPx() }

            // State in px
            val x = remember { Animatable(0f) }
            val y = remember { Animatable(0f) }
            val rotation = remember { Animatable(0f) }
            var color by remember { mutableStateOf(Color(0xFF4CAF50)) }
            var initialized by remember { mutableStateOf(false) }

            // Measure and center once
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        canvasWpx = coords.size.width.toFloat()
                        canvasHpx = coords.size.height.toFloat()
                        if (!initialized && canvasWpx > 0f && canvasHpx > 0f) {
                            val cx = (canvasWpx - rectSizePx) / 2f
                            val cy = (canvasHpx - rectSizePx) / 2f
                            scope.launch {
                                x.snapTo(cx)
                                y.snapTo(cy)
                            }
                            initialized = true
                        }
                    }
            )

            // TOP row: UP behavior
            TargetsRowPx(
                count = 4,
                label = "UP",
                rowHeight = 64.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) { colIndex ->
                if (initialized) {
                    val tx = columnCenterPx(colIndex, canvasWpx, rectSizePx)
                    val ty = (canvasHpx * 0.15f) - rectSizePx / 2f
                    color = Color(0xFF2196F3) // blue
                    scope.launch {
                        x.animateTo(tx, spring(stiffness = Spring.StiffnessMedium))
                        y.animateTo(ty, spring(stiffness = Spring.StiffnessMedium))
                        rotation.animateTo(rotation.value + 360f, spring(stiffness = Spring.StiffnessLow))
                    }
                }
            }

            // BOTTOM row: DOWN behavior
            TargetsRowPx(
                count = 4,
                label = "DOWN",
                rowHeight = 64.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) { colIndex ->
                if (initialized) {
                    val tx = columnCenterPx(colIndex, canvasWpx, rectSizePx)
                    val ty = (canvasHpx * 0.45f) - rectSizePx / 2f
                    color = Color(0xFFFF9800) // orange
                    scope.launch {
                        x.animateTo(tx, spring(stiffness = Spring.StiffnessMedium))
                        y.animateTo(ty, spring(stiffness = Spring.StiffnessMedium))
                        // bounce
                        val up = ty - 40f
                        y.animateTo(up, spring(stiffness = Spring.StiffnessMedium))
                        y.animateTo(ty, spring(stiffness = Spring.StiffnessMedium))
                    }
                }
            }

            // Draggable rectangle
            Box(
                modifier = Modifier
                    .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                    .size(rectSizeDp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .graphicsLayer(
                        rotationZ = rotation.value,
                        transformOrigin = TransformOrigin.Center
                    )
                    .dragAndDropSource {
                        // Long-press to begin drag
                        detectTapGestures(
                            onLongPress = {
                                startTransfer(
                                    transferData = DragAndDropTransferData(
                                        clipData = ClipData.newPlainText("shape", "rect")
                                    )
                                )
                            }
                        )
                    }
            )

            // Reset button
            Button(
                onClick = {
                    if (!initialized) return@Button
                    color = Color(0xFF4CAF50)
                    val cx = (canvasWpx - rectSizePx) / 2f
                    val cy = (canvasHpx - rectSizePx) / 2f
                    scope.launch {
                        x.animateTo(cx, spring())
                        y.animateTo(cy, spring())
                        rotation.animateTo(0f, spring())
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text("Reset to Center")
            }
        }
    }
}

/* Centers the rectangle within a 4-column grid and returns the left X (px). */
private fun columnCenterPx(colIndex: Int, canvasWpx: Float, rectSizePx: Float): Float {
    val colWidth = canvasWpx / 4f
    return colWidth * (colIndex + 0.5f) - rectSizePx / 2f
}

/* A reusable row of drop targets placed by the parent Box via modifier.align(...) */
@Composable
private fun TargetsRowPx(
    count: Int,
    label: String,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    onDropped: (index: Int) -> Unit
) {
    var active by remember { mutableStateOf(-1) }
    Row(
        modifier = modifier
            .height(rowHeight)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        },
                        target = remember {
                            object : DragAndDropTarget {
                                override fun onDrop(event: DragAndDropEvent): Boolean {
                                    active = index
                                    onDropped(index)
                                    return true
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (active == index) {
                    Text(text = label, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}