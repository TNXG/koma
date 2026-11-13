package moe.tnxg.koma.feature.home.timeline.course

import moe.tnxg.koma.ui.component.icon.KomaGlyph
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import moe.tnxg.koma.domain.visual.*
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun courseTone(tone: CourseTone): Color {
    val c=KomaTheme.colors
    return when(tone) {
        CourseTone.Blue->c.toneBlue; CourseTone.Amber->c.toneAmber; CourseTone.Rose->c.toneRose
        CourseTone.Green->c.toneGreen; CourseTone.Violet->c.toneViolet
        CourseTone.Orange->c.toneOrange; CourseTone.Teal->c.toneTeal
    }
}
internal fun courseIcon(icon: CourseIcon): KomaGlyph = when(icon) {
    CourseIcon.Translate->KomaGlyph.Translate; CourseIcon.Formula->KomaGlyph.Formula
    CourseIcon.Fitness->KomaGlyph.Fitness; CourseIcon.Code->KomaGlyph.Code
    CourseIcon.Flask->KomaGlyph.Flask; CourseIcon.Bank->KomaGlyph.Bank
    CourseIcon.Palette->KomaGlyph.Palette; CourseIcon.Briefcase->KomaGlyph.Briefcase
    CourseIcon.Chip->KomaGlyph.Chip; CourseIcon.Heart->KomaGlyph.Heart
    CourseIcon.Book->KomaGlyph.Book
}
