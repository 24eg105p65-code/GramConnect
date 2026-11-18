package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.GovernmentScheme
import com.example.ui.components.AnalyticsBarChart
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun SchemesAndRationScreen(
  viewModel: GramConnectViewModel,
  onNavigateToAi: () -> Unit,
  onOpenSchemesDirectory: () -> Unit = {},
) {
  val schemesState by viewModel.schemes.collectAsStateWithLifecycle()
  val applicationsState by viewModel.schemeApplications.collectAsStateWithLifecycle()
  val rationSummariesState by viewModel.rationSummaries.collectAsStateWithLifecycle()
  val rationRecordsState by viewModel.rationRecords.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var levelFilter by remember { mutableStateOf("ALL") }

  val schemes = (schemesState as? UiState.Success)?.data.orEmpty()
  val myApps = (applicationsState as? UiState.Success)?.data.orEmpty()
  val rationSummaries = (rationSummariesState as? UiState.Success)?.data.orEmpty()
  val myRationRecords = (rationRecordsState as? UiState.Success)?.data.orEmpty()

  Column(modifier = Modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text(if (isTelugu) "పథకాలు" else "Welfare Schemes") },
        modifier = Modifier.testTag("tab_welfare_schemes"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text(if (isTelugu) "రేషన్ & PDS" else "Ration & PDS") },
        modifier = Modifier.testTag("tab_ration_pds"),
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("Analytics") },
        modifier = Modifier.testTag("tab_welfare_analytics"),
      )
    }

    when (selectedTab) {
      0 -> {
        val filteredSchemes =
          schemes.filter { levelFilter == "ALL" || it.level == levelFilter }
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = levelFilter == "ALL", onClick = { levelFilter = "ALL" }, label = { Text("All") })
                FilterChip(
                  selected = levelFilter == "TELANGANA_STATE",
                  onClick = { levelFilter = "TELANGANA_STATE" },
                  label = { Text("Telangana") },
                )
                FilterChip(selected = levelFilter == "CENTRAL", onClick = { levelFilter = "CENTRAL" }, label = { Text("Central") })
              }
              OutlinedButton(onClick = onNavigateToAi) { Text("Ask AI") }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
              onClick = onOpenSchemesDirectory,
              modifier = Modifier.fillMaxWidth().testTag("open_schemes_directory_button"),
            ) {
              Text("Open Full Schemes & Deadlines Screen →")
            }
          }

          if (myApps.isNotEmpty()) {
            item {
              Text("My Tracked Scheme Applications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(myApps, key = { it.id }) { app ->
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(app.schemeTitle, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "Ref: ${app.referenceNumber} • ${app.villageName}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50)) {
                    Text(
                      app.applicationStatus,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }

          items(filteredSchemes, key = { it.id.ifBlank { it.title } }) { scheme ->
            SchemeCard(
              scheme = scheme,
              onTrackApplication = { village, ref ->
                viewModel.applyForSchemeTracking(scheme, village, ref)
              },
            )
          }
        }
      }

      1 -> {
        var cardRef by remember { mutableStateOf("WAP-36-99412") }
        var shopCode by remember { mutableStateOf("FPS-WGL-104") }
        var monthLabel by remember { mutableStateOf("Nov 2026") }
        var entitledKgText by remember { mutableStateOf("18") }
        var receivedKgText by remember { mutableStateOf("18") }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Log Monthly PDS Ration Entitlement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedTextField(
                    value = cardRef,
                    onValueChange = { cardRef = it },
                    label = { Text("Ration Card Ref") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedTextField(
                    value = shopCode,
                    onValueChange = { shopCode = it },
                    label = { Text("FPS Shop Code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedTextField(
                    value = monthLabel,
                    onValueChange = { monthLabel = it },
                    label = { Text("Month") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedTextField(
                    value = entitledKgText,
                    onValueChange = { entitledKgText = it.filter(Char::isDigit) },
                    label = { Text("Entitled kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.7f),
                  )
                  OutlinedTextField(
                    value = receivedKgText,
                    onValueChange = { receivedKgText = it.filter(Char::isDigit) },
                    label = { Text("Received kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.7f),
                  )
                }
                Button(
                  onClick = {
                    viewModel.addRationRecord(
                      cardReference = cardRef,
                      shopCode = shopCode,
                      monthLabel = monthLabel,
                      entitledKg = entitledKgText.toLongOrNull() ?: 15L,
                      receivedKg = receivedKgText.toLongOrNull() ?: 0L,
                    )
                  },
                  modifier = Modifier.fillMaxWidth().testTag("save_ration_record_button"),
                ) {
                  Icon(Icons.Filled.AddCircle, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Save Ration Verification Entry")
                }
              }
            }
          }

          if (myRationRecords.isNotEmpty()) {
            item {
              Text("My Personal Ration History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(myRationRecords, key = { it.id }) { rec ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("${rec.monthLabel} • ${rec.shopCode}", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "Card ${rec.cardReference} • ${rec.receivedRiceKg}/${rec.entitledRiceKg} kg received",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                  ) {
                    Text(
                      rec.distributionStatus,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }

          item {
            Text("Village Fair Price Shop (FPS) Public Summaries", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }

          items(rationSummaries, key = { it.id.ifBlank { it.shopCode } }) { summary ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("${summary.shopCode} (${summary.villageName})", style = MaterialTheme.typography.titleSmall)
                  Text(summary.monthLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                  "Mandal: ${summary.mandalName} • Active Cards: ${summary.activeCards}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  "Rice Distributed: ${summary.distributedRiceKg} kg / ${summary.allocatedRiceKg} kg • Open Issues: ${summary.pendingComplaints}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                )
              }
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            AnalyticsBarChart(
              title = "Telangana & Central Scheme Reach (Lakhs Beneficiaries)",
              items = schemes.map { it.title.substringBefore("–").trim() to (it.beneficiaryCount / 100_000f).coerceAtLeast(1f) },
              barColor = EmeraldDeep,
            )
          }
          item {
            AnalyticsBarChart(
              title = "FPS Rice Distribution Fulfillment (%)",
              items =
                rationSummaries.map {
                  val pct = if (it.allocatedRiceKg > 0) (it.distributedRiceKg * 100f / it.allocatedRiceKg) else 0f
                  it.shopCode to pct
                },
              barColor = SaffronAccent,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SchemeCard(
  scheme: GovernmentScheme,
  onTrackApplication: (String, String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var refInput by remember { mutableStateOf("") }
  var villageInput by remember { mutableStateOf("Ilanda") }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color =
            if (scheme.level == "TELANGANA_STATE") MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = if (scheme.level == "TELANGANA_STATE") "Telangana State" else "Central Govt",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        IconButton(onClick = { expanded = !expanded }) {
          Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = "Toggle scheme details",
          )
        }
      }

      Text(scheme.title, style = MaterialTheme.typography.titleMedium)
      Text(scheme.benefitsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = "Deadline: ${scheme.applicationDeadline.ifBlank { "Open / Rolling" }}",
        style = MaterialTheme.typography.labelMedium,
        color = SaffronAccent,
        fontWeight = FontWeight.SemiBold,
      )

      if (expanded) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Eligibility: ${scheme.eligibilityCriteria}", style = MaterialTheme.typography.bodySmall)
        Text("Required Docs: ${scheme.requiredDocuments}", style = MaterialTheme.typography.bodySmall)
        Text("Process: ${scheme.applicationProcess}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = villageInput,
            onValueChange = { villageInput = it },
            label = { Text("Village") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = refInput,
            onValueChange = { refInput = it },
            label = { Text("MeeSeva / App Ref") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Button(
          onClick = { onTrackApplication(villageInput.ifBlank { "Ilanda" }, refInput) },
          modifier = Modifier.fillMaxWidth().testTag("track_scheme_${scheme.id}"),
        ) {
          Text("Track Application Reference")
        }
      }
    }
  }
}
