package org.brightify.reactant.autolayout.internal.view

import android.view.View
import org.brightify.reactant.autolayout.Constraint
import org.brightify.reactant.autolayout.ConstraintOperator
import org.brightify.reactant.autolayout.ConstraintPriority
import org.brightify.reactant.autolayout.ConstraintVariable
import org.brightify.reactant.autolayout.internal.ConstraintItem
import org.brightify.reactant.autolayout.internal.ConstraintType

/**
 *  @author <a href="mailto:filip@brightify.org">Filip Dolnik</a>
 */
internal class IntrinsicSizeNecessityDecider {

    private val fixedVariables = HashMap<View, HashSet<ConstraintType>>()
    private val viewsNeedingIntrinsicWidth = HashSet<View>()
    private val viewsNeedingIntrinsicHeight = HashSet<View>()

    private val activeConstraints = HashSet<Constraint>()
    private val constraintsToAdd = HashSet<Constraint>()
    private val constraintsToRemove = HashSet<Constraint>()

    fun needsIntrinsicWidth(view: View): Boolean {
        invalidate()
        return viewsNeedingIntrinsicWidth.contains(view) ||
                fixedVariables[view]?.contains(ConstraintType.width) != true
    }

    fun needsIntrinsicHeight(view: View): Boolean {
        invalidate()
        return viewsNeedingIntrinsicHeight.contains(view) ||
                fixedVariables[view]?.contains(ConstraintType.height) != true
    }

    fun addConstraint(constraint: Constraint) {
        if (constraint.isFixing) {
            constraintsToRemove.remove(constraint)
            if (!activeConstraints.contains(constraint)) {
                constraintsToAdd.add(constraint)
            }
        }
    }

    fun removeConstraint(constraint: Constraint) {
        if (!constraintsToAdd.remove(constraint)) {
            if (activeConstraints.contains(constraint)) {
                constraintsToRemove.add(constraint)
            }
        }
    }

    fun updateViewsNeedingIntrinsicSize(viewsConstraints: Map<View, ViewConstraints>) {
        viewsNeedingIntrinsicWidth.clear()
        viewsNeedingIntrinsicHeight.clear()
        viewsConstraints.forEach { (view, viewConstraints) ->
            viewConstraints.intrinsicSizeManager?.let {
                if (it.width.contentHuggingPriority == ConstraintPriority.required ||
                        it.width.contentCompressionResistancePriority == ConstraintPriority.required) {
                    viewsNeedingIntrinsicWidth.add(view)
                }
                if (it.width.isUsingEqualConstraint) {
                    addConstraint(it.width.equalConstraintForNecessityDecider)
                } else {
                    removeConstraint(it.width.equalConstraintForNecessityDecider)
                }
                if (it.height.contentHuggingPriority == ConstraintPriority.required ||
                        it.height.contentCompressionResistancePriority == ConstraintPriority.required) {
                    viewsNeedingIntrinsicHeight.add(view)
                }
                if (it.height.isUsingEqualConstraint) {
                    addConstraint(it.height.equalConstraintForNecessityDecider)
                } else {
                    removeConstraint(it.height.equalConstraintForNecessityDecider)
                }
            }
        }
    }

    private fun invalidate() {
        if (constraintsToAdd.isNotEmpty() || constraintsToRemove.isNotEmpty()) {
            fixedVariables.clear()
            activeConstraints.removeAll(constraintsToRemove)
            activeConstraints.addAll(constraintsToAdd)
            constraintsToRemove.clear()
            constraintsToAdd.clear()
            solve()
        }
    }

    private fun solve() {
        var constraintItems = HashSet<ConstraintItem>()
        activeConstraints.forEach { constraintItems.addAll(it.constraintItems) }
        var lastItemsSize = 0
        while (constraintItems.size != lastItemsSize) {
            lastItemsSize = constraintItems.size
            val newConstraintItems = HashSet(constraintItems)

            constraintItems.forEach { constraintItem ->
                if (constraintItem.leftVariable.isFixed) {
                    constraintItem.rightVariable?.let { addFixedVariable(it) }
                    newConstraintItems.remove(constraintItem)
                } else if (constraintItem.rightVariable?.isFixed != false) {
                    addFixedVariable(constraintItem.leftVariable)
                    newConstraintItems.remove(constraintItem)
                }
            }
            constraintItems = newConstraintItems
        }
    }

    private fun addFixedVariable(constraintVariable: ConstraintVariable) {
        var fixedTypes = fixedVariables[constraintVariable.view]
        if (fixedTypes == null) {
            fixedTypes = HashSet()
            fixedVariables[constraintVariable.view] = fixedTypes
        }

        if (fixedTypes.add(constraintVariable.type)) {
            if (areFixedImplicitly(fixedTypes, ConstraintType.left, ConstraintType.right, ConstraintType.width, ConstraintType.centerX)) {
                fixedTypes.add(ConstraintType.left)
                fixedTypes.add(ConstraintType.right)
                fixedTypes.add(ConstraintType.width)
                fixedTypes.add(ConstraintType.centerX)
            }
            if (areFixedImplicitly(fixedTypes, ConstraintType.top, ConstraintType.bottom, ConstraintType.height, ConstraintType.centerY)) {
                fixedTypes.add(ConstraintType.top)
                fixedTypes.add(ConstraintType.bottom)
                fixedTypes.add(ConstraintType.height)
                fixedTypes.add(ConstraintType.centerY)
            }
        }
    }

    private fun areFixedImplicitly(fixedTypes: HashSet<ConstraintType>, first: ConstraintType, second: ConstraintType,
                                   third: ConstraintType, fourth: ConstraintType): Boolean {
        var count = 0
        if (fixedTypes.contains(first)) {
            count++
        }
        if (fixedTypes.contains(second)) {
            count++
        }
        if (fixedTypes.contains(third)) {
            count++
        }
        if (fixedTypes.contains(fourth)) {
            count++
        }
        return count >= 2
    }

    private val ConstraintVariable.isFixed: Boolean
        get() = fixedVariables[view]?.contains(type) == true

    private val Constraint.isFixing: Boolean
        get() = priority == ConstraintPriority.required && constraintItems.all { it.operator == ConstraintOperator.equal }
}