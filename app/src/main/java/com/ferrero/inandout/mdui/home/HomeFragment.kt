package com.ferrero.inandout.mdui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ferrero.inandout.R
import com.ferrero.inandout.databinding.FragmentHomeBinding
import com.ferrero.inandout.viewmodel.EntityViewModelFactory
import com.ferrero.inandout.viewmodel.home.HomeViewModel


class FragmentHome : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var viewModel: HomeViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_home, container, false)
        viewModel = ViewModelProvider(this, EntityViewModelFactory(this)).get(HomeViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}