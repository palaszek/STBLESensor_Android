/*
 * Copyright (c) 2024 STMicroelectronics.
 * All rights reserved.
 * This software is licensed under terms that can be found in the LICENSE file in
 * the root directory of this software component.
 * If no LICENSE file comes with this software, it is provided AS-IS.
 */
package com.st.demo_showcase.ui.wesu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.acceleration_event.AccelerationEventViewModel
import com.st.blue_sdk.features.acceleration_event.AccelerationType
import com.st.blue_sdk.features.acceleration_event.DetectableEventType
import com.st.core.ARG_NODE_ID
import com.st.demo_showcase.R
import com.st.ui.composables.ComposableLifecycle
import com.st.ui.theme.BlueMSTheme
import com.st.ui.theme.LocalDimensions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import android.widget.ImageView
import android.graphics.drawable.Animatable

@AndroidEntryPoint
class WesuOverviewFragment : Fragment() {

    private val accelerationViewModel: AccelerationEventViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val nodeId = arguments?.getString(ARG_NODE_ID)
            ?: throw IllegalArgumentException("Missing string $ARG_NODE_ID arguments")

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BlueMSTheme {
                    WesuOverviewScreen(
                        modifier = Modifier.fillMaxSize(),
                        nodeId = nodeId,
                        accelerationViewModel = accelerationViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun WesuOverviewScreen(
    modifier: Modifier,
    nodeId: String,
    accelerationViewModel: AccelerationEventViewModel
) {
    val accEventData by accelerationViewModel.accEventData.collectAsStateWithLifecycle()
    val stepCount = accEventData.first.numSteps.value
    val isFallDetected = accEventData.first.accEvent.any { it.value == AccelerationType.FreeFall }
    var isFallLatched by remember { mutableStateOf(false) }
    var wasFallDetected by remember { mutableStateOf(false) }
    var lastStepCount by remember { mutableStateOf(0L) }
    var lastStepTimestamp by remember { mutableStateOf(0L) }
    var isWalking by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isFallDetected) {
        if (isFallDetected && !wasFallDetected) {
            isFallLatched = true
        }
        wasFallDetected = isFallDetected
    }

    LaunchedEffect(stepCount) {
        val now = System.currentTimeMillis()
        if (stepCount > lastStepCount) {
            lastStepCount = stepCount
            lastStepTimestamp = now
            isWalking = true
        } else if (stepCount == 0L) {
            isWalking = false
            lastStepTimestamp = now
        }
    }

    LaunchedEffect(lastStepTimestamp, stepCount) {
        if (stepCount > 0L) {
            delay(5_000)
            if (System.currentTimeMillis() - lastStepTimestamp >= 5_000) {
                isWalking = false
            }
        }
    }

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                accelerationViewModel.startDemo(nodeId = nodeId)
                accelerationViewModel.setDetectableEventCommand(
                    nodeId = nodeId,
                    event = DetectableEventType.FreeFall,
                    enable = true
                )
                accelerationViewModel.setDetectableEventCommand(
                    nodeId = nodeId,
                    event = DetectableEventType.Multiple,
                    enable = true
                )
            }

            Lifecycle.Event.ON_STOP -> {
                accelerationViewModel.setDetectableEventCommand(
                    nodeId = nodeId,
                    event = DetectableEventType.FreeFall,
                    enable = false
                )
                accelerationViewModel.setDetectableEventCommand(
                    nodeId = nodeId,
                    event = DetectableEventType.Multiple,
                    enable = false
                )
                accelerationViewModel.stopDemo(nodeId = nodeId)
            }

            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = LocalDimensions.current.paddingNormal,
                end = LocalDimensions.current.paddingNormal,
                top = LocalDimensions.current.paddingNormal,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(LocalDimensions.current.imageExtraLarge),
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(R.drawable.wesu_walking)
                }
            },
            update = { view ->
                val drawable = view.drawable
                if (drawable is Animatable) {
                    if (isWalking) {
                        drawable.start()
                    } else {
                        drawable.stop()
                    }
                }
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalDimensions.current.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingSmall)
            ) {
                Text(
                    text = stringResource(id = R.string.st_wesu_overview_steps_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        id = R.string.st_wesu_overview_steps_value,
                        stepCount
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalDimensions.current.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.paddingSmall)
            ) {
                Text(
                    text = stringResource(id = R.string.st_wesu_overview_fall_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isFallLatched) {
                        stringResource(id = R.string.st_wesu_overview_fall_detected)
                    } else {
                        stringResource(id = R.string.st_wesu_overview_fall_none)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = if (isFallLatched) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        if (isFallLatched) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { isFallLatched = false }
                ) {
                    Text(text = stringResource(id = R.string.st_wesu_overview_ok_button))
                }
                Spacer(modifier = Modifier.width(LocalDimensions.current.paddingNormal))
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = stringResource(id = R.string.st_wesu_overview_help_button))
                }
            }
            Spacer(modifier = Modifier.height(LocalDimensions.current.paddingNormal))
        }
    }
}
