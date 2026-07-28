package com.shrawan.resumebuilder.controller;

import com.razorpay.RazorpayException;
import com.shrawan.resumebuilder.document.Payment;
import com.shrawan.resumebuilder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.shrawan.resumebuilder.util.AppConstants.PREMIUM;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String ,String> request,
                                         Authentication authentication) throws RazorpayException {
        //0. validate the request
        String planType = request.get("planType");
        if(!PREMIUM.equalsIgnoreCase(planType)){
            return ResponseEntity.badRequest().body(Map.of("message","Invalid Plan type"));
        }
        //1. call the service method
        Payment payment = paymentService.createOrder(authentication.getPrincipal(), planType);
        //2. prepare the response object
        Map<String,Object> response = Map.of(
                "orderId", payment.getRazorpayOrderId(),
                "amount",payment.getAmount(),
                "currency",payment.getCurrency(),
                "receipt",payment.getReceipt()
        );
        //3. return the response
        return ResponseEntity.ok(response);

    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String,String> request){
        //1. validate the request

        String razorpayOrderId = request.get("razorpay_order_id");
        String razorpayPaymentId = request.get("razorpay_payment_id");
        String razorpaySignature = request.get("razorpay_signature");

        if (Objects.isNull(razorpayOrderId) ||
                Objects.isNull(razorpayPaymentId) ||
                Objects.isNull(razorpaySignature)) {
            return ResponseEntity.badRequest().body(Map.of("message", "missing required payment parameters"));
        }
        //2. Call the service method

        boolean isValid = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        //3. return the response

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "message", "Payment verified successfully",
                    "status", "success"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Payment Verification failed"));
        }


    }

    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(Authentication authentication){
        //1. call the service
        List<Payment>payments = paymentService.getUserPayments(authentication.getPrincipal());
        //2. return the response
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable String orderId){
        //1. call the service method
        Payment paymentDetails = paymentService.getPaymentDetails(orderId);
        //2. return response
        return ResponseEntity.ok(paymentDetails);


    }

}










