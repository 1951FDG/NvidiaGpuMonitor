package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import org.jetbrains.annotations.NonNls;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion" })
public class StringUtilsTest {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtilsTest.class);

    @NonNls
    @SuppressWarnings("HardcodedFileSeparator")
    private final String mExpected = "fQDaoFx\\\\Fi\\\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^";

    @NonNls
    @SuppressWarnings("HardcodedFileSeparator")
    private final String mOriginal = "fQDaoFx\\Fi\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^";

    /**
     * Test of escapeOpenVPN method, of class StringUtils.
     */
    @Test
    public void testEscapeOpenVPN() {
        LOGGER.info("escapeOpenVPN");
        Assert.assertEquals(mExpected, StringUtils.escapeOpenVPN(mOriginal));
    }
}
