package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class StakeholderRole(val label: String, val labelTe: String, val tierLabel: String) {
  CITIZEN("Citizen", "పౌరుడు", "Village Public"),
  WARD_REP("Ward Representative", "వార్డు సభ్యుడు", "Ward Level"),
  SARPANCH("Sarpanch", "సర్పంచ్", "Gram Panchayat"),
  VILLAGE_OFFICER("Village Officer", "గ్రామ అధికారి", "Village Admin"),
  MANDAL_OFFICER("Mandal Dept Officer", "మండల అధికారి", "Mandal Smart Module"),
  MANDAL_HEAD("Mandal Head Officer", "మండల ప్రధాన అధికారి", "Mandal Oversight"),
  DISTRICT_OFFICER("District Dept Officer", "జిల్లా అధికారి", "District Board"),
  DISTRICT_HEAD("District Head Officer", "జిల్లా ప్రధాన అధికారి", "District Oversight"),
  FIELD_WORKER("Field Worker", "క్షేత్రస్థాయి సిబ్బంది", "Execution Team"),
  GOVT_ADMIN("Central Govt Admin", "రాష్ట్ర ప్రభుత్వ అడ్మిన్", "State Portal");

  companion object {
    fun safeValueOf(raw: String?): StakeholderRole =
      entries.firstOrNull { it.name == raw } ?: CITIZEN
  }
}

enum class ProblemDepartment(
  val title: String,
  val titleTe: String,
  val subCategories: List<String>,
) {
  WATER_SUPPLY(
    "Water Supply",
    "తాగునీటి సరఫరా",
    listOf(
      "Drinking water shortage",
      "Pipeline damage",
      "Borewell problems",
      "Water tank maintenance",
      "Water quality issues",
    ),
  ),
  ROADS_INFRA(
    "Roads & Infrastructure",
    "రోడ్లు మరియు మౌలిక వసతులు",
    listOf(
      "Damaged roads",
      "Street construction",
      "Bridges",
      "Drainage construction",
      "Public buildings",
    ),
  ),
  ELECTRICITY(
    "Electricity",
    "విద్యుత్ శాఖ",
    listOf("Streetlights", "Damaged electric poles", "Power supply infrastructure"),
  ),
  SANITATION(
    "Sanitation",
    "పారిశుద్ధ్యం",
    listOf("Garbage collection", "Drainage cleaning", "Waste disposal", "Public hygiene"),
  ),
  EDUCATION(
    "Education",
    "విద్యా శాఖ",
    listOf(
      "Government school infrastructure",
      "School toilets",
      "Classrooms",
      "School facilities",
    ),
  ),
  HEALTH(
    "Health",
    "ఆరోగ్య శాఖ",
    listOf(
      "Public health facilities",
      "Government hospital infrastructure",
      "Sanitation in health facilities",
    ),
  ),
  AGRICULTURE(
    "Agriculture & Irrigation",
    "వ్యవసాయం మరియు నీటిపారుదల",
    listOf("Irrigation infrastructure", "Agricultural water facilities", "Canal problems"),
  ),
  WELFARE_SCHEMES(
    "Government Schemes & Welfare",
    "ప్రభుత్వ పథకాలు & సంక్షేమం",
    listOf(
      "Eligible beneficiary not receiving benefits",
      "Application pending for a long time",
      "Application rejected incorrectly",
      "Beneficiary details incorrect",
      "Eligible citizen missing from beneficiary list",
      "Delayed financial assistance",
      "Scholarship not received",
      "Pension not received",
      "Housing scheme application problems",
      "Scheme eligibility verification problems",
    ),
  ),
  PDS_RATION(
    "Ration & PDS",
    "రేషన్ మరియు ప్రజా పంపిణీ",
    listOf(
      "New ration card registration / correction",
      "Family member addition / removal",
      "Ration not received",
      "Insufficient / incorrect ration quantity",
      "Ration shop irregularities",
      "Distribution delays",
      "Ration card not accepted",
    ),
  ),
  OTHER_SERVICES(
    "Other Public Services",
    "ఇతర ప్రభుత్వ సేవలు",
    listOf(
      "Government financial assistance",
      "Pension-related problems",
      "Scholarship-related problems",
      "Beneficiary verification",
      "Delayed government benefits",
    ),
  );

  companion object {
    fun safeValueOf(raw: String?): ProblemDepartment =
      entries.firstOrNull { it.name == raw } ?: WATER_SUPPLY
  }
}

