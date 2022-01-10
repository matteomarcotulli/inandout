package com.ferrero.inandout.viewmodel

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.sap.cloud.android.odata.entitycontainer.ElementBehaviour

open class BaseViewModel() : ViewModel() {

    var listElement : Map<String, ElementBehaviour>? = null
    var pageName : String? = null
    var mainViewModel: MainBusinnesViewModel? = null
    var application: Application? = null

    constructor(pageName : String, fragment: Fragment) : this(){
        mainViewModel = ViewModelProvider(fragment.requireActivity()).get(MainBusinnesViewModel::class.java)
        listElement = mainViewModel?.getFilteredElementList(pageName)
        application = mainViewModel?.getApplication() as Application

        this.pageName = pageName
    }
}