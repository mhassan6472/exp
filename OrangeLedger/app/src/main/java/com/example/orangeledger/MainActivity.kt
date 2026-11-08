package com.example.orangeledger

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val db by lazy { LedgerDb.get(this) }
    private val orange = Color.rgb(255,120,0); private val dark = Color.rgb(23,23,23); private val panel = Color.rgb(36,36,36)
    private lateinit var content: LinearLayout
    private var dateFrom: Long? = null; private var dateTo: Long? = null
    private val backupPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION); getSharedPreferences("prefs",0).edit().putString("backup_tree",uri.toString()).apply(); Toast.makeText(this,"Backup folder selected",Toast.LENGTH_SHORT).show(); BackupAlarmReceiver.schedule(this); scheduleBackup() }
    }
    private val restorePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { try {
            val text=contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: error("Cannot read backup")
            val root=JSONObject(text); val arr=root.getJSONArray("expenses"); val list= mutableListOf<Expense>()
            for(i in 0 until arr.length()){ val o=arr.getJSONObject(i); list.add(Expense(o.getLong("id"),o.getString("name"),o.getDouble("amount"),o.getLong("spentAt"),o.optString("note",""))) }
            db.expenses().clear(); db.expenses().insertAll(list)
            val s=root.getJSONObject("settings"); db.settings().save(LedgerSettings(1,s.optDouble("allowance",0.0),s.optDouble("savings",0.0),s.optString("currency","SAR")))
            refresh(); Toast.makeText(this@MainActivity,"Backup restored",Toast.LENGTH_LONG).show()
        } catch(e:Exception){ Toast.makeText(this@MainActivity,"Restore failed: ${e.message}",Toast.LENGTH_LONG).show() } }
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi(); refresh() }
    private fun buildUi() {
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,16,18,18); setBackgroundColor(dark) }
        val scroll=ScrollView(this); scroll.addView(content); setContentView(scroll)
        content.addView(label("ORANGE LEDGER",22,orange,true))
        content.addView(label("Offline · Private · Local records",12,Color.LTGRAY))
        val settings=LinearLayout(this); settings.orientation=LinearLayout.HORIZONTAL
        val allowance=EditText(this).apply { hint="Spending allowance"; inputType=2; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        val savings=EditText(this).apply { hint="Savings"; inputType=2; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        settings.addView(allowance,LinearLayout.LayoutParams(0,dp(48),1f)); settings.addView(savings,LinearLayout.LayoutParams(0,dp(48),1f)); content.addView(settings)
        val setBtn=button("SET ALLOWANCE & SAVINGS"); content.addView(setBtn)
        setBtn.setOnClickListener { lifecycleScope.launch { val old=db.settings().get() ?: LedgerSettings(); db.settings().save(old.copy(allowance=allowance.text.toString().toDoubleOrNull() ?: old.allowance,savings=savings.text.toString().toDoubleOrNull() ?: old.savings)); refresh() } }
        content.addView(label("ADD EXPENSE",16,orange,true))
        val name=EditText(this).apply { hint="What did you spend on?"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        val amount=EditText(this).apply { hint="Amount (SAR)"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
        content.addView(name); content.addView(amount)
        val save=button("SAVE EXPENSE"); content.addView(save)
        save.setOnClickListener { val n=name.text.toString().trim(); val a=amount.text.toString().toDoubleOrNull(); if(n.isBlank()||a==null||a<=0){ Toast.makeText(this,"Enter expense name and valid amount",Toast.LENGTH_SHORT).show() } else lifecycleScope.launch { db.expenses().insert(Expense(name=n,amount=a,spentAt=System.currentTimeMillis())); name.text.clear(); amount.text.clear(); refresh(); BackupAlarmReceiver.schedule(this); scheduleBackup() } }
        content.addView(label("RECORDS",16,orange,true))
        val rangeRow=LinearLayout(this); rangeRow.orientation=LinearLayout.HORIZONTAL
        val daily=button("TODAY"); val weekly=button("7 DAYS"); val monthly=button("MONTH"); val all=button("ALL")
        listOf(daily,weekly,monthly,all).forEach { rangeRow.addView(it,LinearLayout.LayoutParams(0,dp(44),1f)) }; content.addView(rangeRow)
        daily.setOnClickListener { val cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,0);cal.set(Calendar.MINUTE,0);cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);dateFrom=cal.timeInMillis;dateTo=System.currentTimeMillis();refresh() }
        weekly.setOnClickListener { dateFrom=System.currentTimeMillis()-7L*86400000;dateTo=System.currentTimeMillis();refresh() }
        monthly.setOnClickListener { val c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);dateFrom=c.timeInMillis;dateTo=System.currentTimeMillis();refresh() }
        all.setOnClickListener { dateFrom=null;dateTo=null;refresh() }
        val custom=button("CHOOSE DATE RANGE");content.addView(custom);custom.setOnClickListener { pickDate(true) }
        val backup=LinearLayout(this); backup.orientation=LinearLayout.HORIZONTAL
        val choose=button("BACKUP FOLDER"); val restore=button("IMPORT / RESTORE")
        backup.addView(choose,LinearLayout.LayoutParams(0,dp(48),1f));backup.addView(restore,LinearLayout.LayoutParams(0,dp(48),1f));content.addView(backup)
        choose.setOnClickListener { backupPicker.launch(null) };restore.setOnClickListener { restorePicker.launch(arrayOf("application/json","text/plain")) }
    }
    private fun pickDate(start:Boolean) { val c=Calendar.getInstance();DatePickerDialog(this,{_,y,m,d-> c.set(y,m,d,0,0,0);c.set(Calendar.MILLISECOND,0);if(start){dateFrom=c.timeInMillis;pickDate(false)}else{c.add(Calendar.DAY_OF_MONTH,1);dateTo=c.timeInMillis-1;refresh()}},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show() }
    private fun refresh() { lifecycleScope.launch {
        val s=db.settings().get() ?: LedgerSettings().also{db.settings().save(it)}
        val entries=if(dateFrom!=null&&dateTo!=null) db.expenses().range(dateFrom!!,dateTo!!) else db.expenses().all()
        val spent=entries.sumOf{it.amount}; val remaining=s.allowance-spent
        content.removeViews( content.childCount-1, 0) // no-op safety; records are rendered below by finding marker
        // Rebuild record display at bottom; delete old generated list views using tag.
        for(i in content.childCount-1 downTo 0) if(content.getChildAt(i).tag=="record") content.removeViewAt(i)
        val summary=label("Allowance: ${fmt(s.allowance)} ${s.currency}   ·   Spent: ${fmt(spent)}   ·   Left: ${fmt(remaining)}\nSavings: ${fmt(s.savings)} ${s.currency}",14,Color.WHITE,true); summary.tag="record";content.addView(summary)
        entries.forEach { e -> val row=label("${SimpleDateFormat("dd MMM yyyy, HH:mm",Locale.getDefault()).format(Date(e.spentAt))}  ·  ${e.name}\n− ${fmt(e.amount)} ${s.currency}",14,Color.LTGRAY);row.tag="record";row.setPadding(8,12,8,12);content.addView(row) }
    } }
    private fun scheduleBackup(){ lifecycleScope.launch { try {
        val tree=getSharedPreferences("prefs",0).getString("backup_tree",null) ?: return@launch
        val uri=android.net.Uri.parse(tree); val root=androidx.documentfile.provider.DocumentFile.fromTreeUri(this@MainActivity,uri) ?: return@launch
        val expenses=db.expenses().all(); val s=db.settings().get() ?: LedgerSettings()
        val obj=JSONObject(); val arr=JSONArray();expenses.forEach{ e->arr.put(JSONObject().put("id",e.id).put("name",e.name).put("amount",e.amount).put("spentAt",e.spentAt).put("note",e.note)) };obj.put("expenses",arr);obj.put("settings",JSONObject().put("allowance",s.allowance).put("savings",s.savings).put("currency",s.currency))
        val old=root.findFile("orange-ledger-backup.json");old?.delete();val file=root.createFile("application/json","orange-ledger-backup.json") ?: return@launch
        contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use{it.write(obj.toString())}
    }catch(_:Exception){} } }
    private fun label(t:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply{ text=t;textSize=size.toFloat();setTextColor(color);if(bold)setTypeface(null,android.graphics.Typeface.BOLD);setPadding(0,dp(8),0,dp(8)) }
    private fun button(t:String)=Button(this).apply{text=t;setTextColor(dark);backgroundTintList=android.content.res.ColorStateList.valueOf(orange);textSize=11f}
    private fun fmt(v:Double)=String.format(Locale.getDefault(),"%.2f",v)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
