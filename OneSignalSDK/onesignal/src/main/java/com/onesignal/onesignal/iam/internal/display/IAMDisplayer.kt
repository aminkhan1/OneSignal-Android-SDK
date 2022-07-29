package com.onesignal.onesignal.iam.internal.display

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.util.Base64
import com.onesignal.onesignal.core.internal.application.IApplicationService
import com.onesignal.onesignal.core.internal.logging.Logging
import com.onesignal.onesignal.iam.internal.InAppMessage
import com.onesignal.onesignal.iam.internal.InAppMessageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.lang.Exception

// Manages WebView instances by pre-loading them, displaying them, and closing them when dismissed.
//   Includes a static map for pre-loading, showing, and dismissed so these events can't be duplicated.
// Flow for Displaying WebView
// 1. showHTMLString - Creates WebView and loads page.
// 2. Wait for JavaScriptInterface.postMessage to fire with "rendering_complete"
// 3. This calls showActivity which starts a new WebView
// 4. WebViewActivity will call WebViewManager.instanceFromIam(...) to get this instance and
//       add it's prepared WebView add add it to the Activity.
@TargetApi(Build.VERSION_CODES.KITKAT)
internal class IAMDisplayer(
    private val _applicationService: IApplicationService
) {
    private var lastInstance: WebViewManager? = null

    /**
     * Creates a new WebView
     * Dismiss WebView if already showing one and the new one is a Preview
     *
     * @param message the message to show
     * @param content the html to display on the WebView
     */
    suspend fun showMessageContent(message: InAppMessage, content: InAppMessageContent) {
        val currentActivity = _applicationService.current
        Logging.debug("in app message showMessageContent on currentActivity: $currentActivity")

        /* IMPORTANT
         * This is the starting route for grabbing the current Activity and passing it to InAppMessageView */
        if (currentActivity != null) {
            // Only a preview will be dismissed, this prevents normal messages from being
            // removed when a preview is sent into the app
            if (lastInstance != null && message.isPreview) {
                // Created a callback for dismissing a message and preparing the next one
                lastInstance!!.dismissAndAwaitNextMessage()
                lastInstance = null
                initInAppMessage(currentActivity, message, content)
            } else {
                initInAppMessage(currentActivity, message, content)
            }
            return
        }

        delay(IN_APP_MESSAGE_INIT_DELAY.toLong())
        showMessageContent(message, content)
    }

    suspend fun dismissCurrentInAppMessage() {
        Logging.debug("WebViewManager IAM dismissAndAwaitNextMessage lastInstance: $lastInstance")

        if (lastInstance != null) {
            lastInstance!!.dismissAndAwaitNextMessage()
        }
    }

    private suspend fun initInAppMessage(currentActivity: Activity, message: InAppMessage, content: InAppMessageContent) {
        try {
            val base64Str = Base64.encodeToString(
                content.contentHtml!!.toByteArray(charset("UTF-8")),
                Base64.NO_WRAP
            )
            val webViewManager = WebViewManager(message, currentActivity, content, _applicationService)
            lastInstance = webViewManager

            if (content.isFullBleed) {
                webViewManager.setContentSafeAreaInsets(content, currentActivity)
            }

            // Web view must be created on the main thread.
            withContext(Dispatchers.Main) {
                // Handles exception "MissingWebViewPackageException: Failed to load WebView provider: No WebView installed"
                try {
                    webViewManager.setupWebView(currentActivity, base64Str, content.isFullBleed)
                } catch (e: Exception) {
                    // Need to check error message to only catch MissingWebViewPackageException as it isn't public
                    if (e.message != null && e.message!!.contains("No WebView installed")) {
                        Logging.error("Error setting up WebView: ", e)
                    } else {
                        throw e
                    }
                }
            }
        } catch (e: UnsupportedEncodingException) {
            Logging.error("Catch on initInAppMessage: ", e)
        }
    }

    companion object {
        private const val IN_APP_MESSAGE_INIT_DELAY = 200
    }
}