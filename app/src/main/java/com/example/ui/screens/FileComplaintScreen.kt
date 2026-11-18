package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.firebase.PriorityLevel
import com.example.data.firebase.ProblemDepartment
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel
import com.google.android.gms.location.LocationServices
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileComplaintScreen(
  viewModel: GramConnectViewModel,
  onSubmitted: () -> Unit,
) {
  val context = LocalContext.current
  val districts by viewModel.districts.collectAsStateWithLifecycle()
  val mandals by viewModel.mandals.collectAsStateWithLifecycle()
  val villages by viewModel.villages.collectAsStateWithLifecycle()
  val selectedDistrictId by viewModel.selectedDistrictId.collectAsStateWithLifecycle()
  val selectedMandalId by viewModel.selectedMandalId.collectAsStateWithLifecycle()
  val isTelugu by viewModel.isTelugu.collectAsStateWithLifecycle()

  val isSubmitting by viewModel.isSubmittingComplaint.collectAsStateWithLifecycle()
  val isRecording by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
  val isTranscribing by viewModel.isTranscribingVoice.collectAsStateWithLifecycle()
  val voiceResult by viewModel.voiceResult.collectAsStateWithLifecycle()
  val triageSuggestion by viewModel.triageSuggestion.collectAsStateWithLifecycle()
  val isTriaging by viewModel.isTriaging.collectAsStateWithLifecycle()

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedDept by remember { mutableStateOf(ProblemDepartment.WATER_SUPPLY) }
  var selectedSubCat by remember { mutableStateOf(ProblemDepartment.WATER_SUPPLY.subCategories.first()) }
  var selectedPriority by remember { mutableStateOf(PriorityLevel.MEDIUM) }
  var selectedWard by remember { mutableStateOf("Ward 1") }
  var locationLandmark by remember { mutableStateOf("") }
  var photoUri by remember { mutableStateOf<Uri?>(null) }
  var gpsLat by remember { mutableStateOf<Double?>(null) }
  var gpsLng by remember { mutableStateOf<Double?>(null) }
  var validationTriggered by remember { mutableStateOf(false) }
  var confirmedTrackingCode by remember { mutableStateOf<String?>(null) }

  val selectedDistrict = districts.firstOrNull { it.districtId == selectedDistrictId } ?: districts.firstOrNull()
  val selectedMandal = mandals.firstOrNull { it.mandalId == selectedMandalId } ?: mandals.firstOrNull()
  var selectedVillage by remember(villages) { mutableStateOf(villages.firstOrNull()) }

  LaunchedEffect(voiceResult) {
    voiceResult?.let { vr ->
      if (vr.title.isNotBlank()) title = vr.title.take(140)
      if (vr.description.isNotBlank()) description = vr.description.take(1900)
      selectedDept = vr.department
      selectedSubCat = vr.department.subCategories.first()
      selectedPriority = vr.priority
    }
  }

  LaunchedEffect(triageSuggestion) {
    triageSuggestion?.let { s ->
      selectedDept = s.department
      selectedSubCat = s.subCategory
      selectedPriority = s.priority
    }
  }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) viewModel.startVoiceRecording()
      else viewModel.showMessage("Microphone permission is needed for voice grievance transcription.")
    }

  @SuppressLint("MissingPermission")
  fun fetchCoarseLocation() {
    LocationServices.getFusedLocationProviderClient(context).lastLocation
      .addOnSuccessListener { loc ->
        if (loc != null) {
          gpsLat = loc.latitude
          gpsLng = loc.longitude
          val nearest =
            districts.minByOrNull { d ->
              abs(d.latitude - loc.latitude) + abs(d.longitude - loc.longitude)
            }
          if (nearest != null) {
            viewModel.selectDistrict(nearest.districtId)
            viewModel.showMessage("Detected location near ${nearest.name} (${"%.3f".format(loc.latitude)}, ${"%.3f".format(loc.longitude)})")
          }
        } else {
          viewModel.showMessage("GPS unavailable right now; please choose your district and landmark below.")
        }
      }
      .addOnFailureListener { viewModel.showMessage("Could not read GPS location.") }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) fetchCoarseLocation()
      else viewModel.showMessage("Location permission denied; select district and landmark manually.")
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> photoUri = uri }

  var districtMenuOpen by remember { mutableStateOf(false) }
  var mandalMenuOpen by remember { mutableStateOf(false) }
  var villageMenuOpen by remember { mutableStateOf(false) }
  var subCatMenuOpen by remember { mutableStateOf(false) }

  val titleError = validationTriggered && title.trim().length < 3
  val descError = validationTriggered && description.trim().length < 5
  val locationValid = selectedDistrict != null && selectedMandal != null && selectedVillage != null

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = if (isTelugu) "కొత్త ప్రజా ఫిర్యాదు నమోదు" else "Submit Public Grievance",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = "Describe the issue, choose a department category, and specify its Telangana location to save directly to Firestore.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = SaffronAccent)
            Spacer(Modifier.width(8.dp))
            Text("Voice Complaint & AI Triage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                if (isRecording) {
                  viewModel.stopVoiceRecordingAndTranscribe(forChatOnly = false)
                } else {
                  val hasMic =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                      PackageManager.PERMISSION_GRANTED
                  if (hasMic) viewModel.startVoiceRecording()
                  else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              },
              enabled = !isTranscribing && !isSubmitting,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = if (isRecording) MaterialTheme.colorScheme.error else SaffronAccent
                ),
              modifier = Modifier.weight(1f).testTag("voice_record_button"),
            ) {
              Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = stringResource(R.string.cd_mic_record))
              Spacer(Modifier.width(6.dp))
              Text(if (isRecording) "Stop & Transcribe" else if (isTranscribing) "Transcribing..." else "Record Voice")
            }

            OutlinedButton(
              onClick = { viewModel.runFastTriage(title, description) },
              enabled = !isTriaging && !isSubmitting && (title.isNotBlank() || description.isNotBlank()),
              modifier = Modifier.testTag("ai_triage_button"),
            ) {
              if (isTriaging) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
              else Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("AI Triage")
            }
          }
          if (voiceResult != null) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp)) {
              Text(
                text = "Transcript: ${voiceResult!!.transcript}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(140) },
        label = { Text("Grievance Title *") },
        placeholder = { Text("e.g., Drinking water pipeline leak near Ward 2 tank") },
        isError = titleError,
        supportingText = {
          Text(if (titleError) "Please enter at least 3 characters." else "${title.length}/140 characters")
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("complaint_title_input"),
      )
    }

    item {
      OutlinedTextField(
        value = description,
        onValueChange = { description = it.take(1850) },
        label = { Text("Problem Description *") },
        placeholder = { Text("Describe the issue, severity, duration, and affected households...") },
        isError = descError,
        supportingText = {
          Text(if (descError) "Please provide at least 5 characters describing the issue." else "${description.length}/1850 characters")
        },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth().testTag("complaint_description_input"),
      )
    }

    item {
      Text("Category & Department *", style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(6.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ProblemDepartment.entries.forEach { dept ->
          FilterChip(
            selected = selectedDept == dept,
            onClick = {
              selectedDept = dept
              selectedSubCat = dept.subCategories.first()
            },
            label = { Text(if (isTelugu) dept.titleTe else dept.title) },
            modifier = Modifier.testTag("category_chip_${dept.name.lowercase()}"),
          )
        }
      }
    }

    item {
      ExposedDropdownMenuBox(expanded = subCatMenuOpen, onExpandedChange = { subCatMenuOpen = it }) {
        OutlinedTextField(
          value = selectedSubCat,
          onValueChange = {},
          readOnly = true,
          label = { Text("Issue Subcategory") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCatMenuOpen) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = subCatMenuOpen, onDismissRequest = { subCatMenuOpen = false }) {
          selectedDept.subCategories.forEach { sub ->
            DropdownMenuItem(
              text = { Text(sub) },
              onClick = {
                selectedSubCat = sub
                subCatMenuOpen = false
              },
            )
          }
        }
      }
      Spacer(Modifier.height(10.dp))
      Text("Priority Level", style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PriorityLevel.entries.forEach { p ->
          FilterChip(
            selected = selectedPriority == p,
            onClick = { selectedPriority = p },
            label = { Text(p.label) },
          )
        }
      }
    }

    item {
      Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Location Details *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              if (gpsLat != null && gpsLng != null) {
                Text(
                  text = "GPS Pin: ${"%.4f".format(gpsLat)}, ${"%.4f".format(gpsLng)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldDeep,
                )
              }
            }
            OutlinedButton(
              onClick = {
                val hasLoc =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (hasLoc) fetchCoarseLocation()
                else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
              },
              modifier = Modifier.testTag("detect_gps_button"),
            ) {
              Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.cd_detect_location), modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Auto-Detect GPS")
            }
          }

          ExposedDropdownMenuBox(expanded = districtMenuOpen, onExpandedChange = { districtMenuOpen = it }) {
            OutlinedTextField(
              value = selectedDistrict?.let { "${it.name} (${it.nameTe})" } ?: "Select District",
              onValueChange = {},
              readOnly = true,
              label = { Text("District (Telangana)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtMenuOpen) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
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

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
              expanded = mandalMenuOpen,
              onExpandedChange = { mandalMenuOpen = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = selectedMandal?.name ?: "Mandal",
                onValueChange = {},
                readOnly = true,
                label = { Text("Mandal") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mandalMenuOpen) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = mandalMenuOpen, onDismissRequest = { mandalMenuOpen = false }) {
                mandals.forEach { m ->
                  DropdownMenuItem(
                    text = { Text(m.name) },
                    onClick = {
                      viewModel.selectMandal(m.mandalId)
                      mandalMenuOpen = false
                    },
                  )
                }
              }
            }

            ExposedDropdownMenuBox(
              expanded = villageMenuOpen,
              onExpandedChange = { villageMenuOpen = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = selectedVillage?.name ?: "Village / GP",
                onValueChange = {},
                readOnly = true,
                label = { Text("Village / GP") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villageMenuOpen) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = villageMenuOpen, onDismissRequest = { villageMenuOpen = false }) {
                villages.forEach { v ->
                  DropdownMenuItem(
                    text = { Text(v.name) },
                    onClick = {
                      selectedVillage = v
                      villageMenuOpen = false
                    },
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = selectedWard,
            onValueChange = { selectedWard = it.take(40) },
            label = { Text("Ward / Colony") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("complaint_ward_input"),
          )

          OutlinedTextField(
            value = locationLandmark,
            onValueChange = { locationLandmark = it.take(120) },
            label = { Text("Street / Landmark (Optional)") },
            placeholder = { Text("e.g., Opposite ZPHS gate, Main Bazaar road") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("complaint_landmark_input"),
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          modifier = Modifier.testTag("pick_photo_button"),
        ) {
          Icon(Icons.Filled.PhotoLibrary, contentDescription = stringResource(R.string.cd_pick_photo))
          Spacer(Modifier.width(6.dp))
          Text(if (photoUri == null) "Attach Evidence Photo" else "Change Photo")
        }
        if (photoUri != null) {
          AsyncImage(
            model = photoUri,
            contentDescription = "Attached evidence photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
          )
        }
      }
    }

    item {
      Button(
        onClick = {
          validationTriggered = true
          val dist = selectedDistrict
          val man = selectedMandal
          val vil = selectedVillage
          if (title.trim().length >= 3 && description.trim().length >= 5 && dist != null && man != null && vil != null) {
            val fullDescription =
              if (locationLandmark.isNotBlank()) {
                "${description.trim()}\nLocation Landmark: ${locationLandmark.trim()}"
              } else {
                description.trim()
              }
            viewModel.submitComplaint(
              title = title,
              description = fullDescription,
              category = selectedDept,
              subCategory = selectedSubCat,
              priority = selectedPriority,
              district = dist,
              mandal = man,
              village = vil,
              wardId = selectedWard.trim().ifBlank { "Ward 1" },
              latitude = gpsLat ?: dist.latitude,
              longitude = gpsLng ?: dist.longitude,
              photoUriString = photoUri?.toString(),
              onSuccess = { code ->
                title = ""
                description = ""
                locationLandmark = ""
                photoUri = null
                validationTriggered = false
                confirmedTrackingCode = code
              },
            )
          } else if (!locationValid) {
            viewModel.showMessage("Please select District, Mandal, and Village.")
          }
        },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("submit_complaint_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(10.dp))
          Text("Saving to Firestore...", style = MaterialTheme.typography.titleSmall)
        } else {
          Text("Submit Grievance & Generate Tracking ID", style = MaterialTheme.typography.titleSmall)
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }

  confirmedTrackingCode?.let { code ->
    AlertDialog(
      onDismissRequest = { confirmedTrackingCode = null },
      icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldDeep) },
      title = { Text("Grievance Submitted") },
      text = {
        Text(
          "Your grievance has been saved to Firestore and routed to your Ward & Gram Panchayat.\n\nTracking ID: $code",
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            confirmedTrackingCode = null
            onSubmitted()
          },
          modifier = Modifier.testTag("dialog_view_boards_button"),
        ) { Text("View My Grievances") }
      },
      dismissButton = {
        TextButton(onClick = { confirmedTrackingCode = null }) { Text("File Another") }
      },
    )
  }
}
