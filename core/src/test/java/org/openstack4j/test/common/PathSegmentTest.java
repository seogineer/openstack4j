package org.openstack4j.test.common;

import org.openstack4j.openstack.internal.PathSegment;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(suiteName = "Common")
public class PathSegmentTest {

    private static final String[] SAMPLES = {"billing report+100%", "큐", "é", "😀", "a;b&c=d", "{\"|}", "a-b_c.~",
            "OS::Heat::RandomString", "power@system", "a%20b", "a%2b", "100%", "%zz", "%2"};

    public void encodesReservedAndNonAscii() {
        Assert.assertEquals(PathSegment.encode("billing report+100%"), "billing%20report%2B100%25");
        Assert.assertEquals(PathSegment.encode("큐"), "%ED%81%90");
        Assert.assertEquals(PathSegment.encode("é"), "%C3%A9");
        Assert.assertEquals(PathSegment.encode("😀"), "%F0%9F%98%80");
        Assert.assertEquals(PathSegment.encode("a;b&c=d"), "a%3Bb%26c%3Dd");
        Assert.assertEquals(PathSegment.encode("{\"|}"), "%7B%22%7C%7D");
    }

    public void keepsUnreservedColonAndAt() {
        for (String s : new String[] {"a-b_c.~", "OS::Heat::RandomString", "power@system"})
            Assert.assertEquals(PathSegment.encode(s), s);
    }

    public void keepsValidEscapes() {
        Assert.assertEquals(PathSegment.encode("a%20b"), "a%20b");
        Assert.assertEquals(PathSegment.encode("a%2b"), "a%2b");
    }

    public void encodesStrayPercent() {
        Assert.assertEquals(PathSegment.encode("100%"), "100%25");
        Assert.assertEquals(PathSegment.encode("%zz"), "%25zz");
        Assert.assertEquals(PathSegment.encode("%2"), "%252");
    }

    public void isIdempotent() {
        for (String s : SAMPLES)
            Assert.assertEquals(PathSegment.encode(PathSegment.encode(s)), PathSegment.encode(s), s);
    }

    public void rejectsInvalid() {
        Assert.assertThrows(NullPointerException.class, () -> PathSegment.encode(null));
        for (String s : new String[] {"", " ", "a/b", "a?b", "a#b", ".", "..", "%2E%2e", "a%2Fb", "a%2fb", "\uD800"})
            Assert.assertThrows(IllegalArgumentException.class, () -> PathSegment.encode(s));
    }

    public void validateDoesNotEncode() {
        Assert.assertEquals(PathSegment.validate("a b+c"), "a b+c");
        Assert.assertThrows(IllegalArgumentException.class, () -> PathSegment.validate("a/b"));
        Assert.assertThrows(NullPointerException.class, () -> PathSegment.validate(null));
    }
}
