package com.example.ui.screens

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
import com.example.ui.ArchiveViewModel
import com.example.ui.DialogState
import com.example.ui.components.ArchiveDialogs
import com.example.ui.components.ArchiveItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel) {
    val currentPath by viewModel.currentPath.collectAsState()
    val items by viewModel.items.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val isMultiSelect by viewModel.isMultiSelect.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val dialogState by viewModel.activeDialog.collectAsState()
    val archiveEntries by viewModel.archiveEntries.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    } else {
                        Column {
                            Text(if (currentPath.isEmpty()) "RAR Archiver (Root)" else "RAR Archiver / $currentPath")
                            if (currentPath.isNotEmpty()) {
                                Text(
                                    text = "Tap to go back",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (currentPath.isNotEmpty()) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isMultiSelect) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    } else {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { viewModel.showDialog(DialogState.NewFolder) }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (selectedPaths.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedPaths.size} item(s) selected")
                        Button(
                            onClick = { viewModel.showDialog(DialogState.CreateZip) }
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compress to ZIP")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isMultiSelect && selectedPaths.isEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.showDialog(DialogState.NewFolder) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Folder")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This folder is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)
                        ArchiveItemRow(
                            item = item,
                            isSelected = isSelected,
                            isMultiSelect = isMultiSelect,
                            onClick = {
                                if (isMultiSelect) {
                                    viewModel.toggleSelection(item.path)
                                } else if (item.isDirectory) {
                                    viewModel.loadDirectory(item.path)
                                } else {
                                    viewModel.openFile(item)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelect) {
                                    viewModel.setMultiSelect(true)
                                    viewModel.toggleSelection(item.path)
                                }
                            },
                            onExtractClick = {
                                viewModel.showDialog(DialogState.Extract(item))
                            },
                            onToggleSelect = {
                                viewModel.toggleSelection(item.path)
                            }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }

            ArchiveDialogs(
                dialogState = dialogState,
                archiveEntries = archiveEntries,
                onDismiss = { viewModel.dismissDialog() },
                onCreateFolder = { viewModel.createNewFolder(it) },
                onCreateZip = { viewModel.createZip(it) },
                onExtract = { viewModel.extractArchive(it) }
            )
        }
    }
}
