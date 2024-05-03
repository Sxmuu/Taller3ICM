package com.example.taller_3

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.LocationRequest
import android.net.Uri
import android.os.Bundle
import android.renderscript.RenderScript
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller_3.databinding.ActivityPantalla2Binding
import com.example.taller_3.databinding.ActivityRegistroBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log


class Registro : AppCompatActivity(){

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var myRef: DatabaseReference
    private lateinit var user: User
    private lateinit var imageView: ImageView
    private val database = Firebase.database
    private var storage = Firebase.storage

    private var storageRef = storage.reference

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageView.setImageURI(uri)
        }
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            imageView.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        user = User()
        imageView = binding.ivImage



        binding.btnTakePhoto.setOnClickListener {
            requestCamera()
        }

        binding.btnSelectImage.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnRegister.setOnClickListener {

            user.email = binding.etEmail.text.toString()
            user.password = binding.etPassword.text.toString()
            user.name = binding.etFirstName.text.toString()
            user.lastName = binding.etLastName.text.toString()
            user.identificationNumber = binding.etIdentification.text.toString()

            createUser(user.email, user.password)
        }


    }

    private fun requestCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the camera
                takePicturePreview.launch(null)
            }
            else -> {
                // Request camera permission
                requestPermission.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission is granted, you can take a picture
            takePicturePreview.launch(null)
        } else {
            // Permission is denied, show a message to the user
        }
    }

    private fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if(firebaseUser!= null){
                        user.uid = firebaseUser.uid
                    }

                    myRef = database.getReference(Companion.PATH_USERS+ user.uid)
                    uploadImageFromImageView(imageView)

                    myRef.setValue(user)

                    Toast.makeText(this, "createUserWithEmail:Success", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, Pantalla2::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "createUserWithEmail:Failure: " + task.exception.toString(),
                        Toast.LENGTH_SHORT).show()
                    task.exception?.message?.let { Log.e(TAG, it) }
                }
            }
    }

    companion object {
        const val PATH_USERS = "users/"
    }

    private fun uploadImageFromImageView(imageView: ImageView) {
        // Obtener el Bitmap del ImageView
        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache()
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        // Crear un archivo temporal para guardar el bitmap
        val file = File.createTempFile("profile_photo", ".jpg", getExternalFilesDir(null))
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()

        // Crear la referencia de Firebase Storage
        val imageRef = storage.reference.child("users/${user.uid}/${file.name}")

        user.contactImageUrl = "users/${user.uid}/${file.name}"


        // Subir el archivo a Firebase Storage
        val fileUri = Uri.fromFile(file)
        imageRef.putFile(fileUri)
            .addOnSuccessListener {
                // Se ejecuta si la carga fue exitosa
                Log.i("FBApp", "Successfully uploaded image")
            }
            .addOnFailureListener { exception ->
                // Se ejecuta si la carga falla
                Log.e("FBApp", "Failed to upload image", exception)
            }
    }

}