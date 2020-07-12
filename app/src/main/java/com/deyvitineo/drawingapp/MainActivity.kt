package com.deyvitineo.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.deyvitineo.drawingapp.util.Constants
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.widget.ColorPalette
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: CircleImageView? = null
    private var mColor: String = Constants.DEFAULT_COLOR

    private lateinit var mBrushSizeButton: ImageButton
    private lateinit var mColorPickerPalette: ColorPalette
    private lateinit var mUndoButton: ImageButton
    private lateinit var mLoadImageButton: ImageButton
    private lateinit var mSaveImageButton: ImageButton
    private lateinit var mCurrentColorView: CircleImageView
    private lateinit var mRedoButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWidgets()
        initListeners()

        mImageButtonCurrentPaint = layout_paint_colors[0] as CircleImageView
    }

    //Initializes widgets
    private fun initWidgets() {

        mBrushSizeButton = findViewById(R.id.ib_brush_size)
        mColorPickerPalette = findViewById(R.id.color_picker)
        mUndoButton = findViewById(R.id.ib_undo)
        mLoadImageButton = findViewById(R.id.ib_load_image)
        mSaveImageButton = findViewById(R.id.ib_save)
        mCurrentColorView = findViewById(R.id.civ_current_color)
        mRedoButton = findViewById(R.id.ib_redo)

    }

    //Initializes listeners for all image buttons
    private fun initListeners() {
        mBrushSizeButton.setOnClickListener { showBrushSizeDialog() }
        mColorPickerPalette.setOnClickListener { initColorPicker() }

        mLoadImageButton.setOnClickListener {
            if (isStoragePermissionGranted()) {
                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, Constants.GALLERY_CODE)
            } else {
                requestStoragePermission()
            }
        }

        mSaveImageButton.setOnClickListener {
            if (isStoragePermissionGranted()) {
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            } else {
                requestStoragePermission()
            }
        }

        mUndoButton.setOnClickListener {
            val undoPath = drawing_view.undoPath()
            if (!undoPath) {
                Toast.makeText(this, "There is nothing to undo", Toast.LENGTH_SHORT).show()
            } else {
                mRedoButton.alpha = Constants.FULL_OPACITY //if there is something to redo, change opacity to 100%
            }
        }
        mRedoButton.setOnClickListener {
            val redoPath = drawing_view.redoPath()
            if (!redoPath) {
                mRedoButton.alpha = Constants.HALF_OPACITY //if there is nothing to redo, change the opacity back to 50%
                //"Maybe have the Paths stored temporally stored in room database" "find a way to be able to observe the arrays size on the other class so that this can be done with that instead"
            }
        }
    }


    //Show color picker dialog
    private fun initColorPicker() {
        ColorPickerDialog
            .Builder(this)
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(mColor)
            .setColorListener { color, colorHex ->
                mColor = colorHex
                mCurrentColorView.setColorFilter(color)
                drawing_view.setColor(colorHex)
            }
            .show()
    }

    //shows brush size dialog
    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size")

        val smallBtn = brushDialog.ib_small_brush
        val mediumBtn = brushDialog.ib_medium_brush
        val largeBtn = brushDialog.ib_large_brush
        brushDialog.show()

        smallBtn.setOnClickListener {
            drawing_view.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawing_view.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawing_view.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
    }


    //Selects a color from the basic ones provided
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as CircleImageView
            val colorTag = imageButton.tag.toString()

            mColor = colorTag
            mCurrentColorView.setColorFilter(Color.parseColor(mColor))
            drawing_view.setColor(colorTag)
            mImageButtonCurrentPaint = view
        }
    }

    //TODO: try to move to an util class
    //Requests access to read and write to storage
    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(
                this, "Missing storage permission. This can be added in your phone settings.", Toast.LENGTH_LONG
            ).show()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            Constants.STORAGE_PERMISSION_CODE
        )
    }

    //Loads image from intent after the user selects it
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.GALLERY_CODE) {
                try {
                    if (data?.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(this, "Error loading the image, please try again", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    //Handles results from permissions request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Checks if the user already granted storage (read and write) permissions
    private fun isStoragePermissionGranted(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }


    //TODO: replace with something better
    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        override fun doInBackground(vararg params: Any?): String {
            var result = ""

            if (mBitmap != null) {
                try {
                    val outputStream = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)

                    val filepath =
                        Environment.getExternalStorageDirectory().absolutePath + "/DCIM/DrawingApp"

                    val directory = File(filepath)

                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file =
                        File(directory, "DrawingApp" + System.currentTimeMillis() / 1000 + ".jpeg")

                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(outputStream.toByteArray())
                    fileOutputStream.close()

                    result = file.absolutePath

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (!result?.isEmpty()!!) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully : $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong saving the file",
                    Toast.LENGTH_SHORT
                ).show()
            }

            //Allows user to share image when its saved
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/jpeg"

                startActivity(
                    Intent.createChooser(
                        shareIntent, "Share"
                    )
                )
            }

        }
    }
}
