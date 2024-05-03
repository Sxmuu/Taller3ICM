package com.example.taller_3

import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class ListaDisponibles : AppCompatActivity() {


    private lateinit var listView: ListView
    private lateinit var adapter: DisponiblesAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_disponibles)

        auth = Firebase.auth

        listView = findViewById(R.id.listaDisponibles)
        adapter = DisponiblesAdapter(this, ArrayList())
        listView.adapter = adapter

        loadUsersFromFirebase()
    }

    private fun loadUsersFromFirebase() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = ArrayList<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if(user?.uid == auth.currentUser?.uid){
                        continue
                    }
                    if(user?.disponible == false){
                        continue
                    }
                    user?.let { users.add(it) }
                }
                adapter.clear()
                adapter.addAll(users)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListaDisponibles", "Failed to read users.", error.toException())
            }
        })
    }

}