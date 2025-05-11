package io.github.yearsyan.yaad

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.kongzue.dialogx.dialogs.BottomDialog
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.interfaces.OnBindView
import io.github.yearsyan.yaad.ui.components.EnvDoctorResult
import io.github.yearsyan.yaad.ui.components.FixDialog
import io.github.yearsyan.yaad.ui.screens.MainScreen
import io.github.yearsyan.yaad.utils.GithubUtil
import io.github.yearsyan.yaad.utils.YouGetUtil
import io.github.yearsyan.yaad.utils.YtDlpUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        lifecycleScope.launch(Dispatchers.IO) {
//            checkDependence()
//        }
        setContent {
            MainScreen(lifecycleScope = lifecycleScope)
        }
    }

    private suspend fun checkDependence() {
        val ytDlpExist = YtDlpUtil.isYtDlpExists(this)
        val youGetExist = YouGetUtil.isYouGetExists(this)
        if (!ytDlpExist || !youGetExist) {
            MessageDialog.build()
                .setTitle(R.string.missing_dep)
                .setCustomView(
                    object : OnBindView<MessageDialog>(R.layout.layout_compose) {
                        override fun onBind(dialog: MessageDialog, view: View) {
                            val composeView = view.findViewById<ComposeView>(R.id.compose_view)
                            composeView.setContent {
                                EnvDoctorResult(
                                    ytDlpExist = ytDlpExist,
                                    youGetExist = youGetExist
                                )
                            }
                        }
                    }
                )
                .setOkButton(R.string.repair_all)
                .setCancelButton(R.string.cancel)
                .setOkButtonClickListener({ d,v ->
                    repair(
                        fixYtDlp = !ytDlpExist,
                        fixYouGet = !youGetExist,
                    )
                    return@setOkButtonClickListener false
                })
                .show(this)
        } else {
            val latestVersion = YtDlpUtil.getLatestVersion() ?: return
            val currentVersion = YtDlpUtil.getCurrentVersion(this)
            if (GithubUtil.compareVersions(latestVersion, currentVersion) > 0) {
                showUpdateYtDlpDialog()
            }
        }
    }

    private fun repair(fixYtDlp: Boolean, fixYouGet: Boolean) {
        BottomDialog.build()
            .setCustomView(object : OnBindView<BottomDialog>(R.layout.layout_compose) {
                override fun onBind(dialog: BottomDialog, view: View) {
                    val composeView = view.findViewById<ComposeView>(R.id.compose_view)
                    composeView.setContent {
                        FixDialog(dialog, fixYtDlp, fixYouGet, lifecycleScope)
                    }
                }
            })
            .setCancelable(false)
            .show(this)
    }

    private suspend fun showUpdateYtDlpDialog() {
        // TODO: show update dialog
    }
}


