package com.grindrplus.commands

import android.app.AlertDialog
import com.grindrplus.core.ModContext

class CommandHandler(private val context: ModContext,
                     val recipient: String,
                     private val sender: String = "") {
    private val commandModules: MutableList<CommandModule> = mutableListOf()

    init {
        commandModules.add(Location(context, recipient, sender))
        commandModules.add(Profile(context, recipient, sender))
    }

    fun handle(input: String) {
        val args = input.split(" ")
        val command = args.firstOrNull() ?: return

        if (command == "help") {
            context.currentActivity?.runOnUiThread {
                AlertDialog.Builder(context.currentActivity)
                    .setTitle("Help")
                    .setMessage(commandModules.joinToString("\n") { it.getHelp() }.drop(1))
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .create().show()
            }
        }

        for (module in commandModules) {
            if (module.handle(command, args.drop(1))) break
        }
    }
}