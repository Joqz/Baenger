package com.example.baenger.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.baenger.R
import com.example.baenger.models.SongCardModel


class SearchFragment : Fragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private lateinit var viewModel: SongCardModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SongCardModel::class.java)

    }


}