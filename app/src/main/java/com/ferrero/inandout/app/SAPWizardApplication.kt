package com.ferrero.inandout.app

import android.app.Application
import android.content.SharedPreferences
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.model.AppConfig
import androidx.preference.PreferenceManager
import com.ferrero.inandout.repository.RepositoryFactory
import com.sap.cloud.mobile.foundation.mobileservices.MobileService
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer
import com.sap.cloud.mobile.foundation.logging.Logging
import com.sap.cloud.mobile.foundation.logging.LogService
import ch.qos.logback.classic.Level
import android.graphics.Color
import android.app.NotificationManager
import com.ferrero.inandout.fcm.FCMPushCallbackListener
import com.sap.cloud.mobile.foundation.remotenotification.*
import android.app.Notification
import com.ferrero.inandout.R

import com.ferrero.inandout.service.OfflineWorkerUtil
import com.sap.cloud.mobile.foundation.settings.SharedDeviceService

class SAPWizardApplication: Application() {

    internal var isApplicationUnlocked = false
    lateinit var preferenceManager: SharedPreferences

    /**
     * Application-wide RepositoryFactory
     */
    lateinit var repositoryFactory: RepositoryFactory
        private set

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        repositoryFactory = RepositoryFactory()
        initServices()
    }


    /**
     * Clears all user-specific data and configuration from the application, essentially resetting it to its initial
     * state.
     *
     * If client code wants to handle the reset logic of a service, here is an example:
     *
     *   SDKInitializer.resetServices { service ->
     *       return@resetServices if( service is PushService ) {
     *           PushService.unregisterPushSync(object: CallbackListener {
     *               override fun onSuccess() {
     *               }
     *
     *               override fun onError(p0: Throwable) {
     *               }
     *           })
     *           true
     *       } else {
     *           false
     *       }
     *   }
     */
    fun resetApplication() {
        preferenceManager.also {
            it.edit().clear().apply()
        }
        isApplicationUnlocked = false
        repositoryFactory.reset()
        SDKInitializer.resetServices()
        OfflineWorkerUtil.resetOffline(this)

    }

    private fun initServices() {
        val services = mutableListOf<MobileService>()
        Logging.setConfigurationBuilder(Logging.ConfigurationBuilder().initialLevel(Level.WARN).logToConsole(true).build())
        services.add(LogService())
        services.add(PushService().apply {
            setPushCallbackListener(FCMPushCallbackListener())
            setPushServiceConfig(configPushServiceConfig())
            isEnableAutoMessageHandling = true
            setBackgroundNotificationInterceptor { pushEvent ->
                pushEvent.displayNotification(pushEvent.pushRemoteMessage)
            }
            setForegroundNotificationInterceptor { pushEvent->
                    pushEvent.displayNotificationWhen(pushEvent.pushRemoteMessage, object: ForegroundPushNotificationReady{
                        override fun onConditionReady(): Boolean {
                          return  AppLifecycleCallbackHandler.getInstance().activity?.let {
                                it.javaClass.name != "com.company.neowizardbasicauthpushtest.app.WelcomeActivity"
                            }?:false
                        }
                    })
                }
            })
        services.add(SharedDeviceService(OFFLINE_APP_ENCRYPTION_CONSTANT))

        SDKInitializer.start(this, * services.toTypedArray())
    }

     private fun configPushServiceConfig(): PushServiceConfig{
            val notificationConfig = NotificationConfig(
                    smallIcon = R.mipmap.ic_statusbar,
                    largeIcon = R.mipmap.ic_launcher,
                    notificationTime = 0,
                    isAutoCancel = true,
                    badgeIconType = Notification.BADGE_ICON_SMALL,
                    cancelButtonDescription = applicationContext.resources.getString(R.string.cancel),
                    cancelButtonIcon = R.drawable.ic_close_black_24dp)
            val notificationChannelConfig = NotificationChannelConfig(
                    channelID = getString(R.string.push_channel_id),
                    description = "notification channel for push messages",
                    channelName = getString(R.string.push_channel_name),
                    importance = NotificationManager.IMPORTANCE_HIGH,
                    enableLights = true,
                    lightColor = Color.RED,
                    enableVibration = true,
                    vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            )
            return PushServiceConfig(notificationChannelConfig ,notificationConfig)
      }


    companion object {
        const val KEY_LOG_SETTING_PREFERENCE = "key.log.settings.preference"
        private const val OFFLINE_APP_ENCRYPTION_CONSTANT = "34dab53fc060450280faeed44a36571b"
    }
}