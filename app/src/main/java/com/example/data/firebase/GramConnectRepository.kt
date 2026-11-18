package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GramConnectRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun isBootstrappedAdminUser(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email == "varunbanoth14@gmail.com" && user.isEmailVerified
  }

  private fun <T> Flow<T>.handleSnapshotErrors(path: String, uid: String): Flow<T> =
    catch { error ->
      val firestoreError =
        generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
      }
      throw error
    }

  suspend fun ensureUserProfile(
    uid: String,
    displayName: String,
    email: String,
  ): Result<UserProfile> {
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        val existing =
          existingSnap.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        if (existing != null) return Result.success(existing)
      }
      val profile =
        UserProfile(
          userId = uid,
          displayName = displayName.ifBlank { "Telangana Citizen" }.take(100),
          email = email.ifBlank { "citizen@gramconnect.tg" }.take(160),
          role = StakeholderRole.CITIZEN.name,
          verificationStatus = "VERIFIED",
          districtId = "warangal",
          mandalId = "wardhannapet",
          villageId = "ilanda",
          wardId = "Ward 1",
          preferredLang = "EN",
        )
      docRef.set(profile.toCreateMap()).await()
      Result.success(profile.copy(id = uid))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      db.collection("users")
        .document(uid)
        .snapshots()
        .map {
          it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun updateUserJurisdiction(
    uid: String,
    displayName: String,
    districtId: String,
    mandalId: String,
    villageId: String,
    wardId: String,
    preferredLang: String,
  ): Result<Unit> {
    val path = "users/$uid"
    return try {
      val currentUid = requireUserId()
      check(currentUid == uid) { "Refusing cross-user profile update." }
      db.collection("users")
        .document(uid)
        .update(
          mapOf(
            "displayName" to displayName.trim().take(100).ifBlank { "Citizen" },
            "districtId" to districtId,
            "mandalId" to mandalId,
            "villageId" to villageId,
            "wardId" to wardId,
            "preferredLang" to preferredLang,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observeComplaintsForUser(uid: String, isAdminView: Boolean): Flow<List<Complaint>> = flow {
    val path = "complaints"
    val query =
      if (isAdminView && isBootstrappedAdminUser()) {
        db.collection("complaints")
      } else {
        db.collection("complaints").whereArrayContains("authorizedUids", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snap ->
          snap
            .toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeMySubmittedGrievances(uid: String): Flow<List<Complaint>> = flow {
    val path = "complaints"
    emitAll(
      db.collection("complaints")
        .whereEqualTo("citizenId", uid)
        .snapshots()
        .map { snap ->
          snap
            .toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeFieldWorkerTasks(uid: String): Flow<List<Complaint>> = flow {
    val path = "complaints"
    emitAll(
      db.collection("complaints")
        .whereEqualTo("assignedWorkerId", uid)
        .snapshots()
        .map { snap ->
          snap
            .toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeComplaintHistory(complaintId: String, uid: String): Flow<List<ComplaintHistoryEntry>> =
    flow {
      val path = "complaints/$complaintId/history"
      val query =
        if (isBootstrappedAdminUser()) {
          db.collection("complaints").document(complaintId).collection("history")
        } else {
          db.collection("complaints")
            .document(complaintId)
            .collection("history")
            .whereArrayContains("authorizedUids", uid)
        }
      emitAll(
        query
          .snapshots()
          .map { snap ->
            snap
              .toObjects(
                ComplaintHistoryEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedBy { it.createdAt?.seconds ?: 0L }
          }
          .handleSnapshotErrors(path, uid)
      )
    }

  suspend fun submitComplaint(
    citizenName: String,
    title: String,
    description: String,
    category: ProblemDepartment,
    subCategory: String,
    priority: PriorityLevel,
    districtId: String,
    districtName: String,
    mandalId: String,
    mandalName: String,
    villageId: String,
    villageName: String,
    wardId: String,
    latitude: Double?,
    longitude: Double?,
    photoUriString: String?,
    aiSummary: String?,
  ): Result<String> {
    val complaintId = "cmp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val path = "complaints/$complaintId"
    return try {
      val uid = requireUserId()
      val cleanTitle = title.trim()
      val cleanDesc = description.trim()
      require(cleanTitle.length in 3..140) { "Title must be 3 to 140 characters." }
      require(cleanDesc.length in 5..2000) { "Description must be 5 to 2000 characters." }

      val trackingCode = "GC-TG-2026-${(1000..9999).random()}"
      val complaint =
        Complaint(
          trackingCode = trackingCode,
          citizenId = uid,
          citizenName = citizenName.trim().take(100).ifBlank { "Citizen" },
          title = cleanTitle,
          description = cleanDesc,
          category = category.name,
          subCategory = subCategory.take(100).ifBlank { category.subCategories.first() },
          priority = priority.name,
          status = ComplaintStatus.SUBMITTED.name,
          currentStage = WorkflowStage.CITIZEN.name,
          districtId = districtId,
          districtName = districtName,
          mandalId = mandalId,
          mandalName = mandalName,
          villageId = villageId,
          villageName = villageName,
          wardId = wardId,
          latitude = latitude,
          longitude = longitude,
          estimatedCostInr = 0L,
          fundsRequired = false,
          latestActionNote = "Registered by citizen at $villageName ($wardId)",
          aiSummary = aiSummary?.take(1000),
          photoUriString = photoUriString?.take(500),
          authorizedUids = listOf(uid),
        )
      db.collection("complaints").document(complaintId).set(complaint.toCreateMap()).await()

      val histId = "hist_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val history =
        ComplaintHistoryEntry(
          complaintId = complaintId,
          citizenId = uid,
          actorId = uid,
          actorName = complaint.citizenName,
          actorRole = StakeholderRole.CITIZEN.name,
          actionType = "REGISTERED",
          fromStage = WorkflowStage.CITIZEN.name,
          toStage = WorkflowStage.WARD.name,
          remarks = "Complaint $trackingCode submitted for $villageName, $mandalName, $districtName.",
          authorizedUids = listOf(uid),
        )
      db.collection("complaints")
        .document(complaintId)
        .collection("history")
        .document(histId)
        .set(history.toCreateMap())
        .await()

      createNotification(
        uid = uid,
        title = "Grievance Registered: $trackingCode",
        message = "Your ${category.title} issue in $villageName is routed to $wardId.",
        relatedId = complaintId,
        type = "COMPLAINT",
      )

      Result.success(trackingCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun transitionComplaintWorkflow(
    complaint: Complaint,
    actorName: String,
    actorRole: StakeholderRole,
    nextStatus: ComplaintStatus,
    nextStage: WorkflowStage,
    remarks: String,
    assignedWorkerId: String? = complaint.assignedWorkerId,
    assignedWorkerName: String? = complaint.assignedWorkerName,
    estimatedCostInr: Long = complaint.estimatedCostInr,
    fundsRequired: Boolean = complaint.fundsRequired,
  ): Result<Unit> {
    val path = "complaints/${complaint.id}"
    return try {
      val uid = requireUserId()
      require(complaint.id.isNotBlank()) { "Complaint ID is required." }
      val cleanRemarks = remarks.trim().take(600).ifBlank { "Status updated to ${nextStatus.label}" }

      val updatedAuthorized =
        (complaint.authorizedUids + listOfNotNull(uid, assignedWorkerId)).distinct().take(20)

      val updates =
        mutableMapOf<String, Any>(
          "status" to nextStatus.name,
          "currentStage" to nextStage.name,
          "latestActionNote" to cleanRemarks,
          "authorizedUids" to updatedAuthorized,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (actorRole != StakeholderRole.FIELD_WORKER) {
        updates["estimatedCostInr"] = estimatedCostInr.coerceAtLeast(0L)
        updates["fundsRequired"] = fundsRequired
        if (!assignedWorkerId.isNullOrBlank()) updates["assignedWorkerId"] = assignedWorkerId
        if (!assignedWorkerName.isNullOrBlank()) updates["assignedWorkerName"] = assignedWorkerName
      }

      db.collection("complaints").document(complaint.id).update(updates).await()

      val histId = "hist_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val history =
        ComplaintHistoryEntry(
          complaintId = complaint.id,
          citizenId = complaint.citizenId,
          actorId = uid,
          actorName = actorName.take(100).ifBlank { actorRole.label },
          actorRole = actorRole.name,
          actionType = nextStatus.name,
          fromStage = complaint.currentStage,
          toStage = nextStage.name,
          remarks = cleanRemarks,
          authorizedUids = updatedAuthorized,
        )
      db.collection("complaints")
        .document(complaint.id)
        .collection("history")
        .document(histId)
        .set(history.toCreateMap())
        .await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observeFundRequests(uid: String, isAdminView: Boolean): Flow<List<FundRequest>> = flow {
    val path = "fundRequests"
    val query =
      if (isAdminView && isBootstrappedAdminUser()) {
        db.collection("fundRequests")
      } else {
        db.collection("fundRequests").whereArrayContains("reviewerUids", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snap ->
          snap
            .toObjects(FundRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun createFundRequest(
    complaint: Complaint,
    requesterName: String,
    projectTitle: String,
    fundSource: String,
    estimatedCostInr: Long,
    requestedAmountInr: Long,
    remarks: String,
  ): Result<Unit> {
    val fundId = "fund_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val path = "fundRequests/$fundId"
    return try {
      val uid = requireUserId()
      val req =
        FundRequest(
          complaintId = complaint.id.ifBlank { "direct_project" },
          projectTitle = projectTitle.trim().take(140),
          requesterId = uid,
          requesterName = requesterName.trim().take(100),
          department = complaint.category,
          districtId = complaint.districtId,
          mandalId = complaint.mandalId,
          villageId = complaint.villageId,
          fundSource = fundSource.trim().take(100),
          estimatedCostInr = estimatedCostInr.coerceAtLeast(0L),
          requestedAmountInr = requestedAmountInr.coerceAtLeast(0L),
          approvalStage = "MANDAL",
          status = "REQUESTED",
          remarks = remarks.trim().take(600),
          reviewerUids = listOf(uid),
        )
      db.collection("fundRequests").document(fundId).set(req.toCreateMap()).await()
      if (complaint.id.isNotBlank()) {
        db.collection("complaints")
          .document(complaint.id)
          .update(
            mapOf(
              "fundsRequired" to true,
              "estimatedCostInr" to estimatedCostInr.coerceAtLeast(0L),
              "fundRequestId" to fundId,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun advanceFundRequest(
    fundRequest: FundRequest,
    nextStage: String,
    nextStatus: String,
    sanctionedAmountInr: Long,
    expenditureInr: Long,
    remarks: String,
  ): Result<Unit> {
    val path = "fundRequests/${fundRequest.id}"
    return try {
      requireUserId()
      db.collection("fundRequests")
        .document(fundRequest.id)
        .update(
          mapOf(
            "approvalStage" to nextStage,
            "status" to nextStatus,
            "sanctionedAmountInr" to sanctionedAmountInr.coerceAtLeast(0L),
            "expenditureInr" to expenditureInr.coerceAtLeast(0L),
            "remarks" to remarks.trim().take(600),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observePublicProjects(uid: String): Flow<List<PublicProject>> = flow {
    val path = "publicProjects"
    emitAll(
      db.collection("publicProjects")
        .whereEqualTo("published", true)
        .snapshots()
        .map {
          it.toObjects(PublicProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeSchemes(uid: String): Flow<List<GovernmentScheme>> = flow {
    val path = "schemes"
    emitAll(
      db.collection("schemes")
        .whereEqualTo("published", true)
        .snapshots()
        .map {
          it.toObjects(
            GovernmentScheme::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeRationSummaries(uid: String): Flow<List<RationSummary>> = flow {
    val path = "rationSummaries"
    emitAll(
      db.collection("rationSummaries")
        .whereEqualTo("publicSummary", true)
        .snapshots()
        .map {
          it.toObjects(RationSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  fun observeUserSchemeApplications(uid: String): Flow<List<SchemeApplication>> = flow {
    val path = "users/$uid/schemeApplications"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("schemeApplications")
        .snapshots()
        .map {
          it.toObjects(
            SchemeApplication::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun submitSchemeApplicationTracking(
    scheme: GovernmentScheme,
    villageName: String,
    referenceNumber: String,
    remarks: String?,
  ): Result<Unit> {
    val uid = requireUserId()
    val appId = "app_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    val path = "users/$uid/schemeApplications/$appId"
    return try {
      val app =
        SchemeApplication(
          userId = uid,
          schemeId = scheme.id.ifBlank { "scheme_ref" },
          schemeTitle = scheme.title,
          referenceNumber = referenceNumber.trim().take(50).ifBlank { "TG-APP-${(1000..9999).random()}" },
          villageName = villageName,
          verificationStatus = "PENDING_VERIFICATION",
          applicationStatus = "SUBMITTED",
          remarks = remarks?.trim()?.take(500),
        )
      db.collection("users")
        .document(uid)
        .collection("schemeApplications")
        .document(appId)
        .set(app.toCreateMap())
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  fun observeUserRationRecords(uid: String): Flow<List<RationRecord>> = flow {
    val path = "users/$uid/rationRecords"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("rationRecords")
        .snapshots()
        .map {
          it.toObjects(RationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun addPersonalRationRecord(
    cardReference: String,
    shopCode: String,
    monthLabel: String,
    entitledRiceKg: Long,
    receivedRiceKg: Long,
  ): Result<Unit> {
    val uid = requireUserId()
    val recId = "rat_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    val path = "users/$uid/rationRecords/$recId"
    return try {
      val status =
        when {
          receivedRiceKg <= 0L -> "PENDING"
          receivedRiceKg < entitledRiceKg -> "PARTIAL"
          else -> "DISTRIBUTED"
        }
      val record =
        RationRecord(
          userId = uid,
          cardReference = cardReference.trim().take(50),
          shopCode = shopCode.trim().take(40),
          monthLabel = monthLabel.trim().take(40),
          entitledRiceKg = entitledRiceKg.coerceIn(0L, 500L),
          receivedRiceKg = receivedRiceKg.coerceIn(0L, 500L),
          distributionStatus = status,
        )
      db.collection("users")
        .document(uid)
        .collection("rationRecords")
        .document(recId)
        .set(record.toCreateMap())
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  fun observeNotifications(uid: String): Flow<List<NotificationItem>> = flow {
    val path = "users/$uid/notifications"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("notifications")
        .snapshots()
        .map { snap ->
          snap
            .toObjects(
              NotificationItem::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun markNotificationRead(uid: String, notificationId: String): Result<Unit> {
    val path = "users/$uid/notifications/$notificationId"
    return try {
      db.collection("users")
        .document(uid)
        .collection("notifications")
        .document(notificationId)
        .update(mapOf("readStatus" to true, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private suspend fun createNotification(
    uid: String,
    title: String,
    message: String,
    relatedId: String,
    type: String,
  ) {
    val id = "ntf_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    val item =
      NotificationItem(
        userId = uid,
        title = title.take(140),
        message = message.take(600),
        relatedId = relatedId.take(128),
        notificationType = type,
        readStatus = false,
      )
    db.collection("users")
      .document(uid)
      .collection("notifications")
      .document(id)
      .set(item.toCreateMap())
      .await()
  }

  fun observePublishedFeedback(uid: String): Flow<List<CitizenFeedback>> = flow {
    val path = "feedback"
    emitAll(
      db.collection("feedback")
        .whereEqualTo("moderatedPublished", true)
        .snapshots()
        .map { snap ->
          snap
            .toObjects(CitizenFeedback::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun submitFeedback(
    targetId: String,
    targetTitle: String,
    targetType: String,
    districtName: String,
    villageName: String,
    rating: Int,
    comment: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val safeTarget = targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
    val docId = "${safeTarget}_$uid"
    val path = "feedback/$docId"
    return try {
      val feedback =
        CitizenFeedback(
          citizenId = uid,
          targetId = safeTarget,
          targetTitle = targetTitle.trim().take(140),
          targetType = targetType,
          districtName = districtName,
          villageName = villageName,
          rating = rating.toLong().coerceIn(1L, 5L),
          comment = comment.trim().take(800),
          moderatedPublished = true,
        )
      db.collection("feedback").document(docId).set(feedback.toCreateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  fun observeRoleRequests(uid: String, isAdminView: Boolean): Flow<List<RoleRequest>> = flow {
    val path = "roleRequests"
    val query =
      if (isAdminView && isBootstrappedAdminUser()) {
        db.collection("roleRequests")
      } else {
        db.collection("roleRequests").whereEqualTo("userId", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snap ->
          snap
            .toObjects(RoleRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .handleSnapshotErrors(path, uid)
    )
  }

  suspend fun submitRoleRequest(
    applicantName: String,
    requestedRole: StakeholderRole,
    officialWorkId: String,
    department: ProblemDepartment,
    districtId: String,
    mandalId: String,
    villageId: String,
    wardId: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val reqId = "role_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    val path = "roleRequests/$reqId"
    return try {
      require(officialWorkId.trim().length in 2..64) { "Valid Official Work ID is required." }
      val request =
        RoleRequest(
          userId = uid,
          applicantName = applicantName.trim().take(100),
          requestedRole = requestedRole.name,
          officialWorkId = officialWorkId.trim().take(64),
          department = department.name,
          districtId = districtId,
          mandalId = mandalId,
          villageId = villageId,
          wardId = wardId,
          status = "PENDING",
        )
      db.collection("roleRequests").document(reqId).set(request.toCreateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun reviewRoleRequest(
    request: RoleRequest,
    approve: Boolean,
    reviewerNotes: String,
  ): Result<Unit> {
    val path = "roleRequests/${request.id}"
    return try {
      requireUserId()
      val newStatus = if (approve) "APPROVED" else "REJECTED"
      db.collection("roleRequests")
        .document(request.id)
        .update(
          mapOf(
            "status" to newStatus,
            "reviewerNotes" to reviewerNotes.trim().take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      if (approve) {
        db.collection("users")
          .document(request.userId)
          .update(
            mapOf(
              "role" to request.requestedRole,
              "verificationStatus" to "VERIFIED",
              "officialWorkId" to request.officialWorkId,
              "department" to request.department,
              "districtId" to request.districtId,
              "mandalId" to request.mandalId,
              "villageId" to request.villageId,
              "wardId" to request.wardId,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun seedInitialOfficialCatalogIfAdmin(): Result<Unit> {
    val uid = requireUserId()
    if (!isBootstrappedAdminUser()) {
      return Result.failure(IllegalStateException("Only Central Government Admin can publish official catalog."))
    }
    return try {
      val existingSchemes = db.collection("schemes").whereEqualTo("published", true).get().await()
      if (existingSchemes.isEmpty) {
        OfficialTelanganaCatalog.schemes.forEach { (docId, scheme) ->
          db.collection("schemes").document(docId).set(scheme.toCreateMap()).await()
        }
      }
      val existingProjects = db.collection("publicProjects").whereEqualTo("published", true).get().await()
      if (existingProjects.isEmpty) {
        OfficialTelanganaCatalog.publicProjects(uid).forEach { (docId, proj) ->
          db.collection("publicProjects").document(docId).set(proj.toCreateMap()).await()
        }
      }
      val existingRation = db.collection("rationSummaries").whereEqualTo("publicSummary", true).get().await()
      if (existingRation.isEmpty) {
        OfficialTelanganaCatalog.rationSummaries.forEach { (docId, summary) ->
          db.collection("rationSummaries").document(docId).set(summary.toCreateMap()).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "schemes")
      Result.failure(e)
    }
  }
}

object OfficialTelanganaCatalog {
  val schemes: List<Pair<String, GovernmentScheme>> =
    listOf(
      "rythu_bharosa" to
        GovernmentScheme(
          id = "rythu_bharosa",
          title = "Rythu Bharosa – Farmer Investment Support",
          level = "TELANGANA_STATE",
          department = "Agriculture & Farmer Welfare",
          category = "Farmer Welfare",
          activeStatus = "ACTIVE",
          benefitsSummary = "Financial investment assistance per acre per crop season for eligible Telangana farmers.",
          eligibilityCriteria = "Registered Telangana landholding farmers verified via Mandal Revenue & Agriculture records.",
          requiredDocuments = "Aadhaar Card, Pattadar Passbook, Linked Bank Account",
          applicationProcess = "Verify passbook details at Mandal Agriculture Extension Office / Grama Sabha.",
          applicationDeadline = "31 Dec 2026 (Rabi Cycle)",
          beneficiaryCount = 6850000L,
        ),
      "gruha_jyothi" to
        GovernmentScheme(
          id = "gruha_jyothi",
          title = "Gruha Jyothi – Free Domestic Electricity",
          level = "TELANGANA_STATE",
          department = "Energy & Welfare Department",
          category = "Household Welfare",
          activeStatus = "ACTIVE",
          benefitsSummary = "Zero electricity bill subsidy up to 200 units per month for eligible domestic households.",
          eligibilityCriteria = "Telangana White Ration Card holders with eligible domestic electricity meter.",
          requiredDocuments = "Food Security (Ration) Card, Aadhaar, Electricity Service Number",
          applicationProcess = "Submit Praja Palana verification at Gram Panchayat / Mandal Parishad Office.",
          applicationDeadline = "Open / Rolling Verification",
          beneficiaryCount = 5020000L,
        ),
      "mahalakshmi" to
        GovernmentScheme(
          id = "mahalakshmi",
          title = "Mahalakshmi – Women & LPG Assistance",
          level = "TELANGANA_STATE",
          department = "Women & Child Welfare",
          category = "Women Welfare",
          activeStatus = "ACTIVE",
          benefitsSummary = "Free TSRTC bus travel for Telangana women and subsidized domestic LPG cylinder assistance.",
          eligibilityCriteria = "Resident women of Telangana holding valid Ration Card / domicile ID.",
          requiredDocuments = "Aadhaar Card, Telangana Ration Card, Active Domestic LPG Consumer ID",
          applicationProcess = "Verify household LPG consumer ID through Gram Panchayat Welfare Desk.",
          applicationDeadline = "15 Jan 2027 (Quarterly Batch)",
          beneficiaryCount = 4150000L,
        ),
      "pm_awas_gramin" to
        GovernmentScheme(
          id = "pm_awas_gramin",
          title = "PM Awas Yojana – Gramin & Indiramma Indlu",
          level = "CENTRAL",
          department = "Rural Housing Department",
          category = "Housing",
          activeStatus = "ACTIVE",
          benefitsSummary = "Financial assistance for construction of pucca houses for houseless rural households.",
          eligibilityCriteria = "Rural families without pucca housing identified via Gram Sabha social audit.",
          requiredDocuments = "Aadhaar, Ration Card, Job Card / Income Certificate, Site Document",
          applicationProcess = "Apply at Gram Panchayat Housing Desk; verified by Ward & Mandal Committee.",
          applicationDeadline = "28 Feb 2027 (Phase-II Survey)",
          beneficiaryCount = 1240000L,
        ),
      "aasara_cheyutha" to
        GovernmentScheme(
          id = "aasara_cheyutha",
          title = "Cheyutha Social Security Pensions",
          level = "TELANGANA_STATE",
          department = "SERP / Rural Development",
          category = "Social Security & Pension",
          activeStatus = "ACTIVE",
          benefitsSummary = "Monthly social security pension for elderly citizens, widows, persons with disabilities, and weavers.",
          eligibilityCriteria = "Age/category criteria as per Telangana SERP social security guidelines.",
          requiredDocuments = "Aadhaar, SADAREM Certificate (if applicable), Age Proof, Bank Passbook",
          applicationProcess = "Submit application at MeeSeva / Mandal Parishad Development Office (MPDO).",
          applicationDeadline = "Open / Monthly Sanction Cycle",
          beneficiaryCount = 4420000L,
        ),
      "pm_kisan" to
        GovernmentScheme(
          id = "pm_kisan",
          title = "PM-KISAN Samman Nidhi",
          level = "CENTRAL",
          department = "Ministry of Agriculture & Farmers Welfare",
          category = "Agriculture Welfare",
          activeStatus = "ACTIVE",
          benefitsSummary = "₹6,000 annual income support in three equal DBT installments.",
          eligibilityCriteria = "Small and marginal landholding farmer families across India with e-KYC completed.",
          requiredDocuments = "Aadhaar (e-KYC), Land Record (RoR), Bank Account seeded with NPCI",
          applicationProcess = "Register or verify status via Mandal Agriculture Officer or PM-KISAN portal.",
          applicationDeadline = "31 Mar 2027 (e-KYC & Installment Cutoff)",
          beneficiaryCount = 3780000L,
        ),
    )

  fun publicProjects(adminUid: String): List<Pair<String, PublicProject>> =
    listOf(
      "proj_wgl_water" to
        PublicProject(
          id = "proj_wgl_water",
          title = "Mission Bhagiratha Overhead Tank & Valve Upgrade",
          department = ProblemDepartment.WATER_SUPPLY.name,
          districtId = "warangal",
          districtName = "Warangal",
          mandalName = "Wardhannapet",
          villageName = "Ilanda",
          sanctionedAmountInr = 450000L,
          expenditureInr = 325000L,
          progressPercent = 72L,
          status = "IN_PROGRESS",
          summary = "Replacement of 160mm main distribution sluice valves and chlorination chamber.",
          published = true,
          createdBy = adminUid,
        ),
      "proj_khm_drain" to
        PublicProject(
          id = "proj_khm_drain",
          title = "CC Road & Underground Storm Drain – Ward 4",
          department = ProblemDepartment.ROADS_INFRA.name,
          districtId = "khammam",
          districtName = "Khammam",
          mandalName = "Wyra",
          villageName = "S.N. Puram",
          sanctionedAmountInr = 680000L,
          expenditureInr = 680000L,
          progressPercent = 100L,
          status = "COMPLETED",
          summary = "320m cement concrete internal road with covered side drainage completed.",
          published = true,
          createdBy = adminUid,
        ),
      "proj_nlg_school" to
        PublicProject(
          id = "proj_nlg_school",
          title = "ZPHS Sanitation Block & Solar Drinking Water Unit",
          department = ProblemDepartment.EDUCATION.name,
          districtId = "nalgonda",
          districtName = "Nalgonda",
          mandalName = "Miryalaguda",
          villageName = "Miryalaguda GP",
          sanctionedAmountInr = 340000L,
          expenditureInr = 210000L,
          progressPercent = 62L,
          status = "IN_PROGRESS",
          summary = "Mana Ooru Mana Badi sanitation block and RO water kiosk installation.",
          published = true,
          createdBy = adminUid,
        ),
    )

  val rationSummaries: List<Pair<String, RationSummary>> =
    listOf(
      "fps_wgl_104" to
        RationSummary(
          id = "fps_wgl_104",
          shopCode = "FPS-WGL-104",
          districtId = "warangal",
          mandalName = "Wardhannapet",
          villageName = "Ilanda",
          monthLabel = "Nov 2026",
          activeCards = 482L,
          allocatedRiceKg = 8640L,
          distributedRiceKg = 7910L,
          pendingComplaints = 1L,
        ),
      "fps_khm_219" to
        RationSummary(
          id = "fps_khm_219",
          shopCode = "FPS-KHM-219",
          districtId = "khammam",
          mandalName = "Wyra",
          villageName = "S.N. Puram",
          monthLabel = "Nov 2026",
          activeCards = 395L,
          allocatedRiceKg = 7110L,
          distributedRiceKg = 6890L,
          pendingComplaints = 0L,
        ),
      "fps_nlg_308" to
        RationSummary(
          id = "fps_nlg_308",
          shopCode = "FPS-NLG-308",
          districtId = "nalgonda",
          mandalName = "Miryalaguda",
          villageName = "Miryalaguda GP",
          monthLabel = "Nov 2026",
          activeCards = 610L,
          allocatedRiceKg = 10980L,
          distributedRiceKg = 9420L,
          pendingComplaints = 2L,
        ),
    )
}
