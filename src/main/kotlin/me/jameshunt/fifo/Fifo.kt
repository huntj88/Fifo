package me.jameshunt.fifo

import kotlin.math.absoluteValue

infix fun Double.to(that: Double): Fifo.Transaction = Fifo.Transaction(this, that)

/** just a pair with named variables. I can't come up with a better name without it being super verbose **/
private data class Pair(val gainSoFar: Double, val remainingSold: List<Fifo.Transaction>)

class Fifo(private val purchases: List<Transaction>, private val sales: List<Transaction>) {

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

    internal fun useOnePurchase(purchase: Transaction, remainingSold: List<Transaction>, gain: Double = 0.0): LeftOver {

        val sold = remainingSold.firstOrNull() ?: return LeftOver.PurchaseLeftOver(purchase = purchase, gain = gain)

        val leftOver = this.useOneSaleOnPurchase(purchase = purchase, sold = sold)

        return when (leftOver) {
            is LeftOverOneEach.PurchaseLeftOver -> this.useOnePurchase(
                    purchase = leftOver.purchase,
                    remainingSold = remainingSold.removeFirst(),
                    gain = leftOver.gain + gain
            )
            is LeftOverOneEach.SoldLeftOver -> {
                val newRemainingSold = listOf(leftOver.sold) + remainingSold.removeFirst()
                LeftOver.SoldLeftOver(
                        remainingSold = newRemainingSold,
                        gain = leftOver.gain + gain
                )
            }
            is LeftOverOneEach.BothUsed -> LeftOver.SoldLeftOver(
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

    internal fun useOneSaleOnPurchase(purchase: Transaction, sold: Transaction): LeftOverOneEach {
        val itemsLeft = purchase.items - sold.items

        return when {
            itemsLeft == 0.0 -> LeftOverOneEach.BothUsed(gain = sold.currencyAmount - purchase.currencyAmount)
            itemsLeft > 0 -> {
                val purchaseLeftOver = Transaction(transaction = purchase, itemsLeft = itemsLeft)
                val numSold = purchase.items - purchaseLeftOver.items
                val gain = (sold.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneEach.PurchaseLeftOver(purchase = purchaseLeftOver, gain = gain)
            }
            itemsLeft < 0 -> {
                val soldLeftOver = Transaction(sold, itemsLeft.absoluteValue)
                val numSold = sold.items - soldLeftOver.items
                val gain = (sold.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneEach.SoldLeftOver(sold = soldLeftOver, gain = gain)
            }
            else -> throw IllegalStateException()
        }
    }

    internal sealed class LeftOverOneEach {
        data class PurchaseLeftOver(val purchase: Transaction, val gain: Double) : LeftOverOneEach()
        data class SoldLeftOver(val sold: Transaction, val gain: Double) : LeftOverOneEach()
        data class BothUsed(val gain: Double) : LeftOverOneEach()
    }

    data class Transaction(
            val items: Double,
            val currencyAmount: Double
    ) {
        constructor(transaction: Transaction, itemsLeft: Double) : this(
                items = itemsLeft,
                currencyAmount = transaction.currencyAmount * itemsLeft / transaction.items
        )

        val currencyPerUnit: Double
            get() = this.currencyAmount / items
    }
}
