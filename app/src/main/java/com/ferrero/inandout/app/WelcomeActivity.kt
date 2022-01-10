package com.ferrero.inandout.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sap.cloud.mobile.fiori.onboarding.LaunchScreen
import com.sap.cloud.mobile.fiori.onboarding.ext.LaunchScreenSettings
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.core.FlowContextBuilder
import com.sap.cloud.mobile.foundation.configurationprovider.*
import com.sap.cloud.mobile.foundation.model.AppConfig
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;
import com.sap.cloud.mobile.foundation.remotenotification.ForegroundPushNotificationReady
import com.sap.cloud.mobile.foundation.remotenotification.PushRemoteMessage
import com.sap.cloud.mobile.foundation.remotenotification.PushService
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer.getService
import com.ferrero.inandout.R
import java.util.*

class WelcomeActivity : AppCompatActivity() {
    private val fManager = this.supportFragmentManager
    private var timer: Timer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getStringExtra("alert") != null) {
            var message = PushRemoteMessage()
            if (message != null) {
                val service = getService(PushService::class.java.kotlin)
                service!!.storeNotificationMessage(true, message, object : ForegroundPushNotificationReady {
                    override fun onConditionReady(): Boolean {
                        return !AppLifecycleCallbackHandler.getInstance().activity!!.javaClass.name.contains("WelcomeActivity")
                    }
                })
                if ((application as SAPWizardApplication).isApplicationUnlocked) {
                    val intent = Intent(this, MainBusinessActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                } else if (AppLifecycleCallbackHandler.getInstance().activity != null) {
                    if (AppLifecycleCallbackHandler.getInstance().activity !is WelcomeActivity) {
                        finish()
                    }
                }
            }
        }

        startConfigurationLoader();
        /*
        val welcomeLaunchScreen = LaunchScreen(this)
        welcomeLaunchScreen.initialize(
            LaunchScreenSettings.Builder()
                .setDemoButtonVisible(false)
                .setHeaderLineLabel(getString(R.string.welcome_screen_headline_label))
                .setPrimaryButtonText(getString(R.string.welcome_screen_primary_button_label))
                .setFooterVisible(true)
                .setUrlTermsOfService("http://www.sap.com")
                .setUrlPrivacy("http://www.sap.com")
                .addInfoViewSettings(
                    LaunchScreenSettings.LaunchScreenInfoViewSettings(
                        R.drawable.ic_android_white_circle_24dp,
                        getString(R.string.application_name),
                        getString(R.string.welcome_screen_detail_label)
                    )
                )
                .build()
        )
        welcomeLaunchScreen.setPrimaryButtonOnClickListener {
            timer?.cancel()
            timer = Timer()
            timer!!.schedule(
                object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            startConfigurationLoader();
                        }
                    }
                }, 400
            )
        }
        setContentView(welcomeLaunchScreen)
         */
    }

    private fun startConfigurationLoader() {
        val callback: ConfigurationLoaderCallback = object : ConfigurationLoaderCallback() {
            override fun onCompletion(providerIdentifier: ProviderIdentifier?, success: Boolean) {
                if (success) {
                    startFlow(this@WelcomeActivity)
                } else {
                    DialogHelper(application, R.style.OnboardingDefaultTheme_Dialog_Alert)
                            .showOKOnlyDialog(
                                    fManager,
                                    resources.getString(R.string.config_loader_complete_error_description),
                                    null, null, null
                            )
                }
            }

            override fun onError(configurationLoader: ConfigurationLoader, providerIdentifier: ProviderIdentifier, userInputs: UserInputs, configurationProviderError: ConfigurationProviderError) {
                DialogHelper(application, R.style.OnboardingDefaultTheme_Dialog_Alert)
                        .showOKOnlyDialog(
                                fManager, String.format(resources.getString(
                                R.string.config_loader_on_error_description),
                                providerIdentifier.toString(), configurationProviderError.errorMessage
                        ),
                                null, null, null
                        )
                configurationLoader.processRequestedInputs(UserInputs())
            }

            override fun onInputRequired(configurationLoader: ConfigurationLoader, userInputs: UserInputs) {
                configurationLoader.processRequestedInputs(UserInputs())
            }
        }
        val providers = arrayOf<ConfigurationProvider>(FileConfigurationProvider(this, "sap_mobile_services"))
        this.runOnUiThread {
            val loader = ConfigurationLoader(this, callback, providers)
            loader.loadConfiguration()
        }
    }

    private fun prepareAppConfig(): AppConfig? {
        return try {
            val configData = DefaultPersistenceMethod.getPersistedConfiguration(this)
            AppConfig.createAppConfigFromJsonString(configData.toString())
        } catch (ex: ConfigurationPersistenceException) {
            DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            resources.getString(R.string.config_data_build_json_description),
                            null, null, null
                    )
            null
        } catch (ex: Exception) {
            DialogHelper(this, R.style.OnboardingDefaultTheme_Dialog_Alert)
                    .showOKOnlyDialog(
                            fManager,
                            ex.localizedMessage ?: resources.getString(R.string.error_unknown_app_config),
                            null, null, null
                    )
            null
        }
    }

    internal fun startFlow(activity: Activity) {
        val appConfig = prepareAppConfig() ?: return
        val flowContext =
                FlowContextBuilder()
                    .setApplication(appConfig)
                    .setMultipleUserMode(false)
                    .setFlowStateListener(WizardFlowStateListener(activity.application as SAPWizardApplication))
                    .build()
        Flow.start(activity, flowContext) { _, resultCode, _ ->
            if (resultCode == Activity.RESULT_OK) {
                (application as SAPWizardApplication).isApplicationUnlocked = true
                Intent(application, MainBusinessActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    application.startActivity(it)
                }
            }
        }
    }
}
