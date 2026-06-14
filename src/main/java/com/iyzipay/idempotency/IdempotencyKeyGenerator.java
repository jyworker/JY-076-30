package com.iyzipay.idempotency;

import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateRefundV2Request;

import java.math.BigDecimal;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

public class IdempotencyKeyGenerator {

    private static final String REFUND_V1_PREFIX = "refund_v1_";
    private static final String REFUND_V2_PREFIX = "refund_v2_";

    public static String generateForRefundV1(CreateRefundRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            return request.getIdempotencyKey();
        }

        StringBuilder keyBuilder = new StringBuilder(REFUND_V1_PREFIX);

        String paymentTransactionId = request.getPaymentTransactionId();
        if (paymentTransactionId != null) {
            keyBuilder.append(paymentTransactionId);
        }
        keyBuilder.append("_");

        BigDecimal price = request.getPrice();
        if (price != null) {
            keyBuilder.append(price.toPlainString());
        }
        keyBuilder.append("_");

        String currency = request.getCurrency();
        if (currency != null) {
            keyBuilder.append(currency);
        }

        return hashKey(keyBuilder.toString());
    }

    public static String generateForRefundV2(CreateRefundV2Request request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isEmpty()) {
            return request.getIdempotencyKey();
        }

        StringBuilder keyBuilder = new StringBuilder(REFUND_V2_PREFIX);

        String paymentId = request.getPaymentId();
        if (paymentId != null) {
            keyBuilder.append(paymentId);
        }
        keyBuilder.append("_");

        BigDecimal price = request.getPrice();
        if (price != null) {
            keyBuilder.append(price.toPlainString());
        }

        return hashKey(keyBuilder.toString());
    }

    private static String hashKey(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawKey.getBytes("UTF-8"));
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(rawKey.hashCode());
        } catch (UnsupportedEncodingException e) {
            return String.valueOf(rawKey.hashCode());
        }
    }
}
