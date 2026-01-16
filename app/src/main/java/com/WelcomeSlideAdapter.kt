package com.wtcb.myprompter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WelcomeSlideAdapter(
    private val slides: List<WelcomeSlide>
) : RecyclerView.Adapter<WelcomeSlideAdapter.SlideViewHolder>() {

    class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.slideEmoji)
        val title: TextView = view.findViewById(R.id.slideTitle)
        val description: TextView = view.findViewById(R.id.slideDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_welcome_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.emoji.text = slide.emoji
        holder.title.text = slide.title
        holder.description.text = slide.description
    }

    override fun getItemCount() = slides.size
}