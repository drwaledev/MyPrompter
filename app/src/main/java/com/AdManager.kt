package com.wtcb.myprompter

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val activity: Activity) {

    companion object {
        private const val TAG = "AdManager"
        // REPLACE THIS WITH YOUR REAL AD UNIT ID FROM ADMOB
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-6214381980601535/1289562095"
        // ⬆️ CHANGE THIS TO YOUR AD UNIT ID FROM ADMOB
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    var onAdLoaded: (() -> Unit)? = null
    var onAdFailedToLoad: ((String) -> Unit)? = null
    var onAdShown: (() -> Unit)? = null
    var onAdDismissed: (() -> Unit)? = null
    var onAdFailedToShow: ((String) -> Unit)? = null
    var onUserEarnedReward: ((Int) -> Unit)? = null

    fun loadRewardedAd() {
        if (isLoadingAd) {
            Log.d(TAG, "Ad is already loading")
            return
        }

        if (rewardedAd != null) {
            Log.d(TAG, "Ad already loaded")
            onAdLoaded?.invoke()
            return
        }

        isLoadingAd = true
        Log.d(TAG, "Loading rewarded ad...")

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    isLoadingAd = false
                    onAdLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${error.message}")
                    rewardedAd = null
                    isLoadingAd = false
                    onAdFailedToLoad?.invoke(error.message)
                }
            }
        )
    }

    fun showRewardedAd() {
        val ad = rewardedAd

        if (ad == null) {
            Log.e(TAG, "Ad is not ready")
            onAdFailedToShow?.invoke("Ad not loaded yet")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen content")
                rewardedAd = null
                onAdShown?.invoke()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                onAdDismissed?.invoke()
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Ad failed to show: ${error.message}")
                rewardedAd = null
                onAdFailedToShow?.invoke(error.message)
            }
        }

        ad.show(activity) { rewardItem ->
            val amount = rewardItem.amount
            Log.d(TAG, "User earned reward: $amount")
            onUserEarnedReward?.invoke(amount)
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null

    fun isLoading(): Boolean = isLoadingAd
}