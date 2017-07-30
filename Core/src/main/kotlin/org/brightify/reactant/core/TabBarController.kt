package org.brightify.reactant.core

import android.app.FragmentManager
import android.app.FragmentTransaction
import android.support.design.widget.BottomNavigationView
import android.view.Menu
import android.view.ViewGroup
import android.widget.FrameLayout
import org.brightify.reactant.core.constraint.AutoLayout
import org.brightify.reactant.core.constraint.ConstraintPriority
import org.brightify.reactant.core.constraint.util.snp
import org.brightify.reactant.core.util.push

/**
 *  @author <a href="mailto:matous@brightify.org">Matous Hybl</a>
 */
open class TabBarController(private val controllers: List<ViewController>) : ViewController() {

    lateinit var fragmentContainer: FrameLayout
    lateinit var tabBar: BottomNavigationView

    private val childFragmentManager: FragmentManager
        get() = viewControllerWrapper.childFragmentManager

    private var selectedController: ViewController? = null

    override fun onCreate() {
        fragmentContainer = FrameLayout(activity)
        fragmentContainer.assignId()

        tabBar = BottomNavigationView(activity)
        val layout = AutoLayout(activity)
        contentView = layout
        contentView.assignId()
        contentView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layout.addView(fragmentContainer)
        layout.addView(tabBar)
        fragmentContainer.snp.makeConstraints {
            left.right.top.equalToSuperview()
            bottom.equalTo(tabBar.snp.top)
        }
        fragmentContainer.snp.disableIntrinsicSize()

        tabBar.snp.verticalContentHuggingPriority = ConstraintPriority.required
        tabBar.snp.makeConstraints {
            left.equalToSuperview()
            right.equalToSuperview()
            bottom.equalToSuperview()
        }

        controllers.forEachIndexed { index, controller ->
            val text = controller.tabBarItem?.titleRes?.let { activity.resources.getString(it) } ?: "Undefined"
            val item = tabBar.menu.add(Menu.NONE, index, 0, text)
            val imageRes = controller.tabBarItem?.imageRes
            if (imageRes != null) {
                item.icon = activity.resources.getDrawable(imageRes)
            }
            item.setOnMenuItemClickListener {
                if(tabBar.selectedItemId != item.itemId) {
                    displayController(controller)
                } else {
                    // FIXME pop backstack - check guidelines for correct behavior
                }
                return@setOnMenuItemClickListener false
            }
        }
        selectedController = controllers.firstOrNull()
    }

    override fun onActivityCreated() {
        super.onActivityCreated()

        selectedController?.let { displayController(it) }
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.top?.viewController?.onBackPressed() == true) {
            return true
        }

        val stackSize = childFragmentManager.backStackEntryCount
        if (stackSize > 1) {
            childFragmentManager.popBackStackImmediate()
            childFragmentManager.top?.viewController?.let {
                it.tabBarController = this
                selectedController = it
                tabBar.menu.findItem(controllers.indexOf(it)).setChecked(true)
            }
            return true
        } else {
            return false
        }
    }

    private fun displayController(controller: ViewController, animated: Boolean = false) {
        val transaction = childFragmentManager.beginTransaction()
        transaction.push(fragmentContainer.id, controller.viewControllerWrapper)
        transaction.setTransition(if (animated) FragmentTransaction.TRANSIT_FRAGMENT_OPEN else FragmentTransaction.TRANSIT_NONE)
        transaction.commit()
        controller.tabBarController = this
        selectedController = controller
    }
}

data class TabBarItem(val titleRes: Int, val imageRes: Int)