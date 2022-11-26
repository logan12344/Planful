package com.production.planful.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.production.planful.adapters.EventListWidgetAdapterEmpty

class WidgetServiceEmpty : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapterEmpty(applicationContext)
}
