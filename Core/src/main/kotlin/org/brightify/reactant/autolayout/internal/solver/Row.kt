package org.brightify.reactant.autolayout.internal.solver

import org.brightify.reactant.autolayout.internal.util.isAlmostZero

/**
 *  @author <a href="mailto:filip@brightify.org">Filip Dolnik</a>
 */
internal class Row(var constant: Double = 0.0) {

    val symbols = LinkedHashMap<Symbol, Double>()

    constructor(other: Row): this(other.constant) {
        symbols.putAll(other.symbols)
    }

    fun multiplyBy(coefficient: Double) {
        constant *= coefficient

        symbols.forEach { symbols[it.key] = it.value * coefficient }
    }

    fun addExpression(row: Row, coefficient: Double, subject: Symbol? = null, solver: SimplexSolver? = null) {
        constant += coefficient * row.constant

        row.symbols.forEach { addVariable(it.key, it.value * coefficient, subject, solver) }
    }

    fun addVariable(symbol: Symbol, coefficient: Double, subject: Symbol? = null, solver: SimplexSolver? = null) {
        val oldCoefficient = symbols[symbol]
        if (oldCoefficient != null) {
            val newCoefficient = oldCoefficient + coefficient
            if (newCoefficient.isAlmostZero) {
                subject?.let { solver?.onSymbolRemoved(symbol, it) }
                symbols.remove(symbol)
            } else {
                symbols[symbol] = newCoefficient
            }
        } else if (!coefficient.isAlmostZero) {
            symbols[symbol] = coefficient
            subject?.let { solver?.onSymbolAdded(symbol, it) }
        }
    }

    fun substituteOut(symbol: Symbol, row: Row, subject: Symbol, solver: SimplexSolver) {
        symbols.remove(symbol)?.let { addExpression(row, it, subject, solver) }
    }

    fun changeSubject(old: Symbol, new: Symbol) {
        symbols[old] = newSubject(new)
    }

    fun newSubject(subject: Symbol): Double {
        val coefficient = 1.0 / (symbols.remove(subject) ?: 1.0)
        multiplyBy(-coefficient)
        return coefficient
    }

    fun coefficientFor(symbol: Symbol): Double = symbols[symbol] ?: 0.0
}
