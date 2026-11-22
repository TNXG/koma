package moe.tnxg.koma.feature.home.timeline.course

import moe.tnxg.koma.ui.component.icon.KomaGlyph
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import moe.tnxg.koma.domain.visual.*
import moe.tnxg.koma.ui.theme.KomaTheme
import moe.tnxg.koma.ui.theme.courseTone

@Composable
internal fun courseTone(tone: CourseTone): Color = KomaTheme.colors.courseTone(tone)
internal fun courseIcon(icon: CourseIcon): KomaGlyph = when(icon) {
    CourseIcon.Translate->KomaGlyph.Translate; CourseIcon.Formula->KomaGlyph.Formula
    CourseIcon.Fitness->KomaGlyph.Fitness; CourseIcon.Code->KomaGlyph.Code
    CourseIcon.Flask->KomaGlyph.Flask; CourseIcon.Bank->KomaGlyph.Bank
    CourseIcon.Palette->KomaGlyph.Palette; CourseIcon.Briefcase->KomaGlyph.Briefcase
    CourseIcon.Chip->KomaGlyph.Chip; CourseIcon.Heart->KomaGlyph.Heart
    CourseIcon.Book->KomaGlyph.Book
}
