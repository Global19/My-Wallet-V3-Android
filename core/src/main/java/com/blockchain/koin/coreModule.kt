@file:Suppress("USELESS_CAST")

package com.blockchain.koin

import android.preference.PreferenceManager
import com.blockchain.datamanagers.AccountLookup
import com.blockchain.datamanagers.AddressResolver
import com.blockchain.datamanagers.DataManagerPayloadDecrypt
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.datamanagers.SelfFeeCalculatingTransactionExecutor
import com.blockchain.datamanagers.TransactionExecutor
import com.blockchain.datamanagers.TransactionExecutorViaDataManagers
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.fees.FeeType
import com.blockchain.logging.LastTxUpdateDateOnSettingsService
import com.blockchain.logging.LastTxUpdater
import com.blockchain.logging.NullLogger
import com.blockchain.logging.TimberLogger
import com.blockchain.metadata.MetadataRepository
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.preferences.NotificationPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.preferences.ThePitLinkingPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmHorizonUrlFetcher
import com.blockchain.sunriver.XlmTransactionTimeoutFetcher
import com.blockchain.wallet.SeedAccess
import com.blockchain.wallet.SeedAccessWithoutPrompt
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.balance.ExchangeRates
import info.blockchain.wallet.metadata.MetadataDerivation
import info.blockchain.wallet.util.PrivateKeyFactory
import org.bitcoinj.params.BitcoinMainNetParams
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.access.AccessStateImpl
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.auth.AuthService
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore
import piuk.blockchain.androidcore.data.erc20.datastores.Erc20DataStore
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.metadata.MoshiMetadataRepositoryAdapter
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManagerSeedAccessAdapter
import piuk.blockchain.androidcore.data.payload.PayloadService
import piuk.blockchain.androidcore.data.payload.PromptingSeedAccessAdapter
import piuk.blockchain.androidcore.data.payments.PaymentService
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.settings.SettingsEmailAndSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.SettingsPhoneVerificationQuery
import piuk.blockchain.androidcore.data.settings.SettingsService
import piuk.blockchain.androidcore.data.settings.applyFlag
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import piuk.blockchain.androidcore.data.settings.datastore.SettingsMemoryStore
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.CloudBackupAgent
import piuk.blockchain.androidcore.utils.DeviceIdGenerator
import piuk.blockchain.androidcore.utils.DeviceIdGeneratorImpl
import piuk.blockchain.androidcore.utils.EncryptedPrefs
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.UUIDGenerator
import java.util.UUID

