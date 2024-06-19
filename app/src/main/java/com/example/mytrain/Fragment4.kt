package com.example.mytrain

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.mytrain.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Fragment4 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment4, container, false)
//
//        val userInfoTextView: TextView = view.findViewById(R.id.user_info_text)
//        val sharedPreferences = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
//        val userEmail = sharedPreferences.getString("Email", null)
//
//        if (userEmail != null) {
//            val db = Firebase.firestore
//            db.collection("users")
//                .whereEqualTo("email", userEmail)
//                .get()
//                .addOnSuccessListener { result ->
//                    for (document in result) {
//                        val userInfo = """
//                            Email: ${document.getString("email")}
//                            Weight: ${document.getString("weight")}
//                            Height: ${document.getString("height")}
//                        """.trimIndent()
//                        userInfoTextView.text = userInfo
//                    }
//                }
//                .addOnFailureListener { exception ->
//                    userInfoTextView.text = "Error: ${exception.message}"
//                }
//        } else {
//            userInfoTextView.text = "User not logged in"
//        }

        return view
    }
}
