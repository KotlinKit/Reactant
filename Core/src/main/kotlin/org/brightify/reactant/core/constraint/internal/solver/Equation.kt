package org.brightify.reactant.core.constraint.internal.solver

import org.brightify.reactant.core.constraint.ConstraintPriority
import org.brightify.reactant.core.constraint.internal.ConstraintOperator

/**
 *  @author <a href="mailto:filip.dolnik.96@gmail.com">Filip Dolnik</a>
 */
internal class Equation(val constant: Double = 0.0, val terms: List<Term> = emptyList(),
                        val operator: ConstraintOperator = ConstraintOperator.equal,
                        val priority: ConstraintPriority = ConstraintPriority.required)