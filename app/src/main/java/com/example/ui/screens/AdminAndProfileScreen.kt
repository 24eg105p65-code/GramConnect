package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.ProblemDepartment
import com.example.data.firebase.StakeholderRole
import com.example.data.firebase.signOut
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminAndProfileScreen(viewModel: GramConnectViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
  val roleRequestsState by viewModel.roleRequests.collectAsStateWithLifecycle()
  val districts by viewModel.districts.collectAsStateWithLifecycle()
  val mandals by viewModel.mandals.collectAsStateWithLifecycle()
  val villages by viewModel.villages.collectAsStateWithLifecycle()
  val selectedDistrictId by viewModel.selectedDistrictId.collectAsStateWithLifecycle()
  val selectedMandalId by viewModel.selectedMandalId.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val currentRole = StakeholderRole.safeValueOf(profile?.role)
  val isBootstrappedAdmin = profile?.email == "varunbanoth14@gmail.com"
  val isAdmin = isBootstrappedAdmin || currentRole == StakeholderRole.GOVT_ADMIN
  val roleRequests = (roleRequestsState as? UiState.Success)?.data.orEmpty()

  var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
  var wardInput by remember(profile?.wardId) { mutableStateOf(profile?.wardId ?: "Ward 1") }
  val selectedDistrict = districts.firstOrNull { it.districtId == selectedDistrictId } ?: districts.firstOrNull()
  val selectedMandal = mandals.firstOrNull { it.mandalId == selectedMandalId } ?: mandals.firstOrNull()
  var selectedVillage by remember(villages) { mutableStateOf(villages.firstOrNull()) }

  var customMandal by remember { mutableStateOf("") }
  var customVillage by remember { mutableStateOf("") }

  var requestedRole by remember { mutableStateOf(StakeholderRole.WARD_REP) }
  var requestedDept by remember { mutableStateOf(ProblemDepartment.WATER_SUPPLY) }
  var officialWorkId by remember { mutableStateOf("") }
  var districtMenuOpen by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(profile?.displayName ?: "Citizen Profile", style = MaterialTheme.typography.titleLarge)
              Text(profile?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
              Text(
                currentRole.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }

          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          ExposedDropdownMenuBox(
            expanded = districtMenuOpen,
            onExpandedChange = { districtMenuOpen = it },
          ) {
            OutlinedTextField(
              value = selectedDistrict?.name ?: "Warangal",
              onValueChange = {},
              readOnly = true,
              label = { Text("Home District") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtMenuOpen) },
              modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = districtMenuOpen, onDismissRequest = { districtMenuOpen = false }) {
              districts.forEach { d ->
                DropdownMenuItem(
                  text = { Text("${d.name} (${d.nameTe})") },
                  onClick = {
                    viewModel.selectDistrict(d.districtId)
                    districtMenuOpen = false
                  },
                )
              }
            }
          }

          OutlinedTextField(
            value = wardInput,
            onValueChange = { wardInput = it },
            label = { Text("Ward Number / Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                viewModel.updateJurisdiction(
                  displayName = displayName,
                  districtId = selectedDistrict?.districtId ?: "warangal",
                  mandalId = selectedMandal?.mandalId ?: "wardhannapet",
                  villageId = selectedVillage?.villageId ?: "ilanda",
                  wardId = wardInput.ifBlank { "Ward 1" },
                )
              },
              modifier = Modifier.weight(1f).testTag("save_profile_button"),
            ) { Text("Save Profile") }

            OutlinedButton(
              onClick = { signOut(credentialManager, scope) },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.sign_out))
              Spacer(Modifier.width(6.dp))
              Text(stringResource(R.string.sign_out))
            }
          }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Add Village / Mandal to Local Geo Directory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customMandal,
              onValueChange = { customMandal = it },
              label = { Text("Mandal Name") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = customVillage,
              onValueChange = { customVillage = it },
              label = { Text("Village / GP") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedButton(
            onClick = {
              if (customMandal.isNotBlank() && customVillage.isNotBlank()) {
                viewModel.addCustomVillageToLocalGeo(
                  districtId = selectedDistrict?.districtId ?: "warangal",
                  mandalName = customMandal,
                  villageName = customVillage,
                )
                customMandal = ""
                customVillage = ""
              }
            },
            modifier = Modifier.fillMaxWidth(),
          ) { Text("Save Custom Village to Telangana SQLite Cache") }
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Apply for Verified Official Role", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Text(
            "Official governance permissions require verification by Central Govt Admin using your official Telangana Work ID.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              StakeholderRole.WARD_REP,
              StakeholderRole.SARPANCH,
              StakeholderRole.VILLAGE_OFFICER,
              StakeholderRole.MANDAL_OFFICER,
              StakeholderRole.DISTRICT_OFFICER,
              StakeholderRole.FIELD_WORKER,
              StakeholderRole.GOVT_ADMIN,
            ).forEach { r ->
              FilterChip(
                selected = requestedRole == r,
                onClick = { requestedRole = r },
                label = { Text(r.label) },
              )
            }
          }
          OutlinedTextField(
            value = officialWorkId,
            onValueChange = { officialWorkId = it },
            label = { Text("Official Work / Panchayat ID (e.g., TG-PR-4421)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("official_work_id_input"),
          )
          Button(
            onClick = {
              viewModel.submitRoleRequest(
                requestedRole = requestedRole,
                workId = officialWorkId,
                department = requestedDept,
                districtId = selectedDistrict?.districtId ?: "warangal",
                mandalId = selectedMandal?.mandalId ?: "wardhannapet",
                villageId = selectedVillage?.villageId ?: "ilanda",
                wardId = wardInput.ifBlank { "Ward 1" },
              )
              officialWorkId = ""
            },
            enabled = officialWorkId.trim().length >= 2,
            modifier = Modifier.fillMaxWidth().testTag("submit_role_request_button"),
          ) { Text("Submit Official Verification Request") }
        }
      }
    }

    if (roleRequests.isNotEmpty()) {
      item {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(8.dp))
          Text(
            if (isAdmin) "Central Admin Verification Queue" else "My Role Verification Requests",
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }

      items(roleRequests, key = { it.id }) { req ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${req.applicantName} → ${req.requestedRole}", style = MaterialTheme.typography.titleSmall)
              Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                Text(
                  req.status,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            Text(
              "Work ID: ${req.officialWorkId} • District: ${req.districtId} • ${req.wardId}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isAdmin && req.status == "PENDING") {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { viewModel.reviewRoleRequest(req, approve = true, notes = "Verified by Central Admin") },
                  modifier = Modifier.testTag("approve_role_${req.id}"),
                ) { Text("Approve") }
                OutlinedButton(
                  onClick = { viewModel.reviewRoleRequest(req, approve = false, notes = "Work ID verification incomplete") },
                  modifier = Modifier.testTag("reject_role_${req.id}"),
                ) { Text("Reject") }
              }
            }
          }
        }
      }
    }
  }
}
