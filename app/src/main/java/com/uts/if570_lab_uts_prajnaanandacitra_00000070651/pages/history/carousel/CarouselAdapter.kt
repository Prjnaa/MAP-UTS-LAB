package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.history.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R

class CarouselAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.carouselImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.carousel_item, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val imageUrl = imageUrls[position]

        Glide.with(holder.imageView.context).load(imageUrl).into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }
}
