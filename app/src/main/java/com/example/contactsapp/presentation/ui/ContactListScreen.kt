package com.example.contactsapp.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.contactsapp.data.service.DuplicateContactResult
import com.example.contactsapp.data.service.DuplicateContactServiceClient
import com.example.contactsapp.presentation.viewmodel.ContactsViewModel
import com.example.contactsapp.util.makePhoneCall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val client = remember(context) { DuplicateContactServiceClient(context) }

    var pendingPhoneNumber by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Разрешение на звонки
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingPhoneNumber != null) {
            makePhoneCall(context, pendingPhoneNumber!!)
            pendingPhoneNumber = null
        } else {
            Toast.makeText(context, "Разрешение на звонки не предоставлено", Toast.LENGTH_SHORT).show()
        }
    }

    // Повторная загрузка контактов при возврате на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadContacts(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Привязка к сервису удаления дубликатов
    DisposableEffect(Unit) {
        client.bindService()
        onDispose { client.unbindService() }
    }

    // Начальная загрузка контактов
    LaunchedEffect(Unit) {
        viewModel.loadContacts(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Контакты") }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Удалить одинаковые контакты")
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            // Экран загрузки
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Загрузка контактов...")
            }
        } else {
            // Группировка контактов по первой букве имени
            val groupedContacts = contacts.groupBy {
                it.name.firstOrNull()?.uppercaseChar() ?: '#'
            }.toSortedMap()

            // Отображение сгруппированных контактов
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                groupedContacts.forEach { (initial, contactsForLetter) ->
                    // Заголовок группы
                    item {
                        Text(
                            text = initial.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Контакты в группе
                    items(contactsForLetter.size) { index ->
                        val contact = contactsForLetter[index]
                        ContactListItem(contact = contact) {
                            val phone = it.phoneNumber
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CALL_PHONE
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                makePhoneCall(context, phone)
                            } else {
                                pendingPhoneNumber = phone
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления дубликатов
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить дубликаты?") },
                text = { Text("Вы уверены, что хотите удалить дублирующиеся контакты?") },
                confirmButton = {
                    Text(
                        text = "Удалить",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                showDeleteDialog = false
                                client.removeDuplicateContacts { result ->
                                    when (result) {
                                        DuplicateContactResult.SUCCESS -> {
                                            viewModel.loadContacts(context)
                                            Toast.makeText(context, "Дубликаты удалены", Toast.LENGTH_SHORT).show()
                                        }
                                        DuplicateContactResult.NO_DUPLICATES_FOUND -> {
                                            Toast.makeText(context, "Дубликаты не найдены", Toast.LENGTH_SHORT).show()
                                        }
                                        DuplicateContactResult.ERROR -> {
                                            Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                    )
                },
                dismissButton = {
                    Text(
                        text = "Отмена",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showDeleteDialog = false }
                    )
                }
            )
        }
    }
}
