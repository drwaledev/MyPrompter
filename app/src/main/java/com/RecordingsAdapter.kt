package com.wtcb.myprompter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class RecordingsAdapter(
    private var recordings: List<VideoRecording>,
    private val onPlay: (VideoRecording) -> Unit,
    private val onShare: (VideoRecording) -> Unit,
    private val onDelete: (VideoRecording) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    class RecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.recordingTitle)
        val dateTime: TextView = view.findViewById(R.id.recordingDateTime)
        val duration: TextView = view.findViewById(R.id.recordingDuration)
        val size: TextView = view.findViewById(R.id.recordingSize)
        val btnPlay: MaterialButton = view.findViewById(R.id.btnPlay)
        val btnShare: MaterialButton = view.findViewById(R.id.btnShareRecording)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteRecording)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]

        holder.title.text = recording.title
        holder.dateTime.text = recording.getFormattedDate()
        holder.duration.text = recording.getFormattedDuration()
        holder.size.text = recording.getFormattedSize()

        holder.btnPlay.setOnClickListener { onPlay(recording) }
        holder.btnShare.setOnClickListener { onShare(recording) }
        holder.btnDelete.setOnClickListener { onDelete(recording) }
    }

    override fun getItemCount() = recordings.size

    fun updateData(newRecordings: List<VideoRecording>) {
        recordings = newRecordings
        notifyDataSetChanged()
    }
}