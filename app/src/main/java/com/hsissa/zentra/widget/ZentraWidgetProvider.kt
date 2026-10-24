package com.hsissa.zentra.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hsissa.zentra.R
import com.hsissa.zentra.core.SettingsManager
import com.hsissa.zentra.ui.MainActivity

class ZentraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val lastScore = SettingsManager.getInstance(context).getLastFocusScore()
            val scoreText = if (lastScore != null) {
                "Focus Score: $lastScore"
            } else {
                context.getString(R.string.widget_score_placeholder)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_zentra)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            views.setTextViewText(R.id.tvWidgetScore, scoreText)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun notifyWidgetDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ZentraWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds.isNotEmpty()) {
                for (id in widgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }
}
