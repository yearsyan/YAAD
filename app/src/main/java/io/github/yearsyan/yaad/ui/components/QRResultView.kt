package io.github.yearsyan.yaad.ui.components

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.yearsyan.yaad.R
import io.github.yearsyan.yaad.ui.components.preview.TextPreview

class QRResultDialogFragment(val onDismissListener: () -> Unit = {}) :
    DialogFragment() {
    companion object {
        private const val ARG_RESULT = "arg_result"

        fun newInstance(
            result: String,
            onDismiss: () -> Unit = {}
        ): QRResultDialogFragment {
            return QRResultDialogFragment(onDismiss).apply {
                arguments = Bundle().apply { putString(ARG_RESULT, result) }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val result = requireArguments().getString(ARG_RESULT) ?: ""
        return BottomSheetDialog(requireContext(), R.style.full_screen_dialog)
            .also { dialog ->
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    it.setLayout(MATCH_PARENT, MATCH_PARENT)
                    it.setBackgroundDrawable(
                        android.graphics.Color.TRANSPARENT.toDrawable()
                    )
                    it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    it.addFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val animator =
                            ValueAnimator.ofInt(0, 25).apply {
                                duration = 500
                                addUpdateListener { animation ->
                                    if (!it.decorView.isAttachedToWindow) {
                                        cancel()
                                        return@addUpdateListener
                                    }
                                    val blurRadius =
                                        animation.animatedValue as Int * 5
                                    it.attributes.blurBehindRadius = blurRadius
                                    it.attributes = it.attributes
                                }
                            }

                        dialog.setOnShowListener { animator.start() }
                    }
                }
                dialog.setContentView(R.layout.layout_compose)
                dialog.findViewById<ComposeView>(R.id.compose_view)?.apply {
                    setContent { QRResultView(data = result) }
                }
            }
    }
}

@Composable
fun QRResultView(data: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().wrapContentSize(),
        shape =
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Box(modifier = Modifier.wrapContentSize()) { TextPreview(data = data) }
    }
}
