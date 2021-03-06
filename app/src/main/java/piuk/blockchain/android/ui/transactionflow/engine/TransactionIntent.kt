package piuk.blockchain.android.ui.transactionflow.engine

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.InvoiceTarget
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxOptionValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.ui.base.mvi.MviIntent
import java.util.EmptyStackException

sealed class TransactionIntent : MviIntent<TransactionState> {

    class InitialiseWithSourceAccount(
        private val action: AssetAction,
        private val fromAccount: CryptoAccount,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                sendingAccount = fromAccount,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                currentStep = selectStep(passwordRequired),
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)

        private fun selectStep(passwordRequired: Boolean): TransactionStep =
            if (passwordRequired) {
                TransactionStep.ENTER_PASSWORD
            } else {
                TransactionStep.ENTER_ADDRESS
            }
    }

    class InitialiseWithSourceAndTargetAccount(
        private val action: AssetAction,
        val fromAccount: CryptoAccount,
        val target: TransactionTarget,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                sendingAccount = fromAccount,
                selectedTarget = target,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                currentStep = selectStep(passwordRequired, target),
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)

        private fun selectStep(
            passwordRequired: Boolean,
            target: TransactionTarget
        ): TransactionStep =
            when {
                passwordRequired -> TransactionStep.ENTER_PASSWORD
                target is InvoiceTarget -> TransactionStep.CONFIRM_DETAIL
                else -> TransactionStep.ENTER_AMOUNT
            }
    }

    class ValidatePassword(
        val password: String
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                errorState = TransactionErrorState.NONE
            ).updateBackstack(oldState)
    }

    class UpdatePasswordIsValidated(
        val password: String
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                secondPassword = password,
                currentStep = selectStep(oldState)
            ).updateBackstack(oldState)

        private fun selectStep(oldState: TransactionState): TransactionStep =
            when (oldState.selectedTarget) {
                is NullAddress -> TransactionStep.ENTER_ADDRESS
                is InvoiceTarget -> TransactionStep.CONFIRM_DETAIL
                else -> TransactionStep.ENTER_AMOUNT
            }
    }

    object UpdatePasswordNotValidated : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                errorState = TransactionErrorState.INVALID_PASSWORD,
                secondPassword = ""
            ).updateBackstack(oldState)
    }

    // Check a manually entered address is correct. If it is, the interactor will send a
    // TargetAddressValidated intent which, in turn, will enable the next cta on the enter
    // address sheet
    class ValidateInputTargetAddress(
        val targetAddress: String,
        val expectedCrypto: CryptoCurrency
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class TargetAddressValidated(
        val transactionTarget: TransactionTarget
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                selectedTarget = transactionTarget,
                nextEnabled = true
            ).updateBackstack(oldState)
    }

    class TargetAddressInvalid(private val error: TxValidationFailure) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = error.state.mapToTransactionError(),
                selectedTarget = NullCryptoAccount(),
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    // Fired from the enter address sheet when a target address is confirmed - by selecting from the list
    // (in this build, this will change for swap) or when the CTA is clicked. Move to the enter amount sheet
    // once this has been processed. Do not send this from anywhere _but_ the enter address sheet.
    class TargetSelectionConfirmed(
        val transactionTarget: TransactionTarget
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                selectedTarget = transactionTarget,
                currentStep = TransactionStep.ENTER_AMOUNT,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    object FetchFiatRates : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object FetchTargetRates : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class FiatRateUpdated(
        private val fiatRate: ExchangeRate.CryptoToFiat
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                fiatRate = fiatRate
            ).updateBackstack(oldState)
    }

    class CryptoRateUpdated(
        private val targetRate: ExchangeRate
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                targetRate = targetRate
            ).updateBackstack(oldState)
    }

    // Send by the interactor when the transaction engine is started, informs the FE that amount input
    // can be performed and provides any capability flags to the FE
    class PendingTransactionStarted(
        private val canTransactFiat: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                allowFiatInput = canTransactFiat,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class AmountChanged(
        val amount: Money
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class ModifyTxOption(
        val option: TxOptionValue
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class PendingTxUpdated(
        private val pendingTx: PendingTx
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                pendingTx = pendingTx,
                nextEnabled = pendingTx.validationState == ValidationState.CAN_EXECUTE,
                errorState = pendingTx.validationState.mapToTransactionError()
            ).updateBackstack(oldState)
    }

    // Fired when the cta of the enter amount sheet is clicked. This just moved to the
    // confirm sheet, with CTA disabled pending a validation check.
    object PrepareTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false, // Don't enable until we get a validated pendingTx from the interactor
                currentStep = TransactionStep.CONFIRM_DETAIL
            ).updateBackstack(oldState)
    }

    object ExecuteTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                currentStep = TransactionStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.IN_PROGRESS
            ).updateBackstack(oldState)
    }

    class FatalTransactionError(
        private val error: Throwable
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                currentStep = TransactionStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.ERROR
            ).updateBackstack(oldState)
    }

    object InvalidateTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                pendingTx = null,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    object UpdateTransactionComplete : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                executionStatus = TxExecutionStatus.COMPLETED
            ).updateBackstack(oldState)
    }

    // This fn pops the backstack, thus no need to update the backstack here
    object ReturnToPreviousStep : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState {
            try {
                val stack = oldState.stepsBackStack
                val previousStep = stack.pop()
                return oldState.copy(
                    stepsBackStack = stack,
                    currentStep = previousStep,
                    errorState = TransactionErrorState.NONE
                )
            } catch (e: EmptyStackException) {
                // if the stack is empty, throw
                throw IllegalStateException("Cannot go back")
            }
        }
    }

    // Fired from when the confirm transaction sheet is created.
    // Forces a validation pass; we will get a
    object ValidateTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object EnteredAddressReset : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            errorState = TransactionErrorState.NONE
        )
    }

    fun TransactionState.updateBackstack(oldState: TransactionState) =
        if (oldState.currentStep != this.currentStep && oldState.currentStep.addToBackStack) {
            val updatedStack = oldState.stepsBackStack
            updatedStack.push(oldState.currentStep)

            this.copy(stepsBackStack = updatedStack)
        } else {
            this
        }
}

private fun ValidationState.mapToTransactionError() =
    when (this) {
        ValidationState.INVALID_AMOUNT -> TransactionErrorState.INVALID_AMOUNT
        ValidationState.INSUFFICIENT_FUNDS -> TransactionErrorState.INSUFFICIENT_FUNDS
        ValidationState.INSUFFICIENT_GAS -> TransactionErrorState.NOT_ENOUGH_GAS
        ValidationState.CAN_EXECUTE -> TransactionErrorState.NONE
        ValidationState.UNINITIALISED -> TransactionErrorState.NONE
        ValidationState.INVALID_ADDRESS -> TransactionErrorState.INVALID_ADDRESS
        ValidationState.ADDRESS_IS_CONTRACT -> TransactionErrorState.ADDRESS_IS_CONTRACT
        ValidationState.UNDER_MIN_LIMIT -> TransactionErrorState.BELOW_MIN_LIMIT
        ValidationState.HAS_TX_IN_FLIGHT -> TransactionErrorState.TRANSACTION_IN_FLIGHT
        ValidationState.OPTION_INVALID -> TransactionErrorState.TX_OPTION_INVALID
        ValidationState.OVER_MAX_LIMIT -> TransactionErrorState.ABOVE_MAX_LIMIT
        ValidationState.INVOICE_EXPIRED, // We shouldn't see this here
        ValidationState.UNKNOWN_ERROR -> TransactionErrorState.UNKNOWN_ERROR
    }
