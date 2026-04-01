package com.jetcar.vidrox.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetcar.vidrox.R

@Composable
fun SplashLoading(progress: Float) {

    val animatedProgress by
    animateFloatAsState(
        targetValue = (progress * 1.5F),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0XFF0B0B0B))
            .debugLayoutBorder(Color(0xFFFF5722))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .debugLayoutBorder(Color(0xFF4CAF50)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.banner_fg),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 80.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6F)
                    .debugLayoutBorder(Color(0xFF03A9F4)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.toys_fan_24px),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(Modifier.width(8.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.weight(1F),
                    color = Color(0XFFFF0000),
                    trackColor = Color.LightGray,
                    gapSize = 0.dp,
                    strokeCap = StrokeCap.Square
                )
            }
        }
    }

}