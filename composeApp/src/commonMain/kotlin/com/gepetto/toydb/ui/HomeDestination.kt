package com.gepetto.toydb.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import club.gepetto.composeutils.GcBanner
import club.gepetto.composeutils.GcTheme
import club.gepetto.composeutils.gepetto
import club.gepetto.composeutils.invertedgepetto
import club.gepetto.composeutils.sysBackgroundColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*
import toydb.composeapp.generated.resources.Res

@Composable
fun HomeDestination(
    onNavigateToDashboard: () -> Unit,
    onNavigateToInfo: () -> Unit,
    title: String = stringResource(Res.string.app_name),
    themeMode: Int = 0,
    modifier: Modifier = Modifier
) {
    var showBanner by remember { mutableStateOf(true) }

    val isDark = when (themeMode) {
        1 -> false // Light
        2 -> true  // Dark
        else -> isSystemInDarkTheme()
    }
    val imageRes = if (isDark) {
        club.gepetto.composeutils.Res.drawable.invertedgepetto
    } else {
        club.gepetto.composeutils.Res.drawable.gepetto
    }

    val p1 = stringResource(Res.string.home_banner_p1)
    val p2Pre = stringResource(Res.string.home_banner_p2_pre)
    val linkDashboard = stringResource(Res.string.home_banner_link_dashboard)
    val p2Post = stringResource(Res.string.home_banner_p2_post)
    val p3Pre = stringResource(Res.string.home_banner_p3_pre)
    val linkInfo = stringResource(Res.string.home_banner_link_info)
    val p3Post = stringResource(Res.string.home_banner_p3_post)
    val p4 = stringResource(Res.string.home_banner_p4)

    val linkColor = Color.Blue

    val annotatedBannerText = remember(p1, p2Pre, linkDashboard, p2Post, p3Pre, linkInfo, p3Post, p4) {
        buildAnnotatedString {
            append(p1)
            append("\n")
            append(p2Pre)

            pushStringAnnotation(tag = "ACTION", annotation = "dashboard")
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(linkDashboard)
            }
            pop()

            append(p2Post)
            append("\n")
            append(p3Pre)

            pushStringAnnotation(tag = "ACTION", annotation = "info")
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(linkInfo)
            }
            pop()

            append(p3Post)
            append(p4)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(sysBackgroundColor()),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        val contentScale = if (isLandscape) ContentScale.Fit else ContentScale.Crop

        Image(
            painter = painterResource(imageRes),
            contentDescription = "Gepetto",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )

        HomeTitle(
            title = title,
            onTitleClick = onNavigateToInfo,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        ShowBanner(
            landscape = isLandscape,
            bannerState = showBanner,
            annotatedText = annotatedBannerText,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (isLandscape) 48.dp else 40.dp),
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateToInfo = onNavigateToInfo,
            onBannerStateChanged = { showBanner = it }
        )

        if (!showBanner) {
            FloatingActionButton(
                onClick = { showBanner = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(Res.string.home_banner_show_info)
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ShowBanner(
    landscape: Boolean,
    bannerState: Boolean,
    annotatedText: AnnotatedString,
    modifier: Modifier = Modifier,
    onNavigateToDashboard: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onBannerStateChanged: (Boolean) -> Unit,
) {
    if (bannerState) {
        Box(modifier = modifier) {
            GcBanner(
                modifier = Modifier.align(Alignment.Center),
                startTextPadding = if (landscape) 24.dp else 0.dp,
                endTextPadding = if (landscape) 8.dp else 0.dp,
                imageInnerVerticalPadding = if (landscape) 32.dp else 64.dp,
                imageInnerHorizontalPadding = 48.dp,
            ) {
                ClickableText(
                    text = annotatedText,
                    style = TextStyle(
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (landscape) 14.sp else 12.sp,
                        lineHeight = if (landscape) 20.sp else 17.sp
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "ACTION", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                when (annotation.item) {
                                    "dashboard" -> onNavigateToDashboard()
                                    "info" -> onNavigateToInfo()
                                }
                            }
                    }
                )
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Blue,
                modifier = Modifier
                    .clickable { onBannerStateChanged(false) }
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (landscape) 32.dp else 64.dp,
                        end = if (landscape) 64.dp else 48.dp
                    )
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Landscape", widthDp = 800, heightDp = 480)
@Composable
fun HomeDestinationPreview() {
    GcTheme {
        HomeDestination(
            onNavigateToDashboard = {},
            onNavigateToInfo = {},
            title = "Gepetto Toy Database Manager"
        )
    }
}
