package com.example.republicsavingsapp.ui.rewards

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.republicsavingsapp.AppLog
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentRewardsBinding
import kotlinx.coroutines.launch
import com.example.republicsavingsapp.fair.FairRules
import com.example.republicsavingsapp.fair.PrizeTier
class RewardsFragment : Fragment() {
    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener { findNavController().popBackStack() }
        binding.spinButton.setOnClickListener { spinWheel() }

        refreshTicketCount()
    }

    /** Awards any newly earned tickets, then shows how many are available. */
    private fun refreshTicketCount() {
        val repo = (requireActivity().application as RepublicSavingsApp).ticketRepository
        viewLifecycleOwner.lifecycleScope.launch {
            var tiers: List<PrizeTier> = PrizeTier.values().toList()
            val available = try {
                val count = repo.refresh()
                // The wheel shows what the NEXT ticket can win, based on how much it saved
                repo.nextTicketSaving()?.let { tiers = FairRules.eligibleTiers(it) }
                count
            } catch (e: Exception) {
                AppLog.e("Fair", "Could not refresh tickets", e)
                null
            }
            val b = _binding ?: return@launch // screen was left while loading

            if (available == null) {
                b.spinButton.isEnabled = false
                b.resultText.text = "Couldn't load your tickets. Check your connection."
                return@launch
            }
            b.rewardWheel.setTiers(tiers)
            b.ticketCount.text = available.toString()
            b.spinButton.isEnabled = available > 0
            b.noTicketsHint.visibility = if (available > 0) View.GONE else View.VISIBLE
        }
    }

    private fun spinWheel() {
        val repo = (requireActivity().application as RepublicSavingsApp).ticketRepository
        binding.spinButton.isEnabled = false
        binding.resultText.text = "Spinning..."

        // Fragment scope, not view scope: once a ticket is used the prize must be saved
        lifecycleScope.launch {
            val prize = try {
                repo.spinNextTicket()
            } catch (e: Exception) {
                AppLog.e("Fair", "Spin failed", e)
                _binding?.resultText?.text = "Couldn't spin. Check your connection and try again."
                _binding?.spinButton?.isEnabled = true
                return@launch
            }
            val b = _binding ?: return@launch // left the screen; the prize is already saved

            if (prize != null) {
                b.rewardWheel.spinToPrize(prize) {
                    // The wheel can finish after the screen is gone
                    _binding?.let {
                        it.resultText.text = "You won: ${prize.name}!"
                        refreshTicketCount()
                    }
                }
            } else {
                b.resultText.text = "No tickets available"
                refreshTicketCount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object { const val TAG = "RewardsFragment" }
}