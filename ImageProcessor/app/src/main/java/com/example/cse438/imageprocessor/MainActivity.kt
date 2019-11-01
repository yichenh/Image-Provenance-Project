package com.example.cse438.imageprocessor

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.R.attr.data
import android.app.PendingIntent.getActivity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.jar.Manifest
import java.math.BigInteger
import java.security.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    var currentPhotoPath: String = ""
    var REQUEST_IMAGE_CAPTURE: Int = 1
    var REQUEST_LOAD_IMAGE: Int = 2
    var MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraButton.setOnClickListener(this)
        viewPhotoButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.cameraButton -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager).also {
                        val photoFile : File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            Log.d("ERROR:", "Could not get photo file")
                            null
                        }

                        photoFile?.also {
                            val photoURI = FileProvider.getUriForFile(
                                this,
                                "com.example.cse438.imageprocessor.fileprovider",
                                it
                            )
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                        }
                    }
                }
            }
            R.id.viewPhotoButton -> {
                val i = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(i, REQUEST_LOAD_IMAGE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            galleryAddPic()
        }

        if(requestCode == REQUEST_LOAD_IMAGE && resultCode == RESULT_OK) {
            val returnUri = data?.data
            val intent: Intent = Intent(this, ViewPicture::class.java)
            intent.putExtra("uri", returnUri.toString())
            startActivity(intent)
        }
    }

    private fun galleryAddPic() {
        val f = File(currentPhotoPath)
        //check permission. if not then request permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
        }

        // Getting an android keystore to generate/retrieve key pairs
        var keyStore:KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val alias = "image_keypair"

        if(!keyStore.containsAlias(alias)){
            // Generate key pair if not exist
            var kpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
            kpg.initialize(KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setKeySize(256)
                .build())
            kpg.generateKeyPair()
        }

        val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        var priv:PrivateKey = privateKeyEntry.privateKey
        var pub:PublicKey = keyStore.getCertificate(alias).publicKey



        /*
         * Create a Signature object and initialize it with the private key
         */

        var dsa: Signature  = Signature.getInstance("SHA256withECDSA")

        dsa.initSign(priv)

        // Put the data in

        var picBitmap = BitmapFactory.decodeFile(currentPhotoPath)
        // convert bitmap to byte array
        var stream = ByteArrayOutputStream()
        picBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        var pba = stream.toByteArray()

        dsa.update(pba)


        /*
         * Now that all the data to be signed has been read in, generate a
         * signature for it
         */

        var realSig = dsa.sign()

        dsa.initVerify(pub)
        dsa.update(pba)
        var isReal = dsa.verify(realSig)


        var values = ContentValues()
        values.put(MediaStore.Images.Media.DATA, currentPhotoPath)

        val exif = ExifInterface(currentPhotoPath)
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, realSig.toString())
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "YICHEN 2019")
        exif.saveAttributes()
        this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission granted
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_$timeStamp",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
}
