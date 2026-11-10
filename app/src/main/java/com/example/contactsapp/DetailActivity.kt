package com.example.contactsapp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class DetailActivity : AppCompatActivity() {

    private lateinit var imgPhotoDetail: ImageView
    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etTelephone: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnAppeler: Button
    private lateinit var btnEmail: Button
    private lateinit var btnGalerie: Button
    private lateinit var btnCamera: Button
    private lateinit var btnEnregistrer: Button
    private lateinit var btnAnnuler: Button

    private var contactId: Int = 0
    private var position: Int = -1
    private var photoUri: String? = null

    private var cameraFile: File? = null

    private val galerieLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "Aucune photo sélectionnée", Toast.LENGTH_SHORT).show()
            } else {
                val copie = copierImageDansStockageInterne(uri)
                if (copie != null) {
                    reduireImage(File(Uri.parse(copie).path!!))
                    photoUri = copie
                    afficherPhoto()
                } else {
                    Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_LONG).show()
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { succes ->
            val fichier = cameraFile
            if (succes && fichier != null && fichier.length() > 0) {
                reduireImage(fichier)
                photoUri = Uri.fromFile(fichier).toString()
                afficherPhoto()
            } else {
                fichier?.delete()
                Toast.makeText(this, "Prise de photo annulée", Toast.LENGTH_SHORT).show()
            }
            cameraFile = null
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
            if (accordee) {
                lancerCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permission caméra refusée : impossible de prendre une photo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        title = "Détail du contact"

        imgPhotoDetail = findViewById(R.id.imgPhotoDetail)
        etNom = findViewById(R.id.etNom)
        etPrenom = findViewById(R.id.etPrenom)
        etTelephone = findViewById(R.id.etTelephone)
        etEmail = findViewById(R.id.etEmail)
        btnAppeler = findViewById(R.id.btnAppeler)
        btnEmail = findViewById(R.id.btnEmail)
        btnGalerie = findViewById(R.id.btnGalerie)
        btnCamera = findViewById(R.id.btnCamera)
        btnEnregistrer = findViewById(R.id.btnEnregistrer)
        btnAnnuler = findViewById(R.id.btnAnnuler)

        contactId = intent.getIntExtra("id", 0)
        position = intent.getIntExtra("position", -1)
        etNom.setText(intent.getStringExtra("nom"))
        etPrenom.setText(intent.getStringExtra("prenom"))
        etTelephone.setText(intent.getStringExtra("telephone"))
        etEmail.setText(intent.getStringExtra("email"))
        photoUri = intent.getStringExtra("photoUri")

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getString("photoUri")
            cameraFile = savedInstanceState.getString("cameraPath")?.let { File(it) }
        }

        afficherPhoto()

        btnAppeler.setOnClickListener { appeler() }
        btnEmail.setOnClickListener { envoyerEmail() }
        btnGalerie.setOnClickListener { ouvrirGalerie() }
        btnCamera.setOnClickListener { ouvrirCamera() }
        btnEnregistrer.setOnClickListener { enregistrer() }
        btnAnnuler.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photoUri", photoUri)
        outState.putString("cameraPath", cameraFile?.absolutePath)
    }

    private fun afficherPhoto() {
        if (photoUri.isNullOrEmpty()) {
            imgPhotoDetail.setImageResource(R.drawable.ic_person)
        } else {
            imgPhotoDetail.setImageURI(Uri.parse(photoUri))
        }
    }

    private fun ouvrirGalerie() {
        try {
            galerieLauncher.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application de galerie disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun copierImageDansStockageInterne(source: Uri): String? {
        return try {
            val fichier = File(filesDir, "contact_${contactId}_${System.currentTimeMillis()}.jpg")
            val entree = contentResolver.openInputStream(source) ?: return null
            entree.use { input ->
                fichier.outputStream().use { sortie ->
                    input.copyTo(sortie)
                }
            }
            Uri.fromFile(fichier).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun ouvrirCamera() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Aucune caméra disponible sur cet appareil", Toast.LENGTH_LONG).show()
            return
        }

        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            lancerCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lancerCamera() {
        val fichier = File(filesDir, "contact_${contactId}_${System.currentTimeMillis()}.jpg")
        cameraFile = fichier

        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", fichier)

        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            cameraFile = null
            Toast.makeText(this, "Aucune application caméra disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun reduireImage(fichier: File, tailleMax: Int = 1024) {
        try {
            val chemin = fichier.absolutePath

            val bornes = BitmapFactory.Options()
            bornes.inJustDecodeBounds = true
            BitmapFactory.decodeFile(chemin, bornes)

            var echantillon = 1
            while (bornes.outWidth / echantillon > tailleMax * 2 ||
                bornes.outHeight / echantillon > tailleMax * 2
            ) {
                echantillon *= 2
            }
            val options = BitmapFactory.Options()
            options.inSampleSize = echantillon
            var bitmap = BitmapFactory.decodeFile(chemin, options) ?: return

            val orientation = ExifInterface(chemin).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val angle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (angle != 0f) {
                val matrice = Matrix()
                matrice.postRotate(angle)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrice, true)
            }

            val ratio = minOf(1f, tailleMax.toFloat() / maxOf(bitmap.width, bitmap.height))
            if (ratio < 1f) {
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            }

            fichier.outputStream().use { sortie ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, sortie)
            }
        } catch (e: Exception) {
        }
    }

    private fun appeler() {
        val numero = etTelephone.text.toString().trim().replace(" ", "")

        if (numero.isEmpty()) {
            etTelephone.error = "Aucun numéro à appeler"
            etTelephone.requestFocus()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$numero")

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application de téléphone disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun envoyerEmail() {
        val adresse = etEmail.text.toString().trim()

        if (adresse.isEmpty()) {
            etEmail.error = "Aucune adresse e-mail"
            etEmail.requestFocus()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$adresse")

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Aucune application de messagerie disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun enregistrer() {
        val nom = etNom.text.toString().trim()
        val prenom = etPrenom.text.toString().trim()
        val telephone = etTelephone.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (nom.isEmpty()) {
            etNom.error = "Le nom est obligatoire"
            etNom.requestFocus()
            return
        }
        if (prenom.isEmpty()) {
            etPrenom.error = "Le prénom est obligatoire"
            etPrenom.requestFocus()
            return
        }
        if (telephone.isEmpty()) {
            etTelephone.error = "Le téléphone est obligatoire"
            etTelephone.requestFocus()
            return
        }

        val resultIntent = Intent()
        resultIntent.putExtra("id", contactId)
        resultIntent.putExtra("position", position)
        resultIntent.putExtra("nom", nom)
        resultIntent.putExtra("prenom", prenom)
        resultIntent.putExtra("telephone", telephone)
        resultIntent.putExtra("email", email)
        resultIntent.putExtra("photoUri", photoUri)

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}