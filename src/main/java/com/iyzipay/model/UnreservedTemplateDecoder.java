package com.iyzipay.model;

import com.iyzipay.exception.DuplicateTagException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UnreservedTemplateDecoder {

    private static final int TAG_LENGTH = 2;
    private static final int LENGTH_LENGTH = 2;

    private UnreservedTemplateDecoder() {
    }

    public static Map<String, Object> decode(String tlv) {
        if (tlv == null) {
            throw new NullPointerException("tlv must not be null");
        }
        if (tlv.isEmpty()) {
            throw new IllegalArgumentException("tlv must not be empty");
        }
        return decodeNested(tlv);
    }

    private static Map<String, Object> decodeNested(String tlv) {
        Map<String, Object> result = new HashMap<String, Object>();
        Set<String> seenTags = new HashSet<String>();
        int offset = 0;
        while (offset + TAG_LENGTH + LENGTH_LENGTH <= tlv.length()) {
            String tag = tlv.substring(offset, offset + TAG_LENGTH);
            offset += TAG_LENGTH;

            if (seenTags.contains(tag)) {
                throw new DuplicateTagException("Duplicate tag found: " + tag);
            }
            seenTags.add(tag);

            int length;
            try {
                length = Integer.parseInt(tlv.substring(offset, offset + LENGTH_LENGTH));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid length format for tag: " + tag, e);
            }
            offset += LENGTH_LENGTH;

            if (offset + length > tlv.length()) {
                throw new IllegalArgumentException("Value exceeds TLV boundary for tag: " + tag);
            }

            String value = tlv.substring(offset, offset + length);
            offset += length;

            if (isNestedTlv(value)) {
                Map<String, Object> nestedMap = decodeNested(value);
                result.put(tag, nestedMap);
            } else {
                result.put(tag, value);
            }
        }
        return result;
    }

    private static boolean isNestedTlv(String value) {
        if (value == null || value.length() < TAG_LENGTH + LENGTH_LENGTH) {
            return false;
        }
        int offset = 0;
        while (offset + TAG_LENGTH + LENGTH_LENGTH <= value.length()) {
            String lenStr = value.substring(offset + TAG_LENGTH, offset + TAG_LENGTH + LENGTH_LENGTH);
            int len;
            try {
                len = Integer.parseInt(lenStr);
            } catch (NumberFormatException e) {
                return false;
            }
            offset += TAG_LENGTH + LENGTH_LENGTH + len;
            if (offset > value.length()) {
                return false;
            }
        }
        return offset == value.length();
    }
}
