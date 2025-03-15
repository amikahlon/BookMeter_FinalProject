package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.databinding.FragmentDashboardBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = DashboardFragmentArgs.fromBundle(requireArguments())
        val userName = args.userName
        val showSnackbar = args.showSnackbar

        binding.tvWelcome.text = "Welcome, $userName!"

        if (showSnackbar) {
            SnackbarHelper.showSuccess(binding.root, "Registration successful!")
        }

        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false

            authViewModel.logout { success, message ->
                binding.btnLogout.isEnabled = true

                if (success) {
                    // יצירת אובייקט ניווט עם הפרמטר הנכון
                    val action = DashboardFragmentDirections.actionDashboardFragmentToLoginFragment()
                        .setShowSnackbar(true)
                    findNavController().navigate(action)
                } else {
                    SnackbarHelper.showError(binding.root, "Logout failed: $message")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
