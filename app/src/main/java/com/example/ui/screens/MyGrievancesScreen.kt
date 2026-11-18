package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.Complaint
import com.example.data.firebase.StakeholderRole
import com.example.ui.components.GrievanceStatusBucket
import com.example.ui.components.grievanceStatusBucket
import com.example.ui.theme.StatusProgressBg
import com.example.ui.theme.StatusProgressFg
import com.example.ui.theme.StatusResolvedBg
import com.example.ui.theme.StatusResolvedFg
import com.example.ui.theme.StatusSubmittedBg
import com.example.ui.theme.StatusSubmittedFg
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun MyGrievancesScreen(
  viewModel: GramConnectViewModel,
  onBack: () -> Unit,
  onNavigateToFileComplaint: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val grievancesState by viewModel.mySubmittedGrievances.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val role = StakeholderRole.safeValueOf(profile?.role)
  val allMyGrievances = (grievancesState as? UiState.Success)?.data.orEmpty()

  var selectedBucket by remember { mutableStateOf(GrievanceStatusBucket.ALL) }
  var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }

  val pendingCount = remember(allMyGrievances) {
    allMyGrievances.count { grievanceStatusBucket(it.status) == GrievanceStatusBucket.PENDING }
  }
  val inProgressCount = remember(allMyGrievances) {
    allMyGrievances.count { grievanceStatusBucket(it.status) == GrievanceStatusBucket.IN_PROGRESS }
  }
  val resolvedCount = remember(allMyGrievances) {
    allMyGrievances.count { grievanceStatusBucket(it.status) == GrievanceStatusBucket.RESOLVED }
  }

  val filteredGrievances = remember(allMyGrievances, selectedBucket) {
    if (selectedBucket == GrievanceStatusBucket.ALL) {
      allMyGrievances
    } else {
      allMyGrievances.filter { grievanceStatusBucket(it.status) == selectedBucket }
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(48.dp).testTag("my_grievances_back_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isTelugu) "నా ఫిర్యాదులు & స్థితి" else "My Submitted Grievances",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Live Firestore status • Pending, In Progress & Resolved",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("my_grievances_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          StatusSummaryTile(
            label = "Pending",
            count = pendingCount,
            bgColor = StatusSubmittedBg,
            fgColor = StatusSubmittedFg,
            icon = Icons.Filled.PendingActions,
            modifier = Modifier.weight(1f),
          )
          StatusSummaryTile(
            label = "In Progress",
            count = inProgressCount,
            bgColor = StatusProgressBg,
            fgColor = StatusProgressFg,
            icon = Icons.Filled.HourglassTop,
            modifier = Modifier.weight(1f),
          )
          StatusSummaryTile(
            label = "Resolved",
            count = resolvedCount,
            bgColor = StatusResolvedBg,
            fgColor = StatusResolvedFg,
            icon = Icons.Filled.CheckCircle,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GrievanceStatusBucket.entries.forEach { bucket ->
            val count =
              when (bucket) {
                GrievanceStatusBucket.ALL -> allMyGrievances.size
                GrievanceStatusBucket.PENDING -> pendingCount
                GrievanceStatusBucket.IN_PROGRESS -> inProgressCount
                GrievanceStatusBucket.RESOLVED -> resolvedCount
              }
            FilterChip(
              selected = selectedBucket == bucket,
              onClick = { selectedBucket = bucket },
              label = { Text("${bucket.label} ($count)") },
              modifier = Modifier.testTag("my_grievances_filter_${bucket.name.lowercase()}"),
            )
          }
        }
      }

      when (val state = grievancesState) {
        is UiState.Loading -> {
          item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          if (filteredGrievances.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("my_grievances_empty_state"),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                  )
                  Spacer(Modifier.height(10.dp))
                  Text(
                    text =
                      if (allMyGrievances.isEmpty()) "No grievances submitted yet"
                      else "No grievances in ${selectedBucket.label} status",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Spacer(Modifier.height(6.dp))
                  Text(
                    text = "Submit a public grievance with description, category, and location to track its live status here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(Modifier.height(14.dp))
                  Button(
                    onClick = onNavigateToFileComplaint,
                    modifier = Modifier.testTag("empty_state_file_grievance_button"),
                  ) {
                    Icon(Icons.Filled.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Grievance")
                  }
                }
              }
            }
          } else {
            items(filteredGrievances, key = { it.id }) { complaint ->
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
private fun StatusSummaryTile(
  label: String,
  count: Int,
  bgColor: androidx.compose.ui.graphics.Color,
  fgColor: androidx.compose.ui.graphics.Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = bgColor,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(20.dp))
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleLarge,
        color = fgColor,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fgColor,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
    }
  }
}
