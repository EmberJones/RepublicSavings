package com.example.republicsavingsapp.fair

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Firestore implementation of [TicketDAO]. Tickets live in the "tickets" collection, one document
 * per ticket, using the ticket id as the document id.
 *
 * Both writes use transactions so they are safe if the app checks twice or the user double taps:
 * a transaction re-reads the document and only writes if it is still in the expected state.
 */
class FirestoreTicketDAO(private val firestore: FirebaseFirestore) : TicketDAO {

    private val tickets get() = firestore.collection("tickets")

    override suspend fun getAllForUser(userId: Long): List<Ticket> {
        val snapshot = tickets
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToTicket(it) }
    }

    override suspend fun addIfAbsent(ticket: Ticket): Boolean {
        val ref = tickets.document(ticket.ticketId)
        val data = mapOf<String, Any>(
            "ticketId" to ticket.ticketId,
            "userId" to ticket.userId,
            "period" to ticket.period.name,
            "periodKey" to ticket.periodKey,
            "periodStart" to ticket.periodStart.toString(),   // ISO text, e.g. 2026-09-07
            "periodEnd" to ticket.periodEnd.toString(),
            "savedUnderBudget" to ticket.savedUnderBudget,
            "earnedAt" to ticket.earnedAt,
            "spent" to false
        )
        return firestore.runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                false
            } else {
                transaction.set(ref, data)
                true
            }
        }.await()
    }

    override suspend fun markSpent(ticketId: String, prize: Prize): Boolean {
        val ref = tickets.document(ticketId)
        val update = mapOf<String, Any>(
            "spent" to true,
            "prizeTier" to prize.tier.name,
            "prizeName" to prize.name,
            "prizeValue" to prize.tier.value,
            "spentAt" to System.currentTimeMillis()
        )
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (!snapshot.exists() || snapshot.getBoolean("spent") == true) {
                false
            } else {
                transaction.update(ref, update)
                true
            }
        }.await()
    }

    /** Converts a document to a [Ticket], or null (and a log line) if it is malformed. */
    private fun docToTicket(doc: DocumentSnapshot): Ticket? {
        return try {
            Ticket(
                ticketId = doc.getString("ticketId") ?: doc.id,
                userId = doc.getLong("userId") ?: return null,
                period = TicketPeriod.valueOf(doc.getString("period") ?: return null),
                periodKey = doc.getString("periodKey") ?: return null,
                periodStart = LocalDate.parse(doc.getString("periodStart") ?: return null),
                periodEnd = LocalDate.parse(doc.getString("periodEnd") ?: return null),
                savedUnderBudget = doc.getDouble("savedUnderBudget") ?: 0.0,
                earnedAt = doc.getLong("earnedAt") ?: 0L,
                spent = doc.getBoolean("spent") ?: false,
                prizeTier = doc.getString("prizeTier")?.let { PrizeTier.valueOf(it) },
                prizeName = doc.getString("prizeName"),
                spentAt = doc.getLong("spentAt")
            )
        } catch (e: IllegalArgumentException) {
            Log.e("FirestoreTicketDAO", "Skipping malformed ticket ${doc.id}", e)
            null
        } catch (e: java.time.format.DateTimeParseException) {
            Log.e("FirestoreTicketDAO", "Skipping ticket ${doc.id} with a bad date", e)
            null
        }
    }
}