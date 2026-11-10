package com.example.contactsapp

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class ContactAdapter(
    context: Context,
    private val contacts: MutableList<Contact>
) : ArrayAdapter<Contact>(context, 0, contacts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)

        val contact = contacts[position]

        val imgPhoto = view.findViewById<ImageView>(R.id.imgPhoto)
        val tvNomPrenom = view.findViewById<TextView>(R.id.tvNomPrenom)
        val tvTelephone = view.findViewById<TextView>(R.id.tvTelephone)

        tvNomPrenom.text = "${contact.nom} ${contact.prenom}"
        tvTelephone.text = contact.telephone

        if (contact.photoUri.isNullOrEmpty()) {
            imgPhoto.setImageResource(R.drawable.ic_person)
        } else {
            imgPhoto.setImageURI(Uri.parse(contact.photoUri))
        }

        return view
    }
}