package com.example.wanderwise.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.wanderwise.R
import com.example.wanderwise.ui.MainActivity
import com.example.wanderwise.ui.register.RegisterActivity
import com.example.wanderwise.databinding.ActivityLoginScreenBinding
import com.example.wanderwise.ui.ViewModelFactory
import com.example.wanderwise.data.Result
import com.example.wanderwise.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginScreenBinding
    private lateinit var loginViewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.Main){
            binding = ActivityLoginScreenBinding.inflate(layoutInflater)
            setContentView(binding.root)

            loginViewModel = ViewModelFactory.getInstance(this@LoginScreenActivity).create(LoginViewModel::class.java)

            loginViewModel.getSessionUser().observe(this@LoginScreenActivity) { user ->
                if (user.token.isNotEmpty()) {
                    val intent = Intent(this@LoginScreenActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }

            binding.loginButton.setOnClickListener {
                val email = binding.edLoginEmail.text.toString()
                val password = binding.edLoginPassword.text.toString()

                if (email.isEmpty()) {
                    binding.edLoginEmail.error = getString(R.string.cannot_empty)
                } else if (password.isEmpty()) {
                    binding.edLoginPassword.error = getString(R.string.cannot_empty)
                } else {
                    loginViewModel.loginUser(email, password).observe(this@LoginScreenActivity) { result ->
                        if (result != null) {
                            when (result) {
                                is Result.Loading -> {
                                    isLoading(true)
                                }

                                is Result.Success -> {
                                    showToast(result.data)
                                    isLoading(false)

                                    val intentMain = Intent(this@LoginScreenActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                    startActivity(intentMain)
                                }

                                is Result.Error -> {
                                    showToast(result.error)
                                    isLoading(false)
                                }
                            }
                        }
                    }
                }
            }

            binding.registerButtonText.setOnClickListener {
                val intentRegister = Intent(this@LoginScreenActivity, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intentRegister)
            }
        }

        supportActionBar?.hide()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

}