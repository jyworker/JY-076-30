package com.iyzipay.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import com.iyzipay.HashValidator;
import com.iyzipay.HttpClient;
import com.iyzipay.IyzipayResource;
import com.iyzipay.Options;
import com.iyzipay.ResponseSignatureGenerator;
import com.iyzipay.idempotency.RefundIdempotentExecutor;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateRefundV2Request;

public class Refund extends IyzipayResource implements ResponseSignatureGenerator {

    private static final String IDEMPOTENCY_KEY_HEADER = "x-iyzi-idempotency-key";

    private String paymentId;
    private String paymentTransactionId;
    private BigDecimal price;
    private String currency;
    private String connectorName;
    private String authCode;
    private String hostReference;
    private String refundHostReference;
    private String signature;

    public static Refund create(CreateRefundRequest request, Options options) {
        String path = "/payment/refund";
        Map<String, String> headers = getHttpHeadersV2(path, request, options);
        addIdempotencyHeader(headers, request.getIdempotencyKey());
        return HttpClient.create().post(options.getBaseUrl() + path,
                getHttpProxy(options),
                headers,
                request,
                Refund.class);
    }

    public static Refund createV2(CreateRefundV2Request request, Options options) {
        String path = "/v2/payment/refund";
        Map<String, String> headers = getHttpHeadersV2(path, request, options);
        addIdempotencyHeader(headers, request.getIdempotencyKey());
        return HttpClient.create().post(options.getBaseUrl() + path,
                getHttpProxy(options),
                headers,
                request,
                Refund.class);
    }

    private static void addIdempotencyHeader(Map<String, String> headers, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            headers.put(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }
    }

    public static Refund createIdempotent(CreateRefundRequest request, Options options) {
        return RefundIdempotentExecutor.getInstance().executeRefundV1(request, options);
    }

    public static Refund createV2Idempotent(CreateRefundV2Request request, Options options) {
        return RefundIdempotentExecutor.getInstance().executeRefundV2(request, options);
    }

    public boolean verifySignature(String secretKey) {
        String calculated = generateSignature(secretKey,
                Arrays.asList(getPaymentId(), getPrice(), getCurrency(), getConversationId()));
        return HashValidator.hashValid(getSignature(), calculated);
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getHostReference() {
        return hostReference;
    }

    public void setHostReference(String hostReference) {
        this.hostReference = hostReference;
    }
    public String getRefundHostReference() {
        return refundHostReference;
    }

    public void setRefundHostReference(String refundHostReference) {
        this.refundHostReference = refundHostReference;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