enum class ComplaintStatus(val label: String) {
  SUBMITTED("Submitted"),
  WARD_VERIFYING("Ward Verifying"),
  VILLAGE_LEVEL("Village Review"),
  MANDAL_ESCALATED("Mandal Escalated"),
  DISTRICT_ESCALATED("District Escalated"),
  STATE_ESCALATED("State Escalated"),
  WORK_ASSIGNED("Work Assigned"),
  IN_PROGRESS("In Progress"),
  COMPLETED_PENDING_VERIFY("Completion Review"),
  RESOLVED("Resolved"),
  REJECTED("Rejected");

  companion object {
    fun safeValueOf(raw: String?): ComplaintStatus =
      entries.firstOrNull { it.name == raw } ?: SUBMITTED
  }
}

enum class WorkflowStage(val label: String, val stepIndex: Int) {
  CITIZEN("Citizen", 0),
  WARD("Ward", 1),
  VILLAGE("Village", 2),
  MANDAL("Mandal", 3),
  DISTRICT("District", 4),
  STATE("State", 5),
  FIELD_WORKER("Field Tech", 4),
  CLOSED("Closed", 6);

  companion object {
    fun safeValueOf(raw: String?): WorkflowStage =
      entries.firstOrNull { it.name == raw } ?: CITIZEN
  }
}

