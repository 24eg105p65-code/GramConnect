package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.GovernmentScheme
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusResolvedBg
import com.example.ui.theme.StatusResolvedFg
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun GovernmentSchemesScreen(
  viewModel: GramConnectViewModel,
  onBack: () -> Unit,
  onNavigateToAi: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val schemesState by viewModel.schemes.collectAsStateWithLifecycle()
  val applicationsState by viewModel.schemeApplications.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var selectedLevel by remember { mutableStateOf("ALL") }
  var selectedCategory by remember { mutableStateOf("ALL") }
  var detailScheme by remember { mutableStateOf<GovernmentScheme?>(null) }

  val allSchemes = (schemesState as? UiState.Success)?.data.orEmpty()
  val myApplications = (applicationsState as? UiState.Success)?.data.orEmpty()
  val categories = remember(allSchemes) { listOf("ALL") + allSchemes.map { it.category }.distinct().filter { it.isNotBlank() } }

  val filteredSchemes =
    remember(allSchemes, searchQuery, selectedLevel, selectedCategory) {
      val q = searchQuery.trim().lowercase()
      allSchemes.filter { s ->
        val matchesLevel = selectedLevel == "ALL" || s.level == selectedLevel
        val matchesCat = selectedCategory == "ALL" || s.category == selectedCategory
        val matchesSearch =
          q.isEmpty() ||
            s.title.lowercase().contains(q) ||
            s.department.lowercase().contains(q) ||
            s.eligibilityCriteria.lowercase().contains(q) ||
            s.benefitsSummary.lowercase().contains(q)
        matchesLevel && matchesCat && matchesSearch
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
          modifier = Modifier.size(48.dp).testTag("schemes_screen_back_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isTelugu) "ప్రభుత్వ పథకాలు & అర్హత" else "Government Schemes Directory",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Fetched from Firestore • Eligibility, documents & application deadlines",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onNavigateToAi,
          modifier = Modifier.size(48.dp).testTag("schemes_ask_ai_button"),
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = "Ask GramSahayak AI", tint = SaffronAccent)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("government_schemes_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          placeholder = { Text("Search schemes, eligibility, or department...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("schemes_search_input"),
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(selected = selectedLevel == "ALL", onClick = { selectedLevel = "ALL" }, label = { Text("All (${allSchemes.size})") })
          FilterChip(
            selected = selectedLevel == "TELANGANA_STATE",
            onClick = { selectedLevel = "TELANGANA_STATE" },
            label = { Text("Telangana State") },
          )
          FilterChip(
            selected = selectedLevel == "CENTRAL",
            onClick = { selectedLevel = "CENTRAL" },
            label = { Text("Central Govt") },
          )
        }
      }

      if (categories.size > 1) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            categories.forEach { cat ->
              FilterChip(
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = { Text(if (cat == "ALL") "All Categories" else cat) },
              )
            }
          }
        }
      }

      when (val state = schemesState) {
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
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          if (filteredSchemes.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(20.dp)) {
                  Text("No matching government schemes found", style = MaterialTheme.typography.titleSmall)
                  Spacer(Modifier.height(4.dp))
                  Text(
                    "Adjust your search keywords or clear the level/category filters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(filteredSchemes, key = { it.id.ifBlank { it.title } }) { scheme ->
              val isTracked = myApplications.any { it.schemeId == scheme.id || it.schemeTitle == scheme.title }
              GovernmentSchemeDirectoryCard(
                scheme = scheme,
                isTracked = isTracked,
                onOpenDetails = { detailScheme = scheme },
              )
            }
          }
        }
      }
    }
  }

  detailScheme?.let { scheme ->
    SchemeDetailDialog(
      scheme = scheme,
      onDismiss = { detailScheme = null },
      onTrackApplication = { village, ref ->
        viewModel.applyForSchemeTracking(scheme, village, ref)
        detailScheme = null
      },
      onAskAi = {
        detailScheme = null
        onNavigateToAi()
      },
    )
  }
}

@Composable
private fun GovernmentSchemeDirectoryCard(
  scheme: GovernmentScheme,
  isTracked: Boolean,
  onOpenDetails: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onOpenDetails)
        .testTag("scheme_directory_card_${scheme.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
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
          Surface(color = StatusResolvedBg, shape = RoundedCornerShape(50)) {
            Text(
              text = scheme.activeStatus,
              color = StatusResolvedFg,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        if (isTracked) {
          Icon(
            Icons.Filled.CheckCircle,
            contentDescription = "Tracked application",
            tint = EmeraldDeep,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Text(scheme.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(
        text = "${scheme.department} • ${scheme.category}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = scheme.benefitsSummary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Eligibility: ${scheme.eligibilityCriteria}",
              style = MaterialTheme.typography.bodySmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = SaffronAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Application Deadline: ${scheme.applicationDeadline.ifBlank { "Open / Rolling" }}",
              style = MaterialTheme.typography.labelMedium,
              color = SaffronAccent,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = "${"%,d".format(scheme.beneficiaryCount)} beneficiaries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "View Details & Apply →",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun SchemeDetailDialog(
  scheme: GovernmentScheme,
  onDismiss: () -> Unit,
  onTrackApplication: (String, String) -> Unit,
  onAskAi: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var villageName by remember { mutableStateOf("Ilanda") }
  var refNo by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 720.dp),
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (scheme.level == "TELANGANA_STATE") "Telangana State Scheme" else "Central Government Scheme",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Text(scheme.title, style = MaterialTheme.typography.titleLarge)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close scheme details")
          }
        }

        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), shape = RoundedCornerShape(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = SaffronAccent)
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Deadline: ${scheme.applicationDeadline.ifBlank { "Open / Rolling" }}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Text("Benefits", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(scheme.benefitsSummary, style = MaterialTheme.typography.bodyMedium)

        Text("Eligibility Criteria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(scheme.eligibilityCriteria, style = MaterialTheme.typography.bodyMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldDeep)
          Spacer(Modifier.width(6.dp))
          Text("Required Documents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(scheme.requiredDocuments, style = MaterialTheme.typography.bodySmall)

        Text("Application Process", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(scheme.applicationProcess, style = MaterialTheme.typography.bodySmall)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Track My Scheme Application", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = villageName,
            onValueChange = { villageName = it },
            label = { Text("Village / GP") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = refNo,
            onValueChange = { refNo = it },
            label = { Text("MeeSeva / App Ref") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onAskAi, modifier = Modifier.weight(1f)) {
            Text("Ask AI")
          }
          Button(
            onClick = { onTrackApplication(villageName.ifBlank { "Ilanda" }, refNo) },
            modifier = Modifier.weight(1.4f).testTag("dialog_track_scheme_button"),
          ) {
            Text("Save Tracking")
          }
        }
      }
    }
  }
}
