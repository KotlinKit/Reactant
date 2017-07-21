package org.brightify.reactant.prototyping

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import org.brightify.reactant.core.Button
import org.brightify.reactant.core.ControllerBase
import org.brightify.reactant.core.NavigationController
import org.brightify.reactant.core.ReactantActivity
import org.brightify.reactant.core.Style
import org.brightify.reactant.core.TextView
import org.brightify.reactant.core.ViewBase
import org.brightify.reactant.core.ViewController
import org.brightify.reactant.core.Wireframe
import org.brightify.reactant.core.constraint.ContainerView
import org.brightify.reactant.core.constraint.util.children
import org.brightify.reactant.core.constraint.util.snp
import org.brightify.reactant.core.make
import org.brightify.reactant.prototyping.CustomView.Styles.text

/**
 *  @author <a href="mailto:filip.dolnik.96@gmail.com">Filip Dolnik</a>
 */
class MainWireframe : Wireframe() {

    private val navigationController = NavigationController()

    override fun entryPoint(): ViewController {
        val reactions = InitialController.Reactions {
            navigationController.push(main())
        }

        navigationController.push(make(::InitialController, reactions))

        return navigationController
    }

    private fun main(): MainController {
        val reactions = MainController.Reactions {
            navigationController.pop()
        }

        return make(::MainController, reactions)
    }
}

class MainActivity : ReactantActivity(MainWireframe())

class InitialController(private val reactions: InitialController.Reactions) : ControllerBase<Unit, CustomView, Unit>(
        make(::CustomView, "Initial")) {

    data class Reactions(val onNext: () -> Unit)

    override fun act(action: Unit) {
        reactions.onNext()
//        rootView.requestLayout()
    }
}

class MainController(private val reactions: MainController.Reactions) : ControllerBase<Unit, AnotherView, Unit>(make(::AnotherView)) {

    data class Reactions(val onNext: () -> Unit)

    override fun act(action: Unit) {
        reactions.onNext()
    }
}

class CustomView(title: String, context: Context) : ViewBase<Int, Unit>(context) {

    private val text = make(::TextView, title).apply(Styles.text)
    private val button = make(::Button, "Button")
    private val container = make(::ContainerView)
    private val view = make(::TextView).apply { setBackgroundColor(Color.rgb(0, 255, 0)) }
    private val view2 = make(::TextView).apply { setBackgroundColor(Color.rgb(0, 0, 255)) }

    override fun loadView() {
        children(
                text,
                button,
                container.children(
                        view,
                        view2
                )
        )
    }

    override fun setupConstraints() {
        text.snp.makeConstraints {
            top.equalTo(50)
            left.equalTo(40)
        }

        button.snp.makeConstraints {
            centerX.equalTo(text)
            top.equalTo(text.snp.bottom).offset(30)
        }
        container.snp.makeConstraints {
            bottom.right.equalToSuperview()
            width.height.equalTo(150)
        }
        view.snp.makeConstraints {
            right.left.equalToSuperview()
            top.equalToSuperview()
        }
        view2.snp.makeConstraints {
            top.equalTo(view.snp.bottom)
            bottom.left.right.equalToSuperview()
            height.equalTo(50)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

//        snp.debugValuesRecursive()
//        snp.debugConstraintsRecursive()
    }

    override val actions: List<Observable<Unit>> = listOf(button.clicks())

    override fun update() {
        setBackgroundColor(Color.parseColor("#" + Integer.toHexString(componentState)))
    }

    private object Styles {

        val text = Style<View> {
            setBackgroundColor(Color.rgb(255, 0, 0))
        }
    }
}

class AnotherView(context: Context) : ViewBase<Unit, Unit>(context) {

    private val button = make(::Button, "Main")

    override val actions: List<Observable<Unit>> = listOf(button.clicks())

    override fun loadView() {
        children(button)

        button.textSize = 30f
        button.setBackgroundColor(Color.LTGRAY)
    }

    override fun setupConstraints() {
        button.snp.makeConstraints {
            center.equalToSuperview()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

//        snp.debugValuesRecursive()
//        snp.debugConstraintsRecursive()
    }
}