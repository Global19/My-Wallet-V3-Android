package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.koin.coinifyUsersToKyc
import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.pitAnnouncementFeatureFlag
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.dashboard.announcements.rule.AlgorandAvailableAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.BackupPhraseAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.BitpayAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.BuyBitcoinAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.CloudBackupAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.FiatFundsKycAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.FiatFundsNoKycAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.InterestAvailableAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.IntroTourAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycForAirdropsAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycIncompleteAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycMoreInfoAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.KycResubmissionAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.PaxAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.PitAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.RegisterFingerprintsAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.RegisteredForAirdropMiniAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SimpleBuyAddCardAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SimpleBuyFinishSignupAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SimpleBuyPendingBuyAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.StxCompleteAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.SwapAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.TetherAvailableAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.TransferCryptoAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.TwoFAAnnouncement
import piuk.blockchain.android.ui.dashboard.announcements.rule.VerifyEmailAnnouncement

val dashboardAnnouncementsModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            val availableAnnouncements = getAllAnnouncements()

            AnnouncementList(
                mainScheduler = AndroidSchedulers.mainThread(),
                availableAnnouncements = availableAnnouncements,
                orderAdapter = get(),
                dismissRecorder = get()
            )
        }

        factory {
            AnnouncementConfigAdapterImpl(
                config = get()
            )
        }.bind(AnnouncementConfigAdapter::class)

        factory {
            AnnouncementQueries(
                nabuToken = get(),
                settings = get(),
                nabu = get(),
                tierService = get(),
                sbStateFactory = get()
            )
        }

        factory {
            KycResubmissionAnnouncement(
                kycTiersQueries = get(),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycIncompleteAnnouncement(
                kycTiersQueries = get(),
                sunriverCampaignRegistration = get(),
                dismissRecorder = get(),
                mainScheduler = AndroidSchedulers.mainThread()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycMoreInfoAnnouncement(
                tierService = get(),
                showPopupFeatureFlag = get(coinifyUsersToKyc),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            PitAnnouncement(
                pitLink = get(),
                dismissRecorder = get(),
                featureFlag = get(pitAnnouncementFeatureFlag),
                analytics = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            PaxAnnouncement(
                analytics = get(),
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            IntroTourAnnouncement(
                dismissRecorder = get(),
                prefs = get(),
                analytics = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BitpayAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SwapAnnouncement(
                dataManager = get(),
                queries = get(),
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            VerifyEmailAnnouncement(
                dismissRecorder = get(),
                walletSettings = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            TwoFAAnnouncement(
                dismissRecorder = get(),
                walletStatus = get(),
                walletSettings = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BackupPhraseAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            BuyBitcoinAnnouncement(
                dismissRecorder = get(),
                simpleBuyAvailability = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            RegisterFingerprintsAnnouncement(
                dismissRecorder = get(),
                fingerprints = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            TransferCryptoAnnouncement(
                dismissRecorder = get(),
                walletStatus = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            KycForAirdropsAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            RegisteredForAirdropMiniAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            StxCompleteAnnouncement(
                dismissRecorder = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SimpleBuyFinishSignupAnnouncement(
                dismissRecorder = get(),
                analytics = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SimpleBuyPendingBuyAnnouncement(
                dismissRecorder = get(),
                analytics = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            SimpleBuyAddCardAnnouncement(
                dismissRecorder = get(),
                analytics = get(),
                queries = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            FiatFundsNoKycAnnouncement(
                dismissRecorder = get(),
                featureEligibility = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            FiatFundsKycAnnouncement(
                dismissRecorder = get(),
                featureEligibility = get(),
                custodialWalletManager = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            AlgorandAvailableAnnouncement(
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            TetherAvailableAnnouncement(
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            CloudBackupAnnouncement(
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)

        factory {
            InterestAvailableAnnouncement(
                dismissRecorder = get()
            )
        }.bind(AnnouncementRule::class)
    }

    single {
        DismissRecorder(
            prefs = get(),
            clock = get()
        )
    }

    single {
        object : DismissClock {
            override fun now(): Long = System.currentTimeMillis()
        }
    }.bind(DismissClock::class)
}

fun getAllAnnouncements(): List<AnnouncementRule> {
    return payloadScope.getAll()
}
