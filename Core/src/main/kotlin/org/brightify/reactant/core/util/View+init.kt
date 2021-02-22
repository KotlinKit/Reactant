package org.brightify.reactant.core.util

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.brightify.reactant.autolayout.AutoLayout
import org.brightify.reactant.core.ReactantActivity

/**
 *  @author <a href="mailto:filip.dolnik.96@gmail.com">Filip Dolnik</a>
 */
fun View(): View {
    return View(ReactantActivity.context)
}

fun ImageView(): ImageView {
    return ImageView(ReactantActivity.context)
}

fun ImageButton(): ImageButton {
    return ImageButton(ReactantActivity.context)
}

fun TextView(): TextView {
    return TextView(ReactantActivity.context)
}

fun TextView(text: String): TextView {
    return TextView().apply { this.text = text }
}

fun Button(): Button {
    return Button(ReactantActivity.context)
}

fun Button(text: String): Button {
    return Button().apply { this.text = text }
}

fun EditText(): EditText {
    return EditText(ReactantActivity.context)
}

fun TextInputEditText(): TextInputEditText {
    return TextInputEditText(ReactantActivity.context)
}

fun ProgressBar(): ProgressBar {
    return ProgressBar(ReactantActivity.context)
}

fun Switch(): Switch {
    return Switch(ReactantActivity.context)
}

fun AutoLayout(): AutoLayout {
    return AutoLayout(ReactantActivity.context)
}

fun ScrollView(): ScrollView {
    return ScrollView(ReactantActivity.context)
}

fun FrameLayout(): FrameLayout {
    return FrameLayout(ReactantActivity.context)
}

fun RecyclerView(): RecyclerView {
    return RecyclerView(ReactantActivity.context)
}

fun SwipeRefreshLayout(): SwipeRefreshLayout {
    return SwipeRefreshLayout(ReactantActivity.context)
}

fun TextInputLayout(): TextInputLayout {
    return TextInputLayout(ReactantActivity.context)
}

fun FloatingActionButton(): FloatingActionButton {
    return FloatingActionButton(ReactantActivity.context)
}