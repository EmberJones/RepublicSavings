package com.example.republicsavingsapp.fair

import android.content.Context

/** [FairLocalStore] backed by SharedPreferences, with separate entries for each user id. */
class SharedPreferencesFairStore(context: Context) : FairLocalStore {

    private val prefs = context.getSharedPreferences("fair_store", Context.MODE_PRIVATE)

    override fun getSavings(userId: Long): List<Double> =
        FairLocalCodec.decodeSavings(prefs.getString("savings_$userId", null))

    override fun setSavings(userId: Long, savings: List<Double>) {
        prefs.edit().putString("savings_$userId", FairLocalCodec.encodeSavings(savings)).apply()
    }

    override fun getPrizeHistory(userId: Long): List<PrizeRecord> =
        FairLocalCodec.decodePrizes(prefs.getString("prizes_$userId", null))

    override fun addPrize(userId: Long, prize: PrizeRecord) {
        val updated = listOf(prize) + getPrizeHistory(userId)
        prefs.edit().putString("prizes_$userId", FairLocalCodec.encodePrizes(updated)).apply()
    }
}