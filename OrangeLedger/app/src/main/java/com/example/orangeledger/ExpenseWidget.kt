package com.example.orangeledger

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ExpenseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views=RemoteViews(context.packageName,R.layout.expense_widget)
            val intent=Intent(context,QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending=PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(android.R.id.content,pending)
            manager.updateAppWidget(id,views)
        }
    }
}
