package com.example.orangeledger

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*

class BackupAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        schedule(context)
        val app=context.applicationContext
        val pendingResult=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tree=app.getSharedPreferences("prefs",0).getString("backup_tree",null) ?: return@launch
                val root=DocumentFile.fromTreeUri(app,android.net.Uri.parse(tree)) ?: return@launch
                val db=LedgerDb.get(app)
                val items=db.expenses().all(); val settings=db.settings().get() ?: LedgerSettings()
                val arr=org.json.JSONArray()
                items.forEach { e -> arr.put(org.json.JSONObject().put("id",e.id).put("name",e.name).put("amount",e.amount).put("spentAt",e.spentAt).put("note",e.note)) }
                val obj=org.json.JSONObject().put("expenses",arr).put("settings",org.json.JSONObject().put("allowance",settings.allowance).put("savings",settings.savings).put("currency",settings.currency))
                root.findFile("orange-ledger-backup.json")?.delete()
                val file=root.createFile("application/json","orange-ledger-backup.json") ?: return@launch
                app.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { it.write(obj.toString()) }
            } catch (_:Exception) { } finally { pendingResult.finish() }
        }
    }
    companion object {
        fun schedule(context:Context) {
            val c=java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY,23);set(java.util.Calendar.MINUTE,59);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0);if(timeInMillis<=System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR,1) }
            val intent=Intent(context,BackupAlarmReceiver::class.java)
            val pi=PendingIntent.getBroadcast(context,456,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val am=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.timeInMillis,pi)
        }
    }
}
