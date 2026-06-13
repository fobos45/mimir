package com.mimir.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mimir.app.service.ConnectionService
import com.mimir.app.ui.components.AddContactDialog
import com.mimir.app.ui.screens.*
import com.mimir.app.ui.theme.MimirTheme
import uniffi.mimir.CallStatus

class MainActivity : ComponentActivity() {

    private val callVm:     CallViewModel     by viewModels()
    private val contactsVm: ContactsViewModel by viewModels()
    private val settingsVm: SettingsViewModel by viewModels()

    private var navController: NavHostController? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        startService()

        setContent {
            MimirTheme {
                val nav = rememberNavController()
                LaunchedEffect(nav) { navController = nav }
                MimirApp(
                    navController = nav,
                    callVm        = callVm,
                    contactsVm    = contactsVm,
                    settingsVm    = settingsVm,
                    initialIntent = intent,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val nav = navController ?: return
        intent.getStringExtra("open_chat")?.let { pubkey ->
            nav.navigate("chat/$pubkey") { launchSingleTop = true }
        }
        intent.getStringExtra("incoming_call")?.let {
            nav.navigate("call") { launchSingleTop = true }
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
        ))
    }

    private fun startService() {
        ContextCompat.startForegroundService(
            this, Intent(this, ConnectionService::class.java)
        )
    }
}

@Composable
fun MimirApp(
    navController: NavHostController,
    callVm: CallViewModel,
    contactsVm: ContactsViewModel,
    settingsVm: SettingsViewModel,
    initialIntent: Intent?,
) {
    val contacts        by contactsVm.contacts.collectAsStateWithLifecycle()
    val callStatus      by callVm.callStatus.collectAsStateWithLifecycle()
    val callPubkey      by callVm.callPubkey.collectAsStateWithLifecycle()
    val callDuration    by callVm.callDuration.collectAsStateWithLifecycle()
    val connectionState by contactsVm.connectionState.collectAsStateWithLifecycle()
    val peers           by settingsVm.peers.collectAsStateWithLifecycle()
    var showAddDialog   by remember { mutableStateOf(false) }

    LaunchedEffect(initialIntent) {
        initialIntent?.getStringExtra("open_chat")?.let {
            navController.navigate("chat/$it") { launchSingleTop = true }
        }
        initialIntent?.getStringExtra("incoming_call")?.let {
            navController.navigate("call") { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = "contacts") {

        composable("contacts") {
            val myKey by contactsVm.myPubkeyHex.collectAsStateWithLifecycle()
            ContactListScreen(
                contacts        = contacts,
                onOpenChat      = { navController.navigate("chat/$it") },
                onAddContact    = { showAddDialog = true },
                myPubkeyHex     = myKey,
                connectionState = connectionState,
                onOpenSettings  = { navController.navigate("settings") },
            )
        }

        composable("chat/{pubkey}") { back ->
            val pubkey = back.arguments?.getString("pubkey") ?: return@composable
            val ctx = LocalContext.current
            val vm = remember(pubkey) {
                ChatViewModel(ctx.applicationContext as MimirApplication, pubkey)
            }
            val msgs    by vm.messages.collectAsStateWithLifecycle()
            val contact by vm.contact.collectAsStateWithLifecycle()
            ChatScreen(
                contact     = contact,
                messages    = msgs,
                onBack      = { navController.popBackStack() },
                onSendText  = vm::sendText,
                onSendFile  = vm::sendFile,
                onStartCall = {
                    callVm.startCall(pubkey)
                    navController.navigate("call")
                },
            )
        }

        composable("call") {
            val contact = contacts.find { it.pubkeyHex == callPubkey }
            CallScreen(
                contact   = contact,
                status    = callStatus,
                duration  = callDuration,
                onAnswer  = { callVm.answerCall(true) },
                onDecline = { callVm.answerCall(false); navController.popBackStack() },
                onHangup  = { callVm.hangup(); navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack       = { navController.popBackStack() },
                onOpenPeers  = { navController.navigate("peers") },
            )
        }

        composable("peers") {
            val ctx = LocalContext.current
            PeersScreen(
                peers            = peers,
                onBack           = { navController.popBackStack() },
                onToggle         = settingsVm::togglePeer,
                onAdd            = settingsVm::addPeer,
                onRemove         = settingsVm::removePeer,
                onRestartService = {
                    // Останавливаем и перезапускаем сервис с новыми пирами
                    ctx.stopService(android.content.Intent(ctx, ConnectionService::class.java))
                    ContextCompat.startForegroundService(
                        ctx, android.content.Intent(ctx, ConnectionService::class.java)
                    )
                },
            )
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            existingKeys = contacts.map { it.pubkeyHex }.toSet(),
            onDismiss    = { showAddDialog = false },
            onAdd        = { key, name -> contactsVm.addContact(key, name) }
        )
    }

    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.RECEIVING) {
            navController.navigate("call") { launchSingleTop = true }
        }
    }
}
