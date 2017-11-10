package piuk.blockchain.android.ui.shapeshift.newexchange

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.support.annotation.StringRes
import android.support.design.widget.CoordinatorLayout
import android.view.View
import android.widget.EditText
import com.jakewharton.rxbinding2.widget.RxTextView
import info.blockchain.wallet.payload.data.Account
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_new_exchange.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.consume
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.text.DecimalFormatSymbols
import java.util.*
import javax.inject.Inject


class NewExchangeActivity : BaseMvpActivity<NewExchangeView, NewExchangePresenter>(), NewExchangeView,
        NumericKeyboardCallback {

    @Suppress("MemberVisibilityCanPrivate")
    @Inject lateinit var newExchangePresenter: NewExchangePresenter

    override val locale: Locale
        get() = Locale.getDefault()
    private val btcSymbol = CryptoCurrencies.BTC.symbol.toUpperCase()
    private val ethSymbol = CryptoCurrencies.ETHER.symbol.toUpperCase()
    private val compositeDisposable = CompositeDisposable()
    private val defaultDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val editTexts by unsafeLazy {
        listOf(edittext_from_crypto, edittext_to_crypto, edittext_to_fiat, edittext_from_fiat)
    }

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_exchange)
        setupToolbar(toolbar_general, R.string.shapeshift_exchange)

        button_continue.setOnClickListener { presenter.onContinuePressed() }
        textview_use_max.setOnClickListener { presenter.onMaxPressed() }
        textview_use_min.setOnClickListener { presenter.onMinPressed() }
        imageview_from_dropdown.setOnClickListener { presenter.onFromChooserClicked() }
        imageview_to_dropdown.setOnClickListener { presenter.onToChooserClicked() }

        imageview_switch_currency.setOnClickListener {
            imageview_switch_currency.createSpringAnimation(
                    SpringAnimation.ROTATION,
                    imageview_switch_currency.rotation + ROTATION,
                    SpringForce.STIFFNESS_LOW,
                    SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            ).start()

            presenter.onSwitchCurrencyClicked()
        }

        setupListeners()
        setupKeypad()

        onViewReady()
    }

    override fun updateUi(
            fromCurrency: CryptoCurrencies,
            displayDropDown: Boolean,
            fromLabel: String,
            toLabel: String,
            fiatHint: String
    ) {
        // Titles
        textview_to_address.text = toLabel
        textview_from_address.text = fromLabel
        // Hints
        edittext_to_fiat.hint = fiatHint
        edittext_from_fiat.hint = fiatHint
        edittext_from_crypto.hint = "0.00"
        edittext_to_crypto.hint = "0.00"

        when (fromCurrency) {
            CryptoCurrencies.BTC -> showFromBtc(displayDropDown)
            CryptoCurrencies.ETHER -> showFromEth(displayDropDown)
            CryptoCurrencies.BCC -> throw IllegalArgumentException("BCC not supported")
        }
    }

    override fun launchAccountChooserActivityTo() {
        AccountChooserActivity.startForResult(
                this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND,
                PaymentRequestType.SHAPE_SHIFT,
                getString(R.string.to)
        )
    }

    override fun launchAccountChooserActivityFrom() {
        AccountChooserActivity.startForResult(
                this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND,
                PaymentRequestType.SHAPE_SHIFT,
                getString(R.string.from)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {

            val account: Account = data.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM)
                    .toKotlinObject()

            when (requestCode) {
                AccountChooserActivity.REQUEST_CODE_CHOOSE_SENDING_ACCOUNT_FROM_SEND ->
                    presenter.onFromAccountChanged(account)
                AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_SEND ->
                    presenter.onToAccountChanged(account)
                else -> throw IllegalArgumentException("Unknown request code $requestCode")
            }
        }
    }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(message)
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            if (!isFinishing) dismiss()
            progressDialog = null
        }
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun finishPage() = finish()

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = newExchangePresenter

    override fun getView() = this

    override fun updateFromCryptoText(text: String) {
        edittext_from_crypto.setText(text)
    }

    override fun updateToCryptoText(text: String) {
        edittext_to_crypto.setText(text)
    }

    override fun updateFromFiatText(text: String) {
        edittext_from_fiat.setText(text)
    }

    override fun updateToFiatText(text: String) {
        edittext_to_fiat.setText(text)
    }

    override fun clearEditTexts() {
        editTexts.forEach { it.text.clear() }
    }

    override fun onKeypadClose() {
        val height = resources.getDimension(R.dimen.action_bar_height).toInt()
        // Resize activity to default
        shapeshift_scrollview.apply {
            setPadding(0, 0, 0, 0)
            layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, 0) }

            postDelayed({ smoothScrollTo(0, 0) }, 100)
        }
    }

    override fun onKeypadOpen() = Unit

    override fun onKeypadOpenCompleted() {
        // Resize activity around view
        val height = resources.getDimension(R.dimen.action_bar_height).toInt()
        shapeshift_scrollview.apply {
            setPadding(0, 0, 0, shapeshift_keyboard.height)
            layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, 0) }

            scrollTo(0, bottom)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        closeKeyPad()
    }

    override fun onBackPressed() {
        if (isKeyboardVisible()) {
            closeKeyPad()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupListeners() {
        mapOf(
                edittext_to_crypto to presenter::onToCryptoValueChanged,
                edittext_from_crypto to presenter::onFromCryptoValueChanged,
                edittext_to_fiat to presenter::onToFiatValueChanged,
                edittext_from_fiat to presenter::onFromFiatValueChanged
        ).map {
            it.key.setOnClickListener { clearEditTexts() }
            return@map getTextWatcherObservable(it.key, it.value)
        }.map {
            // Resume if any formatting errors occur
            it.onErrorResumeNext(it).subscribe()
            // Dispose subscriptions onDestroy as strong reference held to View
        }.forEach { compositeDisposable.addAll(it) }
    }

    private fun setupKeypad() {
        editTexts.forEach {
            it.disableSoftKeyboard()
            shapeshift_keyboard.enableOnView(it)
        }
        shapeshift_keyboard.apply {
            setDecimalSeparator(defaultDecimalSeparator)
            setCallback(this@NewExchangeActivity)
        }
    }

    private fun getTextWatcherObservable(editText: EditText, presenterFunction: (String) -> Unit) =
            RxTextView.textChanges(editText)
                    // Logging
                    .doOnError { Timber.e(it) }
                    // Skip first event emitted when subscribing
                    .skip(1)
                    // Convert to String
                    .map { it.toString() }
                    // Ignore elements emitted by non-user events (ie presenter updates)
                    .doOnNext { if (currentFocus == editText) presenterFunction(it) }
                    // Scheduling
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())

    private fun showFromBtc(displayDropDown: Boolean) {
        // Units
        textview_unit_from.text = btcSymbol
        textview_unit_to.text = ethSymbol
        // Visibility
        imageview_to_dropdown.gone()
        if (displayDropDown) imageview_from_dropdown.visible()
    }

    private fun showFromEth(displayDropDown: Boolean) {
        // Units
        textview_unit_from.text = ethSymbol
        textview_unit_to.text = btcSymbol
        // Visibility
        if (displayDropDown) imageview_to_dropdown.visible()
        imageview_from_dropdown.gone()
    }

    private fun isKeyboardVisible(): Boolean = shapeshift_keyboard.isVisible

    private fun closeKeyPad() {
        shapeshift_keyboard.setNumpadVisibility(View.GONE)
    }

    companion object {

        private val ROTATION = 180f

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, NewExchangeActivity::class.java))
        }

    }

}