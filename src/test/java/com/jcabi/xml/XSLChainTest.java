/*
 * Copyright (c) 2012-2025 Yegor Bugayenko
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.xml;

import com.jcabi.matchers.XhtmlMatchers;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link XSLChain}.
 * @since 0.12
 * @checkstyle AbbreviationAsWordInNameCheck (5 lines)
 */
final class XSLChainTest {

    @Test
    void makesXslTransformations() {
        final XSL first = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'",
                " version='2.0'>",
                "<xsl:template match='/'><done/>",
                "</xsl:template></xsl:stylesheet>"
            )
        );
        final XSL second = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet ",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform' ",
                " version='2.0' >",
                "<xsl:template match='/done'><twice/>",
                "</xsl:template> </xsl:stylesheet>"
            )
        );
        MatcherAssert.assertThat(
            new XSLChain(Arrays.asList(first, second)).transform(
                new XMLDocument("<a/>")
            ),
            XhtmlMatchers.hasXPath("/twice")
        );
    }

}
