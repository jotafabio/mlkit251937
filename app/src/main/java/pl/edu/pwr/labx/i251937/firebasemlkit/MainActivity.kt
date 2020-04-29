package pl.edu.pwr.labx.i251937.firebasemlkit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.wtf
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.FirebaseVision.*
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_first.*

class MainActivity : AppCompatActivity() {
    //pickPhotoRequestCode is an integer which identify our callback in
    //onActivityResult method, suggest to use values above 100.
    var pickPhotoRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        fabGallery.setOnClickListener {
            textView.text = ""
            pickImageGallery()
        }
        fabCamera.setOnClickListener {
            textView.text = ""
            pickImageCamera()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        pickPhotoRequestCode = 101
        intent.type = "image/*"
        startActivityForResult(intent, pickPhotoRequestCode)
    }

    private fun pickImageCamera() {
        //Read about Camera capture on https://developer.android.com/training/camera/photobasics
        pickPhotoRequestCode = 102
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)
                ?.also { startActivityForResult(takePictureIntent, pickPhotoRequestCode) }
        }
        intent.type = "image/*"
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int, data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                pickPhotoRequestCode -> {
                    val bitmap = getImageFromData(data, requestCode)
                    bitmap?.apply {
                        imageView.setImageBitmap(this)
                        processImageTagging(bitmap)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getImageFromData(data: Intent?, requestCode: Int): Bitmap? {
        return if (requestCode == 102) {
            data?.extras?.get("data") as Bitmap
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, data?.data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun processImageTagging(bitmap: Bitmap) {
        val visionImg = FirebaseVisionImage.fromBitmap(bitmap)
        var txtLabels = textView.text.toString()

        getInstance().onDeviceImageLabeler.processImage(visionImg)
            .addOnSuccessListener { tagsImg ->
                if ( tagsImg.isNotEmpty() ){
                    txtLabels += "Labels: ["
                    txtLabels += tagsImg.joinToString("/") { it.text }
                    txtLabels += "]"
                    textView.text = txtLabels.toString()
                }
            }
            .addOnFailureListener { ex ->
                wtf("LAB", ex)
            }

        getInstance().onDeviceTextRecognizer.processImage(visionImg)
            .addOnSuccessListener{ tagsTxt ->
                if (tagsTxt.text.isNotEmpty()) {
                    txtLabels += System.lineSeparator()
                    txtLabels += "OCR: ["
                    txtLabels += tagsTxt.text
                    txtLabels += "]"
                    textView.text = txtLabels.toString()
                }
            }
            .addOnFailureListener{ ex ->
                wtf( "TXT", ex)
            }


    }

}
