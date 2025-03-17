package com.example.bookmeter.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bookmeter.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class LoadingStateManager(private val fragment: Fragment) {

    private lateinit var loadingLayout: ViewGroup
    private lateinit var loadingText: TextView
    private lateinit var logoImageView: View
    private lateinit var logoUnderline: View
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var contentView: View
    private var textAnimator: ValueAnimator? = null
    private var logoAnimatorSet: AnimatorSet? = null
    

    fun init(rootView: View, contentViewId: Int) {
        loadingLayout = rootView.findViewById(R.id.loadingLayout)
        loadingText = rootView.findViewById(R.id.loadingText)
        logoImageView = rootView.findViewById(R.id.logoImageView)
        logoUnderline = rootView.findViewById(R.id.logoUnderline)
        progressBar = rootView.findViewById(R.id.loadingProgressBar)
        contentView = rootView.findViewById(contentViewId)
    }
    

    fun showLoading(message: String = "Loading...") {
        if (fragment.isAdded) {
            loadingText.text = message
            contentView.visibility = View.INVISIBLE
            loadingLayout.visibility = View.VISIBLE
            
            // Add subtle animations
            animateLogoElements()
        }
    }
    

    fun hideLoading() {
        if (fragment.isAdded) {
            loadingLayout.visibility = View.GONE
            contentView.visibility = View.VISIBLE
            textAnimator?.cancel()
            logoAnimatorSet?.cancel()
        }
    }
    

    fun updateLoadingMessage(message: String) {
        if (fragment.isAdded) {
            loadingText.text = message
        }
    }


    private fun animateLogoElements() {
        textAnimator?.cancel()
        logoAnimatorSet?.cancel()
        
        // Reset views
        logoUnderline.alpha = 0f
        logoUnderline.scaleX = 0.3f
        
        // Create logo pulse animation
        val logoPulse = ObjectAnimator.ofFloat(logoImageView, "alpha", 0.9f, 1f, 0.9f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Animate underline appearing
        val underlineAlpha = ObjectAnimator.ofFloat(logoUnderline, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 300
        }
        
        val underlineWidth = ObjectAnimator.ofFloat(logoUnderline, "scaleX", 0.3f, 1f).apply {
            duration = 800
            startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Animate the loading text
        textAnimator = ObjectAnimator.ofFloat(loadingText, "alpha", 0.6f, 1f, 0.6f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        
        // Create an animation set
        logoAnimatorSet = AnimatorSet().apply {
            playTogether(logoPulse, underlineAlpha, underlineWidth)
            start()
        }
    }
}
