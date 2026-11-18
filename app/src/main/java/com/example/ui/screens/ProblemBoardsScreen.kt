package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.firebase.Complaint
import com.example.data.firebase.ComplaintStatus
import com.example.data.firebase.ProblemDepartment
import com.example.data.firebase.StakeholderRole
import com.example.data.firebase.WorkflowStage
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StageStepperBar
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

private enum class GovernanceBoardFilter(val label: String) {
  ALL("All Grievances"),
  MY_SUBMITTED("My Submitted"),
  WARD_VILLAGE("Ward & Sarpanch"),
  MANDAL("Mandal Module"),
  DISTRICT_STATE("District & State"),
  FIELD_WORK("Field Tech Queue"),
  RESOLVED("Resolved"),
}

@Composable
fun ProblemBoardsScreen(viewModel: GramConnectViewModel) {
  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val complaintsState by viewModel.complaints.collectAsStateWithLifecycle()
  val myGrievancesState by viewModel.mySubmittedGrievances.collectAsStateWithLifecycle()
  val fieldTasksState by viewModel.fieldTasks.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val role = StakeholderRole.safeValueOf(profile?.role)
  val allComplaints = (complaintsState as? UiState.Success)?.data.orEmpty()
  val mySubmitted = (myGrievancesState as? UiState.Success)?.data.orEmpty()
  val assignedTasks = (fieldTasksState as? UiState.Success)?.data.orEmpty()

  var boardFilter by remember { mutableStateOf(GovernanceBoardFilter.ALL) }
  var selectedDept by remember { mutableStateOf<ProblemDepartment?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }

  val mergedList =
    remember(allComplaints, mySubmitted, assignedTasks, boardFilter, selectedDept, searchQuery) {
      val source =
        when (boardFilter) {
          GovernanceBoardFilter.MY_SUBMITTED -> mySubmitted
          GovernanceBoardFilter.FIELD_WORK ->
            (assignedTasks + allComplaints.filter { it.currentStage == WorkflowStage.FIELD_WORKER.name || it.status == ComplaintStatus.WORK_ASSIGNED.name || it.status == ComplaintStatus.IN_PROGRESS.name }).distinctBy { it.id }
          else -> allComplaints
        }
      source.filter { c ->
        val matchesBoard =
          when (boardFilter) {
            GovernanceBoardFilter.ALL,
            GovernanceBoardFilter.MY_SUBMITTED -> true
            GovernanceBoardFilter.WARD_VILLAGE ->
              c.currentStage in listOf(WorkflowStage.CITIZEN.name, WorkflowStage.WARD.name, WorkflowStage.VILLAGE.name)
            GovernanceBoardFilter.MANDAL -> c.currentStage == WorkflowStage.MANDAL.name
            GovernanceBoardFilter.DISTRICT_STATE ->
              c.currentStage in listOf(WorkflowStage.DISTRICT.name, WorkflowStage.STATE.name)
            GovernanceBoardFilter.FIELD_WORK -> true
            GovernanceBoardFilter.RESOLVED -> c.status == ComplaintStatus.RESOLVED.name
          }
        val matchesDept = selectedDept == null || c.category == selectedDept?.name
        val q = searchQuery.trim().lowercase()
        val matchesSearch =
          q.isEmpty() ||
            c.title.lowercase().contains(q) ||
            c.trackingCode.lowercase().contains(q) ||
            c.villageName.lowercase().contains(q) ||
            c.districtName.lowercase().contains(q)
        matchesBoard && matchesDept && matchesSearch
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = if (isTelugu) "సమస్యల పర్యవేక్షణ బోర్డులు" else "Governance Problem Boards",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = "Role: ${role.label} • Traceable Ward → Gram Panchayat → Mandal Smart Module → District → State escalation",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text("Search tracking ID, village, or title...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("boards_search_input"),
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GovernanceBoardFilter.entries.forEach { filter ->
          FilterChip(
            selected = boardFilter == filter,
            onClick = { boardFilter = filter },
            label = { Text(filter.label) },
            modifier = Modifier.testTag("board_filter_${filter.name.lowercase()}"),
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedDept == null,
          onClick = { selectedDept = null },
          label = { Text("All Depts") },
        )
        ProblemDepartment.entries.forEach { dept ->
          FilterChip(
            selected = selectedDept == dept,
            onClick = { selectedDept = if (selectedDept == dept) null else dept },
            label = { Text(if (isTelugu) dept.titleTe else dept.title) },
          )
        }
      }
    }

    when (complaintsState) {
      is UiState.Loading -> {
        item {
          Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
      }
      is UiState.Error -> {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = (complaintsState as UiState.Error).message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }
      is UiState.Success -> {
        if (mergedList.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(20.dp)) {
                Text("No matching grievances in this board", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                  "Try switching board filters or file a new complaint from the Home or File tab.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(mergedList, key = { it.id }) { complaint ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComplaintDetailDialog(
  complaint: Complaint,
  role: StakeholderRole,
  viewModel: GramConnectViewModel,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val context = LocalContext.current
  val history by viewModel.observeHistory(complaint.id).collectAsStateWithLifecycle(initialValue = emptyList())
  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val profile = (profileState as? UiState.Success)?.data
  val isBootstrappedAdmin = profile?.email == "varunbanoth14@gmail.com"
  val isVerifiedOfficerOrAdmin =
    isBootstrappedAdmin ||
      (profile?.verificationStatus == "VERIFIED" &&
        role in
        listOf(
          StakeholderRole.WARD_REP,
          StakeholderRole.SARPANCH,
          StakeholderRole.VILLAGE_OFFICER,
          StakeholderRole.MANDAL_OFFICER,
          StakeholderRole.MANDAL_HEAD,
          StakeholderRole.DISTRICT_OFFICER,
          StakeholderRole.DISTRICT_HEAD,
          StakeholderRole.GOVT_ADMIN,
        ))
  val isAssignedFieldWorker =
    complaint.assignedWorkerId == viewModel.currentUserId || role == StakeholderRole.FIELD_WORKER

  var actionRemark by remember { mutableStateOf("") }
  var showFundForm by remember { mutableStateOf(false) }
  var fundProjectTitle by remember(complaint.id) { mutableStateOf("${complaint.title} – Repair Estimate") }
  var fundSource by remember { mutableStateOf("15th Finance Commission / Gram Panchayat") }
  var fundEstimatedText by remember { mutableStateOf("85000") }
  var fundRequestedText by remember { mutableStateOf("85000") }

  val dept = ProblemDepartment.safeValueOf(complaint.category)
  val isClosed = complaint.status == ComplaintStatus.RESOLVED.name || complaint.status == ComplaintStatus.REJECTED.name

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 760.dp),
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = complaint.trackingCode,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Text(complaint.title, style = MaterialTheme.typography.titleLarge)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("close_complaint_dialog")) {
            Icon(Icons.Filled.Close, contentDescription = "Close details")
          }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PriorityBadge(complaint.priority)
          StatusPill(complaint.status)
        }

        Spacer(Modifier.height(12.dp))
        StageStepperBar(currentStageRaw = complaint.currentStage)
        Spacer(Modifier.height(12.dp))

        Text(
          text = "${dept.title} • ${complaint.subCategory}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "${complaint.villageName} (${complaint.wardId}), ${complaint.mandalName} Mandal, ${complaint.districtName} District",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(complaint.description, style = MaterialTheme.typography.bodyMedium)

        if (!complaint.aiSummary.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp)) {
            Text(
              text = "AI Triage Summary: ${complaint.aiSummary}",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (!complaint.photoUriString.isNullOrBlank()) {
          Spacer(Modifier.height(10.dp))
          AsyncImage(
            model = complaint.photoUriString,
            contentDescription = "Complaint evidence photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
          )
        }

        if (complaint.latitude != null && complaint.longitude != null) {
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              val uri =
                Uri.parse(
                  "geo:${complaint.latitude},${complaint.longitude}?q=${complaint.latitude},${complaint.longitude}(${Uri.encode(complaint.trackingCode)})"
                )
              runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            },
            modifier = Modifier.testTag("complaint_open_maps"),
          ) {
            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("View Issue Pin in Google Maps")
          }
        }

        if (complaint.fundsRequired || complaint.estimatedCostInr > 0L) {
          Spacer(Modifier.height(10.dp))
          Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), shape = RoundedCornerShape(10.dp)) {
            Text(
              text = "Estimated Cost: ${formatInr(complaint.estimatedCostInr)} • Funds Linked: ${if (complaint.fundRequestId != null) "Yes" else "Pending"}",
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (!isClosed && (isVerifiedOfficerOrAdmin || isAssignedFieldWorker)) {
          Spacer(Modifier.height(14.dp))
          HorizontalDivider()
          Spacer(Modifier.height(10.dp))
          Text("Official Workflow Action", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(6.dp))
          OutlinedTextField(
            value = actionRemark,
            onValueChange = { actionRemark = it },
            label = { Text("Action / Inspection Remarks") },
            placeholder = { Text("Enter field inspection notes or escalation reason...") },
            modifier = Modifier.fillMaxWidth().testTag("workflow_remark_input"),
          )
          Spacer(Modifier.height(8.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (isVerifiedOfficerOrAdmin) {
              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.WARD_VERIFYING,
                    nextStage = WorkflowStage.WARD,
                    remarks = actionRemark.ifBlank { "Verified by Ward Representative." },
                  )
                }
              ) { Text("Ward Verify") }

              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.VILLAGE_LEVEL,
                    nextStage = WorkflowStage.VILLAGE,
                    remarks = actionRemark.ifBlank { "Forwarded to Sarpanch & Gram Panchayat." },
                  )
                }
              ) { Text("To Sarpanch") }

              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.MANDAL_ESCALATED,
                    nextStage = WorkflowStage.MANDAL,
                    remarks = actionRemark.ifBlank { "Escalated to Mandal Smart Module (${dept.title})." },
                  )
                }
              ) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Mandal")
              }

              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.DISTRICT_ESCALATED,
                    nextStage = WorkflowStage.DISTRICT,
                    remarks = actionRemark.ifBlank { "Escalated to District Department Board." },
                  )
                }
              ) { Text("District") }

              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.WORK_ASSIGNED,
                    nextStage = WorkflowStage.FIELD_WORKER,
                    remarks = actionRemark.ifBlank { "Assigned to Field Execution Team." },
                    assignedWorkerId = viewModel.currentUserId,
                    assignedWorkerName = profile?.displayName ?: "Field Engineer",
                  )
                }
              ) {
                Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Assign Field")
              }

              OutlinedButton(onClick = { showFundForm = !showFundForm }) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Request Funds")
              }

              Button(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = role,
                    nextStatus = ComplaintStatus.RESOLVED,
                    nextStage = WorkflowStage.CLOSED,
                    remarks = actionRemark.ifBlank { "Issue verified and resolved on ground." },
                  )
                },
                modifier = Modifier.testTag("resolve_complaint_button"),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Resolve")
              }
            }

            if (isAssignedFieldWorker) {
              OutlinedButton(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = StakeholderRole.FIELD_WORKER,
                    nextStatus = ComplaintStatus.IN_PROGRESS,
                    nextStage = WorkflowStage.FIELD_WORKER,
                    remarks = actionRemark.ifBlank { "Field repair work started." },
                  )
                }
              ) { Text("Start Field Work") }

              Button(
                onClick = {
                  viewModel.transitionComplaint(
                    complaint = complaint,
                    actorRole = StakeholderRole.FIELD_WORKER,
                    nextStatus = ComplaintStatus.COMPLETED_PENDING_VERIFY,
                    nextStage = WorkflowStage.VILLAGE,
                    remarks = actionRemark.ifBlank { "Field work completed; awaiting Panchayat verification." },
                  )
                }
              ) { Text("Mark Completed") }
            }
          }

          if (showFundForm && isVerifiedOfficerOrAdmin) {
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create Public Fund Request", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                  value = fundProjectTitle,
                  onValueChange = { fundProjectTitle = it },
                  label = { Text("Project Work Title") },
                  modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                  value = fundSource,
                  onValueChange = { fundSource = it },
                  label = { Text("Scheme / Fund Source") },
                  modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedTextField(
                    value = fundEstimatedText,
                    onValueChange = { fundEstimatedText = it.filter(Char::isDigit) },
                    label = { Text("Est. Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedTextField(
                    value = fundRequestedText,
                    onValueChange = { fundRequestedText = it.filter(Char::isDigit) },
                    label = { Text("Requested (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                  )
                }
                Button(
                  onClick = {
                    val est = fundEstimatedText.toLongOrNull() ?: 0L
                    val req = fundRequestedText.toLongOrNull() ?: est
                    viewModel.createFundRequest(
                      complaint = complaint,
                      projectTitle = fundProjectTitle,
                      fundSource = fundSource,
                      estimatedCostInr = est,
                      requestedAmountInr = req,
                      remarks = actionRemark.ifBlank { "Submitted from complaint ${complaint.trackingCode}" },
                    )
                    showFundForm = false
                  },
                  modifier = Modifier.fillMaxWidth(),
                ) { Text("Submit Fund Proposal") }
              }
            }
          }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Immutable Audit Timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
          Text(
            "Tracking log initialized for ${complaint.trackingCode}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          history.forEach { entry ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "${entry.actionType} (${entry.fromStage} → ${entry.toStage})",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(entry.remarks, style = MaterialTheme.typography.bodySmall)
                Text(
                  text = "By ${entry.actorName} (${entry.actorRole})",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