val coreModule = module {

    single { RxBus() }

    factory { AuthService(get(), get()) }

    factory { PrivateKeyFactory() }

    factory { RootUtil() }

    scope(payloadScopeQualifier) {

        factory { PayloadService(get()) }

        factory { PayloadDataManager(get(), get(), get(), get(), get()) }

        factory { DataManagerPayloadDecrypt(get(), get()) as PayloadDecrypt }

        factory { PromptingSeedAccessAdapter(PayloadDataManagerSeedAccessAdapter(get()), get()) }
            .bind(SeedAccessWithoutPrompt::class)
            .bind(SeedAccess::class)

        scoped {
            MetadataManager(
                payloadDataManager = get(),
                metadataInteractor = get(),
                metadataDerivation = MetadataDerivation(BitcoinMainNetParams.get()),
                crashLogger = get()
            )
        }

        scoped { MoshiMetadataRepositoryAdapter(get(), get()) as MetadataRepository }

        factory { AddressResolver(get(), get(), get()) }

        factory { AccountLookup(get(), get(), get()) }

        factory {
            TransactionExecutorViaDataManagers(
                payloadDataManager = get(),
                ethDataManager = get(),
                paxAccount = get(paxAccount),
                usdtAccount = get(usdtAccount),
                sendDataManager = get(),
                addressResolver = get(),
                accountLookup = get(),
                defaultAccountDataManager = get(),
                ethereumAccountWrapper = get(),
                xlmSender = get(),
                coinSelectionRemoteConfig = get(),
                analytics = get()
            ) as TransactionExecutor
        }

        factory(regularFee) {
            SelfFeeCalculatingTransactionExecutor(
                get(),
                get(),
                get(),
                FeeType.Regular
            ) as TransactionExecutorWithoutFees
        }

        factory(priorityFee) {
            SelfFeeCalculatingTransactionExecutor(
                get(),
                get(),
                get(),
                FeeType.Priority
            ) as TransactionExecutorWithoutFees
        }

        factory(regularFee) {
            get<TransactionExecutorWithoutFees>(regularFee) as MaximumSpendableCalculator
        }

        factory(priorityFee) {
            get<TransactionExecutorWithoutFees>(priorityFee) as MaximumSpendableCalculator
        }

        scoped { EthDataStore() }

        scoped { Erc20DataStore() }

        scoped { BchDataStore() }

        scoped { WalletOptionsState() }

        scoped { SettingsDataManager(get(), get(), get(), get()) }

        scoped { SettingsService(get()) }

        scoped {
            SettingsDataStore(SettingsMemoryStore(), get<SettingsService>().getSettingsObservable())
        }

        factory { WalletOptionsDataManager(get(), get(), get(), get(explorerUrl)) }
            .bind(XlmTransactionTimeoutFetcher::class).bind(XlmHorizonUrlFetcher::class)

        factory { ExchangeRateDataManager(get(), get()) }.bind(ExchangeRates::class)

        scoped { ExchangeRateDataStore(get(), get()) }

        scoped { FeeDataManager(get(), get(), get()) }

        factory {
            AuthDataManager(
                prefs = get(),
                authService = get(),
                accessState = get(),
                aesUtilWrapper = get(),
                prngHelper = get(),
                crashLogger = get()
            )
        }

        factory { LastTxUpdateDateOnSettingsService(get()) }.bind(LastTxUpdater::class)

        factory { SendDataManager(get(), get(), get()) }

        factory { SettingsPhoneVerificationQuery(get()).applyFlag(get(smsVerifFeatureFlag)) }

        factory { SettingsPhoneNumberUpdater(get()) }.bind(PhoneNumberUpdater::class)

        factory { SettingsEmailAndSyncUpdater(get(), get()) }.bind(EmailSyncUpdater::class)
    }

    single {
        BlockExplorer(
            get(explorerRetrofit),
            get(apiRetrofit),
            getProperty("api-code")
        )
    }

    factory { ExchangeRateService(get()) }

    factory {
        DeviceIdGeneratorImpl(
            ctx = get(),
            analytics = get()
        )
    }.bind(DeviceIdGenerator::class)

    factory {
        object : UUIDGenerator {
            override fun generateUUID(): String = UUID.randomUUID().toString()
        }
    }.bind(UUIDGenerator::class)

    single {
        PrefsUtil(
            store = get(),
            backupStore = CloudBackupAgent.backupPrefs(ctx = get()),
            idGenerator = get(),
            uuidGenerator = get()
        )
    }.bind(PersistentPrefs::class)
        .bind(CurrencyPrefs::class)
        .bind(NotificationPrefs::class)
        .bind(DashboardPrefs::class)
        .bind(SecurityPrefs::class)
        .bind(ThePitLinkingPrefs::class)
        .bind(SimpleBuyPrefs::class)
        .bind(WalletStatus::class)
        .bind(EncryptedPrefs::class)

    factory { PaymentService(get(), get(), get()) }

    factory {
        PreferenceManager.getDefaultSharedPreferences(
            /* context = */ get()
        )
    }

    single {
        if (BuildConfig.DEBUG)
            TimberLogger()
        else
            NullLogger
    }

    factory { EthereumAccountWrapper() }

    single {
        AccessStateImpl(
            context = get(),
            prefs = get(),
            rxBus = get(),
            crashLogger = get()
        )
    }.bind(AccessState::class)

    factory {
        val accessState = get<AccessState>()
        object : LogoutTimer {
            override fun start() {
                accessState.startLogoutTimer()
            }

            override fun stop() {
                accessState.stopLogoutTimer()
            }
        }
    }.bind(LogoutTimer::class)

    factory { AESUtilWrapper() }
}
