package edu.cit.arnejo.dormshare.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
 * Supports both real Stripe API and mock mode for testing
 * Handles creation, confirmation, and status checking of payment intents
 */
@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.api.key:sk_test_dummy_key_for_sandbox}")
    private String stripeApiKey;

    @Value("${stripe.sandbox.mode:true}")
    private boolean sandboxMode;

    @Value("${stripe.public.key:pk_test_dummy_key_for_sandbox}")
    private String stripePublicKey;

    @Value("${stripe.mock.enabled:false}")
    private boolean mockEnabled;

    private Map<String, Map<String, Object>> mockPaymentIntents = new HashMap<>();

    /**
     * Initialize Stripe API key on service startup
     */
    public StripeService(@Value("${stripe.api.key:sk_test_dummy_key_for_sandbox}") String apiKey) {
        if (!isValidTestKey(apiKey)) {
            logger.warn("Stripe API key appears to be invalid. Mock mode will be enabled for testing.");
        } else {
            Stripe.apiKey = apiKey;
            logger.info("Stripe Service initialized with API key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
        }
    }

    /**
     * Validate if the key looks like a real Stripe test key
     */
    private boolean isValidTestKey(String key) {
        return key != null && key.startsWith("sk_test_") && key.length() > 20 && !key.contains("dummy");
    }

    /**
     * Create a payment intent for stripe checkout
     * Uses mock mode if real Stripe is not configured
     * @param amountInCents Amount in cents (e.g., $10.00 = 1000)
     * @param payerId ID of the user making payment
     * @param payeeId ID of the user receiving payment
     * @param description Description of the payment
     * @return Map containing clientSecret and paymentIntentId
     */
    public Map<String, Object> createPaymentIntent(Long amountInCents, Long payerId, Long payeeId, String description) {
        try {
            if (mockEnabled || !isValidTestKey(stripeApiKey)) {
                logger.info("Using mock payment intent for testing");
                return createMockPaymentIntent(amountInCents, payerId, payeeId, description);
            }

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
            // include public key so frontend can initialize Stripe.js
            response.put("publicKey", stripePublicKey);

            logger.info("Payment intent created: {} for amount: {} PHP", intent.getId(), amountInCents);
            return response;

        } catch (Exception e) {
            logger.error("Error creating payment intent, falling back to mock: {}", e.getMessage());
            return createMockPaymentIntent(amountInCents, payerId, payeeId, description);
        }
    }

    /**
     * Create a mock payment intent for testing
     */
    private Map<String, Object> createMockPaymentIntent(Long amountInCents, Long payerId, Long payeeId, String description) {
        // Generate Stripe-compatible IDs (remove dashes from UUID)
        String uuid1 = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String uuid2 = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String paymentIntentId = "pi_" + uuid1;
        String clientSecret = paymentIntentId + "_secret_" + uuid2;

        Map<String, Object> intentData = new HashMap<>();
        intentData.put("id", paymentIntentId);
        intentData.put("clientSecret", clientSecret);
        intentData.put("amount", amountInCents);
        intentData.put("currency", "php");
        intentData.put("status", "requires_payment_method");
        intentData.put("description", description);
        intentData.put("payerId", payerId);
        intentData.put("payeeId", payeeId);

        mockPaymentIntents.put(paymentIntentId, intentData);

        Map<String, Object> response = new HashMap<>();
        response.put("clientSecret", clientSecret);
        response.put("paymentIntentId", paymentIntentId);
        response.put("amount", amountInCents);
        response.put("currency", "php");
        response.put("status", "requires_payment_method");
        response.put("publicKey", stripePublicKey);
        response.put("mockMode", true);

        logger.info("Mock payment intent created: {} for amount: {} PHP", paymentIntentId, amountInCents);
        return response;
    }

    /**
     * Retrieve payment intent status
     * Uses mock mode if real Stripe is not configured
     * @param paymentIntentId Stripe payment intent ID
     * @return Map containing payment intent details
     */
    public Map<String, Object> getPaymentIntentStatus(String paymentIntentId) {
        try {
            // Check if this is a mock payment intent (stored in our map)
            if (mockPaymentIntents.containsKey(paymentIntentId)) {
                return getMockPaymentIntentStatus(paymentIntentId);
            }

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
     * Get mock payment intent status
     */
    private Map<String, Object> getMockPaymentIntentStatus(String paymentIntentId) {
        Map<String, Object> intentData = mockPaymentIntents.get(paymentIntentId);
        if (intentData == null) {
            throw new RuntimeException("Payment intent not found: " + paymentIntentId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("paymentIntentId", intentData.get("id"));
        response.put("status", intentData.get("status"));
        response.put("amount", intentData.get("amount"));
        response.put("currency", intentData.get("currency"));
        response.put("mockMode", true);

        return response;
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
            // Check if this is a mock payment intent
            if (mockPaymentIntents.containsKey(paymentIntentId)) {
                return confirmMockPaymentIntent(paymentIntentId, paymentMethodId);
            }

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
     * Confirm a mock payment intent
     */
    private Map<String, Object> confirmMockPaymentIntent(String paymentIntentId, String paymentMethodId) {
        Map<String, Object> intentData = mockPaymentIntents.get(paymentIntentId);
        if (intentData == null) {
            throw new RuntimeException("Payment intent not found: " + paymentIntentId);
        }

        // For testing, automatically mark as succeeded
        intentData.put("status", "succeeded");
        intentData.put("paymentMethodId", paymentMethodId);

        Map<String, Object> response = new HashMap<>();
        response.put("paymentIntentId", paymentIntentId);
        response.put("status", "succeeded");
        response.put("amount", intentData.get("amount"));
        response.put("currency", intentData.get("currency"));
        response.put("mockMode", true);

        logger.info("Mock payment intent confirmed: {} with status: succeeded", paymentIntentId);
        return response;
    }

    /**
     * Check if a payment was successful
     * @param paymentIntentId Stripe payment intent ID
     * @return true if payment succeeded, false otherwise
     */
    public boolean isPaymentSuccessful(String paymentIntentId) {
        try {
            if (mockPaymentIntents.containsKey(paymentIntentId)) {
                Map<String, Object> intentData = mockPaymentIntents.get(paymentIntentId);
                return intentData != null && "succeeded".equals(intentData.get("status"));
            }

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
        return stripePublicKey;
    }
}
