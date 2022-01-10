package com.ferrero.inandout.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.sap.cloud.android.odata.entitycontainer.ElementBehaviour

class MainBusinnesViewModel(
        private val context: Context,
        private val application: Application
) : ViewModel() {

    private var listElement : List<ElementBehaviour>? = null

    fun getFilteredElementList(namePage : String) : Map<String, ElementBehaviour>?{
        return listElement?.filter { it.element.startsWith(namePage) }?.map { it.element to it }?.toMap()
    }

    fun getApplication(): Application {
        return application
    }

}