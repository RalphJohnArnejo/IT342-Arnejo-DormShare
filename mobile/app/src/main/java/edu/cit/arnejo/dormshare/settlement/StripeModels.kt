package edu.cit.arnejo.dormshare.settlement

/**
 * Response from POST /api/payments/stripe/intent.
 * Maps the backend's payment intent response fields.
 */
data class StripeIntentResponse(
    val paymentIntentId: String? = null,
    val clientSecret: String? = null,
    val publicKey: String? = null,
    val amount: Long? = null,
    val currency: String? = null,
    val status: String? = null,
    val mockMode: Boolean? = null,
    val message: String? = null
)

/**
 * Response from POST /api/payments/stripe/confirm.
 * Maps the backend's payment confirmation response fields.
 */
data class StripeConfirmResponse(
    val paymentIntentId: String? = null,
    val status: String? = null,
    val mockMode: Boolean? = null,
    val message: String? = null
)
