package com.donut.mixfile.ui.component.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.donut.mixfile.util.addComposeView

class MixDialogBuilder(
    private var title: String,
    private val subtitle: String = "",
    private val tag: String = title,
    autoClose: Boolean = true,
    private val properties: DialogProperties = DialogProperties(
        dismissOnBackPress = autoClose,
        dismissOnClickOutside = autoClose
    ),
    private val containerColor: Color? = null,
    private val iconContentColor: Color? = null,
    private val titleContentColor: Color? = null,
    private val textContentColor: Color? = null,
    private val colorScheme: ColorScheme? = null,
) {
    private var content = @Composable {}
    private var positiveButton = @Composable {}
    private var negativeButton = @Composable {}
    private var neutralButton = @Composable {}
    private var close: () -> Unit = {}
    private val disMissListeners = mutableListOf<() -> Unit>()

    companion object {
        val dialogCache = mutableMapOf<String, () -> Unit>()
    }

    fun setContent(content: @Composable () -> Unit) {
        this.content = content
    }

    fun onDismiss(listener: () -> Unit) {
        disMissListeners.add(listener)
    }

    fun closeDialog() {
        close()
    }

    fun setDefaultNegative(text: String = "取消") {
        setNegativeButton(text) { closeDialog() }
    }

    @Composable
    private fun BuildButton(text: String, callBack: (close: () -> Unit) -> Unit) {
        return TextButton(onClick = {
            callBack(close)
        }) {
            Text(text = text)
        }
    }

    fun setPositiveButton(text: String, callBack: (close: () -> Unit) -> Unit) {
        positiveButton = {
            BuildButton(text = text, callBack)
        }
    }

    fun setNegativeButton(text: String, callBack: (close: () -> Unit) -> Unit) {
        negativeButton = {
            BuildButton(text = text, callBack)
        }
    }

    fun setNeutralButton(text: String, callBack: (close: () -> Unit) -> Unit) {
        neutralButton = {
            BuildButton(text = text, callBack)
        }
    }

    fun setBottomContent(content: @Composable () -> Unit) {
        neutralButton = {
            content()
        }
    }

    fun show() {
        close = showAlertDialog(
            title,
            subtitle,
            content,
            positiveButton,
            negativeButton,
            neutralButton,
            properties,
            onDismiss = {
                disMissListeners.forEach {
                    it()
                }
            },
            containerColor,
            iconContentColor,
            titleContentColor,
            textContentColor,
            colorScheme
        )
        dialogCache[tag]?.invoke()
        dialogCache[tag] = close
    }
}


fun showAlertDialog(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: (@Composable () -> Unit)? = null,
    neutralButton: @Composable () -> Unit = {},
    properties: DialogProperties = DialogProperties(),
    onDismiss: () -> Unit = {},
    containerColor: Color? = null,
    iconContentColor: Color? = null,
    titleContentColor: Color? = null,
    textContentColor: Color? = null,
    scheme: ColorScheme? = null,
): () -> Unit {
    return addComposeView(scheme) { removeView ->
        val mixedDismissButton = @Composable {
            neutralButton()
            (dismissButton ?: {
                TextButton(onClick = {
                    removeView()
                }) {
                    Text(text = "关闭")
                }
            })()
        }
        AlertDialog(
            modifier = Modifier
                .systemBarsPadding()
                .heightIn(0.dp, 600.dp),
            properties = properties,
            containerColor = containerColor ?: AlertDialogDefaults.containerColor,
            iconContentColor = iconContentColor ?: AlertDialogDefaults.iconContentColor,
            titleContentColor = titleContentColor ?: AlertDialogDefaults.titleContentColor,
            textContentColor = textContentColor ?: AlertDialogDefaults.textContentColor,
            title = {
                Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            },
            onDismissRequest = {
                removeView()
                onDismiss()
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(0.dp)
                ) {
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, modifier = Modifier.fillMaxWidth())
                    }
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState()),
                    ) {
                        content()
                    }
                }
            },
            confirmButton = confirmButton,
            dismissButton = mixedDismissButton
        )
    }
}