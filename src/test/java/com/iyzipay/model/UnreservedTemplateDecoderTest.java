package com.iyzipay.model;

import com.iyzipay.exception.DuplicateTagException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class UnreservedTemplateDecoderTest {

    private static String buildTlv(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }

    @Test(expected = NullPointerException.class)
    public void should_throw_null_pointer_exception_given_null_tlv() {
        UnreservedTemplateDecoder.decode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_illegal_argument_exception_given_empty_tlv() {
        UnreservedTemplateDecoder.decode("");
    }

    @Test
    public void should_decode_single_level_tlv() {
        String tlv = buildTlv("00", "hello") + buildTlv("01", "world");
        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);
        assertEquals("hello", result.get("00"));
        assertEquals("world", result.get("01"));
    }

    @Test(expected = DuplicateTagException.class)
    public void should_throw_duplicate_tag_exception_for_same_level_duplicate() {
        String tlv = buildTlv("00", "hello") + buildTlv("00", "world");
        UnreservedTemplateDecoder.decode(tlv);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_decode_nested_tlv_with_same_tag_as_parent() {
        String nestedValue = buildTlv("00", "child") + buildTlv("02", "test");
        String tlv = buildTlv("00", "parent") + buildTlv("01", nestedValue);

        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);

        assertEquals("parent", result.get("00"));
        assertNotNull(result.get("01"));
        assertTrue(result.get("01") instanceof Map);

        Map<String, Object> nested = (Map<String, Object>) result.get("01");
        assertEquals("child", nested.get("00"));
        assertEquals("test", nested.get("02"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_decode_multiple_nested_tlvs() {
        String nested1 = buildTlv("00", "first");
        String nested2 = buildTlv("01", "second");
        String tlv = buildTlv("00", nested1) + buildTlv("01", nested2);

        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);

        assertTrue(result.get("00") instanceof Map);
        assertTrue(result.get("01") instanceof Map);

        Map<String, Object> nestedMap1 = (Map<String, Object>) result.get("00");
        Map<String, Object> nestedMap2 = (Map<String, Object>) result.get("01");

        assertEquals("first", nestedMap1.get("00"));
        assertEquals("second", nestedMap2.get("01"));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DuplicateTagException.class)
    public void should_throw_duplicate_tag_exception_for_duplicate_in_nested_level() {
        String nestedValue = buildTlv("00", "child1") + buildTlv("00", "child2");
        String tlv = buildTlv("01", nestedValue);
        UnreservedTemplateDecoder.decode(tlv);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_decode_deeply_nested_tlv() {
        String level3 = buildTlv("00", "deepest");
        String level2 = buildTlv("00", level3) + buildTlv("01", "hello");
        String level1 = buildTlv("00", level2);
        String tlv = buildTlv("01", level1);

        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);

        Map<String, Object> l1 = (Map<String, Object>) result.get("01");
        Map<String, Object> l2 = (Map<String, Object>) l1.get("00");
        Map<String, Object> l3 = (Map<String, Object>) l2.get("00");

        assertEquals("deepest", l3.get("00"));
        assertEquals("hello", l2.get("01"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_handle_mixed_simple_and_nested_values() {
        String nested = buildTlv("01", "inner");
        String tlv = buildTlv("00", "simple") + buildTlv("01", nested) + buildTlv("02", "simple2");

        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);

        assertEquals("simple", result.get("00"));
        assertEquals("simple2", result.get("02"));
        assertTrue(result.get("01") instanceof Map);

        Map<String, Object> nestedMap = (Map<String, Object>) result.get("01");
        assertEquals("inner", nestedMap.get("01"));
    }

    @Test
    public void should_decode_simple_value_when_not_tlv_format() {
        String tlv = buildTlv("00", "notatlv") + buildTlv("01", "hello");
        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);
        assertEquals("notatlv", result.get("00"));
        assertEquals("hello", result.get("01"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void should_allow_same_tag_in_different_nested_branches() {
        String branch1 = buildTlv("00", "val1");
        String branch2 = buildTlv("00", "val2");
        String tlv = buildTlv("01", branch1) + buildTlv("02", branch2);

        Map<String, Object> result = UnreservedTemplateDecoder.decode(tlv);

        Map<String, Object> b1 = (Map<String, Object>) result.get("01");
        Map<String, Object> b2 = (Map<String, Object>) result.get("02");

        assertEquals("val1", b1.get("00"));
        assertEquals("val2", b2.get("00"));
    }
}
