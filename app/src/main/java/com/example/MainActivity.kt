package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.GeminiAiRepository
import com.example.data.firebase.GramConnectRepository
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.local.GramConnectDatabase
import com.example.data.local.LocalGeoRepository
import com.example.ui.screens.AdminAndProfileScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FileComplaintScreen
import com.example.ui.screens.FundsAndTransparencyScreen
import com.example.ui.screens.GovernmentSchemesScreen
import com.example.ui.screens.GramSahayakAiScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MyGrievancesScreen
import com.example.ui.screens.ProblemBoardsScreen
import com.example.ui.screens.SchemesAndRationScreen
import com.example.ui.theme.GramConnectTheme
import com.example.ui.theme.SaffronAccent
import com.example.ui.viewmodel.GramConnectViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_FILE = "file_complaint"
const val ROUTE_MY_GRIEVANCES = "my_grievances"
const val ROUTE_BOARDS = "problem_boards"
const val ROUTE_SCHEMES = "schemes_ration"
const val ROUTE_GOVERNMENT_SCHEMES = "government_schemes"
const val ROUTE_FUNDS = "funds_transparency"
const val ROUTE_AI = "gram_sahayak_ai"
const val ROUTE_ADMIN = "admin_profile"

private data class NavDestination(
  val route: String,
  val label: String,
  val icon: ImageVector,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    configureAppCheck(this, intent)
    setContent {
      GramConnectTheme {
        AppRoot()
      }
    }
  }
}

