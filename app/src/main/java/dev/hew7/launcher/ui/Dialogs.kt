package dev.hew7.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.hew7.launcher.data.AppEntry
import kotlinx.coroutines.delay

private val DialogShape = RoundedCornerShape(22.dp)
private val ItemShape = RoundedCornerShape(11.dp)

@Composable
private fun LauncherDialog(title: String, onDismiss: () -> Unit, content: @Composable (FocusRequester) -> Unit) {
    val c = LocalLauncherColors.current
    val first = remember { FocusRequester() }
    // The app menu opens on a long press while OK is still held. Ignore every key event until a
    // fresh press (not a held-down repeat) arrives, so letting go doesn't select "App info".
    var armed by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .onPreviewKeyEvent { e ->
                    if (!armed && e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) armed = true
                    !armed
                }
                .width(320.dp)
                .background(c.glassStrong, DialogShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProvideTextStyle(LocalTextStyle.current.copy(shadow = null)) {
                Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                content(first)
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { first.requestFocus() }
    }
}

@Composable
internal fun MenuItem(
    icon: ImageVector?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val c = LocalLauncherColors.current
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(44.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(ItemShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = c.text,
            focusedContainerColor = c.focusFill,
            focusedContentColor = c.onFocusFill,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            FittedText(
                text,
                focused,
                fontSize = 14.sp,
                style = textStyle,
                modifier = Modifier.padding(start = if (icon != null) 12.dp else 0.dp).weight(1f),
            )
            if (value != null) Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun AppMenuDialog(
    app: AppEntry,
    onInfo: () -> Unit,
    onMove: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    LauncherDialog(app.label, onDismiss) { first ->
        MenuItem(Icons.Rounded.Info, "App Info", onInfo, Modifier.focusRequester(first))
        MenuItem(Icons.Rounded.SwapHoriz, "Move", onMove)
        if (!app.isSystem) MenuItem(Icons.Rounded.Delete, "Uninstall", onUninstall)
    }
}

