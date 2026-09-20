package com.example.republicsavingsapp.ui.expense

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import coil.load
import com.example.republicsavingsapp.databinding.DialogReceiptBinding
import java.io.File

class ReceiptDialogFragment : DialogFragment() {

    private var _binding: DialogReceiptBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val path = arguments?.getString(ARG_PATH)
        if (path.isNullOrBlank()) {
            dismiss()
            return
        }

        binding.receiptImage.load(File(path)) {
            crossfade(true)
        }

        binding.closeButton.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PATH = "arg_path"

        fun newInstance(path: String) = ReceiptDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_PATH, path) }
        }
    }
}