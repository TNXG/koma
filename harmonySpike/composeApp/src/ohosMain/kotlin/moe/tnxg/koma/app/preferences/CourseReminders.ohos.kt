package moe.tnxg.koma.app.preferences

import androidx.compose.runtime.Composable
import moe.tnxg.koma.domain.model.TimetableSnapshot

/** OHOS 端课程提醒依赖通知服务（M1 未接入），降级为 no-op。 */
@Composable
internal actual fun SyncCourseReminders(
    snapshot: TimetableSnapshot?,
    startDate: String,
    preferences: AppPreferences,
) {}
