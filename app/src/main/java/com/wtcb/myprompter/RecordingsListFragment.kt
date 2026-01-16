package com.wtcb.myprompter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class RecordingsListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateText: TextView

    private lateinit var adapter: RecordingsAdapter
    private lateinit var storageManager: StorageManager
    private var allRecordings = listOf<VideoRecording>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scripts_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storageManager = StorageManager(requireContext())

        recyclerView = view.findViewById(R.id.recyclerView)
        searchInput = view.findViewById(R.id.searchInput)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        setupRecyclerView()
        setupSearch()
        loadRecordings()
    }

    private fun setupRecyclerView() {
        adapter = RecordingsAdapter(
            recordings = emptyList(),
            onPlay = { recording -> playRecording(recording) },
            onShare = { recording -> shareRecording(recording) },
            onDelete = { recording -> deleteRecording(recording) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterRecordings(s.toString())
            }
        })
    }

    private fun loadRecordings() {
        allRecordings = storageManager.getAllRecordings()
        adapter.updateData(allRecordings)
        updateEmptyState()
    }

    private fun filterRecordings(query: String) {
        val filtered = if (query.isEmpty()) {
            allRecordings
        } else {
            storageManager.searchRecordings(query)
        }
        adapter.updateData(filtered)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (allRecordings.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyStateText.text = "No recordings yet"
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun playRecording(recording: VideoRecording) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(recording.uri), "video/*")
        }
        try {
            startActivity(Intent.createChooser(intent, "Play video with"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No video player found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecording(recording: VideoRecording) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(recording.uri))
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Share video via"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to share video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecording(recording: VideoRecording) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Recording?")
            .setMessage("Are you sure you want to delete this recording?")
            .setPositiveButton("Delete") { _, _ ->
                storageManager.deleteRecording(recording.id)

                try {
                    requireContext().contentResolver.delete(Uri.parse(recording.uri), null, null)
                } catch (e: Exception) {
                    // Ignore
                }

                loadRecordings()
                Toast.makeText(requireContext(), "Recording deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadRecordings()
    }
}