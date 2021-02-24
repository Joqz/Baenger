package com.example.baenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.baenger.fragments.PartyModeFragment
import com.example.baenger.fragments.PlayFragment
import com.example.baenger.fragments.SearchFragment
import kotlinx.android.synthetic.main.activity_play.*
import androidx.fragment.app.Fragment

class SearchActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.hide()

        val searchFragment = SearchFragment()
        val playFragment = PlayFragment()
        val partyModeFragment = PartyModeFragment()


        makeCurrentFragment(SearchFragment())



        bottom_navbar.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.search -> makeCurrentFragment(searchFragment)
                R.id.play -> makeCurrentFragment(playFragment)
                R.id.partymode -> makeCurrentFragment(partyModeFragment)
            }
            true
        }

    }



    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply{
            replace(R.id.fl_wrapper, fragment)
            commit()
        }


}