package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.Complaint
import com.example.data.firebase.ComplaintStatus
import com.example.data.firebase.PriorityLevel
import com.example.data.firebase.WorkflowStage
import com.example.data.local.DistrictEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusEscalatedBg
import com.example.ui.theme.StatusEscalatedFg
import com.example.ui.theme.StatusProgressBg
import com.example.ui.theme.StatusProgressFg
import com.example.ui.theme.StatusResolvedBg
import com.example.ui.theme.StatusResolvedFg
import com.example.ui.theme.StatusSubmittedBg
import com.example.ui.theme.StatusSubmittedFg
import kotlin.math.hypot

enum class GrievanceStatusBucket(val label: String) {
  ALL("All"),
  PENDING("Pending"),
  IN_PROGRESS("In Progress"),
  RESOLVED("Resolved"),
}

fun grievanceStatusBucket(statusRaw: String): GrievanceStatusBucket =
  when (ComplaintStatus.safeValueOf(statusRaw)) {
    ComplaintStatus.SUBMITTED,
    ComplaintStatus.WARD_VERIFYING,
    ComplaintStatus.VILLAGE_LEVEL -> GrievanceStatusBucket.PENDING
    ComplaintStatus.MANDAL_ESCALATED,
    ComplaintStatus.DISTRICT_ESCALATED,
    ComplaintStatus.STATE_ESCALATED,
    ComplaintStatus.WORK_ASSIGNED,
    ComplaintStatus.IN_PROGRESS,
    ComplaintStatus.COMPLETED_PENDING_VERIFY -> GrievanceStatusBucket.IN_PROGRESS
    ComplaintStatus.RESOLVED,
    ComplaintStatus.REJECTED -> GrievanceStatusBucket.RESOLVED
  }

