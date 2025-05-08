package io.github.yearsyan.yaad

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.style.MaterialStyle
import io.github.yearsyan.yaad.utils.AssetUtil
import io.github.yearsyan.yaad.utils.YtDlpUtil.YT_DLP_FILE_NAME
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        DialogX.init(this);
        DialogX.globalStyle = MaterialStyle.style()
        DialogX.globalTheme = DialogX.THEME.AUTO
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        initZip()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initZip() {
        GlobalScope.launch(Dispatchers.IO) {
            AssetUtil.copyAssetIfNeeded(this@AppApplication, YT_DLP_FILE_NAME)
        }
    }
}