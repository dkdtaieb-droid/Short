package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArchiveRepository
import com.example.model.ArchiveItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface DialogState {
    object None : DialogState
    object NewFolder : DialogState
    object CreateZip : DialogState
    data class Extract(val item: ArchiveItem) : DialogState
    data class ViewArchive(val item: ArchiveItem) : DialogState
    data class TextPreview(val title: String, val content: String) : DialogState
}

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ArchiveRepository(application)

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _items = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val items: StateFlow<List<ArchiveItem>> = _items.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isMultiSelect = MutableStateFlow(false)
    val isMultiSelect: StateFlow<Boolean> = _isMultiSelect.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activeDialog = MutableStateFlow<DialogState>(DialogState.None)
    val activeDialog: StateFlow<DialogState> = _activeDialog.asStateFlow()

    private val _archiveEntries = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val archiveEntries: StateFlow<List<ArchiveItem>> = _archiveEntries.asStateFlow()

    init {
        loadDirectory("")
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            _selectedPaths.value = emptySet()
            _isMultiSelect.value = false
            _items.value = repository.getItemsInPath(path)
            _isLoading.value = false
        }
    }

    fun navigateBack() {
        val current = _currentPath.value
        if (current.isEmpty()) return
        val parent = File(current).parent ?: ""
        loadDirectory(if (parent == "/") "" else parent)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val all = repository.getItemsInPath(_currentPath.value)
            if (query.isBlank()) {
                _items.value = all
            } else {
                _items.value = all.filter { it.name.contains(query, ignoreCase = true) }
            }
        }
    }

    fun toggleSelection(path: String) {
        val current = _selectedPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedPaths.value = current
        if (current.isEmpty()) {
            _isMultiSelect.value = false
        }
    }

    fun setMultiSelect(enabled: Boolean) {
        _isMultiSelect.value = enabled
        if (!enabled) {
            _selectedPaths.value = emptySet()
        }
    }

    fun selectAll() {
        _selectedPaths.value = _items.value.map { it.path }.toSet()
        _isMultiSelect.value = true
    }

    fun createNewFolder(folderName: String) {
        viewModelScope.launch {
            if (folderName.isBlank()) return@launch
            repository.createDirectory(_currentPath.value, folderName.trim())
            loadDirectory(_currentPath.value)
            showSnackbar("Folder created successfully")
            dismissDialog()
        }
    }

    fun createZip(archiveName: String) {
        viewModelScope.launch {
            if (archiveName.isBlank()) return@launch
            val selected = _selectedPaths.value.toList()
            if (selected.isEmpty()) {
                showSnackbar("No items selected for archiving")
                return@launch
            }
            _isLoading.value = true
            val result = repository.createZipArchive(_currentPath.value, archiveName.trim(), selected)
            _isLoading.value = false
            if (result.isSuccess) {
                showSnackbar("Archive created successfully")
                loadDirectory(_currentPath.value)
            } else {
                showSnackbar("Failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
            dismissDialog()
        }
    }

    fun extractArchive(item: ArchiveItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.extractArchive(item.path, _currentPath.value)
            _isLoading.value = false
            if (result.isSuccess) {
                showSnackbar("Extracted successfully")
                loadDirectory(_currentPath.value)
            } else {
                showSnackbar("Extraction failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
            dismissDialog()
        }
    }

    fun openArchive(item: ArchiveItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val entries = repository.getArchiveEntries(item.path)
            _archiveEntries.value = entries
            _isLoading.value = false
            _activeDialog.value = DialogState.ViewArchive(item)
        }
    }

    fun openFile(item: ArchiveItem) {
        viewModelScope.launch {
            if (item.isArchive) {
                openArchive(item)
            } else if (item.extension.lowercase() in listOf("txt", "log", "json", "xml", "md", "spec")) {
                _isLoading.value = true
                val content = repository.readFileContent(item.path)
                _isLoading.value = false
                _activeDialog.value = DialogState.TextPreview(item.name, content)
            } else {
                showSnackbar("Selected: ${item.name} (${item.size} bytes)")
            }
        }
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            val paths = _selectedPaths.value
            if (paths.isEmpty()) return@launch
            _isLoading.value = true
            for (path in paths) {
                repository.deleteItem(path)
            }
            _isLoading.value = false
            showSnackbar("Deleted ${paths.size} item(s)")
            loadDirectory(_currentPath.value)
        }
    }

    fun showDialog(state: DialogState) {
        _activeDialog.value = state
    }

    fun dismissDialog() {
        _activeDialog.value = DialogState.None
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
