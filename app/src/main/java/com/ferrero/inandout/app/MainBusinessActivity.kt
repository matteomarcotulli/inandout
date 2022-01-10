package com.ferrero.inandout.app

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.ferrero.inandout.R

import androidx.preference.PreferenceManager
import androidx.fragment.app.DialogFragment
import android.view.MenuItem
import android.view.View
import android.app.NotificationManager
import android.util.Base64
import androidx.work.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.ferrero.inandout.archcomp.Utils
import com.ferrero.inandout.archcomp.gone
import com.ferrero.inandout.archcomp.invisible
import com.ferrero.inandout.archcomp.visible
import com.ferrero.inandout.databinding.ActivityMainBusinessBinding
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineNetworkErrorScreenSettings
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineTransactionIssueScreenSettings
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate
import com.sap.cloud.mobile.foundation.mobileservices.ServiceListener
import com.sap.cloud.mobile.foundation.mobileservices.ServiceResult
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import com.ferrero.inandout.service.*
import com.ferrero.inandout.viewmodel.EntityViewModelFactory
import com.ferrero.inandout.viewmodel.MainBusinnesViewModel
import com.google.android.gms.common.util.CollectionUtils.setOf
import java.security.SecureRandom
import java.util.*
import kotlinx.android.synthetic.main.activity_main_business.*

import org.slf4j.LoggerFactory


class MainBusinessActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private var destinationId: Int? = null
    private var isOfflineStoreInitialized = false
    private var dialogFragment: DialogFragment? = null
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBusinessBinding
    lateinit var viewModel: MainBusinnesViewModel

    private val appBarConfiguration = AppBarConfiguration(
            setOf(
                    R.id.fragment_home
            ))

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.OPEN

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            offlineInitSyncScreen.updateProgressBar(currentStep, totalSteps)
        }

        override fun getStartPoint(): Int {
            return OfflineOpenWorker.startPointForOpen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(this)

        viewModel = ViewModelProvider(this, EntityViewModelFactory(this, this.application)).get(MainBusinnesViewModel::class.java)
        binding.lifecycleOwner = this

        setSupportActionBar(binding.mainToolbar.toolbarMain)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        dialogFragment = DialogHelper.ErrorDialogFragment(
                message = getString(R.string.offline_navigation_dialog_message),
                title =  getString(R.string.offline_navigation_dialog_title),
                positiveButtonCaption = getString(R.string.offline_navigation_dialog_positive_option),
                negativeButtonCaption = getString(R.string.offline_navigation_dialog_negative_option),
                positiveAction =  {
                    if (!isOfflineStoreInitialized) {
                        application.getSystemService(NotificationManager::class.java).cancel(OfflineBaseWorker.OFFLINE_NOTIFICATION_CHANNEL_INT_ID)
                        WorkManager.getInstance(application).cancelUniqueWork(OfflineWorkerUtil.OFFLINE_OPEN_WORKER_UNIQUE_NAME)
                    }
                    backToWelcome()
                }
        ).also {
            it.isCancelable = false
        }
    }

    private fun standardsetToolbar() {
        binding.mainToolbar.toolbarMain.setNavigationIcon(R.drawable.ic_keyboard_arrow_left)
        binding.mainToolbar.rightButton.gone()
        binding.secondaryBar.visible()
        binding.mainToolbar.rightText.gone()
        binding.mainToolbar.leftText.gone()
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        binding.mainToolbar.titlePage = destination.label.toString()

        destinationId = destination.id
        when (destination.id) {
            R.id.fragment_home -> {
                binding.mainToolbar.toolbarMain.setNavigationIcon(R.drawable.ic_craftman_user_scura)
                if (Utils.checkOnlineDevice(this)) binding.mainToolbar.rightButton.invisible() else binding.mainToolbar.rightButton.visible()
                binding.secondaryBar.gone()
                binding.mainToolbar.rightText.gone()
                binding.mainToolbar.leftText.gone()
            }
            else -> standardsetToolbar()
        }
    }

    private fun startEntitySetListActivity() {
        if (isOfflineStoreInitialized) {
            OfflineWorkerUtil.resetOpenRequest()
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
        } else {
            LOGGER.info("Waiting for the sync finish.")
            WorkManager.getInstance(applicationContext)
                    .getWorkInfoByIdLiveData(OfflineWorkerUtil.openRequest!!.id)
                    .observe(this, Observer { workInfo ->
                        if (workInfo != null && workInfo.state.isFinished) {
                            OfflineWorkerUtil.removeProgressListener(progressListener)
                            OfflineWorkerUtil.resetOpenRequest()
                            when (workInfo.state) {
                                WorkInfo.State.FAILED -> {
                                    when (workInfo.outputData.getInt(OfflineWorkerUtil.OUTPUT_ERROR_KEY, 0)) {
                                        -1 -> {
                                            offlineNetworkErrorAction()
                                        }
                                        -10425 -> {
                                            offlineTransactionIssueAction()
                                        }
                                        else -> {
                                            DialogHelper(application).showOKOnlyDialog(
                                                    supportFragmentManager,
                                                    message = workInfo.outputData.getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) ?: "Offline sync failed.",
                                                    title = getString(R.string.offline_initial_open_error),
                                                    positiveAction = {
                                                        startActivity(Intent(this, WelcomeActivity::class.java).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                        })
                                                    })
                                        }
                                    }
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                }
                            }
                        }
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        offlineNetworkErrorScreen.visibility = View.INVISIBLE
        offlineTransactionIssueScreen.visibility = View.INVISIBLE
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isOfflineStoreInitialized = sharedPreferences.getBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (isOfflineStoreInitialized) {
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.VISIBLE
        } else {
            OfflineWorkerUtil.addProgressListener(progressListener)
            offlineInitSyncScreen.visibility = View.VISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
        }
        val isMultipleUserMode = UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync()!!
        val appConfig = FlowContextRegistry.flowContext.appConfig!!
        //If is single user mode, create and save a key into user secure store for accessing offline DB
        if (!isMultipleUserMode
                && UserSecureStoreDelegate.getInstance().getData<String>(OfflineWorkerUtil.OFFLINE_DATASTORE_ENCRYPTION_KEY) == null) {
            val bytes = ByteArray(32)
            val random = SecureRandom()
            random.nextBytes(bytes)
            UserSecureStoreDelegate.getInstance().saveData(OfflineWorkerUtil.OFFLINE_DATASTORE_ENCRYPTION_KEY, Base64.encodeToString(bytes, Base64.NO_WRAP),
                    object: ServiceListener<Any> {
                        override fun onServiceDone(result: ServiceResult<Any>) {
                            if (result is ServiceResult.SUCCESS) {
                                OfflineWorkerUtil.initializeOffline(application, appConfig, isMultipleUserMode)
                                OfflineWorkerUtil.open(application)
                                startEntitySetListActivity()
                            }
                        }
                    })
            Arrays.fill(bytes, 0.toByte())
        } else {
            OfflineWorkerUtil.initializeOffline(application, appConfig, isMultipleUserMode)
            OfflineWorkerUtil.open(application)
            startEntitySetListActivity()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            if (offlineNetworkErrorScreen.visibility == View.VISIBLE || offlineTransactionIssueScreen.visibility == View.VISIBLE) {
                backToWelcome()
            } else {
                dialogFragment?.show(supportFragmentManager, "Back")
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (offlineNetworkErrorScreen.visibility == View.VISIBLE || offlineTransactionIssueScreen.visibility == View.VISIBLE) {
            backToWelcome()
        } else {
            dialogFragment?.show(supportFragmentManager, "Back")
        }
    }

    private fun backToWelcome() {
        OfflineWorkerUtil.removeProgressListener(progressListener)
        OfflineWorkerUtil.resetOpenRequest()
        startActivity(Intent(this, WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }

    private fun offlineNetworkErrorAction() {
        this@MainBusinessActivity.runOnUiThread {
            offlineNetworkErrorScreen.visibility = View.VISIBLE
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
            val offlineNetworkErrorScreenSettings = OfflineNetworkErrorScreenSettings.Builder().build()
            with(offlineNetworkErrorScreen) {
                initialize(offlineNetworkErrorScreenSettings)
                setButtonClickListener(View.OnClickListener {
                    OfflineWorkerUtil.addProgressListener(progressListener)
                    OfflineWorkerUtil.open(application)
                    startEntitySetListActivity()
                    //(R.string.initializing_offline_store)
                    offlineNetworkErrorScreen.visibility = View.INVISIBLE
                    offlineInitSyncScreen.visibility = View.VISIBLE
                })
            }
        }
    }

    private fun offlineTransactionIssueAction() {
        this@MainBusinessActivity.runOnUiThread {
            offlineTransactionIssueScreen.visibility = View.VISIBLE
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
            val offlineTransactionIssueScreenSettings = OfflineTransactionIssueScreenSettings.Builder().build()
            with(offlineTransactionIssueScreen) {
                initialize(offlineTransactionIssueScreenSettings)
                CoroutineScope(IO).launch {
                    val user = UserSecureStoreDelegate.getInstance().getUserInfoById(OfflineWorkerUtil.offlineODataProvider!!.previousUser)
                    withContext(Main) {
                        user?.let {
                            setPrevUserName(it.name)
                            setPrevUserMail(it.email)
                        } ?: run {
                            setPrevUserName(OfflineWorkerUtil.offlineODataProvider!!.previousUser)
                        }
                    }
                }
            }
            offlineTransactionIssueScreen.setButtonClickListener(View.OnClickListener {
                startActivity(Intent(this, WelcomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            })
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainBusinessActivity::class.java)
    }
}
