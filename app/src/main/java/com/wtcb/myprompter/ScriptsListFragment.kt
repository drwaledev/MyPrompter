package com.wtcb.myprompter

import android.content.Intent
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

class ScriptsListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: TextInputEditText
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateText: TextView

    private lateinit var adapter: ScriptsAdapter
    private lateinit var storageManager: StorageManager
    private var allScripts = listOf<Script>()

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
        loadScripts()
    }

    private fun setupRecyclerView() {
        adapter = ScriptsAdapter(
            scripts = emptyList(),
            onLoad = { script -> loadScript(script) },
            onShare = { script -> shareScript(script) },
            onDelete = { script -> deleteScript(script) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterScripts(s.toString())
            }
        })
    }

    private fun loadScripts() {
        allScripts = storageManager.getAllScripts()
        adapter.updateData(allScripts)
        updateEmptyState()
    }

    private fun filterScripts(query: String) {
        val filtered = if (query.isEmpty()) {
            allScripts
        } else {
            storageManager.searchScripts(query)
        }
        adapter.updateData(filtered)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (allScripts.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyStateText.text = "No scripts yet"
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadScript(script: Script) {
        val intent = Intent().apply {
            putExtra("SCRIPT_CONTENT", script.content)
        }
        requireActivity().setResult(android.app.Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun shareScript(script: Script) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, script.title)
            putExtra(Intent.EXTRA_TEXT, script.content)
        }
        startActivity(Intent.createChooser(shareIntent, "Share script via"))
    }

    private fun deleteScript(script: Script) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Script?")
            .setMessage("Are you sure you want to delete \"${script.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                storageManager.deleteScript(script.id)
                loadScripts()
                Toast.makeText(requireContext(), "Script deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadScripts()
    }
}