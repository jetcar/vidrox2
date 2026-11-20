package com.jetcar.vidrox.remote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jetcar.vidrox.remote.core.RemoteAction
import com.jetcar.vidrox.remote.core.RemoteCommand
import com.jetcar.vidrox.remote.network.RemoteCommandClient
import com.jetcar.vidrox.remote.network.RemoteDiscovery
import com.jetcar.vidrox.remote.network.RemoteEndpoint
import com.jetcar.vidrox.remote.update.RemoteRelease
import com.jetcar.vidrox.remote.update.RemoteUpdateDialog
import com.jetcar.vidrox.remote.update.getRemoteUpdate

class MainActivity : ComponentActivity() {
    private val sharedTarget = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readSharedTarget(intent)
        enableEdgeToEdge()
        setContent { VidroXRemoteTheme { RemoteScreen(sharedTarget.value) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readSharedTarget(intent)
    }

    private fun readSharedTarget(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedTarget.value = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        }
    }
}

private val RemoteColors = darkColorScheme(
    primary = Color(0xFFFF4B45),
    onPrimary = Color.White,
    background = Color(0xFF090B10),
    surface = Color(0xFF151922),
    onBackground = Color(0xFFF2F4FA),
    onSurface = Color(0xFFF2F4FA),
)

@Composable
private fun VidroXRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RemoteColors, content = content)
}

@Composable
private fun RemoteScreen(sharedTarget: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var endpoints by remember { mutableStateOf<List<RemoteEndpoint>>(emptyList()) }
    var selected by remember { mutableStateOf<RemoteEndpoint?>(null) }
    var status by remember { mutableStateOf("поиск устройств…") }
    var showRemote by rememberSaveable { mutableStateOf(false) }
    var pendingVideoTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var castSucceededTick by rememberSaveable { mutableIntStateOf(0) }
    var availableUpdate by remember { mutableStateOf<RemoteRelease?>(null) }
    val commandClient = remember { RemoteCommandClient() }

    LaunchedEffect(Unit) {
        availableUpdate = getRemoteUpdate(context)
    }

    DisposableEffect(context, lifecycleOwner) {
        var discovery: RemoteDiscovery? = null

        fun startDiscovery() {
            if (discovery != null) return
            discovery = RemoteDiscovery(
                context = context,
                onEndpointsChanged = { found ->
                    endpoints = found
                    selected = selected?.let { current -> found.firstOrNull { it == current } }
                        ?: found.firstOrNull()
                },
                onStatus = { status = it },
            ).also(RemoteDiscovery::start)
        }

        fun stopDiscovery() {
            discovery?.close()
            discovery = null
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startDiscovery()
                Lifecycle.Event.ON_PAUSE -> stopDiscovery()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startDiscovery()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopDiscovery()
            commandClient.close()
        }
    }

    fun sendTo(
        endpoint: RemoteEndpoint?,
        action: RemoteAction,
        argument: String = "",
        onSuccess: () -> Unit = {},
    ) {
        if (endpoint == null) {
            status = "VidroX не найден"
            return
        }
        val command = RemoteCommand(action, argument.trim())
        status = "отправка…"
        commandClient.send(endpoint, command) { result ->
            status = result.fold(
                onSuccess = {
                    onSuccess()
                    "готово"
                },
                onFailure = { error -> humanError(error) },
            )
        }
    }

    fun send(action: RemoteAction, argument: String = "", onSuccess: () -> Unit = {}) {
        sendTo(selected, action, argument, onSuccess)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        YouTubeBrowser(
            sharedTarget = sharedTarget,
            receiverLabel = selected?.label,
            status = status,
            castSucceededTick = castSucceededTick,
            onOpenRemote = { showRemote = true },
            onSendVideo = { target -> pendingVideoTarget = target },
        )
        if (showRemote) {
            RemoteLayout(
                endpoints = endpoints,
                selected = selected,
                status = status,
                onSelect = { selected = it },
                onClose = { showRemote = false },
                send = { action, argument -> send(action, argument) },
            )
        }
    }

    BackHandler(enabled = showRemote) {
        showRemote = false
    }

    availableUpdate?.let { release ->
        RemoteUpdateDialog(release = release) { availableUpdate = null }
    }

    pendingVideoTarget?.let { target ->
        DevicePickerDialog(
            endpoints = endpoints,
            selected = selected,
            onDismiss = {
                pendingVideoTarget = null
            },
            onSelect = { endpoint ->
                selected = endpoint
                pendingVideoTarget = null
                sendTo(endpoint, RemoteAction.OPEN, target) { castSucceededTick++ }
            },
        )
    }
}

