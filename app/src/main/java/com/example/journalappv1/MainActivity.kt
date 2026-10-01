// MainActivity.kt
package com.example.journalappv1

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts

data class JournalEntry(
    val id: Long,
    val text: String,
    val timestamp: Long
)

class MainActivity : AppCompatActivity() {
    private lateinit var entryEditText: EditText
    private lateinit var postButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JournalAdapter
    private lateinit var paginationLayout: LinearLayout
    private lateinit var olderButton: Button
    private lateinit var newerButton: Button

    private val entries = mutableListOf<JournalEntry>()
    private var filteredEntries = mutableListOf<JournalEntry>()
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var searchQuery = ""
    private var isCustomFilter = false
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        entryEditText = findViewById(R.id.entryEditText)
        postButton = findViewById(R.id.postButton)
        recyclerView = findViewById(R.id.recyclerView)
        paginationLayout = findViewById(R.id.paginationLayout)
        olderButton = findViewById(R.id.olderButton)
        newerButton = findViewById(R.id.newerButton)

        adapter = JournalAdapter(filteredEntries, this) { entry ->
            showEntryOptionsDialog(entry)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        olderButton.setOnClickListener {
            currentPage++
            applyFilter()
        }
        newerButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyFilter()
            }
        }

        postButton.setOnClickListener { postEntry() }

        loadEntries()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasPin = PinManager.isPinEnabled(this)
        menu.findItem(R.id.action_set_pin).isVisible = !hasPin
        menu.findItem(R.id.action_change_pin).isVisible = hasPin
        menu.findItem(R.id.action_remove_pin).isVisible = hasPin
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> { showFilterDialog(); true }
            R.id.action_export -> { exportJournal(); true }
            R.id.action_export_txt -> { exportJournalAsText(); true }
            R.id.action_import -> { showImportWarningDialog(); true }
            R.id.action_about -> { showAboutDialog(); true }
            R.id.action_set_pin -> { showSetPinDialog(); true }
            R.id.action_change_pin -> { showChangePinDialog(); true }
            R.id.action_remove_pin -> { showRemovePinDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun postEntry() {
        val text = entryEditText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()
        val entry = JournalEntry(id = now, text = text, timestamp = now)
        entries.add(0, entry)
        saveEntries()

        // Reset to page 0 so the new entry is visible
        if (!isCustomFilter) currentPage = 0
        applyFilter()

        entryEditText.text.clear()
        Toast.makeText(this, "Entry posted", Toast.LENGTH_SHORT).show()
    }

    private fun showEntryOptionsDialog(entry: JournalEntry) {
        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy, hh:mm a", Locale.getDefault())
        AlertDialog.Builder(this)
            .setTitle(dateFormat.format(Date(entry.timestamp)))
            .setItems(arrayOf("Edit", "Copy", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditDialog(entry)
                    1 -> copyEntry(entry)
                    2 -> showDeleteDialog(entry)
                }
            }
            .show()
    }

    private fun showEditDialog(entry: JournalEntry) {
        val editText = EditText(this).apply {
            setText(entry.text)
            setSelection(entry.text.length)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            minLines = 4
            gravity = Gravity.TOP
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        val container = FrameLayout(this).apply {
            val margin = (16 * resources.displayMetrics.density).toInt()
            addView(editText, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margin, 0, margin, 0) })
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Entry")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) editEntry(entry, newText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editEntry(entry: JournalEntry, newText: String) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            entries[index] = entry.copy(text = newText)
            saveEntries()
            applyFilter()
            Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyEntry(entry: JournalEntry) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Journal Entry", entry.text))
        Toast.makeText(this, "Entry copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val searchEditText: EditText = dialogView.findViewById(R.id.searchEditText)
        val startDateButton: Button = dialogView.findViewById(R.id.startDateButton)
        val endDateButton: Button = dialogView.findViewById(R.id.endDateButton)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        var tempStartDate = startDate
        var tempEndDate = endDate

        searchEditText.setText(searchQuery)
        startDateButton.text = startDate?.let { dateFormat.format(Date(it)) } ?: "Select Start Date"
        endDateButton.text = endDate?.let { dateFormat.format(Date(it)) } ?: "Select End Date"

        val clearFilterButton: Button = dialogView.findViewById(R.id.clearFilterButton)
        clearFilterButton.setOnClickListener {
            searchEditText.text.clear()
            tempStartDate = null
            tempEndDate = null
            startDateButton.text = "Select Start Date"
            endDateButton.text = "Select End Date"
        }

        startDateButton.setOnClickListener {
            showDatePicker { tempStartDate = it; startDateButton.text = dateFormat.format(Date(it)) }
        }
        endDateButton.setOnClickListener {
            showDatePicker { tempEndDate = it; endDateButton.text = dateFormat.format(Date(it)) }
        }

        AlertDialog.Builder(this)
            .setTitle("Search & Filter")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                searchQuery = searchEditText.text.toString().trim()
                startDate = tempStartDate
                endDate = tempEndDate
                isCustomFilter = searchQuery.isNotEmpty() || startDate != null || endDate != null
                currentPage = 0
                applyFilter()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showDeleteDialog(entry: JournalEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Delete") { _, _ -> deleteEntry(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntry(entry: JournalEntry) {
        entries.remove(entry)
        saveEntries()
        applyFilter()
        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
    }

    private fun exportJournal() {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No entries to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val fileName = "journal_export_${System.currentTimeMillis()}.json"
            writeToDownloads(fileName, Gson().toJson(entries), "application/json")
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportJournalAsText() {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No entries to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy, hh:mm a", Locale.getDefault())
            val content = entries.joinToString("\n\n---\n\n") { entry ->
                "=== ${dateFormat.format(Date(entry.timestamp))} ===\n\n${entry.text}"
            }
            val fileName = "journal_export_${System.currentTimeMillis()}.txt"
            writeToDownloads(fileName, content, "text/plain")
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun writeToDownloads(fileName: String, content: String, mimeType: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { out -> out.write(content.toByteArray()) }
                Toast.makeText(this, "Exported to Downloads/$fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            java.io.File(downloadsDir, fileName).writeText(content)
            Toast.makeText(this, "Exported to Downloads/$fileName", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAboutDialog() {
        val content = assets.open("updates.md").bufferedReader().use { it.readText() }

        val textView = TextView(this).apply {
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        Markwon.create(this).setMarkdown(textView, content)

        val scrollView = ScrollView(this).apply { addView(textView) }

        AlertDialog.Builder(this)
            .setTitle("About / Help")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showImportWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Import Journal")
            .setMessage("Warning: Importing will erase all existing journal entries and replace them with the imported data. This cannot be undone.\n\nDo you want to continue?")
            .setPositiveButton("Import") { _, _ -> openFilePicker() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFilePicker() {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, IMPORT_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_FILE_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { importJournal(it) }
        }
    }

    private fun importJournal(uri: android.net.Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) {
                val type = object : TypeToken<List<JournalEntry>>() {}.type
                val importedEntries: List<JournalEntry> = Gson().fromJson(json, type)
                entries.clear()
                entries.addAll(importedEntries)
                entries.sortByDescending { it.timestamp }
                saveEntries()
                searchQuery = ""
                isCustomFilter = false
                currentPage = 0
                applyFilter()
                Toast.makeText(this, "Imported ${importedEntries.size} entries successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyFilter() {
        filteredEntries.clear()
        if (isCustomFilter) {
            val query = searchQuery.lowercase()
            filteredEntries.addAll(entries.filter { entry ->
                val matchesStart = startDate?.let { entry.timestamp >= it } ?: true
                val matchesEnd = endDate?.let { entry.timestamp <= it + 86400000L } ?: true
                val matchesSearch = query.isEmpty() || entry.text.lowercase().contains(query)
                matchesStart && matchesEnd && matchesSearch
            })
            paginationLayout.visibility = View.GONE
        } else {
            val now = System.currentTimeMillis()
            val pageMs = 30L * 24 * 60 * 60 * 1000
            val windowEnd = now - currentPage * pageMs
            val windowStart = windowEnd - pageMs
            filteredEntries.addAll(entries.filter { it.timestamp in windowStart..windowEnd })
            paginationLayout.visibility = View.VISIBLE
            newerButton.isEnabled = currentPage > 0
        }
        adapter.notifyDataSetChanged()
    }

    private fun saveEntries() {
        getSharedPreferences("JournalApp", MODE_PRIVATE)
            .edit().putString("entries", Gson().toJson(entries)).apply()
    }

    private fun loadEntries() {
        val json = getSharedPreferences("JournalApp", MODE_PRIVATE).getString("entries", null)
        if (json != null) {
            val type = object : TypeToken<List<JournalEntry>>() {}.type
            entries.addAll(Gson().fromJson<List<JournalEntry>>(json, type))
        }
        applyFilter()
    }

    companion object {
        private const val IMPORT_FILE_REQUEST_CODE = 1001
    }

    private val lockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLockScreenShowing = false
        if (result.resultCode != RESULT_OK) {
            finish()
        }
    }

    private var isLockScreenShowing = false

    private fun checkLock() {
        if (PinManager.isPinEnabled(this) && !AppLockState.isUnlocked && !isLockScreenShowing) {
            isLockScreenShowing = true
            lockLauncher.launch(Intent(this, LockActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        checkLock()
        updateSecureFlag()
    }

    private fun updateSecureFlag() {
        if (PinManager.isPinEnabled(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun pinInputField(): EditText = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = "Enter PIN"
    }

    private fun showSetPinDialog() {
        val pinField = pinInputField()
        AlertDialog.Builder(this)
            .setTitle("Set PIN")
            .setMessage("Also disables screenshots while a PIN is set.")
            .setView(pinField)
            .setPositiveButton("Next") { _, _ ->
                val pin = pinField.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                showConfirmPinDialog(pin)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showConfirmPinDialog(pin: String) {
        val confirmField = pinInputField()
        AlertDialog.Builder(this)
            .setTitle("Confirm PIN")
            .setView(confirmField)
            .setPositiveButton("Save") { _, _ ->
                if (confirmField.text.toString() == pin) {
                    PinManager.setPin(this, pin)
                    AppLockState.isUnlocked = true
                    updateSecureFlag()
                    Toast.makeText(this, "PIN set", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PINs didn't match", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePinDialog() {
        val currentField = pinInputField().apply { hint = "Current PIN" }
        AlertDialog.Builder(this)
            .setTitle("Change PIN")
            .setView(currentField)
            .setPositiveButton("Next") { _, _ ->
                if (PinManager.verifyPin(this, currentField.text.toString())) {
                    showSetPinDialog()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemovePinDialog() {
        val currentField = pinInputField().apply { hint = "Current PIN" }
        AlertDialog.Builder(this)
            .setTitle("Remove PIN")
            .setView(currentField)
            .setPositiveButton("Remove") { _, _ ->
                if (PinManager.verifyPin(this, currentField.text.toString())) {
                    PinManager.removePin(this)
                    updateSecureFlag()
                    Toast.makeText(this, "PIN removed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}

class JournalAdapter(
    private val entries: List<JournalEntry>,
    context: Context,
    private val onTitleClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.ViewHolder>() {

    private val markwon = Markwon.create(context)

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val dateTimeText: TextView = view.findViewById(R.id.dateTimeText)
        val entryText: TextView = view.findViewById(R.id.entryText)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy, hh:mm a", Locale.getDefault())
        holder.dateTimeText.text = dateFormat.format(Date(entry.timestamp))
        markwon.setMarkdown(holder.entryText, entry.text)
        holder.dateTimeText.setOnClickListener { onTitleClick(entry) }
    }

    override fun getItemCount() = entries.size
}
