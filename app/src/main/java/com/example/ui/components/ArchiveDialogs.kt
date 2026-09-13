package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.ArchiveItem
import com.example.ui.DialogState

@Composable
fun ArchiveDialogs(
    dialogState: DialogState,
    archiveEntries: List<ArchiveItem>,
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateZip: (String) -> Unit,
    onExtract: (ArchiveItem) -> Unit
) {
    when (dialogState) {
        is DialogState.None -> {}
        is DialogState.NewFolder -> {
            var folderName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Create New Folder") },
                text = {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { onCreateFolder(folderName) }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is DialogState.CreateZip -> {
            var zipName by remember { mutableStateOf("archive.zip") }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Create ZIP Archive") },
                text = {
                    Column {
                        Text("Compress selected items into a ZIP archive.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = zipName,
                            onValueChange = { zipName = it },
                            label = { Text("Archive Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { onCreateZip(zipName) }) {
                        Text("Compress")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is DialogState.Extract -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Extract Archive") },
                text = {
                    Text("Extract '${dialogState.item.name}' to current folder?")
                },
                confirmButton = {
                    Button(onClick = { onExtract(dialogState.item) }) {
                        Text("Extract Here")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is DialogState.ViewArchive -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Archive Contents: ${dialogState.item.name}") },
                text = {
                    Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                        if (archiveEntries.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Archive is empty or invalid format")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(archiveEntries) { entry ->
                                    ListItem(
                                        headlineContent = { Text(entry.name) },
                                        supportingContent = { Text("${entry.size} bytes") },
                                        leadingContent = {
                                            Icon(
                                                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                                contentDescription = null,
                                                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    )
                                    Divider(modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onExtract(dialogState.item) }) {
                        Text("Extract All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            )
        }
        is DialogState.TextPreview -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(dialogState.title) },
                text = {
                    Box(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = dialogState.content,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) { Text("Done") }
                }
            )
        }
    }
}
