package piuk.blockchain.android.coincore.xlm

import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import java.util.concurrent.atomic.AtomicBoolean

internal class XlmCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    override val label: String = "",
    private val address: String,
    private val xlmManager: XlmDataManager,
    override val exchangeRates: ExchangeRateDataManager,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val walletOptionsDataManager: WalletOptionsDataManager
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.XLM) {

    override val isDefault: Boolean = true // Only one account ever, so always default

    private val hasFunds = AtomicBoolean(false)

    override val isFunded: Boolean
        get() = hasFunds.get()

    override val accountBalance: Single<Money>
        get() = xlmManager.getBalance()
            .doOnSuccess {
                hasFunds.set(it > CryptoValue.ZeroXlm)
            }
            .map { it as Money }

    override val actionableBalance: Single<Money>
        get() = xlmManager.getBalanceAndMin().map {
            it.balance - it.minimumBalance
        }

    override val receiveAddress: Single<ReceiveAddress>
        get() = Single.just(
            XlmAddress(_address = address, _label = label)
        )

    override val activity: Single<ActivitySummaryList>
        get() = xlmManager.getTransactionList()
            .mapList {
                XlmActivitySummaryItem(
                    it,
                    exchangeRates,
                    account = this
                ) as ActivitySummaryItem
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override fun createTxEngine(): TxEngine =
        XlmOnChainTxEngine(
            xlmDataManager = xlmManager,
            xlmFeesFetcher = xlmFeesFetcher,
            walletOptionsDataManager = walletOptionsDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted
        )

    constructor(
        payloadManager: PayloadDataManager,
        account: AccountReference.Xlm,
        xlmManager: XlmDataManager,
        exchangeRates: ExchangeRateDataManager,
        xlmFeesFetcher: XlmFeesFetcher,
        walletOptionsDataManager: WalletOptionsDataManager
    ) : this(
        payloadManager = payloadManager,
        label = account.label,
        address = account.accountId,
        xlmManager = xlmManager,
        exchangeRates = exchangeRates,
        xlmFeesFetcher = xlmFeesFetcher,
        walletOptionsDataManager = walletOptionsDataManager
    )
}
