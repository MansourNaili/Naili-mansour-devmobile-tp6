package com.example.contactsapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ContactAdapter
    private val contacts = mutableListOf<Contact>()

    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val position = data.getIntExtra("position", -1)
                    if (position in contacts.indices) {
                        val c = contacts[position]
                        c.nom = data.getStringExtra("nom") ?: c.nom
                        c.prenom = data.getStringExtra("prenom") ?: c.prenom
                        c.telephone = data.getStringExtra("telephone") ?: c.telephone
                        c.email = data.getStringExtra("email") ?: c.email
                        c.photoUri = data.getStringExtra("photoUri")

                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Contact mis à jour", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Mes contacts"

        listView = findViewById(R.id.listViewContacts)

        loadSampleContacts()

        adapter = ContactAdapter(this, contacts)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val c = contacts[position]

            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("id", c.id)
            intent.putExtra("position", position)
            intent.putExtra("nom", c.nom)
            intent.putExtra("prenom", c.prenom)
            intent.putExtra("telephone", c.telephone)
            intent.putExtra("email", c.email)
            intent.putExtra("photoUri", c.photoUri)

            detailLauncher.launch(intent)
        }
    }

    private fun loadSampleContacts() {
        contacts.add(Contact(1, "Dupont", "Claire", "06 12 34 56 78", "claire.dupont@example.com"))
        contacts.add(Contact(2, "Martin", "Thomas", "07 23 45 67 89", "thomas.martin@example.com"))
        contacts.add(Contact(3, "Bernard", "Sophie", "06 98 76 54 32", "sophie.bernard@example.com"))
        contacts.add(Contact(4, "Petit", "Lucas", "07 11 22 33 44", "lucas.petit@example.com"))
        contacts.add(Contact(5, "Moreau", "Julien", "06 55 44 33 22", "julien.moreau@example.com"))
    }
}