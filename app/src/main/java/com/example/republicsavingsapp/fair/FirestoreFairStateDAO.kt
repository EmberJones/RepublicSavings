package com.example.republicsavingsapp.fair

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreFairStateDAO(private val firestore: FirebaseFirestore) : FairStateDAO {

    private suspend fun userRef(userId: Long): DocumentReference? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("userID", userId)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.reference
    }

    override suspend fun getState(userId: Long): FairState {
        val ref = userRef(userId) ?: return FairState(0, emptySet())
        val doc = ref.get().await()
        return FairState(
            tickets = (doc.getLong("tickets") ?: 0L).toInt(),
            awardedKeys = (doc.get("fairAwarded") as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
        )
    }

    override suspend fun addAwards(userId: Long, recordKeys: List<String>): List<String> {
        val ref = userRef(userId) ?: return emptyList()
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val existing = (snapshot.get("fairAwarded") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val fresh = recordKeys.distinct().filter { it !in existing }
            if (fresh.isNotEmpty()) {
                transaction.update(ref, mapOf<String, Any>(
                    "tickets" to ((snapshot.getLong("tickets") ?: 0L) + fresh.size),
                    "fairAwarded" to (existing + fresh)
                ))
            }
            fresh
        }.await()
    }

    override suspend fun useTicket(userId: Long): Boolean {
        val ref = userRef(userId) ?: return false
        return firestore.runTransaction { transaction ->
            val have = transaction.get(ref).getLong("tickets") ?: 0L
            if (have <= 0L) {
                false
            } else {
                transaction.update(ref, mapOf<String, Any>("tickets" to (have - 1)))
                true
            }
        }.await()
    }
}