package com.shrawan.resumebuilder.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.shrawan.resumebuilder.document.Payment;
import com.shrawan.resumebuilder.document.User;
import com.shrawan.resumebuilder.dto.AuthResponse;
import com.shrawan.resumebuilder.repository.PaymentRepository;
import com.shrawan.resumebuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.shrawan.resumebuilder.util.AppConstants.PREMIUM;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;


    public Payment createOrder(@Nullable Object principal, String planType) throws RazorpayException {
        //0. Initial steps
        AuthResponse authResponse = authService.getProfile(principal);
        //1. initialize the razorpay client
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        //2. prepare the json object to pass the razorpay
        int amount = 99900;
        String currency = "INR";
        String receipt = PREMIUM + "_" + UUID.randomUUID().toString().substring(0, 8);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);

        //3. call the razorpay API to create the order

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        //4. save the order detail to the DB
        Payment newPayment = Payment.builder()
                .userId(authResponse.getId())
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(amount)
                .currency(currency)
                .planType(planType)
                .status("created")
                .receipt(receipt)
                .build();
        //5. return the result
        return paymentRepository.save(newPayment);

    }

    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            boolean isValidSignature = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            if (isValidSignature) {
                Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() -> new RuntimeException("Payment not found"));
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setRazorpaySignature(razorpaySignature);
                payment.setStatus("paid");
                paymentRepository.save(payment);
                UpgradeUserSubscription(payment.getUserId(),payment.getPlanType());

                return true;
            }

            return false;

        } catch (RazorpayException e) {
            log.error("Error verifying payment signature: {}", e.getMessage());
            return false;
        }

    }

    private void UpgradeUserSubscription(String userId, String planType) {
        User existingUser = userRepository.findById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        existingUser.setSubscriptionPlan(planType);
        userRepository.save(existingUser);
        log.info("user {} upgraded to {} plan", userId, planType);
    }

    public List<Payment> getUserPayments(Object principal) {
        //1. get the current profile
        AuthResponse authResponse = authService.getProfile(principal);
        //2. call the repo finder method
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(authResponse.getId());


    }

    public Payment getPaymentDetails(String orderId) {
        //1 . call the repo finder method
        return paymentRepository.findByRazorpayOrderId(orderId)
                .orElseGet(() -> paymentRepository.findByRazorpayPaymentId(orderId)
                        .orElseThrow(() -> new RuntimeException("Payment not found")));
    }
}