@Composable
private fun AppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
  var checkingAutoSignIn by remember { mutableStateOf(Firebase.auth.currentUser == null) }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { checkingAutoSignIn = false },
        onUnauthenticated = { checkingAutoSignIn = false },
        scope = scope,
      )
    } else {
      checkingAutoSignIn = false
    }
  }

  when {
    currentUser != null -> {
      val user = currentUser!!
      key(user.uid) {
        AuthenticatedGramConnectApp(user = user)
      }
    }
    checkingAutoSignIn -> {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(Modifier.height(12.dp))
          Text("Connecting to GramConnect...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
    else -> {
      AuthScreen(onAuthSuccess = { checkingAutoSignIn = false })
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedGramConnectApp(user: FirebaseUser) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val viewModel: GramConnectViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val dbId = appContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            val repo = GramConnectRepository(firestore)
            val geoRepo = LocalGeoRepository(GramConnectDatabase.getInstance(appContext).geoDao())
            val aiRepo = GeminiAiRepository()
            GramConnectViewModel(
              repo = repo,
              geoRepo = geoRepo,
              aiRepo = aiRepo,
              currentUserId = user.uid,
              currentUserName = user.displayName ?: "Telangana Citizen",
              currentUserEmail = user.email ?: "",
            )
          }
        },
    )

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

  val snackbarHostState = remember { SnackbarHostState() }
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()
  var showNotificationsSheet by remember { mutableStateOf(false) }

  LaunchedEffect(statusMessage) {
    val msg = statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  fun navigateTo(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val bottomDestinations =
    listOf(
      NavDestination(ROUTE_HOME, "Home", Icons.Filled.Home),
      NavDestination(ROUTE_BOARDS, "Boards", Icons.Filled.Campaign),
      NavDestination(ROUTE_SCHEMES, "Schemes", Icons.Filled.CardGiftcard),
      NavDestination(ROUTE_FUNDS, "Funds", Icons.Filled.AccountBalance),
      NavDestination(ROUTE_ADMIN, "Profile", Icons.Filled.Person),
    )

  val railDestinations =
    listOf(
      NavDestination(ROUTE_HOME, "Home", Icons.Filled.Home),
      NavDestination(ROUTE_FILE, "File", Icons.Filled.Add),
      NavDestination(ROUTE_BOARDS, "Boards", Icons.Filled.Campaign),
      NavDestination(ROUTE_SCHEMES, "Schemes", Icons.Filled.CardGiftcard),
      NavDestination(ROUTE_FUNDS, "Funds", Icons.Filled.AccountBalance),
      NavDestination(ROUTE_AI, "AI Help", Icons.Filled.AutoAwesome),
      NavDestination(ROUTE_ADMIN, "Profile", Icons.Filled.Person),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail) {
        NavigationRail(modifier = Modifier.testTag("navigation_rail")) {
          railDestinations.forEach { dest ->
            NavigationRailItem(
              selected = currentRoute == dest.route,
              onClick = { navigateTo(dest.route) },
              icon = { Icon(dest.icon, contentDescription = dest.label) },
              label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useRail) {
            NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
              bottomDestinations.forEach { dest ->
                NavigationBarItem(
                  selected = currentRoute == dest.route,
                  onClick = { navigateTo(dest.route) },
                  icon = { Icon(dest.icon, contentDescription = dest.label) },
                  label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  modifier = Modifier.testTag("nav_${dest.route}"),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (!useRail && currentRoute != ROUTE_FILE && currentRoute != ROUTE_AI) {
            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              SmallFloatingActionButton(
                onClick = { navigateTo(ROUTE_AI) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = SaffronAccent,
                modifier = Modifier.testTag("fab_gram_sahayak"),
              ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Open GramSahayak AI")
              }
              FloatingActionButton(
                onClick = { navigateTo(ROUTE_FILE) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_file_complaint"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "File New Grievance")
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = ROUTE_HOME,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          composable(ROUTE_HOME) {
            HomeDashboardScreen(
              viewModel = viewModel,
              onNavigateToFileComplaint = { navigateTo(ROUTE_FILE) },
              onNavigateToMyGrievances = { navigateTo(ROUTE_MY_GRIEVANCES) },
              onNavigateToBoards = { navigateTo(ROUTE_BOARDS) },
              onNavigateToSchemes = { navigateTo(ROUTE_SCHEMES) },
              onNavigateToGovernmentSchemes = { navigateTo(ROUTE_GOVERNMENT_SCHEMES) },
              onNavigateToAi = { navigateTo(ROUTE_AI) },
              onOpenNotifications = { showNotificationsSheet = true },
            )
          }
          composable(ROUTE_MY_GRIEVANCES) {
            MyGrievancesScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onNavigateToFileComplaint = { navigateTo(ROUTE_FILE) },
            )
          }
          composable(ROUTE_GOVERNMENT_SCHEMES) {
            GovernmentSchemesScreen(
              viewModel = viewModel,
              onBack = { navController.popBackStack() },
              onNavigateToAi = { navigateTo(ROUTE_AI) },
            )
          }
          composable(ROUTE_FILE) {
            FileComplaintScreen(
              viewModel = viewModel,
              onSubmitted = { navigateTo(ROUTE_MY_GRIEVANCES) },
            )
          }
          composable(ROUTE_BOARDS) {
            ProblemBoardsScreen(viewModel = viewModel)
          }
          composable(ROUTE_SCHEMES) {
            SchemesAndRationScreen(
              viewModel = viewModel,
              onNavigateToAi = { navigateTo(ROUTE_AI) },
              onOpenSchemesDirectory = { navigateTo(ROUTE_GOVERNMENT_SCHEMES) },
            )
          }
          composable(ROUTE_FUNDS) {
            FundsAndTransparencyScreen(viewModel = viewModel)
          }
          composable(ROUTE_AI) {
            GramSahayakAiScreen(viewModel = viewModel)
          }
          composable(ROUTE_ADMIN) {
            AdminAndProfileScreen(viewModel = viewModel)
          }
        }
      }
    }
  }

  if (showNotificationsSheet) {
    BackHandler { showNotificationsSheet = false }
    ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
      val items = (notificationsState as? UiState.Success)?.data.orEmpty()
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Civic Alerts & Escalations", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        if (items.isEmpty()) {
          Text(
            "No alerts yet. Updates appear here when your grievances are registered or escalated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp),
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
              Surface(
                color =
                  if (item.readStatus) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                  else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(item.message, style = MaterialTheme.typography.bodySmall)
                  }
                  if (!item.readStatus) {
                    TextButton(onClick = { viewModel.markNotificationRead(item.id) }) {
                      Text("Read")
                    }
                  }
                }
              }
            }
          }
          Spacer(Modifier.height(20.dp))
        }
      }
    }
  }
}
