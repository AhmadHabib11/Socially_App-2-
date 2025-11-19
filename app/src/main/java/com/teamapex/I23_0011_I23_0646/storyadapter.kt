package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StoryAdapter(
    private val context: Context,
    private val stories: List<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val storyProfileImage: ImageView = view.findViewById(R.id.storyProfileImage)
        val storyUserName: TextView = view.findViewById(R.id.storyUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        // Set username
        holder.storyUserName.text = story.username

        // Load profile picture
        if (story.profilePic.isNotEmpty()) {
            // If profile pic is a path, you might need to load it differently
            // For now, we'll use a placeholder or default image
            holder.storyProfileImage.setImageResource(R.drawable.settings)
        } else {
            holder.storyProfileImage.setImageResource(R.drawable.settings)
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }

    override fun getItemCount(): Int = stories.size
}