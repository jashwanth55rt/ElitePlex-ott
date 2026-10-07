package com.example.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ElitePlexApplication
import com.example.databinding.FragmentSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClearCache.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                requireContext().cacheDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    binding.tvCacheSize.text = "0 MB"
                    Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnClearHistory.setOnClickListener {
            val app = requireActivity().application as ElitePlexApplication
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                app.database.clearAllTables()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Watch history cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
