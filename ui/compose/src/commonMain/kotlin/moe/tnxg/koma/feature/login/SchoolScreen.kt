package moe.tnxg.koma.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.tnxg.koma.ui.component.button.KomaTextButton
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import moe.tnxg.koma.ui.component.icon.KomaIcon
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.component.text.komaFontFamily
import moe.tnxg.koma.ui.theme.KomaTheme

internal data class School(val id: String, val name: String, val auth: String, val keywords: List<String>) {
    fun matches(query: String): Boolean = name.contains(query, ignoreCase = true) ||
        keywords.any { it.contains(query, ignoreCase = true) }
}

internal val SCHOOLS = listOf(
    School("xjvut", "新疆理工职业大学", "统一身份认证", listOf("新疆理工", "xjvut", "xjlg")),
)

internal val SCHOOL_NAME = SCHOOLS.first().name

@Composable
fun SchoolScreen(onSelect: () -> Unit, onBack: () -> Unit) {
    val colors = KomaTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val keyword = query.trim()
    val results = remember(keyword) { if (keyword.isEmpty()) SCHOOLS else SCHOOLS.filter { it.matches(keyword) } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 430.dp).fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 34.dp)) {
                KomaTextButton("返回", onBack, Modifier.padding(vertical = 12.dp))
                KomaText("登录哪所学校？", size = 26, weight = FontWeight.ExtraBold,
                    letterSpacing = -.52f, lineHeight = 1.2f)
                Spacer(Modifier.height(6.dp))
                KomaText("选择学校后，使用该校学号和教务密码登录", size = 12.5f,
                    weight = FontWeight.Medium, color = colors.ink3)
                Spacer(Modifier.height(20.dp))
                SchoolSearch(query) { query = it }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp)) {
                if (results.isEmpty()) item {
                    KomaText("没有找到「$keyword」", Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        size = 13, weight = FontWeight.Medium, color = colors.ink3)
                }
                itemsIndexed(results, key = { _, school -> school.id }) { index, school ->
                    SchoolRow(school, rowShape(index, results.size), index > 0, onSelect)
                }
                item {
                    KomaText("暂未收录的学校会陆续支持。账号凭据仅加密保存在本机。",
                        Modifier.padding(horizontal = 2.dp, vertical = 16.dp), size = 12, color = colors.ink3)
                }
            }
        }
    }
}

@Composable
private fun SchoolSearch(query: String, onChange: (String) -> Unit) {
    val colors = KomaTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)
        .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        KomaIcon(KomaGlyph.Search, null, Modifier.size(18.dp), color = colors.ink4)
        BasicTextField(query, onChange, Modifier.weight(1f).padding(horizontal = 10.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = TextStyle(color = colors.ink, fontFamily = komaFontFamily(), fontSize = 14.sp,
                fontWeight = FontWeight.Medium, lineHeight = 20.3.sp),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { field ->
                Box { if (query.isEmpty()) KomaText("搜索学校名称或简称", size = 14,
                    weight = FontWeight.Medium, color = colors.ink5); field() }
            })
        if (query.isNotEmpty()) KomaIcon(KomaGlyph.CloseCircle, "清除", Modifier.size(28.dp)
            .clickable(remember { MutableInteractionSource() }, null) { onChange("") }.padding(5.dp),
            color = colors.ink5)
    }
}

@Composable
private fun SchoolRow(school: School, shape: Shape, divider: Boolean, onSelect: () -> Unit) {
    val colors = KomaTheme.colors
    Column(Modifier.fillMaxWidth().clip(shape).background(colors.surface)) {
        if (divider) Box(Modifier.padding(start = 56.dp).fillMaxWidth().height(1.dp).background(colors.line2))
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            KomaIcon(KomaGlyph.Bank, null, Modifier.size(24.dp), color = colors.accent)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                KomaText(school.name, size = 14, weight = FontWeight.SemiBold, maxLines = 1)
                KomaText(school.auth, Modifier.padding(top = 3.dp), size = 12, color = colors.ink3)
            }
            KomaIcon(KomaGlyph.Right, null, Modifier.size(16.dp), color = colors.ink5)
        }
    }
}

private fun rowShape(index: Int, count: Int): Shape {
    val top = if (index == 0) 20.dp else 0.dp
    val bottom = if (index == count - 1) 20.dp else 0.dp
    return RoundedCornerShape(top, top, bottom, bottom)
}
