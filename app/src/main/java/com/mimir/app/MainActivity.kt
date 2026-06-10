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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mimir.app.data.MimirBridge
import com.mimir.app.service.ConnectionService
import com.mimir.app.ui.components.AddContactDialog
import com.mimir.app.ui.screens.*
import com.mimir.app.ui.theme.MimirTheme
import uniffi.mimir.CallStatus

class MainActivity : ComponentActivity() {

    private val callVm: CallViewModel by viewModels()
    private val contactsVm: ContactsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* результаты разрешений */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        startService()

        setContent {
            MimirTheme {
                MimirApp(
                    callVm     = callVm,
                    contactsVm = contactsVm,
                    initialOpenChat = intent.getStringExtra("open_chat"),
                    initialIncomingCall = intent.getStringExtra("incoming_call"),
                )
            }
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
    callVm: CallViewModel,
    contactsVm: ContactsViewModel,
    initialOpenChat: String? = null,
    initialIncomingCall: String? = null,
) {
    val navController = rememberNavController()
    val contacts by contactsVm.contacts.collectAsStateWithLifecycle()
    val callStatus by callVm.callStatus.collectAsStateWithLifecycle()
    val callPubkey by callVm.callPubkey.collectAsStateWithLifecycle()
    val callDuration by callVm.callDuration.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Входящий звонок из intent
    LaunchedEffect(initialIncomingCall) {
        initialIncomingCall?.let { navController.navigate("call") }
    }

    // Переход к чату из уведомления
    LaunchedEffect(initialOpenChat) {
        initialOpenChat?.let { navController.navigate("chat/$it") }
    }

    NavHost(navController = navController, startDestination = "contacts") {

        composable("contacts") {
            val myKey = MimirBridge.publicKey()?.let {
                with(MimirBridge) { it.toHex() }
            } ?: ""

            ContactListScreen(
                contacts    = contacts,
                onOpenChat  = { navController.navigate("chat/$it") },
                onAddContact = { showAddDialog = true },
                myPubkeyHex = myKey,
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
                contact    = contact,
                messages   = msgs,
                onBack     = { navController.popBackStack() },
                onSendText = vm::sendText,
                onSendFile = vm::sendFile,
                onStartCall = {
                    callVm.startCall(pubkey)
                    navController.navigate("call")
                },
            )
        }

        composable("call") {
            val contact = contacts.find { it.pubkeyHex == callPubkey }
            CallScreen(
                contact  = contact,
                status   = callStatus,
                duration = callDuration,
                onAnswer  = { callVm.answerCall(true) },
                onDecline = { callVm.answerCall(false); navController.popBackStack() },
                onHangup  = { callVm.hangup(); navController.popBackStack() },
            )
        }
    }

    // Диалог добавления контакта
    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { key, name -> contactsVm.addContact(key, name) }
        )
    }

    // Автоматический переход к экрану звонка при входящем
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.RECEIVING) {
            navController.navigate("call") {
                launchSingleTop = true
            }
        }
    }
}
