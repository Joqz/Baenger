package com.example.baenger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.baenger.models.SongCard
import kotlinx.android.synthetic.main.search_songcard.view.*

class SongAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SongCard> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.search_songcard, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder){

            is SongViewHolder ->{
                holder.bind(items.get(position))
            }
        }
    }


    class SongViewHolder constructor(
        itemView: View
    ): RecyclerView.ViewHolder(itemView){

        val songImage = itemView.spotify_songimage
        val songArtist = itemView.spotify_songartist
        val songName = itemView.spotify_songname

        fun bind(songCard: SongCard){

            songArtist.setText(songCard.artist)
            songName.setText(songCard.songname)

            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)

            Glide.with(itemView.context)
                .load(songCard.songimage)
                .into(songImage)
        }
    }
}