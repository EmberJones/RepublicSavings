package com.example.republicsavingsapp.fair

data class FairState(
    val tickets: Int,                // unused tickets
    val awardedKeys: Set<String>     // every week/month already turned into a ticket (see TicketAward.recordKey)
)

/**
 * The Fair's online data. It lives on the user's document in the "users" collection:
 *  - tickets: how many unused tickets the user has
 *  - fairAwarded: the record keys of every period already awarded, so none is awarded twice
 */
interface FairStateDAO {
    suspend fun getState(userId: Long): FairState

    suspend fun addAwards(userId: Long, recordKeys: List<String>): List<String>

    suspend fun useTicket(userId: Long): Boolean
}