package moe.tnxg.koma.feature.home.profile

import androidx.compose.foundation.layout.*
import moe.tnxg.koma.ui.component.icon.KomaGlyph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import moe.tnxg.koma.domain.calendar.formatMonthDay
import moe.tnxg.koma.feature.home.dock.dockPress
import moe.tnxg.koma.feature.home.icon.HomeIcon
import moe.tnxg.koma.ui.component.sheet.KomaSheet
import moe.tnxg.koma.ui.component.text.KomaText
import moe.tnxg.koma.ui.theme.KomaTheme

@Composable
internal fun MeDrawer(open:Boolean,studentName:String?,semesterName:String,startDate:String,
    onDismiss:()->Unit,onReimport:()->Unit,onEditStart:()->Unit,onLogout:()->Unit,
) {
    val c=KomaTheme.colors
    KomaSheet(open,onDismiss,horizontalPadding=12.dp) {
        Column(Modifier.fillMaxWidth().widthIn(max=430.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Column(Modifier.padding(start=8.dp,end=8.dp,top=4.dp,bottom=14.dp)) {
                KomaText(studentName?:"同学",size=17,weight=FontWeight.ExtraBold,letterSpacing=-.34f)
                KomaText(semesterName,Modifier.padding(top=2.dp),size=12,weight=FontWeight.Medium,color=c.ink4)
            }
            ProfileRow(KomaGlyph.Refresh,"重新导入课表",c.ink3,onClick=onReimport)
            ProfileRow(KomaGlyph.Calendar,"修改开学时间",c.ink3,formatMonthDay(LocalDate.parse(startDate)),onEditStart)
            ProfileRow(KomaGlyph.Exit,"退出登录并清除课表",c.danger,onClick=onLogout)
        }
    }
}

@Composable
private fun ProfileRow(icon:KomaGlyph,text:String,color:Color,value:String?=null,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().dockPress(1f,onClick).padding(horizontal=10.dp,vertical=13.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        HomeIcon(icon,color,Modifier.size(17.dp))
        KomaText(text,size=14,weight=FontWeight.Medium,color=color)
        if(value!=null) { Spacer(Modifier.weight(1f));KomaText(value,size=12,weight=FontWeight.Medium,color=KomaTheme.colors.ink4) }
    }
}
