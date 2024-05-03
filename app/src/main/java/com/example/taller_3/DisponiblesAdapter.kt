package com.example.taller_3

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase

class DisponiblesAdapter(context: Context, users: List<User>) : ArrayAdapter<User>(context, 0, users) {

    private var storage = Firebase.storage

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView ?: LayoutInflater.from(context).inflate(R.layout.activity_disponibles_adapter, parent, false)
        val user = getItem(position)
        val tvName = convertView.findViewById<TextView>(R.id.textUserName)
        val ivImage = convertView.findViewById<ImageView>(R.id.imageUser)
        val ivButton = convertView.findViewById<Button>(R.id.buttonViewLocation)

        tvName.text = "${user?.name} ${user?.lastName}"

        user?.contactImageUrl?.let {
            val imageRef = storage.reference.child(it)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Picasso.get()
                    .load(uri.toString())
                    .into(ivImage)
            }.addOnFailureListener {
                // Handle any errors
            }
        }

        ivButton.setOnClickListener {
            val intent = Intent(context, MapsActivity::class.java)
            intent.putExtra("User", user)
            context.startActivity(intent)
        }

        return convertView
    }
}
