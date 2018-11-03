package me.jameshunt.fifo

import kotlin.math.absoluteValue

internal infix fun Double.to(that: Double): Fifo.Transaction = Fifo.Transaction(this, that)

class Fifo(private val purchased: List<Transaction>, private val sold: List<Transaction>) {

    fun findRealizedGain(): Double {
        return purchased
                .fold(Pair(0.0, sold)) { acc, transaction ->
                    val leftOver = useOnePurchase(transaction, acc.second)
                    val resultFromOnePass = when (leftOver) {
                        is LeftOver.PurchaseLeftOver -> Pair(leftOver.gain, listOf())
                        is LeftOver.BothUsed -> Pair(leftOver.gain, leftOver.remainingSold)
                        is LeftOver.SoldLeftOver -> Pair(leftOver.gain, leftOver.remainingSold)
                    }
                    Pair(acc.first + resultFromOnePass.first, resultFromOnePass.second)
                }.first
    }

    internal fun useOnePurchase(purchase: Transaction, remainingSold: List<Transaction>, gain: Double = 0.0): LeftOver {

        remainingSold.firstOrNull() ?: return LeftOver.PurchaseLeftOver(purchase, gain)

        val leftOver = useOneSaleOnPurchase(purchase, remainingSold.first())

        return when (leftOver) {
            is LeftOverOneEach.PurchaseLeftOver -> useOnePurchase(
                    purchase = leftOver.purchase,
                    remainingSold = remainingSold.subList(1, remainingSold.size),
                    gain = leftOver.gain + gain
            )
            is LeftOverOneEach.SoldLeftOver -> {
                val newRemainingSold = listOf(leftOver.sold) + remainingSold.subList(1, remainingSold.size)
                LeftOver.SoldLeftOver(
                        remainingSold = newRemainingSold,
                        gain = leftOver.gain + gain
                )
            }
            is LeftOverOneEach.BothUsed -> LeftOver.BothUsed(
                    remainingSold = remainingSold.subList(1, remainingSold.size),
                    gain = leftOver.gain + gain
            )
        }
    }

    sealed class LeftOver {
        data class PurchaseLeftOver(val purchase: Transaction, val gain: Double) : LeftOver()
        data class SoldLeftOver(val remainingSold: List<Transaction>, val gain: Double) : LeftOver()
        data class BothUsed(val remainingSold: List<Transaction>, val gain: Double) : LeftOver()
    }


    internal fun useOneSaleOnPurchase(purchase: Transaction, sale: Transaction): LeftOverOneEach {
        val itemsLeft = purchase.items - sale.items

        return when {
            itemsLeft > 0 -> {
                val purchaseLeftOver = Transaction(purchase, itemsLeft)
                val numSold = (purchase.items - purchaseLeftOver.items)
                val gain = (sale.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneEach.PurchaseLeftOver(purchaseLeftOver, gain)
            }
            itemsLeft == 0.0 -> LeftOverOneEach.BothUsed(sale.currencyAmount - purchase.currencyAmount)
            itemsLeft < 0 -> {
                val saleLeftOver = Transaction(sale, itemsLeft.absoluteValue)
                val numSold = (sale.items - saleLeftOver.items)
                val gain = (sale.currencyPerUnit - purchase.currencyPerUnit) * numSold

                LeftOverOneEach.SoldLeftOver(saleLeftOver, gain)
            }
            else -> throw IllegalStateException()
        }
    }

    sealed class LeftOverOneEach {
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
            get() = currencyAmount / items
    }
}

