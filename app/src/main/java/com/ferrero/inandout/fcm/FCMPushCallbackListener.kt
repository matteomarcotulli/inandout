package com.ferrero.inandout.fcm

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.ferrero.inandout.R
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.remotenotification.*

class FCMPushCallbackListener : PushCallbackListener {

    override fun onRecieve(context: Context, message: PushRemoteMessage) {

        AppLifecycleCallbackHandler.getInstance().activity?.let { foregroundActivity ->
            showMessageDialog(foregroundActivity as FragmentActivity, message)
        }
    }

    private fun showMessageDialog(activity: FragmentActivity, message: PushRemoteMessage) {
        var textMessage = message.alert ?: activity.getString(R.string.push_text)
        var notificationTitle = message.title?: activity.getString(R.string.push_message)

        DialogHelper(activity).showDialogWithCancelAction(
                fragmentManager = activity.supportFragmentManager,
                message = textMessage,
                negativeAction = {},
                title = notificationTitle,
                positiveAction = {
                    message.notificationID?.let {
                        PushService.setPushMessageStatus(it, PushService.NotificationStatus.CONSUMED)
                    }
                }
        )
    }

}
