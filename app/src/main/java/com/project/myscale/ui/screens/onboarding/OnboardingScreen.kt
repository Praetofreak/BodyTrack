package com.project.myscale.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.project.myscale.BodyTrackApplication
import com.project.myscale.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val icons: List<ImageVector>
)

private val pages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_title_1,
        descriptionRes = R.string.onboarding_desc_1,
        icons = listOf(Icons.Rounded.MonitorWeight, Icons.AutoMirrored.Rounded.TrendingUp)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_title_2,
        descriptionRes = R.string.onboarding_desc_2,
        icons = listOf(Icons.AutoMirrored.Rounded.ShowChart)
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_title_3,
        descriptionRes = R.string.onboarding_desc_3,
        icons = listOf(Icons.Rounded.Settings)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val preferencesManager = (context.applicationContext as BodyTrackApplication).preferencesManager

    val completeOnboarding: () -> Unit = {
        scope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = completeOnboarding) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // Page indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom button
        if (pagerState.currentPage == pages.size - 1) {
            Button(
                onClick = completeOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.titleSmall)
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.onboarding_next), style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            page.icons.forEach { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
