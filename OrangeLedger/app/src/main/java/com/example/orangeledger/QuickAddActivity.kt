package com.example.orangeledger

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class QuickAddActivity : Activity() {
    private val db by lazy { LedgerDb.get(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val orange=Color.rgb(255,120,0); val dark=Color.rgb(23,23,23)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(24,24,24,24); setBackgroundColor(dark) }
        val title=TextView(this).apply { text="QUICK EXPENSE"; textSize=22f; setTextColor(orange); setPadding(0,0,0,20) }
        val name=EditText(this).apply { hint="Expense name"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine=true; imeOptions=EditorInfo.IME_ACTION_NEXT }
        val amount=EditText(this).apply { hint="Amount (SAR)"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); singleLine=true; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; imeOptions=EditorInfo.IME_ACTION_DONE }
        val save=Button(this).apply { text="SAVE"; setTextColor(dark); backgroundTintList=android.content.res.ColorStateList.valueOf(orange) }
        root.addView(title);root.addView(name);root.addView(amount);root.addView(save);setContentView(root)
        save.setOnClickListener { val n=name.text.toString().trim(); val a=amount.text.toString().toDoubleOrNull()
            if(n.isBlank()||a==null||a<=0) Toast.makeText(this,"Enter a name and valid amount",Toast.LENGTH_SHORT).show()
            else lifecycleScope.launch { db.expenses().insert(Expense(name=n,amount=a,spentAt=System.currentTimeMillis())); BackupAlarmReceiver.schedule(this@QuickAddActivity); Toast.makeText(this@QuickAddActivity,"Expense saved",Toast.LENGTH_SHORT).show(); finish() }
        }
        amount.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_DONE){save.performClick();true}else false }
    }
}
