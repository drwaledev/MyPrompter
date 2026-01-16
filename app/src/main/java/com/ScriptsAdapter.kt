package com.wtcb.myprompter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ScriptsAdapter(
    private var scripts: List<Script>,
    private val onLoad: (Script) -> Unit,
    private val onShare: (Script) -> Unit,
    private val onDelete: (Script) -> Unit
) : RecyclerView.Adapter<ScriptsAdapter.ScriptViewHolder>() {

    class ScriptViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.scriptTitle)
        val preview: TextView = view.findViewById(R.id.scriptPreview)
        val date: TextView = view.findViewById(R.id.scriptDate)
        val wordCount: TextView = view.findViewById(R.id.scriptWordCount)
        val btnLoad: MaterialButton = view.findViewById(R.id.btnLoad)
        val btnShare: MaterialButton = view.findViewById(R.id.btnShare)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]

        holder.title.text = script.title
        holder.preview.text = script.getPreview(100)
        holder.date.text = script.getFormattedDate()

        val wordCount = script.content.trim().split("\\s+".toRegex()).size
        holder.wordCount.text = "$wordCount words"

        holder.btnLoad.setOnClickListener { onLoad(script) }
        holder.btnShare.setOnClickListener { onShare(script) }
        holder.btnDelete.setOnClickListener { onDelete(script) }
    }

    override fun getItemCount() = scripts.size

    fun updateData(newScripts: List<Script>) {
        scripts = newScripts
        notifyDataSetChanged()
    }
}