package com.ferrero.inandout.viewmodel.home

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.ferrero.inandout.viewmodel.BaseViewModel
import com.ferrero.inandout.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.entitycontainer.ElementBehaviour
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata

class HomeViewModel(private val fragment : Fragment) : /*BaseViewModel("", fragment)*/ ViewModel()  {
    var elementViewModel : EntityViewModel<ElementBehaviour>? = null
/*
    fun initElementBehaviour () {
        elementViewModel = application?.let { EntityViewModel(it, EntityContainerMetadata.EntitySets.elementBehaviour,ElementBehaviour.element) }
    }
*/
}