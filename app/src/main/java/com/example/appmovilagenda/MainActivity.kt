package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var todoEditText: EditText
    private lateinit var addButton: Button
    private lateinit var signOutButton: Button
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<Todo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else if (!user.isEmailVerified) {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerView)
        todoEditText = findViewById(R.id.todoEditText)
        addButton = findViewById(R.id.addButton)
        signOutButton = findViewById(R.id.signOutButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        todoAdapter = TodoAdapter(todoList)
        recyclerView.adapter = todoAdapter

        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        addButton.setOnClickListener {
            val todoText = todoEditText.text.toString()
            if (todoText.isNotEmpty()) {
                val todo = Todo(text = todoText, userId = auth.currentUser!!.uid)
                db.collection("todos").add(todo)
                todoEditText.text.clear()
            }
        }

        loadTodos()
    }

    private fun loadTodos() {
        db.collection("todos")
            .whereEqualTo("userId", auth.currentUser!!.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                todoList.clear()
                for (doc in snapshots!!) {
                    val todo = doc.toObject(Todo::class.java).copy(id = doc.id)
                    todoList.add(todo)
                }
                todoAdapter.notifyDataSetChanged()
            }
    }
}