package com.example.republicsavingsapp.fair

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FairLocalCodecTest {

    @Test
    fun savings_roundTrip() {
        val savings = listOf(650.0, 100.0, 312.5)
        assertEquals(savings, FairLocalCodec.decodeSavings(FairLocalCodec.encodeSavings(savings)))
    }

    @Test
    fun savings_emptyOrMissingOrDamagedTextGivesNoSavings() {
        assertTrue(FairLocalCodec.decodeSavings(FairLocalCodec.encodeSavings(emptyList())).isEmpty())
        assertTrue(FairLocalCodec.decodeSavings(null).isEmpty())
        assertEquals(listOf(100.0, 200.0), FairLocalCodec.decodeSavings("100.0,oops,200.0"))
    }

    @Test
    fun prizes_roundTripKeepingOrderAndNamesWithSpaces() {
        val prizes = listOf(
            PrizeRecord(2000L, PrizeTier.LARGE, "Whale watching tour"),
            PrizeRecord(1000L, PrizeTier.SMALL, "Pizza")
        )
        assertEquals(prizes, FairLocalCodec.decodePrizes(FairLocalCodec.encodePrizes(prizes)))
    }

    @Test
    fun prizes_damagedLinesAreSkippedNotFatal() {
        val text = "1000|SMALL|Pizza\nnot a prize\n2000|NOSUCHTIER|Mystery\nabc|SMALL|Bad time\n3000|MEDIUM|A nice dinner"
        assertEquals(
            listOf(PrizeRecord(1000L, PrizeTier.SMALL, "Pizza"), PrizeRecord(3000L, PrizeTier.MEDIUM, "A nice dinner")),
            FairLocalCodec.decodePrizes(text)
        )
        assertTrue(FairLocalCodec.decodePrizes(null).isEmpty())
        assertTrue(FairLocalCodec.decodePrizes("").isEmpty())
    }
}