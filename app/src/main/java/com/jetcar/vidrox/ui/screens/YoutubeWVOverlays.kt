package com.jetcar.vidrox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text

private val NAV_BUTTON_SIZE = 44.dp
private val NAV_BUTTON_COLOR = Color.Black.copy(alpha = 0.22f)
private val OFFLINE_PANEL_COLOR = Color(0xCC101010)

@Composable
internal fun OfflineOverlay(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(OFFLINE_PANEL_COLOR)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Offline",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Connection was lost. Check your network and refresh.",
                color = Color.White.copy(alpha = 0.88f),
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(text = "Refresh")
            }
        }
    }
}

@Composable
internal fun DirectionPadOverlay(
    modifier: Modifier = Modifier,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NavigationButton(symbol = "^", onClick = onUp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavigationButton(symbol = "<", onClick = onLeft)
            NavigationButton(symbol = ">", onClick = onRight)
        }
        NavigationButton(symbol = "v", onClick = onDown)
    }
}

@Composable
private fun NavigationButton(
    symbol: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(NAV_BUTTON_SIZE)
            .clip(CircleShape)
            .background(NAV_BUTTON_COLOR)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, color = Color.White)
    }
}
