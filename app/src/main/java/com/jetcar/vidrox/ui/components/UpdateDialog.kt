package com.jetcar.vidrox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.jetcar.vidrox.utils.ReleaseData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_UPDATE_COUNTDOWN_SECONDS = 10

@Composable
fun UpdateDialog(releaseData: ReleaseData, onDismiss: () -> Unit) {
    val isShowDialog = rememberSaveable { mutableStateOf(true) }
    val isDownload = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val countdown = remember { mutableIntStateOf(AUTO_UPDATE_COUNTDOWN_SECONDS) }

    val triggerUpdate = {
        isDownload.value = true
        isShowDialog.value = false
    }

    if (isDownload.value)
        UpdateAppScreen(releaseData.tagName, releaseData.downloadUrl, onDismiss)

    if (isShowDialog.value) {
        // Count down every second; auto-update when it reaches 0
        LaunchedEffect(Unit) {
            while (countdown.intValue > 0) {
                delay(1000)
                countdown.intValue--
            }
            triggerUpdate()
        }

        Dialog(
            onDismissRequest = {
                isShowDialog.value = false
                onDismiss()
            }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.border(2.dp, Color.White, RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0XFF201c1c))
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "VidroX - ${releaseData.tagName} available!",
                        modifier = Modifier.padding(bottom = 20.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    Text(
                        text = AnnotatedString.fromHtml(releaseData.changelog),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Row {
                        YTButton("Cancel") {
                            isShowDialog.value = false
                            onDismiss()
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        YTButton(
                            text = "Update (${countdown.intValue})",
                            modifier = Modifier.focusRequester(focusRequester),
                            onClick = triggerUpdate
                        )
                    }
                }
            }
        }

        // Request focus for Update button
        LaunchedEffect(Unit) {
            scope.launch { focusRequester.requestFocus() }
        }
    }
}

@Composable
fun YTButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.defaultMinSize(minWidth = 120.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.DarkGray.copy(alpha = 0.5F),
            contentColor = Color.White,
        )
    ) { Text(text = text, fontSize = 16.sp) }
}
