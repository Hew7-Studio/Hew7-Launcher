package dev.hew7.launcher.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.hew7.launcher.R

/** Fonts bundled with the launcher (all SIL Open Font License, from Google Fonts). */
enum class LauncherFont(val label: String) {
    SYSTEM("System default"),
    NUNITO("Nunito"),
    INTER("Inter"),
    POPPINS("Poppins"),
    OUTFIT("Outfit"),
    MANROPE("Manrope"),
    QUICKSAND("Quicksand"),
    LEXEND("Lexend"),
    DM_SANS("DM Sans");

    val family: FontFamily by lazy {
        when (this) {
            SYSTEM -> FontFamily.Default
            NUNITO -> variable(R.font.nunito)
            INTER -> variable(R.font.inter)
            OUTFIT -> variable(R.font.outfit)
            MANROPE -> variable(R.font.manrope)
            QUICKSAND -> variable(R.font.quicksand)
            LEXEND -> variable(R.font.lexend)
            DM_SANS -> variable(R.font.dm_sans)
            POPPINS -> FontFamily(
                Font(R.font.poppins_light, FontWeight.Light),
                Font(R.font.poppins_regular, FontWeight.Normal),
                Font(R.font.poppins_semibold, FontWeight.SemiBold),
                Font(R.font.poppins_bold, FontWeight.Bold),
            )
        }
    }

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: NUNITO
    }
}

/** One variable-weight font file exposed at the weights the launcher uses. */
private fun variable(res: Int) = FontFamily(
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(res, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)
