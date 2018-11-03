package me.jameshunt.fifo

import org.junit.Assert
import org.junit.Test

class UseOneSaleOnPurchaseTest {

    private val fifo: Fifo by lazy {
        val purchased = listOf(
                2.0 to 20.0,
                20.0 to 300.84,
                4.0 to 100.0,
                3.0 to 58.93,
                3.0 to 64.54,
                17.0 to 210.11,
                12.0 to 137.48,
                1.0 to 32.00,
                1.0 to 12.9
        )

        val sold = listOf(
                2.0 to 10.0,
                14.0 to 200.84,
                16.0 to 300.0,
                3.0 to 48.93,
                4.0 to 84.54,
                6.0 to 110.11,
                2.0 to 37.48,
                1.0 to 22.00,
                1.0 to 4.9
        )

        //printTotals(purchased, sold)

        Fifo(purchased, sold)
    }


    @Test
    fun useOneSaleOnPurchaseTest() {
        Assert.assertEquals(
                Fifo.LeftOverOneEach.SoldLeftOver(sold = Fifo.Transaction(items = 1.0, currencyAmount = 23.333333333333332), gain = 26.666666666666664),
                fifo.useOneSaleOnPurchase(2.0 to 20.0, 3.0 to 70.0)
        )

        Assert.assertEquals(
                Fifo.LeftOverOneEach.PurchaseLeftOver(purchase = Fifo.Transaction(items = 1.0, currencyAmount = 5.0), gain = 55.0),
                fifo.useOneSaleOnPurchase(4.0 to 20.0, 3.0 to 70.0)
        )

        Assert.assertEquals(
                Fifo.LeftOverOneEach.PurchaseLeftOver(purchase = Fifo.Transaction(items = 1.0, currencyAmount = 5.0), gain = 3.0),
                fifo.useOneSaleOnPurchase(4.0 to 20.0, 3.0 to 18.0)
        )

        Assert.assertEquals(
                Fifo.LeftOverOneEach.PurchaseLeftOver(purchase = Fifo.Transaction(items = 1.0, currencyAmount = 5.0), gain = -3.0),
                fifo.useOneSaleOnPurchase(4.0 to 20.0, 3.0 to 12.0)
        )
    }

    @Test
    fun useOnePurchaseTest() {
        Assert.assertEquals(
                Fifo.LeftOver.BothUsed(remainingSold = listOf(), gain = 10.0),
                fifo.useOnePurchase(4.0 to 20.0, listOf(2.0 to 15.0, 2.0 to 15.0))
        ) // should be gain of 10

        Assert.assertEquals(
                Fifo.LeftOver.PurchaseLeftOver(Fifo.Transaction(items = 1.0, currencyAmount = 4.0), gain = 14.0),
                fifo.useOnePurchase(5.0 to 20.0, listOf(2.0 to 15.0, 2.0 to 15.0))
        ) // should be gain of 14

        Assert.assertEquals(
                Fifo.LeftOver.PurchaseLeftOver(Fifo.Transaction(items = 2.0, currencyAmount = 4.0), gain = 49.0),
                fifo.useOnePurchase(10.0 to 20.0, listOf(2.0 to 15.0, 6.0 to 50.0))
        ) // should be gain of 49

        Assert.assertEquals(
                Fifo.LeftOver.BothUsed(remainingSold = listOf(), gain = 45.0),
                fifo.useOnePurchase(8.0 to 20.0, listOf(2.0 to 15.0, 6.0 to 50.0))
        ) // should be gain of 45
    }

    @Test
    fun findRealizedGain() {
        Assert.assertEquals(64.38, fifo.findRealizedGain(),0.1)
    }

    private fun printTotals(purchased: List<Fifo.Transaction>, sold: List<Fifo.Transaction>) {
        println(purchased
                .asSequence()
                .reduce { acc, pair ->
                    Fifo.Transaction(pair.items + acc.items, pair.currencyAmount + acc.currencyAmount)
                }
        )

        println(sold
                .asSequence()
                .reduce { acc, pair ->
                    Fifo.Transaction(pair.items + acc.items, pair.currencyAmount + acc.currencyAmount)
                }
        )
    }
}