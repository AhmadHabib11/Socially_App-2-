package com.teamapex.I23_0011_I23_0646

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserSearchAdapter(
    private val users: MutableList<MyData>,
    private val onClick: (MyData) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], onClick)
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<MyData>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val username: TextView = itemView.findViewById(R.id.tvUsername)
        private val fullName: TextView = itemView.findViewById(R.id.tvFullName)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val statusText: TextView = itemView.findViewById(R.id.tvStatusText)

        fun bind(user: MyData, onClick: (MyData) -> Unit) {
            username.text = user.name
            fullName.text = user.name // You might want a separate fullName field in MyData

            // Optionally show online status if you have that data
            // statusText.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            // statusIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE

            // Load profile image using Glide if you have the URL
            // Glide.with(itemView.context)
            //     .load(user.profileImageUrl)
            //     .placeholder(R.drawable.profilepic)
            //     .into(profileImage)

            itemView.setOnClickListener {
                onClick(user)
            }
        }
    }
}