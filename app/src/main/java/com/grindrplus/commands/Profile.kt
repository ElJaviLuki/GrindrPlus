package com.grindrplus.commands

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import com.grindrplus.core.ModContext
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.ui.Utils.copyToClipboard

class Profile(context: ModContext, recipient: String, sender: String) : CommandModule(context, recipient, sender) {
    @Command("open", help = "Open a user's profile")
    private fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            return openProfile(args[0], context)
        } else {
            context.showToast(Toast.LENGTH_LONG,
                "Please provide valid ID")
        }
    }

    @SuppressLint("SetTextI18n")
    @Command("id", help = "Get and copy profile IDs")
    private fun id(args: List<String>) {
        val dialogView = LinearLayout(context.currentActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 0, 60, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = context.currentActivity?.let {
            AppCompatTextView(it).apply {
                text = "• Your ID: $recipient\n• Profile ID: $sender"
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialogView.addView(textView)
        context.currentActivity?.runOnUiThread {
            val dialog = AlertDialog.Builder(context.currentActivity)
                .setTitle("Profile IDs")
                .setView(dialogView)
                .setPositiveButton("Copy my ID") { _, _ ->
                    copyToClipboard("Your ID", recipient, context)
                }
                .setNegativeButton("Copy profile ID") { _, _ ->
                    copyToClipboard("Profile ID", sender, context)
                }
                .setNeutralButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}