@Composable
fun StatusPill(statusRaw: String) {
  val status = ComplaintStatus.safeValueOf(statusRaw)
  val displayText = if (status == ComplaintStatus.SUBMITTED) "Pending" else status.label
  val (bg, fg) =
    when (status) {
      ComplaintStatus.SUBMITTED,
      ComplaintStatus.WARD_VERIFYING,
      ComplaintStatus.VILLAGE_LEVEL -> StatusSubmittedBg to StatusSubmittedFg
      ComplaintStatus.WORK_ASSIGNED,
      ComplaintStatus.IN_PROGRESS,
      ComplaintStatus.COMPLETED_PENDING_VERIFY -> StatusProgressBg to StatusProgressFg
      ComplaintStatus.MANDAL_ESCALATED,
      ComplaintStatus.DISTRICT_ESCALATED,
      ComplaintStatus.STATE_ESCALATED,
      ComplaintStatus.REJECTED -> StatusEscalatedBg to StatusEscalatedFg
      ComplaintStatus.RESOLVED -> StatusResolvedBg to StatusResolvedFg
    }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
  ) {
    Text(
      text = displayText,
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun PriorityBadge(priorityRaw: String) {
  val priority = PriorityLevel.safeValueOf(priorityRaw)
  val (bg, fg) =
    when (priority) {
      PriorityLevel.LOW -> EmeraldSoft to EmeraldDeep
      PriorityLevel.MEDIUM -> StatusSubmittedBg to StatusSubmittedFg
      PriorityLevel.HIGH -> StatusProgressBg to StatusProgressFg
      PriorityLevel.CRITICAL -> StatusEscalatedBg to StatusEscalatedFg
    }
  Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
    Text(
      text = priority.label.uppercase(),
      color = fg,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun StageStepperBar(currentStageRaw: String, modifier: Modifier = Modifier) {
  val current = WorkflowStage.safeValueOf(currentStageRaw)
  val stages = listOf("Citizen", "Ward", "Village", "Mandal", "District", "State", "Resolved")
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    stages.forEachIndexed { idx, label ->
      val active = idx <= current.stepIndex
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier.size(if (idx == current.stepIndex) 12.dp else 9.dp)
              .clip(CircleShape)
              .background(
                when {
                  idx == current.stepIndex -> SaffronAccent
                  active -> EmeraldDeep
                  else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }
              )
        )
        Spacer(Modifier.height(3.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color =
            if (active) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          fontWeight = if (idx == current.stepIndex) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
fun TelanganaDistrictMapCard(
  districts: List<DistrictEntity>,
  selectedDistrict: DistrictEntity?,
  complaints: List<Complaint>,
  isTelugu: Boolean,
  onSelectDistrict: (DistrictEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val countsByDistrict = complaints.groupingBy { it.districtId }.eachCount()

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isTelugu) "తెలంగాణ 33 జిల్లాల పాలనా పటం" else "Telangana 33-District Governance Map",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text =
              selectedDistrict?.let {
                "${if (isTelugu) it.nameTe else it.name} • ${it.mandalCount} Mandals • ${it.gpCount} GPs"
              } ?: "Tap any district node to inspect jurisdiction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (selectedDistrict != null) {
          FilledTonalButton(
            onClick = {
              val uri =
                Uri.parse(
                  "geo:${selectedDistrict.latitude},${selectedDistrict.longitude}?q=${Uri.encode(selectedDistrict.name + ", Telangana")}"
                )
              val intent = Intent(Intent.ACTION_VIEW, uri)
              runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.testTag("open_google_maps_button"),
          ) {
            Icon(
              Icons.Filled.Map,
              contentDescription = stringResource(R.string.cd_open_map),
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Maps", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      val primaryColor = EmeraldDeep
      val vibrantColor = EmeraldVibrant
      val accentColor = SaffronAccent
      val bgStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      val bgEnd = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
              RoundedCornerShape(16.dp),
            )
            .pointerInput(districts) {
              detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val nearest =
                  districts.minByOrNull { d ->
                    val dx = d.mapX * w - offset.x
                    val dy = d.mapY * h - offset.y
                    hypot(dx.toDouble(), dy.toDouble())
                  }
                if (nearest != null) {
                  val dx = nearest.mapX * w - offset.x
                  val dy = nearest.mapY * h - offset.y
                  if (hypot(dx.toDouble(), dy.toDouble()) <= 64.0) {
                    onSelectDistrict(nearest)
                  }
                }
              }
            }
            .testTag("telangana_district_map_canvas")
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          // Stylized Telangana boundary polygon
          val w = size.width
          val h = size.height
          val borderPath =
            Path().apply {
              moveTo(0.32f * w, 0.05f * h)
              lineTo(0.55f * w, 0.08f * h)
              lineTo(0.68f * w, 0.22f * h)
              lineTo(0.89f * w, 0.52f * h)
              lineTo(0.76f * w, 0.72f * h)
              lineTo(0.52f * w, 0.78f * h)
              lineTo(0.36f * w, 0.88f * h)
              lineTo(0.20f * w, 0.94f * h)
              lineTo(0.11f * w, 0.72f * h)
              lineTo(0.18f * w, 0.44f * h)
              lineTo(0.24f * w, 0.18f * h)
              close()
            }
          drawPath(
            path = borderPath,
            color = primaryColor.copy(alpha = 0.09f),
          )
          drawPath(
            path = borderPath,
            color = primaryColor.copy(alpha = 0.35f),
            style = Stroke(width = 2.5f),
          )

          districts.forEach { d ->
            val cx = d.mapX * w
            val cy = d.mapY * h
            val isSelected = selectedDistrict?.districtId == d.districtId
            val issueCount = countsByDistrict[d.districtId] ?: 0

            if (issueCount > 0) {
              drawCircle(
                color = accentColor.copy(alpha = 0.25f),
                radius = 18f + (issueCount.coerceAtMost(5) * 4f),
                center = Offset(cx, cy),
              )
            }
            if (isSelected) {
              drawCircle(
                color = accentColor,
                radius = 16f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f),
              )
            }
            drawCircle(
              color = if (isSelected) accentColor else if (issueCount > 0) vibrantColor else primaryColor,
              radius = if (isSelected) 10f else 6.5f,
              center = Offset(cx, cy),
            )
          }
        }

        if (selectedDistrict != null) {
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = SaffronAccent,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              val count = countsByDistrict[selectedDistrict.districtId] ?: 0
              Text(
                text = "${selectedDistrict.name} • HQ: ${selectedDistrict.headquarters} • $count active issues",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AnalyticsBarChart(
  title: String,
  items: List<Pair<String, Float>>,
  barColor: Color = EmeraldDeep,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(10.dp))
      val maxVal = (items.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
      items.forEach { (label, value) ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(110.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Box(modifier = Modifier.weight(1f).height(14.dp)) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
              drawRoundRect(
                color = trackColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(7f, 7f),
              )
              val fraction = (value / maxVal).coerceIn(0.04f, 1f)
              drawRoundRect(
                color = barColor,
                size = Size(size.width * fraction, size.height),
                cornerRadius = CornerRadius(7f, 7f),
              )
            }
          }
          Spacer(Modifier.width(8.dp))
          Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

fun formatInr(amount: Long): String {
  return "₹%,d".format(amount)
}
