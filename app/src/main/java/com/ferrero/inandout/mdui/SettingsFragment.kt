package com.ferrero.inandout.mdui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference

import com.ferrero.inandout.R
import com.ferrero.inandout.app.WelcomeActivity
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.flowv2.core.Flow.Companion.start
import com.sap.cloud.mobile.flowv2.model.FlowConstants
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry

import androidx.preference.PreferenceManager
import androidx.preference.ListPreference
import ch.qos.logback.classic.Level
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import android.util.Log
import com.ferrero.inandout.app.SAPWizardApplication
import com.sap.cloud.mobile.foundation.logging.Logging
import android.widget.Toast
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** This fragment represents the settings screen. */
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var logLevelPreference: ListPreference
    private lateinit var logUploadPreference: Preference
    private val logUploadListener = object : Logging.UploadListener {
        override fun onSuccess() {
            logUploadPreference.isEnabled = true
            Toast.makeText(requireActivity(), R.string.log_upload_ok, Toast.LENGTH_LONG).show()
            LOGGER.info("Log is uploaded to the server.")
        }

        override fun onError(throwable: Throwable) {
            logUploadPreference.isEnabled = true
            val message = throwable.localizedMessage ?: getString(R.string.log_upload_failed)
            DialogHelper(requireContext()).showOKOnlyDialog(
                    fragmentManager = requireActivity().supportFragmentManager,
                    message = message
            )
            LOGGER.error("Log upload failed with error message: $message")
        }

        override fun onProgress(i: Int) {
            // You could add a progress indicator and update it from here
        }
    }
    private var changePassCodePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        logLevelPreference = findPreference(getString(R.string.log_level))!!
        prepareLogSetting(logLevelPreference)
        // Upload log
        logUploadPreference = findPreference(getString(R.string.upload_log))!!
        logUploadPreference.setOnPreferenceClickListener {
            logUploadPreference.isEnabled = false
            Logging.upload()
            false
        }

        changePassCodePreference = findPreference(getString(R.string.manage_passcode))
        changePassCodePreference!!.setOnPreferenceClickListener {
            changePassCodePreference!!.isEnabled = false
            val flowContext =
                FlowContextRegistry.flowContext.copy(flowType = FlowType.CHANGEPASSCODE)
            start(this.requireActivity(), flowContext) { requestCode, _, _ ->
                if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                    changePassCodePreference!!.isEnabled = true
                }
            }
            false
        }
        // Reset App
        val resetAppPreference : Preference = findPreference(getString(R.string.reset_app))!!
        resetAppPreference.setOnPreferenceClickListener {
            start(
                this.requireActivity(),
                flowContext = FlowContextRegistry.flowContext.copy(flowType = FlowType.RESET),
                flowActivityResultCallback = { _, resultCode, _ ->
                    if (resultCode == Activity.RESULT_OK) {
                        Intent(this.requireContext(), WelcomeActivity::class.java).also {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            this.requireContext().startActivity(it)
                        }
                    }
                })
            false
        }
    }

    override fun onResume() {
        super.onResume()
        prepareLogSetting(logLevelPreference)
        Logging.addUploadListener(logUploadListener)
    }

    override fun onPause() {
        super.onPause()
        Logging.removeUploadListener(logUploadListener)
    }

    private fun logStrings() = mapOf<Level, String>(
        Level.ALL to getString(R.string.log_level_path),
        Level.DEBUG to getString(R.string.log_level_debug),
        Level.INFO to getString(R.string.log_level_info),
        Level.WARN to getString(R.string.log_level_warning),
        Level.ERROR to getString(R.string.log_level_error),
        Level.OFF to getString(R.string.log_level_none)
    )

    private fun prepareLogSetting(logLevelPreference: ListPreference) {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext)
        val str: String? = sharedPreferences.getString(
            SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE,
            LogPolicy().toString()
        )
        val settings = LogPolicy.createFromJsonString(str!!)
        Log.d(TAG, "log settings: $settings")
        logLevelPreference.entries = logStrings().values.toTypedArray()
        logLevelPreference.entryValues = arrayOf(
                Level.ALL.levelInt.toString(),
                Level.DEBUG.levelInt.toString(),
                Level.INFO.levelInt.toString(),
                Level.WARN.levelInt.toString(),
                Level.ERROR.levelInt.toString(),
                Level.OFF.levelInt.toString()
        )
        logLevelPreference.isPersistent = true
        logLevelPreference.summary = logStrings()[LogPolicy.getLogLevel(settings)]
        logLevelPreference.value = LogPolicy.getLogLevel(settings).levelInt.toString()
        logLevelPreference.setOnPreferenceChangeListener { preference, newValue ->
            val logLevel = Level.toLevel(Integer.valueOf(newValue as String))
            val newSettings = settings.copy(logLevel = LogPolicy.getLogLevelString(logLevel))
            sharedPreferences.edit()
                .putString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, newSettings.toString())
                .apply()
            LogPolicy.setRootLogLevel(newSettings)
            preference.summary = logStrings()[LogPolicy.getLogLevel(newSettings)]
            true
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(SettingsFragment::class.java)
        private val TAG = SettingsFragment::class.simpleName
    }
}
