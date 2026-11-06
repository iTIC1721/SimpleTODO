package com.simpletodo.simpletodo.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.simpletodo.simpletodo.R
import com.simpletodo.simpletodo.ui.theme.mutedText

private const val REPO_URL = "https://github.com/iTIC1721/SimpleTODO"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

/** Runtime libraries bundled in the APK. All are Apache License 2.0. */
private val openSourceLibraries = listOf(
    "Kotlin 표준 라이브러리, Kotlin Coroutines",
    "AndroidX Core, Activity, Lifecycle",
    "Jetpack Compose (UI, Material 3, Material Icons)",
    "AndroidX Room",
)

/** "1.0.2 (3)" — version name and code, read from the installed package. */
fun appVersionText(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
}.getOrDefault("")

@Composable
fun AppInfoScreen(onBack: () -> Unit, onOpenLicenses: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { appVersionText(context) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = { ScreenTopBar(title = "앱 정보", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon()
                Column {
                    Text("SimpleTODO", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "버전 $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            InfoSection(title = "지원") {
                LinkRow(label = "소스 코드") { context.openUrl(REPO_URL) }
                LinkRow(label = "오류 신고 및 문의") { context.openUrl(ISSUES_URL) }
            }

            InfoSection(title = "법적 고지") {
                LinkRow(label = "오픈소스 라이선스", onClick = onOpenLicenses)
            }
        }
    }
}

/** Libraries bundled in the app, kept on their own screen rather than listed on 앱 정보. */
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = { ScreenTopBar(title = "오픈소스 라이선스", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            openSourceLibraries.forEach { library ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(library, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("Apache 2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.mutedText)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            LinkRow(
                label = "Apache License 2.0 전문",
                modifier = Modifier.padding(top = 16.dp),
            ) { context.openUrl(APACHE_LICENSE_URL) }
        }
    }
}

/**
 * The launcher icon's foreground on the same white tile the launcher uses.
 * (The adaptive mipmap itself can't be drawn by painterResource.)
 */
@Composable
private fun AppIcon() {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 28.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.mutedText,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

/** A tappable row: label on the left, a muted value and a chevron on the right. */
@Composable
fun LinkRow(label: String, modifier: Modifier = Modifier, value: String? = null, onClick: () -> Unit) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.mutedText)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.mutedText,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}