@Composable
private fun DevicePickerDialog(
    endpoints: List<RemoteEndpoint>,
    selected: RemoteEndpoint?,
    onDismiss: () -> Unit,
    onSelect: (RemoteEndpoint) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите ТВ") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (endpoints.isEmpty()) {
                    Text("Ищу VidroX в этой Wi‑Fi сети…")
                    Text(
                        "Откройте VidroX на телевизоре и оставьте его на экране.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                endpoints.forEach { endpoint ->
                    val label = if (endpoint == selected) "✓ ${endpoint.label}" else endpoint.label
                    OutlinedButton(
                        onClick = { onSelect(endpoint) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                            Text(endpoint.hostAddress, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@Composable
private fun RemoteLayout(
    endpoints: List<RemoteEndpoint>,
    selected: RemoteEndpoint?,
    status: String,
    onSelect: (RemoteEndpoint) -> Unit,
    onClose: () -> Unit,
    send: (RemoteAction, String) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            val compact = maxHeight < 760.dp
            val useTwoColumns = maxWidth > maxHeight && maxWidth >= 600.dp
            if (useTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ConnectionPanel(
                        endpoints = endpoints,
                        selected = selected,
                        status = status,
                        onSelect = onSelect,
                        onClose = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        compact = true,
                    )
                    ControlsPanel(
                        send = send,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        compact = true,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ConnectionPanel(
                        endpoints = endpoints,
                        selected = selected,
                        status = status,
                        onSelect = onSelect,
                        onClose = onClose,
                        modifier = if (compact) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        compact = compact,
                    )
                    ControlsPanel(
                        send = send,
                        modifier = if (compact) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth().weight(1f)
                        },
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionPanel(
    endpoints: List<RemoteEndpoint>,
    selected: RemoteEndpoint?,
    status: String,
    onSelect: (RemoteEndpoint) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    compact: Boolean,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("VidroX Remote", fontSize = if (compact) 22.sp else 28.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onClose) { Text("YouTube") }
        }
        Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (endpoints.isEmpty()) Text("Устройства пока не найдены")
            endpoints.forEach { endpoint ->
                if (endpoint == selected) {
                    Button(onClick = { onSelect(endpoint) }) { Text(endpoint.label) }
                } else {
                    OutlinedButton(onClick = { onSelect(endpoint) }) { Text(endpoint.label) }
                }
            }
        }
    }
}

@Composable
private fun ControlsPanel(
    send: (RemoteAction, String) -> Unit,
    modifier: Modifier,
    compact: Boolean,
) {
    val buttonHeight = if (compact) 46.dp else 58.dp
    val buttonWidth = if (compact) 54.dp else 68.dp
    val wideButtonWidth = if (compact) 88.dp else 104.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton("−10", RemoteAction.SEEK_BACK, send, buttonWidth, buttonHeight)
            ControlButton("▶ / ❚❚", RemoteAction.PLAY_PAUSE, send, wideButtonWidth, buttonHeight)
            ControlButton("+10", RemoteAction.SEEK_FORWARD, send, buttonWidth, buttonHeight)
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    action: RemoteAction,
    send: (RemoteAction, String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
) {
    Button(
        onClick = { send(action, "") },
        modifier = Modifier.size(width = width, height = height),
    ) {
        Text(label, fontSize = 17.sp)
    }
}

private fun humanError(error: Throwable): String = when (error.message) {
    "missing_argument", "invalid_youtube_target" -> "Неверная ссылка YouTube"
    "receiver_error" -> "Приёмник отклонил команду"
    "receiver_not_ready" -> "VidroX ещё загружается"
    "unsupported_protocol" -> "Обновите VidroX на ТВ"
    else -> "Нет связи с VidroX"
}
