package com.example.cse438.imageprocessor

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Bitmap
import android.R.attr.data
import android.app.PendingIntent.getActivity
import android.net.Uri
import kotlinx.android.synthetic.main.activity_view_picture.*


class ViewPicture : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_picture)

        val returnUri = Uri.parse(intent.extras.getString("uri"))

        val bitmapImage = MediaStore.Images.Media.getBitmap(this.contentResolver, returnUri)
        img.setImageBitmap(bitmapImage)
    }
}
