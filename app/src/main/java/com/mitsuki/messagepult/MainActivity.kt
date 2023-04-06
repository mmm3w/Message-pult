package com.mitsuki.messagepult

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sp = getSharedPreferences("iWechat", Context.MODE_PRIVATE)

        val cropIdView = findViewById<EditText>(R.id.main_corp_id_input).apply {
            setText(sp.getString("corpid", ""))
        }

        val cropSecretView = findViewById<EditText>(R.id.main_corp_secret_input).apply {
            setText(sp.getString("corpsecret", ""))
        }

        val agentIdView = findViewById<EditText>(R.id.main_agent_id_input).apply {
            sp.getInt("agentid", 0).takeIf { it != 0 }?.also { setText(it.toString()) }
        }

        findViewById<View>(R.id.main_save).setOnClickListener {
            sp.edit()
                .putString("corpid", cropIdView.text.toString())
                .putString("corpsecret", cropSecretView.text.toString())
                .putInt("agentid", agentIdView.text.toString().toInt())
                .apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
    }
}