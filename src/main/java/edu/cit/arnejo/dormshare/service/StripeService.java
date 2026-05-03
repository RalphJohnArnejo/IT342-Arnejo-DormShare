package edu.cit.arnejo.dormshare.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;

/**
 * Service for managing Stripe payment operations in sandbox mode
 * Handles creation, confirmation, and status checking of payment intents
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api.key:sk_test_dummy_key_for_sandbox}")
    private String stripeApiKey;

    @Value("${stripe.sandbox.mode:true}")
    private boolean sandboxMode;

    /**
     * Initialize Stripe API key on service startup
     */
    public StripeService(@Value("${stripe.api.key:sk_test_dummy_key_for_sandbox}") String apiKey) {
        Stripe.apiKey = apiKey;
        logger.info("Stripe Service initialized with API key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
    }

    /**
     * Create a payment intent for stripe checkout
     * @param amountInCents Amount in cents (e.g., $10.00 = 1000)
     * @param payerId ID of the user making payment
     * @param payeeId ID of the user receiving payment
     * @param description Description of the payment
     * @return Map containing clientSecret and paymentIntentId
     */
    public Map<String, Object> createPaymentIntent(Long amountInCents, Long payerId, Long payeeId, String description) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("php") // Philippine Peso for sandbox testing
                    .setDescription(description)
                    .putMetadata("payerId", payerId.toString())
                    .putMetadata("payeeId", payeeId.toString())
                    .putMetadata("sandbox", String.valueOf(sandboxMode))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("paymentIntentId", intent.getId());
            response.put("amount", intent.getAmount());
            response.put("currency", intent.getCurrency());
            response.put("status", intent.getStatus());

            logger.info("Payment intent created: {} for amount: {} PHP", intent.getId(), amountInCents);
            return response;

        } catch (Exception e) {
            logger.error("Error creating payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Retrieve payment intent status
     * @param paymentIntentId Stripe payment intent ID
     * @return Map containing payment intent details
     */
    public Map<String, Object> getPaymentIntentStatus(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", intent.getId());
            response.put("status", intent.getStatus());
            response.put("amount", intent.getAmount());
            response.put("currency", intent.getCurrency());
            response.put("lastPaymentError", intent.getLastPaymentError());

            return response;

        } catch (Exception e) {
            logger.error("Error retrieving payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve payment intent: " + e.getMessage());
        }
    }

    /**
     * Confirm a payment intent (for payment method confirmation)
     * This is typically used after the frontend obtains a payment method token
     * @param paymentIntentId Stripe payment intent ID
     * @param paymentMethodId Stripe payment method ID
     * @return Map containing confirmation status
     */
    public Map<String, Object> confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        try {
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .setReturnUrl("http://localhost:5173/settlement?status=confirmed")
                    .build();

            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent.confirm(params);

            Map<String, Object> response = new HashMap<>();
            response.put("paymentIntentId", intent.getId());
            response.put("status", intent.getStatus());
            response.put("amount", intent.getAmount());
            response.put("currency", intent.getCurrency());

            logger.info("Payment intent confirmed: {} with status: {}", intent.getId(), intent.getStatus());
            return response;

        } catch (Exception e) {
            logger.error("Error confirming payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to confirm payment intent: " + e.getMessage());
        }
    }

    /**
     * Check if a payment was successful
     * @param paymentIntentId Stripe payment intent ID
     * @return true if payment succeeded, false otherwise
     */
    public boolean isPaymentSuccessful(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(intent.getStatus());
        } catch (Exception e) {
            logger.error("Error checking payment status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get Stripe API key (for frontend Stripe.js initialization)
     * Note: Only public key should be used on frontend
     * @return Public key for Stripe
     */
    public String getPublicKey() {
        // In real implementation, this would come from properties
        // For now, return a test public key
        return "pk_test_51234567890abcdefghijk";
    }
}
