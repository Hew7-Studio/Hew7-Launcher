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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import kotlin.math.abs
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.material.icons.rounded.Shop
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
import androidx.compose.ui.draw.clip
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

/** Apps in view at once; the rest peek in at the edge. */
private const val PerSet = 7

/** The spot (0-based) where the selection stays while the row glides past: the 4th app. */
private const val HoldAt = 3

/** Turns off the row scrolling by itself when focus moves; the dock glides by its own rule. */
@OptIn(ExperimentalFoundationApi::class)
private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) = 0f
}

/** A request to select an app in the dock; [id] makes repeated requests for the same app count. */
data class DockFocus(val pkg: String, val id: Int)

private val TileShape = RoundedCornerShape(13.dp)
private val IconShape = RoundedCornerShape(26)
private val DockShape = RoundedCornerShape(26.dp)

/**
 * The app dock. Left on the first app reveals a hidden Play Store tile, Right on the last app
 * wraps round to the first, and long press opens the app menu.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
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

    val storeSlot = if (playStore != null) 1 else 0
    val focusedIndex = apps.indexOfFirst { it.pkg == focusedPkg }
    // [PerSet] apps are in view. The highlight moves from the 1st app to the 4th ([HoldAt]); from
    // there it stays put and the apps glide past it, until the last 7 are in view, when it moves
    // on from the 4th to the 7th (and the same on the way back).
    val sets = ((apps.size + PerSet - 1) / PerSet).coerceAtLeast(1)
    val set = if (apps.size <= 1 || focusedIndex < 0) 0 else (focusedIndex * (sets - 1) + (apps.size - 1) / 2) / (apps.size - 1)
    fun firstShown(i: Int) = (i - HoldAt).coerceIn(0, (apps.size - PerSet).coerceAtLeast(0))

    // Home brings the dock back to its start (without selecting anything), past the hidden Play
    // Store slot so the first app sits exactly where moving will keep it.
    LaunchedEffect(homePresses) {
        storeRevealed = false
        listState.scrollToItem(storeSlot)
    }

    /** Glides the row so list item [index] is the first one in view. */
    suspend fun glideTo(index: Int) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (item != null) {
            if (item.offset != 0) listState.animateScrollBy(item.offset.toFloat(), spring(dampingRatio = 1f, stiffness = 300f))
        } else {
            listState.animateScrollToItem(index)
        }
    }

    // Glide the row under the highlight once it has reached the 4th spot.
    LaunchedEffect(focusedPkg, apps) {
        val pkg = focusedPkg ?: return@LaunchedEffect
        when {
            pkg == playStore?.pkg -> glideTo(0)
            focusedIndex >= 0 -> glideTo(firstShown(focusedIndex) + storeSlot)
        }
    }

    // When the Play Store tile folds away, the row's glide would stop short by whatever was still
    // folding, so wait for it to finish, then line the row up exactly.
    LaunchedEffect(storeRevealed) {
        if (storeRevealed || playStore == null) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }?.size ?: 0 }.first { it == 0 }
        val i = apps.indexOfFirst { it.pkg == focusedPkg }
        glideTo((if (i >= 0) firstShown(i) else 0) + storeSlot)
    }

    // Select a requested app. The tile may not be laid out yet on the first frame, so keep trying
    // briefly until it actually has focus.
    LaunchedEffect(focusRequest, apps.isNotEmpty()) {
        val req = focusRequest ?: return@LaunchedEffect
        val index = apps.indexOfFirst { it.pkg == req.pkg }
        if (index < 0) return@LaunchedEffect
        storeRevealed = false
        // An app already on screen is selected where it is and the row glides it into place;
        // one further along is brought into place straight away.
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index + storeSlot }) listState.scrollToItem(firstShown(index) + storeSlot)
        repeat(30) {
            runCatching { tileFocus[req.pkg]?.requestFocus() }
            if (focusedPkg == req.pkg) return@LaunchedEffect
            delay(50)
        }
    }

    fun goToFirst() = scope.launch {
        val first = apps.firstOrNull() ?: return@launch
        storeRevealed = false
        // From far along, bring the start back first (the first app's tile must be on screen to select it).
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == storeSlot }) listState.scrollToItem(storeSlot)
        focusTile(first.pkg)
    }

    BackHandler(enabled = movingPkg == null && storeRevealed) {
        storeRevealed = false
        focusTile(apps.firstOrNull()?.pkg)
    }

    // Hew7 2.0: the tiles float over the wallpaper; a glass bar only if Dock transparency asks for one.
    val dockColor = if (c.dark) Color.Black.copy(alpha = dockAlpha) else Color.White.copy(alpha = dockAlpha)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // A window of [PerSet] apps in the middle; the next and previous sets peek in at the edges,
    // fading out, so it's clear there's more.
    val tileW = if (tileStyle == TileStyle.BANNERS) 107.dp else 76.dp
    val gap = if (tileStyle == TileStyle.ICONS) 14.dp else 12.dp
    val window = tileW * PerSet + gap * (PerSet - 1)
    val side = ((maxWidth - window) / 2).coerceAtLeast(24.dp)
    // One app's slot along the row, in pixels.
    val stepPx = with(LocalDensity.current) { (tileW + gap).toPx() }
    // With [PerSet] apps or fewer there's nothing to scroll: they sit centred in the window and the
    // highlight just moves from app to app.
    val centring = if (apps.size < PerSet) (tileW + gap) * ((PerSet - apps.size) / 2f) else 0.dp
    Column(Modifier.fillMaxWidth()) {
        // Android TV scrolls a row by itself when focus moves (to keep the selected item about a
        // third of the way in). That fought the dock's own glide, pulling the row sideways and
        // skipping apps, so the dock turns it off and moves only by its own rule (see glideTo).
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(start = side + centring, end = side + centring, top = 30.dp, bottom = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (dockAlpha > 0f) Modifier.background(dockColor, DockShape) else Modifier)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Fade the peeking tiles out towards the screen edges.
                    val fade = side.toPx()
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            (fade / size.width) to Color.Black,
                            (1f - fade / size.width) to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
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
                val selected = index == focusedIndex
                // The selected app is held on screen at its spot (the 4th, once there) while the
                // row glides underneath it, so the highlight itself never travels sideways.
                Box(
                    Modifier
                        .zIndex(if (selected) 1f else 0f)
                        .graphicsLayer {
                            if (!selected) return@graphicsLayer
                            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index + storeSlot }
                            translationX = if (item == null) 0f else (index - firstShown(index)) * stepPx - item.offset
                        },
                ) {
                AppTile(
                    app = app,
                    label = app.label,
                    style = tileStyle,
                    moving = moving,
                    showName = showNames,
                    // Apps beside the selected one turn slightly towards it.
                    lean = if (focusedIndex >= 0 && index != focusedIndex) (index - focusedIndex).coerceIn(-1, 1).toFloat() else 0f,
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
                                    // It opens just left of the first app, and the row glides to show it.
                                    scope.launch {
                                        delay(40)
                                        runCatching { storeFocus.requestFocus() }
                                    }
                                    true
                                }
                                // The last app is the end of the row: Right stays on it.
                                e.key == Key.DirectionRight && index == apps.lastIndex -> true
                                else -> false
                            }
                        },
                )
                }
            }
        }
        }

        if (movingPkg != null) {
            Text(
                "Use ◀ ▶ to move, OK to finish",
                color = c.subtext,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            )
        } else if (sets > 1 || playStore != null) {
            // Where you are along the dock: a small Play Store mark first (its tile hides to the
            // left of the first app), then one dot per set of apps.
            val onStore = focusedPkg != null && focusedPkg == playStore?.pkg
            Row(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).height(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (playStore != null) {
                    val a by animateFloatAsState(if (onStore) 1f else 0.4f, tween(250), label = "storeMark")
                    Icon(
                        Icons.Rounded.Shop,
                        contentDescription = "Play Store, to the left of the first app",
                        tint = c.text.copy(alpha = a),
                        modifier = Modifier.size(if (onStore) 12.dp else 10.dp).padding(end = 2.dp),
                    )
                }
                if (sets > 1) repeat(sets) { i ->
                    val on = !onStore && i == set
                    val w by animateDpAsState(if (on) 18.dp else 6.dp, spring(dampingRatio = 0.8f, stiffness = 400f), label = "dot")
                    Box(
                        Modifier
                            .width(w)
                            .height(6.dp)
                            .background(c.text.copy(alpha = if (on) 0.9f else 0.35f), RoundedCornerShape(50)),
                    )
                }
            }
        }
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
    /** -1 or 1 while another app is selected (which side of it this one is), else 0. */
    lean: Float = 0f,
) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    // Soft, settled springs (barely any bounce), so moving along the dock flows instead of jolting.
    val spring = spring<Float>(dampingRatio = 0.82f, stiffness = 240f)
    // 0 at rest, 1 fully popped out.
    val pop by animateFloatAsState(if (focused) 1f else 0f, spring, label = "pop")
    val turn by animateFloatAsState(lean, spring(dampingRatio = 0.9f, stiffness = 220f), label = "lean")
    val sheen = remember { Animatable(-1f) }
    LaunchedEffect(focused) {
        if (focused) {
            sheen.snapTo(-0.5f)
            sheen.animateTo(1.5f, tween(700, delayMillis = 120, easing = FastOutSlowInEasing))
        } else {
            sheen.snapTo(-1f)
        }
    }
    val bannerStyle = style == TileStyle.BANNERS
    val banner = bannerStyle && app.banner != null
    val shape = if (bannerStyle) TileShape else IconShape
    // Includes the 4dp gap around the tile.
    val size = if (bannerStyle) DpSize(107.dp, 65.dp) else DpSize(76.dp, 76.dp)
    val ringShape = if (bannerStyle) RoundedCornerShape(16.dp) else RoundedCornerShape(30)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size.width)) {
      Box(Modifier.size(size)) {
        // Soft shadow beneath the lifted tile, drawn once into its own layer; the animation only
        // fades and stretches that layer, so nothing is redrawn while the tile pops. (Drawn by
        // hand: the TV's GPU renders elevation shadows as a pale bar.)
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = pop.coerceIn(0f, 1f)
                    alpha = p
                    scaleX = 0.85f + 0.3f * p
                    translationY = 4.dp.toPx() * p
                }
                .drawWithCache {
                    val w = size.width.toPx() * 0.95f
                    val h = 18.dp.toPx()
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
                    // The selected app zooms up towards you and lifts; the others step back a
                    // little and turn slightly towards it, like cards on a shelf.
                    val p = pop.coerceIn(0f, 1.2f)
                    val back = abs(turn)
                    val s = 1f + 0.3f * pop - 0.06f * back
                    scaleX = s
                    scaleY = s
                    translationY = -12.dp.toPx() * pop
                    // Tips back a touch as it rises, so it reads as coming towards you.
                    rotationX = -7f * pop
                    rotationY = -16f * turn
                    cameraDistance = 9f * density
                    alpha = 0.82f + 0.18f * p.coerceAtMost(1f) - 0.06f * back
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
            } else if (bannerStyle) {
                // No banner: the icon in the middle of a white tile.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(app.icon, contentDescription = app.label, contentScale = ContentScale.Fit, modifier = Modifier.size(40.dp).clip(IconShape))
                }
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
        // Every app's name (App names setting), or only the selected app's, as in the design.
        Text(
            label,
            color = c.text,
            fontSize = if (showName) 12.sp else 11.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .requiredWidth(size.width + 24.dp) // a little overhang so most names fit
                .padding(top = 8.dp)
                .alpha(if (showName || focused) 1f else 0f),
        )
    }
}
