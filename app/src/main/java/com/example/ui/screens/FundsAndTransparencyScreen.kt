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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.StakeholderRole
import com.example.ui.components.AnalyticsBarChart
import com.example.ui.components.formatInr
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun FundsAndTransparencyScreen(viewModel: GramConnectViewModel) {
  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val fundRequestsState by viewModel.fundRequests.collectAsStateWithLifecycle()
  val projectsState by viewModel.publicProjects.collectAsStateWithLifecycle()
  val feedbackState by viewModel.feedbackList.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val role = StakeholderRole.safeValueOf(profile?.role)
  val isBootstrappedAdmin = profile?.email == "varunbanoth14@gmail.com"
  val canApproveFunds =
    isBootstrappedAdmin ||
      (profile?.verificationStatus == "VERIFIED" &&
        role in
        listOf(
          StakeholderRole.MANDAL_OFFICER,
          StakeholderRole.MANDAL_HEAD,
          StakeholderRole.DISTRICT_OFFICER,
          StakeholderRole.DISTRICT_HEAD,
          StakeholderRole.GOVT_ADMIN,
        ))

  val fundRequests = (fundRequestsState as? UiState.Success)?.data.orEmpty()
  val projects = (projectsState as? UiState.Success)?.data.orEmpty()
  val feedbackList = (feedbackState as? UiState.Success)?.data.orEmpty()

  var selectedTab by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    TabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Public Works & Feedback") },
        modifier = Modifier.testTag("tab_public_works"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Fund Sanctions (${fundRequests.size})") },
        modifier = Modifier.testTag("tab_fund_sanctions"),
      )
    }

    when (selectedTab) {
      0 -> {
        var rating by remember { mutableIntStateOf(5) }
        var feedbackTargetType by remember { mutableStateOf("DEVELOPMENT_PROJECT") }
        var feedbackTitle by remember { mutableStateOf("Village Infrastructure & Water Works") }
        var feedbackComment by remember { mutableStateOf("") }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            AnalyticsBarChart(
              title = "Sanctioned Public Works Budgets (₹ Lakhs)",
              items = projects.map { it.districtName to (it.sanctionedAmountInr / 100_000f).coerceAtLeast(1f) },
              barColor = EmeraldDeep,
            )
          }

          item {
            Text("Active Village Development Works", style = MaterialTheme.typography.titleMedium)
          }

          items(projects, key = { it.id.ifBlank { it.title } }) { proj ->
            val progress = (proj.progressPercent.toFloat() / 100f).coerceIn(0f, 1f)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(proj.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                  Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                    Text(
                      proj.status,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                }
                Text(
                  "${proj.villageName}, ${proj.mandalName}, ${proj.districtName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(proj.summary, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Spent: ${formatInr(proj.expenditureInr)}", style = MaterialTheme.typography.labelMedium)
                  Text("Sanctioned: ${formatInr(proj.sanctionedAmountInr)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Citizen Accountability & Rating", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf(
                    "DEVELOPMENT_PROJECT" to "Project",
                    "COMPLAINT_RESOLUTION" to "Resolution",
                    "REPRESENTATIVE_SERVICE" to "Officer",
                  ).forEach { (type, label) ->
                    FilterChip(
                      selected = feedbackTargetType == type,
                      onClick = { feedbackTargetType = type },
                      label = { Text(label) },
                    )
                  }
                }
                OutlinedTextField(
                  value = feedbackTitle,
                  onValueChange = { feedbackTitle = it },
                  label = { Text("Work / Service Evaluated") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("Rating:", style = MaterialTheme.typography.labelLarge)
                  Spacer(Modifier.width(8.dp))
                  (1..5).forEach { star ->
                    IconButton(onClick = { rating = star }, modifier = Modifier.size(48.dp)) {
                      Icon(
                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$star stars",
                        tint = SaffronAccent,
                      )
                    }
                  }
                }
                OutlinedTextField(
                  value = feedbackComment,
                  onValueChange = { feedbackComment = it },
                  label = { Text("Citizen Verification Remarks") },
                  minLines = 2,
                  modifier = Modifier.fillMaxWidth().testTag("feedback_comment_input"),
                )
                Button(
                  onClick = {
                    if (feedbackComment.trim().length >= 3) {
                      viewModel.submitFeedback(
                        targetId = "fb_${System.currentTimeMillis()}",
                        targetTitle = feedbackTitle,
                        targetType = feedbackTargetType,
                        districtName = profile?.districtId?.replaceFirstChar(Char::uppercaseChar) ?: "Warangal",
                        villageName = profile?.villageId?.replaceFirstChar(Char::uppercaseChar) ?: "Ilanda",
                        rating = rating,
                        comment = feedbackComment,
                      )
                      feedbackComment = ""
                    } else {
                      viewModel.showMessage("Enter at least 3 characters for your review.")
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag("submit_feedback_button"),
                ) { Text("Publish Citizen Feedback") }
              }
            }
          }

          if (feedbackList.isNotEmpty()) {
            item { Text("Recent Public Ratings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(feedbackList, key = { it.id }) { fb ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fb.targetTitle, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("${fb.rating}/5 ★", style = MaterialTheme.typography.labelLarge, color = SaffronAccent, fontWeight = FontWeight.Bold)
                  }
                  Text(fb.comment, style = MaterialTheme.typography.bodySmall)
                  Text(
                    "${fb.villageName}, ${fb.districtName} • ${fb.targetType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (fundRequests.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(20.dp)) {
                  Text("No fund proposals in your queue", style = MaterialTheme.typography.titleSmall)
                  Spacer(Modifier.height(4.dp))
                  Text(
                    "Verified Village, Mandal, and District Officers can raise fund requests directly from any complaint's detail dialog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(fundRequests, key = { it.id }) { req ->
              Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(req.projectTitle, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                      Text(
                        "${req.approvalStage} • ${req.status}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  }
                  Text("Source: ${req.fundSource} • By ${req.requesterName}", style = MaterialTheme.typography.bodySmall)
                  Text(
                    "Requested: ${formatInr(req.requestedAmountInr)} • Sanctioned: ${formatInr(req.sanctionedAmountInr)} • Spent: ${formatInr(req.expenditureInr)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  if (canApproveFunds && req.status != "COMPLETED" && req.status != "REJECTED") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedButton(
                        onClick = {
                          viewModel.advanceFundRequest(
                            fundRequest = req,
                            nextStage = "DISTRICT",
                            nextStatus = "MANDAL_RECOMMENDED",
                            sanctionedAmountInr = req.sanctionedAmountInr,
                            expenditureInr = req.expenditureInr,
                            remarks = "Recommended by Mandal Officer",
                          )
                        }
                      ) { Text("Recommend") }
                      Button(
                        onClick = {
                          viewModel.advanceFundRequest(
                            fundRequest = req,
                            nextStage = "STATE",
                            nextStatus = "SANCTIONED",
                            sanctionedAmountInr = req.requestedAmountInr,
                            expenditureInr = req.expenditureInr,
                            remarks = "Funds sanctioned for execution",
                          )
                        }
                      ) { Text("Sanction Budget") }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
