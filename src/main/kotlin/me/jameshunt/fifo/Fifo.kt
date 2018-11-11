package me.jameshunt.fifo

import kotlin.math.absoluteValue

/**
 * Functional approach to computing gain using "First in, First out"
 *
 * Yay, no state
 */




object Fifo {

    sealed class Results(val gainSoFar: Double) {
        class LeftOverPurchases(val leftOverPurchases: List<Fifo.Transaction>, gainSoFar: Double): Results(gainSoFar)
        class LeftOverSales(val leftOverSales: List<Fifo.Transaction>, gainSoFar: Double): Results(gainSoFar)
    }

    private data class AccumulateResults(
            val gainSoFar: Double,
            val remainingSold: List<Fifo.Transaction>,
            val remainingPurchases: List<Fifo.Transaction> = listOf()
    )

    data class Transaction(val items: Double, val currencyAmount: Double) {

        constructor(transaction: Transaction, itemsLeft: Double) : this(
                items = itemsLeft,
                currencyAmount = transaction.currencyAmount * itemsLeft / transaction.items
        )

        val currencyPerUnit: Double
            get() = this.currencyAmount / this.items
    }

    fun findRealizedGain(purchases: List<Transaction>, sales: List<Transaction>): Results {

        val initialValue = AccumulateResults(gainSoFar = 0.0, remainingSold = sales)
        return purchases.fold(initial = initialValue) { acc, purchase ->

            val onePassResults = this.useOnePurchase(
                    purchase = purchase,
                    remainingSold = acc.remainingSold
            ).handleLeftOver()

            val currentGain = acc.gainSoFar + onePassResults.gainSoFar
            val remainingPurchases = acc.remainingPurchases + onePassResults.remainingPurchases

            AccumulateResults(
                    gainSoFar = currentGain,
                    remainingSold = onePassResults.remainingSold,
                    remainingPurchases = remainingPurchases
            )
        }.let {
            when {
                it.remainingPurchases.isNotEmpty() -> Results.LeftOverPurchases(it.remainingPurchases, it.gainSoFar)
                else -> Results.LeftOverSales(it.remainingSold, it.gainSoFar)
            }
        }
    }

    private fun LeftOver.handleLeftOver(): AccumulateResults = when (this) {
        is LeftOver.PurchaseLeftOver -> AccumulateResults(gainSoFar = this.gain, remainingSold = listOf(), remainingPurchases = listOf(this.purchase))
        is LeftOver.SoldLeftOver -> AccumulateResults(gainSoFar = this.gain, remainingSold = this.remainingSold)
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

    internal tailrec fun useOnePurchase(purchase: Transaction, remainingSold: List<Transaction>, gain: Double = 0.0): LeftOver {

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
            else -> LeftOverOneSale.BothUsed(gain = sold.currencyAmount - purchase.currencyAmount)
        }
    }

    internal sealed class LeftOverOneSale {
        data class PurchaseLeftOver(val purchase: Transaction, val gain: Double) : LeftOverOneSale()
        data class SoldLeftOver(val sold: Transaction, val gain: Double) : LeftOverOneSale()
        data class BothUsed(val gain: Double) : LeftOverOneSale()
    }
}
