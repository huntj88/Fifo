package me.jameshunt.fifo

import kotlin.math.absoluteValue

infix fun Double.to(that: Double): Fifo.Transaction = Fifo.Transaction(this, that)

/**
 * Functional approach to computing gain using "First in, First out"
 *
 * Yay, no state
 */

/** just a pair with named variables. I can't come up with a better name without it being super verbose **/
private data class Pair(val gainSoFar: Double, val remainingSold: List<Fifo.Transaction>)

class Fifo(private val purchases: List<Transaction>, private val sales: List<Transaction>) {

    data class Transaction(val items: Double, val currencyAmount: Double) {

        constructor(transaction: Transaction, itemsLeft: Double) : this(
                items = itemsLeft,
                currencyAmount = transaction.currencyAmount * itemsLeft / transaction.items
        )

        val currencyPerUnit: Double
            get() = this.currencyAmount / this.items
    }

    fun findRealizedGain(): Double {

        val initialValue = Pair(gainSoFar = 0.0, remainingSold = this.sales)
        return this.purchases.fold(initial = initialValue) { acc, purchase ->

            val onePassResults = this.useOnePurchase(
                    purchase = purchase,
                    remainingSold = acc.remainingSold
            ).handleRemainingSold()

            val currentGain = acc.gainSoFar + onePassResults.gainSoFar
            Pair(gainSoFar = currentGain, remainingSold = onePassResults.remainingSold)
        }.gainSoFar
    }

    private fun LeftOver.handleRemainingSold(): Pair = when (this) {
        is LeftOver.PurchaseLeftOver -> Pair(gainSoFar = this.gain, remainingSold = listOf())
        is LeftOver.SoldLeftOver -> Pair(gainSoFar = this.gain, remainingSold = this.remainingSold)
    }

    /**
     * recursive function
     *
     * When one purchase is "used", The gain made on that purchase is computed
     * against on all sales before you run out of "inventory" from that purchase.
     *
     * The sales used to calculate the gain on the purchase are also removed from the list
     *
     * After it computes the gain on that purchase, it is added to the existing gain
     */

    internal fun useOnePurchase(purchase: Transaction, remainingSold: List<Transaction>, gain: Double = 0.0): LeftOver {

        val sold = remainingSold.firstOrNull() ?: return LeftOver.PurchaseLeftOver(purchase = purchase, gain = gain)

        val leftOver = this.useOneSaleOnPurchase(purchase = purchase, sold = sold)

        return when (leftOver) {
            is LeftOverOneSale.PurchaseLeftOver -> this.useOnePurchase(
                    purchase = leftOver.purchase,
                    remainingSold = remainingSold.removeFirst(),
                    gain = leftOver.gain + gain
            )
            is LeftOverOneSale.SoldLeftOver -> {
                val newRemainingSold = listOf(leftOver.sold) + remainingSold.removeFirst()
                LeftOver.SoldLeftOver(
                        remainingSold = newRemainingSold,
                        gain = leftOver.gain + gain
                )
            }
            is LeftOverOneSale.BothUsed -> LeftOver.SoldLeftOver(
                    remainingSold = remainingSold.removeFirst(),
                    gain = leftOver.gain + gain
            )
        }
    }

    private fun List<Transaction>.removeFirst(): List<Transaction> = this.subList(1, this.size)

    internal sealed class LeftOver {
        data class PurchaseLeftOver(val purchase: Transaction, val gain: Double) : LeftOver()
        data class SoldLeftOver(val remainingSold: List<Transaction>, val gain: Double) : LeftOver()
    }

    /**
     * applies one sale against a purchase
     */

    internal fun useOneSaleOnPurchase(purchase: Transaction, sold: Transaction): LeftOverOneSale {
        val itemsLeft = purchase.items - sold.items

        return when {
            itemsLeft == 0.0 -> LeftOverOneSale.BothUsed(gain = sold.currencyAmount - purchase.currencyAmount)
            itemsLeft > 0 -> {
                val purchaseLeftOver = Transaction(transaction = purchase, itemsLeft = itemsLeft)
                val numSold = purchase.items - purchaseLeftOver.items
                val gain = (sold.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneSale.PurchaseLeftOver(purchase = purchaseLeftOver, gain = gain)
            }
            itemsLeft < 0 -> {
                val soldLeftOver = Transaction(sold, itemsLeft.absoluteValue)
                val numSold = sold.items - soldLeftOver.items
                val gain = (sold.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneSale.SoldLeftOver(sold = soldLeftOver, gain = gain)
            }
            else -> throw IllegalStateException()
        }
    }

    internal sealed class LeftOverOneSale {
        data class PurchaseLeftOver(val purchase: Transaction, val gain: Double) : LeftOverOneSale()
        data class SoldLeftOver(val sold: Transaction, val gain: Double) : LeftOverOneSale()
        data class BothUsed(val gain: Double) : LeftOverOneSale()
    }
}
