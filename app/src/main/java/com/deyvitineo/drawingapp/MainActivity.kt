package com.deyvitineo.drawingapp

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null
    private var mColor: String = "#000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageButtonCurrentPaint = layout_paint_colors[1] as ImageButton

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_selected
            )
        )

        ib_brush_size.setOnClickListener {
            showBrushSizeDialog()
        }

        color_picker.setOnClickListener {
            initColorPicker()
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
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            mColor = colorTag
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.pallet_selected
                )
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            mImageButtonCurrentPaint = view
        }
    }
}