enum class PriorityLevel(val label: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High"),
  CRITICAL("Critical");

  companion object {
    fun safeValueOf(raw: String?): PriorityLevel =
      entries.firstOrNull { it.name == raw } ?: MEDIUM
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val role: String = StakeholderRole.CITIZEN.name,
  val verificationStatus: String = "VERIFIED",
  val officialWorkId: String? = null,
  val department: String? = null,
  val districtId: String = "warangal",
  val mandalId: String = "wardhannapet",
  val villageId: String = "ilanda",
  val wardId: String = "Ward 1",
  val preferredLang: String = "EN",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "email" to email,
        "role" to role,
        "verificationStatus" to verificationStatus,
        "officialWorkId" to officialWorkId,
        "department" to department,
        "districtId" to districtId,
        "mandalId" to mandalId,
        "villageId" to villageId,
        "wardId" to wardId,
        "preferredLang" to preferredLang,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      .filterValues { it != null }
      .mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class RoleRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val applicantName: String = "",
  val requestedRole: String = StakeholderRole.WARD_REP.name,
  val officialWorkId: String = "",
  val department: String = ProblemDepartment.WATER_SUPPLY.name,
  val districtId: String = "warangal",
  val mandalId: String = "wardhannapet",
  val villageId: String = "ilanda",
  val wardId: String = "Ward 1",
  val status: String = "PENDING",
  val reviewerNotes: String? = null,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "applicantName" to applicantName,
        "requestedRole" to requestedRole,
        "officialWorkId" to officialWorkId,
        "department" to department,
        "districtId" to districtId,
        "mandalId" to mandalId,
        "villageId" to villageId,
        "wardId" to wardId,
        "status" to "PENDING",
        "reviewerNotes" to reviewerNotes,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      .filterValues { it != null }
      .mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Complaint(
  @DocumentId val id: String = "",
  val trackingCode: String = "",
  val citizenId: String = "",
  val citizenName: String = "",
  val title: String = "",
  val description: String = "",
  val category: String = ProblemDepartment.WATER_SUPPLY.name,
  val subCategory: String = "",
  val priority: String = PriorityLevel.MEDIUM.name,
  val status: String = ComplaintStatus.SUBMITTED.name,
  val currentStage: String = WorkflowStage.CITIZEN.name,
  val districtId: String = "",
  val districtName: String = "",
  val mandalId: String = "",
  val mandalName: String = "",
  val villageId: String = "",
  val villageName: String = "",
  val wardId: String = "Ward 1",
  val latitude: Double? = null,
  val longitude: Double? = null,
  val assignedOfficerId: String? = null,
  val assignedWorkerId: String? = null,
  val assignedWorkerName: String? = null,
  val estimatedCostInr: Long = 0L,
  val fundsRequired: Boolean = false,
  val fundRequestId: String? = null,
  val latestActionNote: String? = null,
  val aiSummary: String? = null,
  val photoUriString: String? = null,
  val authorizedUids: List<String> = emptyList(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
        "trackingCode" to trackingCode,
        "citizenId" to citizenId,
        "citizenName" to citizenName,
        "title" to title,
        "description" to description,
        "category" to category,
        "subCategory" to subCategory,
        "priority" to priority,
        "status" to ComplaintStatus.SUBMITTED.name,
        "currentStage" to WorkflowStage.CITIZEN.name,
        "districtId" to districtId,
        "districtName" to districtName,
        "mandalId" to mandalId,
        "mandalName" to mandalName,
        "villageId" to villageId,
        "villageName" to villageName,
        "wardId" to wardId,
        "latitude" to latitude,
        "longitude" to longitude,
        "assignedOfficerId" to assignedOfficerId,
        "assignedWorkerId" to assignedWorkerId,
        "assignedWorkerName" to assignedWorkerName,
        "estimatedCostInr" to estimatedCostInr,
        "fundsRequired" to fundsRequired,
        "fundRequestId" to fundRequestId,
        "latestActionNote" to latestActionNote,
        "aiSummary" to aiSummary,
        "photoUriString" to photoUriString,
        "authorizedUids" to authorizedUids,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      .filterValues { it != null }
      .mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class ComplaintHistoryEntry(
  @DocumentId val id: String = "",
  val complaintId: String = "",
  val citizenId: String = "",
  val actorId: String = "",
  val actorName: String = "",
  val actorRole: String = "",
  val actionType: String = "",
  val fromStage: String = "",
  val toStage: String = "",
  val remarks: String = "",
  val authorizedUids: List<String> = emptyList(),
  val createdAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "complaintId" to complaintId,
      "citizenId" to citizenId,
      "actorId" to actorId,
      "actorName" to actorName,
      "actorRole" to actorRole,
      "actionType" to actionType,
      "fromStage" to fromStage,
      "toStage" to toStage,
      "remarks" to remarks,
      "authorizedUids" to authorizedUids,
      "createdAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class FundRequest(
  @DocumentId val id: String = "",
  val complaintId: String = "",
  val projectTitle: String = "",
  val requesterId: String = "",
  val requesterName: String = "",
  val department: String = ProblemDepartment.WATER_SUPPLY.name,
  val districtId: String = "",
  val mandalId: String = "",
  val villageId: String = "",
  val fundSource: String = "15th Finance Commission",
  val estimatedCostInr: Long = 0L,
  val requestedAmountInr: Long = 0L,
  val sanctionedAmountInr: Long = 0L,
  val expenditureInr: Long = 0L,
  val approvalStage: String = "MANDAL",
  val status: String = "REQUESTED",
  val remarks: String? = null,
  val reviewerUids: List<String> = emptyList(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
        "complaintId" to complaintId,
        "projectTitle" to projectTitle,
        "requesterId" to requesterId,
        "requesterName" to requesterName,
        "department" to department,
        "districtId" to districtId,
        "mandalId" to mandalId,
        "villageId" to villageId,
        "fundSource" to fundSource,
        "estimatedCostInr" to estimatedCostInr,
        "requestedAmountInr" to requestedAmountInr,
        "sanctionedAmountInr" to 0L,
        "expenditureInr" to 0L,
        "approvalStage" to approvalStage,
        "status" to "REQUESTED",
        "remarks" to remarks,
        "reviewerUids" to reviewerUids,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      .filterValues { it != null }
      .mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class PublicProject(
  @DocumentId val id: String = "",
  val title: String = "",
  val department: String = ProblemDepartment.WATER_SUPPLY.name,
  val districtId: String = "",
  val districtName: String = "",
  val mandalName: String = "",
  val villageName: String = "",
  val sanctionedAmountInr: Long = 0L,
  val expenditureInr: Long = 0L,
  val progressPercent: Long = 0L,
  val status: String = "IN_PROGRESS",
  val summary: String = "",
  val published: Boolean = true,
  val createdBy: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "title" to title,
      "department" to department,
      "districtId" to districtId,
      "districtName" to districtName,
      "mandalName" to mandalName,
      "villageName" to villageName,
      "sanctionedAmountInr" to sanctionedAmountInr,
      "expenditureInr" to expenditureInr,
      "progressPercent" to progressPercent,
      "status" to status,
      "summary" to summary,
      "published" to published,
      "createdBy" to createdBy,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class GovernmentScheme(
  @DocumentId val id: String = "",
  val title: String = "",
  val level: String = "TELANGANA_STATE",
  val department: String = "",
  val category: String = "",
  val activeStatus: String = "ACTIVE",
  val benefitsSummary: String = "",
  val eligibilityCriteria: String = "",
  val requiredDocuments: String = "",
  val applicationProcess: String = "",
  val applicationDeadline: String = "Open / Rolling",
  val beneficiaryCount: Long = 0L,
  val published: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "title" to title,
      "level" to level,
      "department" to department,
      "category" to category,
      "activeStatus" to activeStatus,
      "benefitsSummary" to benefitsSummary,
      "eligibilityCriteria" to eligibilityCriteria,
      "requiredDocuments" to requiredDocuments,
      "applicationProcess" to applicationProcess,
      "applicationDeadline" to applicationDeadline,
      "beneficiaryCount" to beneficiaryCount,
      "published" to published,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class RationSummary(
  @DocumentId val id: String = "",
  val shopCode: String = "",
  val districtId: String = "",
  val mandalName: String = "",
  val villageName: String = "",
  val monthLabel: String = "",
  val activeCards: Long = 0L,
  val allocatedRiceKg: Long = 0L,
  val distributedRiceKg: Long = 0L,
  val pendingComplaints: Long = 0L,
  val publicSummary: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "shopCode" to shopCode,
      "districtId" to districtId,
      "mandalName" to mandalName,
      "villageName" to villageName,
      "monthLabel" to monthLabel,
      "activeCards" to activeCards,
      "allocatedRiceKg" to allocatedRiceKg,
      "distributedRiceKg" to distributedRiceKg,
      "pendingComplaints" to pendingComplaints,
      "publicSummary" to publicSummary,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class SchemeApplication(
  @DocumentId val id: String = "",
  val userId: String = "",
  val schemeId: String = "",
  val schemeTitle: String = "",
  val referenceNumber: String = "",
  val villageName: String = "",
  val verificationStatus: String = "PENDING_VERIFICATION",
  val applicationStatus: String = "SUBMITTED",
  val remarks: String? = null,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
        "userId" to userId,
        "schemeId" to schemeId,
        "schemeTitle" to schemeTitle,
        "referenceNumber" to referenceNumber,
        "villageName" to villageName,
        "verificationStatus" to "PENDING_VERIFICATION",
        "applicationStatus" to "SUBMITTED",
        "remarks" to remarks,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      .filterValues { it != null }
      .mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class RationRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val cardReference: String = "",
  val shopCode: String = "",
  val monthLabel: String = "",
  val entitledRiceKg: Long = 0L,
  val receivedRiceKg: Long = 0L,
  val distributionStatus: String = "DISTRIBUTED",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "userId" to userId,
      "cardReference" to cardReference,
      "shopCode" to shopCode,
      "monthLabel" to monthLabel,
      "entitledRiceKg" to entitledRiceKg,
      "receivedRiceKg" to receivedRiceKg,
      "distributionStatus" to distributionStatus,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class NotificationItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val message: String = "",
  val relatedId: String = "",
  val notificationType: String = "COMPLAINT",
  val readStatus: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "userId" to userId,
      "title" to title,
      "message" to message,
      "relatedId" to relatedId,
      "notificationType" to notificationType,
      "readStatus" to readStatus,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CitizenFeedback(
  @DocumentId val id: String = "",
  val citizenId: String = "",
  val targetId: String = "",
  val targetTitle: String = "",
  val targetType: String = "DEVELOPMENT_PROJECT",
  val districtName: String = "",
  val villageName: String = "",
  val rating: Long = 5L,
  val comment: String = "",
  val moderatedPublished: Boolean = true,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "citizenId" to citizenId,
      "targetId" to targetId,
      "targetTitle" to targetTitle,
      "targetType" to targetType,
      "districtName" to districtName,
      "villageName" to villageName,
      "rating" to rating,
      "comment" to comment,
      "moderatedPublished" to moderatedPublished,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}
