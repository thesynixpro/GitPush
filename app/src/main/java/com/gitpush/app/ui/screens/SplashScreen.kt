package com.gitpush.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gitpush.app.ui.theme.NeonCyan
import com.gitpush.app.ui.theme.NeonViolet
import com.gitpush.app.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = tween(700, easing = FastOutSlowInEasing), label = "logo"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(600), label = "fade"
    )
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.25f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow"
    )
    LaunchedEffect(Unit) {
        started = true
        delay(1500)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(VoidBlack, Color(0xFF0B1626), VoidBlack))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo mark with digital glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha)
                    .scale(scale)
                    .blur((18 * glow).dp)
                    .background(NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(32.dp))
            )
            Box(
                modifier = Modifier
                    .offset(y = (-120).dp)
                    .size(120.dp)
                    .alpha(alpha)
                    .scale(scale)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, NeonViolet)),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⇪", fontSize = 54.sp, color = Color.Black, fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { contentDescription = "GitPush logo" }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "GitPush", fontSize = 34.sp, fontWeight = FontWeight.Black,
                color = Color.White, modifier = Modifier.alpha(alpha)
            )
            Text(
                "Push files directly to GitHub", fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(alpha)
            )
            Spacer(Modifier.height(26.dp))
            CircularProgressIndicator(
                color = NeonCyan, strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp).alpha(alpha)
            )
        }
    }
}
