package io.github.yearsyan.yaad.ui.components.preview

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yearsyan.yaad.services.UrlHandler

enum class PreviewType {
    Http,
    SystemRouter,
    Wifi,
    Text
}

fun getPreviewType(context: Context, data: String): PreviewType {
    if (UrlHandler.isHttpLink(data)) {
        return PreviewType.Http
    } else if (UrlHandler.canRoute(context, data)) {
        return PreviewType.SystemRouter
    }
    return PreviewType.Text
}

@Composable
fun TextPreview(data: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val previewType = getPreviewType(context, data)
    Box(
        modifier =
            modifier.padding(
                PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 0.dp,
                    bottom = 12.dp
                )
            )
    ) {
        when (previewType) {
            PreviewType.Http -> HttpPreviewCard(data)
            PreviewType.SystemRouter -> Column {}
            PreviewType.Wifi -> Column {}
            PreviewType.Text -> Text(data)
        }
    }
}
