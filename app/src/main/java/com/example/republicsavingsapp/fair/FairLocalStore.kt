package com.example.republicsavingsapp.fair

data class PrizeRecord(val wonAt: Long, val tier: PrizeTier, val prizeName: String)

interface FairLocalStore {
    /** Saved amount of each unused ticket, oldest ticket first. */
    fun getSavings(userId: Long): List<Double>
    fun setSavings(userId: Long, savings: List<Double>)

    /** Prizes won, newest first. */
    fun getPrizeHistory(userId: Long): List<PrizeRecord>
    fun addPrize(userId: Long, prize: PrizeRecord)
}

/** Turns local data into text and back, so it can be unit tested without Android. */
object FairLocalCodec {
    fun encodeSavings(savings: List<Double>): String = savings.joinToString(",")

    fun decodeSavings(text: String?): List<Double> =
        text?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() } ?: emptyList()

    fun encodePrizes(prizes: List<PrizeRecord>): String =
        prizes.joinToString("\n") { "${it.wonAt}|${it.tier.name}|${it.prizeName}" }

    /** Malformed lines are skipped rather than crashing the Fair screen. */
    fun decodePrizes(text: String?): List<PrizeRecord> =
        text?.lines()?.mapNotNull { decodePrize(it) } ?: emptyList()

    private fun decodePrize(line: String): PrizeRecord? {
        val parts = line.split("|", limit = 3)
        if (parts.size != 3) return null
        val wonAt = parts[0].toLongOrNull() ?: return null
        val tier = PrizeTier.values().firstOrNull { it.name == parts[1] } ?: return null
        return PrizeRecord(wonAt, tier, parts[2])
    }
}