package dev.hew7.launcher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

enum class SourceKind { TUNER, HDMI, AV, COMPONENT, OTHER }

data class TvSource(
    val id: String,
    val label: String,
    val kind: SourceKind,
    /** A device is plugged in and switched on (HDMI-CEC / signal detection, where the TV reports it). */
    val active: Boolean,
)

/**
 * The TV's physical inputs (aerial tuner, HDMI, AV...), switched through the TV's own live-TV app.
 * Inputs that apps register (Google Play Movies previews, IPTV services) are left out: the
 * framework marks every hardware connector's input id with "/HW".
 */
class InputRepository(private val context: Context) {

    fun list(): List<TvSource> {
        val tim = context.getSystemService(TvInputManager::class.java) ?: return emptyList()
        return tim.tvInputList
            .filter { "/HW" in it.id && !it.isHidden(context) }
            .map { info ->
                val label = info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                    ?: info.loadLabel(context).toString()
                TvSource(
                    id = info.id,
                    label = label,
                    kind = kindOf(info),
                    active = runCatching { tim.getInputState(info.id) == TvInputManager.INPUT_STATE_CONNECTED }.getOrDefault(false),
                )
            }
            .sortedWith(compareBy({ it.kind.ordinal }, { it.label }))
    }

    /** Returns false if no app on the TV handled the switch. */
    fun switchTo(source: TvSource): Boolean {
        val uri = if (source.kind == SourceKind.TUNER) {
            TvContract.buildChannelsUriForInput(source.id)
        } else {
            TvContract.buildChannelUriForPassthroughInput(source.id)
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun kindOf(info: TvInputInfo) = when (info.type) {
        TvInputInfo.TYPE_TUNER -> SourceKind.TUNER
        TvInputInfo.TYPE_HDMI, TvInputInfo.TYPE_DVI, TvInputInfo.TYPE_DISPLAY_PORT -> SourceKind.HDMI
        TvInputInfo.TYPE_COMPOSITE, TvInputInfo.TYPE_SVIDEO, TvInputInfo.TYPE_SCART -> SourceKind.AV
        TvInputInfo.TYPE_COMPONENT, TvInputInfo.TYPE_VGA -> SourceKind.COMPONENT
        else -> SourceKind.OTHER
    }
}
