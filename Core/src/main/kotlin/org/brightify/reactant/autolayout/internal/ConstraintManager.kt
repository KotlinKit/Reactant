package org.brightify.reactant.autolayout.internal

import android.view.View
import org.brightify.reactant.autolayout.AutoLayout
import org.brightify.reactant.autolayout.Constraint
import org.brightify.reactant.autolayout.ConstraintVariable
import org.brightify.reactant.autolayout.exception.AutoLayoutNotFoundException
import org.brightify.reactant.autolayout.exception.ViewNotManagedByCommonAutoLayoutException
import org.brightify.reactant.autolayout.internal.solver.BackgroundSolver
import org.brightify.reactant.autolayout.internal.solver.SimplexSolver
import org.brightify.reactant.autolayout.internal.solver.Term
import org.brightify.reactant.autolayout.internal.view.IntrinsicSizeManager
import org.brightify.reactant.autolayout.internal.view.IntrinsicSizeNecessityDecider
import org.brightify.reactant.autolayout.internal.view.ViewConstraints
import org.brightify.reactant.autolayout.internal.view.VisibilityManager
import org.brightify.reactant.autolayout.util.children
import org.brightify.reactant.autolayout.util.description

/**
 *  @author <a href="mailto:filip@brightify.org">Filip Dolnik</a>
 */
internal class ConstraintManager {

    private val solver = BackgroundSolver(SimplexSolver())
    private val constraints = HashMap<View, HashSet<Constraint>>()
    internal val viewConstraints = HashMap<View, ViewConstraints>()
    private val intrinsicSizeNecessityDecider = IntrinsicSizeNecessityDecider()

    private val managedViews: Set<View>
        get() = viewConstraints.keys

    val allConstraints: List<Constraint>
        get() = constraints.flatMap { it.value }

    fun addConstraint(constraint: Constraint) {
        if (constraints[constraint.view]?.contains(constraint) == true) {
            return
        }

        if (verifyViewsUsedByConstraint(constraint)) {
            solver.addConstraint(constraint)
            constraint.isManaged = true
            constraints[constraint.view]?.add(constraint)
            intrinsicSizeNecessityDecider.addConstraint(constraint)
        } else if (!managedViews.contains(constraint.view)) {
            throw IllegalStateException("View ${constraint.view.description} is not managed by correct ConstraintManager.")
        } else {
            throw ViewNotManagedByCommonAutoLayoutException(constraint.view,
                    constraint.constraintItems.mapNotNull { it.rightVariable?.view }.first { !managedViews.contains(it) })
        }
    }

    fun removeConstraint(constraint: Constraint) {
        if (constraints[constraint.view]?.remove(constraint) != true) {
            return
        }

        solver.removeConstraint(constraint)
        constraint.isManaged = false
        intrinsicSizeNecessityDecider.removeConstraint(constraint)
    }

    fun addManagedView(view: View) {
        if (managedViews.contains(view)) {
            return
        }

        constraints[view] = HashSet()
        val constraints = ViewConstraints(view)
        viewConstraints[view] = constraints
        constraints.initialize()
    }

    fun removeManagedView(view: View) {
        if (!managedViews.contains(view)) {
            return
        }

        constraints.remove(view)?.forEach { removeConstraint(it) }
        normalizeConstraints()

        viewConstraints.remove(view)
    }

    fun join(constraintManager: ConstraintManager) {
        constraints.putAll(constraintManager.constraints)
        constraintManager.constraints.forEach {
            it.value.forEach { constraint ->
                solver.addConstraint(constraint)
                intrinsicSizeNecessityDecider.addConstraint(constraint)
            }
        }
        viewConstraints.putAll(constraintManager.viewConstraints)
    }

    fun split(view: View): ConstraintManager {
        val leavingViews = HashSet<View>()
        fun addRecursive(leaving: View) {
            leavingViews.add(leaving)
            if (leaving is AutoLayout) {
                leaving.children.forEach {
                    addRecursive(it)
                }
            }
        }
        addRecursive(view)

        val newConstraintManager = ConstraintManager()

        val leavingSizeManagers = viewConstraints.filterKeys { leavingViews.contains(it) }
        newConstraintManager.viewConstraints.putAll(leavingSizeManagers)
        leavingSizeManagers.forEach { viewConstraints.remove(it.key) }

        val leavingConstraints = constraints.filterKeys { leavingViews.contains(it) }
        newConstraintManager.constraints.putAll(leavingConstraints)
        newConstraintManager.normalizeConstraints()
        newConstraintManager.constraints.forEach {
            it.value.forEach { constraint ->
                newConstraintManager.solver.addConstraint(constraint)
            }
        }

        leavingViews.forEach {
            constraints.remove(it)?.forEach { constraint ->
                solver.removeConstraint(constraint)
            }
        }
        normalizeConstraints()

        return newConstraintManager
    }

    fun removeUserViewConstraints(view: View) {
        ((constraints[view] ?: HashSet()) - (viewConstraints[view]?.usedConstraints ?: emptySet())).forEach { removeConstraint(it) }
    }

    fun getValueForVariable(variable: ConstraintVariable): Double {
        var result = 0.0
        Term(variable).baseTerms.forEach {
            result += it.coefficient * solver.getValueForVariable(it.variable)
        }
        return result
    }

    fun getVisibilityManager(view: View): VisibilityManager = viewConstraints[view]?.visibilityManager ?: throw AutoLayoutNotFoundException(
            view)

    fun getIntrinsicSizeManager(view: View): IntrinsicSizeManager? = viewConstraints[view]?.intrinsicSizeManager

    fun needsIntrinsicWidth(view: View): Boolean = getIntrinsicSizeManager(view) != null &&
            intrinsicSizeNecessityDecider.needsIntrinsicWidth(view)

    fun needsIntrinsicHeight(view: View): Boolean = getIntrinsicSizeManager(view) != null &&
            intrinsicSizeNecessityDecider.needsIntrinsicHeight(view)

    fun updateIntrinsicSizeNecessityDecider() {
        intrinsicSizeNecessityDecider.updateViewsNeedingIntrinsicSize(viewConstraints)
    }

    private fun verifyViewsUsedByConstraint(constraint: Constraint): Boolean {
        return constraint.constraintItems.all { constraintItem ->
            managedViews.contains(constraintItem.leftVariable.view) && constraintItem.rightVariable?.let { managedViews.contains(it.view) } != false
        }
    }

    private fun normalizeConstraints() {
        HashMap(constraints).forEach {
            HashSet(it.value).forEach { constraint ->
                if (!verifyViewsUsedByConstraint(constraint)) {
                    removeConstraint(constraint)
                }
            }
        }
    }
}
