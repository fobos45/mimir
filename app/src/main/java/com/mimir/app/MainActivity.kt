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
import com.mimir.app.ui.screens.*import com.mimir.app.ui.theme.MimirTheme
import uniffi.mimir.CallStatus

class MainActivity : ComponentActivity() {

    private val callVm: CallViewModel by viewModels()
    private val contactsVm: ContactsViewModel by viewModels()

    // Храним ссылку на navController чтобы использовать в onNewIntent
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
                // Сохраняем ссылку для onNewIntent
                LaunchedEffect(nav) { navController = nav }

                MimirApp(
                    navController = nav,
                    callVm        = callVm,
                    contactsVm    = contactsVm,
                    initialIntent = intent,
                )
            }
        }
    }

    // Вызывается когда приложение уже открыто и пришёл новый Intent
    // (например тап на уведомление при открытом приложении)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val nav = navController ?: return
        intent.getStringExtra("open_chat")?.let { pubkey ->
            nav.navigate("chat/$pubkey") {
                launchSingleTop = true
                // Не добавляем дубликат если уже на этом экране
                restoreState = false
            }
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
    initialIntent: Intent?,
) {
    val contacts         by contactsVm.contacts.collectAsStateWithLifecycle()
    val callStatus       by callVm.callStatus.collectAsStateWithLifecycle()
    val callPubkey       by callVm.callPubkey.collectAsStateWithLifecycle()
    val callDuration     by callVm.callDuration.collectAsStateWithLifecycle()
    val connectionState  by contactsVm.connectionState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Переход при первом запуске из intent (приложение было закрыто)
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
    }

    // Диалог добавления контакта
    if (showAddDialog) {
        AddContactDialog(
            existingKeys = contacts.map { it.pubkeyHex }.toSet(),
            onDismiss    = { showAddDialog = false },
            onAdd        = { key, name -> contactsVm.addContact(key, name) }
        )
    }

    // Автоматический переход к экрану звонка при входящем
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.RECEIVING) {
            navController.navigate("call") { launchSingleTop = true }
        }
    }
}
