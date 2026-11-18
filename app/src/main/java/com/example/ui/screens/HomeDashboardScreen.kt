package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.Complaint
import com.example.data.firebase.ProblemDepartment
import com.example.data.firebase.StakeholderRole
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StageStepperBar
import com.example.ui.components.StatusPill
import com.example.ui.components.TelanganaDistrictMapCard
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun HomeDashboardScreen(
  viewModel: GramConnectViewModel,
  onNavigateToFileComplaint: () -> Unit,
  onNavigateToMyGrievances: () -> Unit,
  onNavigateToBoards: () -> Unit,
  onNavigateToSchemes: () -> Unit,
  onNavigateToGovernmentSchemes: () -> Unit,
  onNavigateToAi: () -> Unit,
  onOpenNotifications: () -> Unit,
) {
  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val complaintsState by viewModel.mySubmittedGrievances.collectAsStateWithLifecycle()
  val districts by viewModel.districts.collectAsStateWithLifecycle()
  val selectedDistrictId by viewModel.selectedDistrictId.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val role = StakeholderRole.safeValueOf(profile?.role)
  val selectedDistrict = districts.firstOrNull { it.districtId == selectedDistrictId } ?: districts.firstOrNull()
  val complaints = (complaintsState as? UiState.Success)?.data.orEmpty()
  val unreadCount =
    (notificationsState as? UiState.Success)?.data?.count { !it.readStatus } ?: 0

  var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      // Civic Hero Header Card
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldVibrant)))
              .padding(18.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  color = SaffronAccent,
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = if (isTelugu) role.labelTe else "${role.label} • ${role.tierLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                  text =
                    if (isTelugu) "నమస్కారం, ${profile?.displayName ?: "పౌరులు"}"
                    else "Namaskaram, ${profile?.displayName ?: "Citizen"}",
                  style = MaterialTheme.typography.headlineSmall,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    "${selectedDistrict?.name ?: "Warangal"} • ${profile?.wardId ?: "Ward 1"} • Telangana State",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { viewModel.toggleLanguage() },
                  modifier = Modifier.testTag("language_toggle_button"),
                ) {
                  Icon(
                    Icons.Filled.Translate,
                    contentDescription = "Switch English / Telugu",
                    tint = Color.White,
                  )
                }
                IconButton(
                  onClick = onOpenNotifications,
                  modifier = Modifier.testTag("notifications_button"),
                ) {
                  BadgedBox(
                    badge = {
                      if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) }
                      }
                    }
                  ) {
                    Icon(
                      Icons.Filled.Notifications,
                      contentDescription = stringResource(R.string.cd_notifications),
                      tint = Color.White,
                    )
                  }
                }
              }
            }

            Spacer(Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              HeaderStatPill(
                label = if (isTelugu) "మొత్తం సమస్యలు" else "My / Active",
                value = complaints.size.toString(),
                modifier = Modifier.weight(1f),
              )
              HeaderStatPill(
                label = if (isTelugu) "పెండింగ్" else "In Workflow",
                value = complaints.count { it.status != "RESOLVED" && it.status != "REJECTED" }.toString(),
                modifier = Modifier.weight(1f),
              )
              HeaderStatPill(
                label = if (isTelugu) "పరిష్కారం" else "Resolved",
                value = complaints.count { it.status == "RESOLVED" }.toString(),
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    item {
      // Quick Action Tiles
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = if (isTelugu) "ప్రధాన సేవలు" else "Citizen & Governance Actions",
          style = MaterialTheme.typography.titleMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionTile(
            title = if (isTelugu) "ఫిర్యాదు నమోదు" else "File Grievance",
            subtitle = "Photo, GPS & Voice AI",
            icon = Icons.Filled.AddAlert,
            accent = SaffronAccent,
            onClick = onNavigateToFileComplaint,
            modifier = Modifier.weight(1f).testTag("quick_file_grievance"),
          )
          QuickActionTile(
            title = if (isTelugu) "పథకాలు & రేషన్" else "Schemes & PDS",
            subtitle = "Welfare & Ration Card",
            icon = Icons.Filled.CardGiftcard,
            accent = EmeraldDeep,
            onClick = onNavigateToSchemes,
            modifier = Modifier.weight(1f).testTag("quick_schemes_ration"),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionTile(
            title = if (isTelugu) "సమస్యల బోర్డు" else "Problem Boards",
            subtitle = "Ward → Mandal → District",
            icon = Icons.Filled.Campaign,
            accent = EmeraldVibrant,
            onClick = onNavigateToBoards,
            modifier = Modifier.weight(1f).testTag("quick_problem_boards"),
          )
          QuickActionTile(
            title = if (isTelugu) "గ్రామసహాయక్ AI" else "GramSahayak AI",
            subtitle = "Bilingual Voice & Chat",
            icon = Icons.Filled.AutoAwesome,
            accent = SaffronAccent,
            onClick = onNavigateToAi,
            modifier = Modifier.weight(1f).testTag("quick_gram_sahayak"),
          )
        }
        Card(
          modifier =
            Modifier.fillMaxWidth()
              .clickable(onClick = onNavigateToGovernmentSchemes)
              .testTag("quick_government_schemes_directory"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isTelugu) "ప్రభుత్వ పథకాల జాబితా & చివరి తేదీలు" else "Government Schemes & Deadlines Directory",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Check Telangana State & Central eligibility criteria and cutoffs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = "Open →",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    item {
      TelanganaDistrictMapCard(
        districts = districts,
        selectedDistrict = selectedDistrict,
        complaints = complaints,
        isTelugu = isTelugu,
        onSelectDistrict = { viewModel.selectDistrict(it.districtId) },
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isTelugu) "నా ఫిర్యాదుల ట్రాకింగ్" else "My Submitted Grievances",
          style = MaterialTheme.typography.titleMedium,
        )
        TextButton(
          onClick = onNavigateToMyGrievances,
          modifier = Modifier.testTag("view_all_my_grievances_button"),
        ) {
          Text(if (isTelugu) "అన్నీ చూడండి" else "View All & Filter")
        }
      }
    }

    when (val state = complaintsState) {
      is UiState.Loading -> {
        item {
          Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      }
      is UiState.Error -> {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        if (state.data.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "No grievances filed yet",
                  style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                  text = "Tap 'File Grievance' to report a village issue with voice transcription, GPS, or photo evidence.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(state.data.take(8), key = { it.id }) { complaint ->
            ComplaintSummaryCard(
              complaint = complaint,
              isTelugu = isTelugu,
              onClick = { selectedComplaint = complaint },
            )
          }
        }
      }
    }
  }

  selectedComplaint?.let { comp ->
    ComplaintDetailDialog(
      complaint = comp,
      role = role,
      viewModel = viewModel,
      onDismiss = { selectedComplaint = null },
    )
  }
}

@Composable
private fun HeaderStatPill(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    color = Color.White.copy(alpha = 0.14f),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
    }
  }
}

@Composable
private fun QuickActionTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = accent.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
        }
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ComplaintSummaryCard(
  complaint: Complaint,
  isTelugu: Boolean,
  onClick: () -> Unit,
) {
  val dept = ProblemDepartment.safeValueOf(complaint.category)
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("complaint_card_${complaint.trackingCode}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = complaint.trackingCode,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PriorityBadge(complaint.priority)
          StatusPill(complaint.status)
        }
      }

      Text(
        text = complaint.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = "${if (isTelugu) dept.titleTe else dept.title} • ${complaint.villageName}, ${complaint.mandalName}, ${complaint.districtName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      StageStepperBar(currentStageRaw = complaint.currentStage)

      if (!complaint.latestActionNote.isNullOrBlank()) {
        Text(
          text = "Latest: ${complaint.latestActionNote}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
