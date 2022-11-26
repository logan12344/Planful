package com.production.planful.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.production.planful.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) =
        EventListWidgetAdapter(applicationContext, intent)
}
