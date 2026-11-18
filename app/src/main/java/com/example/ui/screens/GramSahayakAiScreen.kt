package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.AiModelTier
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel

@Composable
fun GramSahayakAiScreen(viewModel: GramConnectViewModel) {
  val context = LocalContext.current
  val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val isReplying by viewModel.isAiReplying.collectAsStateWithLifecycle()
  val aiError by viewModel.aiError.collectAsStateWithLifecycle()
  val selectedTier by viewModel.aiTier.collectAsStateWithLifecycle()
  val isRecording by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
  val isTranscribing by viewModel.isTranscribingVoice.collectAsStateWithLifecycle()

  var inputPrompt by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) viewModel.startVoiceRecording()
      else viewModel.showMessage("Microphone permission required to speak with GramSahayak.")
    }

  LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SaffronAccent)
          Spacer(Modifier.width(8.dp))
          Text("GramSahayak AI (Telugu & English)", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AiModelTier.entries.forEach { tier ->
            FilterChip(
              selected = selectedTier == tier,
              onClick = { viewModel.setAiTier(tier) },
              label = { Text(tier.label) },
              modifier = Modifier.testTag("ai_tier_${tier.name.lowercase()}"),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(
            "Rythu Bharosa eligibility?",
            "Gruha Jyothi 200 units documents?",
            "How does grievance escalation work?",
            "తెలుగులో రేషన్ కార్డు ఫిర్యాదు వివరించండి",
          ).forEach { quick ->
            AssistChip(
              onClick = { viewModel.sendChatMessage(quick) },
              label = { Text(quick) },
            )
          }
        }
      }
    }

    if (aiError != null) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
      ) {
        Text(
          text = aiError!!,
          color = MaterialTheme.colorScheme.onErrorContainer,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(10.dp),
        )
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = if (msg.isFromUser) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
          Surface(
            color =
              if (msg.isFromUser) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surface,
            contentColor =
              if (msg.isFromUser) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = if (msg.isFromUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = if (msg.isFromUser) "Citizen" else "GramSahayak",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                  if (msg.isFromUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                  else SaffronAccent,
              )
              Spacer(Modifier.height(3.dp))
              if (msg.isStreaming && msg.text.isBlank()) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Text(msg.text, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      }
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = {
            if (isRecording) {
              viewModel.stopVoiceRecordingAndTranscribe(forChatOnly = true)
            } else {
              val hasMic =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                  PackageManager.PERMISSION_GRANTED
              if (hasMic) viewModel.startVoiceRecording()
              else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          },
          enabled = !isTranscribing && !isReplying,
          colors =
            IconButtonDefaults.iconButtonColors(
              containerColor =
                if (isRecording) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondaryContainer,
              contentColor =
                if (isRecording) MaterialTheme.colorScheme.onError
                else MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          modifier = Modifier.size(48.dp).testTag("ai_voice_button"),
        ) {
          Icon(
            if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Speak question",
          )
        }

        OutlinedTextField(
          value = inputPrompt,
          onValueChange = { inputPrompt = it },
          placeholder = {
            Text(
              if (isRecording) "Listening... tap stop to ask"
              else if (isTranscribing) "Transcribing audio..."
              else "Ask about schemes, ration, or escalation..."
            )
          },
          maxLines = 3,
          modifier = Modifier.weight(1f).testTag("ai_chat_input"),
        )

        IconButton(
          onClick = {
            val txt = inputPrompt.trim()
            if (txt.isNotEmpty()) {
              inputPrompt = ""
              viewModel.sendChatMessage(txt)
            }
          },
          enabled = inputPrompt.isNotBlank() && !isReplying,
          colors =
            IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.size(48.dp).testTag("ai_send_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
        }
      }
    }
  }
}
