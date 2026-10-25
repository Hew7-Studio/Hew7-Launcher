package dev.hew7.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.hew7.launcher.data.AppEntry
import dev.hew7.launcher.data.TileStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A request to select an app in the dock; [id] makes repeated requests for the same app count. */
data class DockFocus(val pkg: String, val id: Int)

private val TileShape = RoundedCornerShape(14.dp)
private val IconShape = RoundedCornerShape(26)
private val DockShape = RoundedCornerShape(26.dp)

/**
 * The app dock. Left on the first app reveals a hidden Play Store tile, Right on the last app
 * wraps round to the first, and long press opens the app menu.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppSection(
    apps: List<AppEntry>,
    playStore: AppEntry?,
    tileStyle: TileStyle,
    dockAlpha: Float,
    movingPkg: String?,
    homePresses: Int,
    /** Select this app (Down from rest selects the first app; Back out of an app selects that app). */
    focusRequest: DockFocus?,
    showNames: Boolean,
    onLaunch: (String) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onMove: (String, Int) -> Unit,
    onMoveDone: () -> Unit,
    /** The selected app, or null while nothing in the dock is selected. */
    onSelected: (String?) -> Unit = {},
) {
    val c = LocalLauncherColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val tileFocus = remember { mutableMapOf<String, FocusRequester>() }
    val storeFocus = remember { FocusRequester() }
    var focusedPkg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusedPkg) { onSelected(focusedPkg) }
    var storeRevealed by remember { mutableStateOf(false) }
    var storeHadFocus by remember { mutableStateOf(false) }

    fun focusTile(pkg: String?) = scope.launch {
        delay(40)
        runCatching { (pkg?.let { tileFocus[it] } ?: return@launch).requestFocus() }
    }

    // Home brings the dock back to its start (without selecting anything).
    LaunchedEffect(homePresses) {
        storeRevealed = false
        listState.scrollToItem(0)
    }

    // Select a requested app. The tile may not be laid out yet on the first frame, so keep trying
    // briefly until it actually has focus.
    LaunchedEffect(focusRequest, apps.isNotEmpty()) {
        val req = focusRequest ?: return@LaunchedEffect
        val index = apps.indexOfFirst { it.pkg == req.pkg }
        if (index < 0) return@LaunchedEffect
        storeRevealed = false
        listState.scrollToItem(index + if (playStore != null) 1 else 0)
        repeat(30) {
            runCatching { tileFocus[req.pkg]?.requestFocus() }
            if (focusedPkg == req.pkg) return@LaunchedEffect
            delay(50)
        }
    }

    fun goToFirst() = scope.launch {
        val first = apps.firstOrNull() ?: return@launch
        storeRevealed = false
        listState.scrollToItem(0)
        focusTile(first.pkg)
    }

    BackHandler(enabled = movingPkg == null && storeRevealed) {
        storeRevealed = false
        focusTile(apps.firstOrNull()?.pkg)
    }

    val dockColor = if (c.dark) Color.Black.copy(alpha = dockAlpha) else Color.White.copy(alpha = dockAlpha)

    Column(Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(if (tileStyle == TileStyle.ICONS) 14.dp else 12.dp),
            // Without names the icons sit centred in the dock.
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (showNames) 6.dp else 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(dockColor, DockShape)
                // The name under an app only shows while you're in the dock.
                .onFocusChanged { if (!it.hasFocus) focusedPkg = null }
                .focusRestorer(),
        ) {
            if (playStore != null) {
                item(key = "__store") {
                    AnimatedVisibility(
                        visible = storeRevealed,
                        // Slide only, with no clipping and no fade: the tile's pop-out (scale and
                        // lift) reaches above the slot, and both clipping and a fade (which draws
                        // into a layer the size of the slot) would cut its top off for a moment.
                        enter = expandHorizontally(clip = false),
                        exit = shrinkHorizontally(clip = false),
                    ) {
                        AppTile(
                            app = playStore,
                            label = "Play Store",
                            style = tileStyle,
                            moving = false,
                            showName = showNames,
                            onClick = { onLaunch(playStore.pkg) },
                            onLongClick = null,
                            modifier = Modifier
                                .focusRequester(storeFocus)
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) {
                                        goToFirst()
                                        true
                                    } else false
                                }
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        storeHadFocus = true
                                        focusedPkg = playStore.pkg
                                    } else if (storeHadFocus) {
                                        storeHadFocus = false
                                        storeRevealed = false
                                    }
                                },
                        )
                    }
                }
            }
            itemsIndexed(apps, key = { _, app -> app.pkg }) { index, app ->
                val moving = app.pkg == movingPkg
                AppTile(
                    app = app,
                    label = app.label,
                    style = tileStyle,
                    moving = moving,
                    showName = showNames,
                    onClick = { if (movingPkg != null) onMoveDone() else onLaunch(app.pkg) },
                    onLongClick = { if (movingPkg == null) onLongPress(app) },
                    modifier = Modifier
                        .focusRequester(tileFocus.getOrPut(app.pkg) { FocusRequester() })
                        .onFocusChanged {
                            if (it.isFocused) focusedPkg = app.pkg
                            else if (focusedPkg == app.pkg) focusedPkg = null
                        }
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when {
                                moving -> when (e.key) {
                                    Key.DirectionLeft -> { onMove(app.pkg, -1); focusTile(app.pkg); true }
                                    Key.DirectionRight -> { onMove(app.pkg, 1); focusTile(app.pkg); true }
                                    Key.DirectionUp, Key.DirectionDown -> true
                                    else -> false
                                }
                                movingPkg != null -> false
                                // Down jumps back to the first app (nothing happens if already there).
                                e.key == Key.DirectionDown -> {
                                    if (index != 0) goToFirst()
                                    true
                                }
                                e.key == Key.DirectionLeft && index == 0 && playStore != null && !storeRevealed -> {
                                    storeRevealed = true
                                    scope.launch {
                                        delay(40)
                                        listState.scrollToItem(0)
                                        runCatching { storeFocus.requestFocus() }
                                    }
                                    true
                                }
                                // Right on the last app wraps round to the first.
                                e.key == Key.DirectionRight && index == apps.lastIndex -> {
                                    scope.launch {
                                        listState.scrollToItem(0)
                                        focusTile(apps.first().pkg)
                                    }
                                    true
                                }
                                else -> false
                            }
                        },
                )
            }
        }

        if (movingPkg != null) {
            Text(
                "Use ◀ ▶ to move, OK to finish",
                color = c.subtext,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

/**
 * One app in the dock, with its name underneath while selected. The selected app pops out of the
 * dock in 3D: it tips up towards you and settles, lifts with a real shadow and a soft contact
 * shadow on the dock beneath it, and a sheen of light sweeps across it once. While moving an app,
 * a blue ring marks it instead.
 */
@Composable
private fun AppTile(
    app: AppEntry,
    label: String,
    style: TileStyle,
    moving: Boolean,
    showName: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    val spring = spring<Float>(dampingRatio = 0.55f, stiffness = 380f)
    // 0 at rest, 1 fully popped out.
    val pop by animateFloatAsState(if (focused) 1f else 0f, spring, label = "pop")
    val tilt = remember { Animatable(0f) }
    val sheen = remember { Animatable(-1f) }
    LaunchedEffect(focused) {
        if (focused) {
            launch {
                tilt.snapTo(18f)
                tilt.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 260f))
            }
            sheen.snapTo(-0.5f)
            sheen.animateTo(1.5f, tween(650, delayMillis = 90, easing = FastOutSlowInEasing))
        } else {
            tilt.snapTo(0f)
            sheen.snapTo(-1f)
        }
    }
    val banner = style == TileStyle.BANNERS && app.banner != null
    val shape = if (banner) TileShape else IconShape
    // Includes the 4dp gap around the tile.
    val size = if (style == TileStyle.BANNERS) DpSize(152.dp, 88.dp) else DpSize(76.dp, 76.dp)
    val ringShape = if (banner) RoundedCornerShape(18.dp) else RoundedCornerShape(30)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size.width)) {
      Box(Modifier.size(size)) {
        // Soft contact shadow on the dock, drawn once into its own layer; the animation only
        // fades and stretches that layer, so nothing is redrawn while the tile pops. (Drawn by
        // hand: the TV's GPU renders elevation shadows as a pale bar.)
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = pop.coerceIn(0f, 1f)
                    alpha = p
                    scaleX = 0.8f + 0.2f * p
                }
                .drawWithCache {
                    val w = size.width.toPx() * 0.9f
                    val h = 14.dp.toPx()
                    val center = Offset(this.size.width / 2f, this.size.height + 1.dp.toPx())
                    val brush = Brush.radialGradient(
                        0f to Color.Black.copy(alpha = 0.5f),
                        0.55f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Transparent,
                        center = center,
                        radius = w / 2f,
                    )
                    onDrawBehind {
                        // A round gradient squashed flat, so the edge fades out on every side.
                        scale(scaleX = 1f, scaleY = h / w, pivot = center) {
                            drawCircle(brush, radius = w / 2f, center = center)
                        }
                    }
                },
        )
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
                .size(size)
                .onFocusChanged { focused = it.isFocused }
                .graphicsLayer {
                    val s = 1f + 0.2f * pop
                    scaleX = s
                    scaleY = s
                    translationY = -14.dp.toPx() * pop
                    rotationX = tilt.value
                    cameraDistance = 10f * density
                }
                .then(if (moving) Modifier.border(2.5.dp, Color(0xFF8AB4F8), ringShape) else Modifier)
                .padding(4.dp),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White,
                contentColor = Color(0xFF15161A),
                focusedContainerColor = Color.White,
                focusedContentColor = Color(0xFF15161A),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            glow = ClickableSurfaceDefaults.glow(),
        ) {
            if (banner) {
                Image(app.banner!!, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Image(app.icon, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // The sheen: a band of light crossing the icon diagonally. Drawn once; the animation
            // only slides its layer across, so no redrawing per frame.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val v = sheen.value
                        alpha = if (v > -0.5f && v < 1.5f) 1f else 0f
                        translationX = (v - 0.5f) * this.size.width
                    }
                    .drawWithCache {
                        val band = this.size.width * 0.45f
                        val x = this.size.width * 0.5f
                        val brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            start = Offset(x - band, 0f),
                            end = Offset(x, this.size.height),
                        )
                        onDrawBehind { drawRect(brush) }
                    },
            )
            if (moving) {
                Row(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
      }
        // Names are shown for every app or for none (App names setting).
        if (showName) {
            Text(
                label,
                color = c.text,
                fontSize = 12.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .requiredWidth(size.width + 12.dp) // a little overhang so most names fit
                    .padding(top = 6.dp),
            )
        }
    }
}
