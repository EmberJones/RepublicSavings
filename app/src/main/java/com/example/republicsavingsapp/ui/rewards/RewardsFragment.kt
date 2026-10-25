package com.example.republicsavingsapp.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentRewardsBinding
import kotlinx.coroutines.launch

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

        binding.closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.spinButton.setOnClickListener {
            spinWheel()
        }

        refreshTicketCount()
    }

    private fun refreshTicketCount() {
        val repo = (requireActivity().application as RepublicSavingsApp).ticketRepository
        lifecycleScope.launch {
            val available = repo.refresh()
            binding.ticketCount.text = available.toString()
            if (available > 0) {
                binding.spinButton.isEnabled = true
                binding.noTicketsHint.visibility = View.GONE
            } else {
                binding.spinButton.isEnabled = false
                binding.noTicketsHint.visibility = View.VISIBLE
            }
        }
    }

    private fun spinWheel() {
        val repo = (requireActivity().application as RepublicSavingsApp).ticketRepository
        binding.spinButton.isEnabled = false
        binding.resultText.text = "Spinning..."

        lifecycleScope.launch {
            val prize = repo.spinNextTicket()
            if (prize != null) {
                binding.rewardWheel.spinToPrize(prize) {
                    binding.resultText.text = "You won: ${prize.name}!"
                    refreshTicketCount()
                }
            } else {
                binding.resultText.text = "No tickets available"
                refreshTicketCount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
