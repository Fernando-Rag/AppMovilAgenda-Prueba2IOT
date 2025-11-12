package com.example.appmovilagenda

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class TodoAdapter(private val todoList: List<Todo>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentTodo = todoList[position]
        holder.textView.text = currentTodo.text
        holder.checkBox.isChecked = currentTodo.completed

        if (currentTodo.completed) {
            holder.textView.paintFlags = holder.textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.textView.paintFlags =
                holder.textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.checkBox.setOnClickListener {
            currentTodo.completed = !currentTodo.completed
            db.collection("todos").document(currentTodo.id).set(currentTodo)
        }
    }

    override fun getItemCount() = todoList.size
}