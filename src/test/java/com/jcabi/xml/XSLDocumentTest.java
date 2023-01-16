/*
 * Copyright (c) 2012-2022, jcabi.com
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link XSLDocument}.
 * @since 0.1
 * @checkstyle AbbreviationAsWordInNameCheck (5 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
final class XSLDocumentTest {

    @Test
    void makesXslTransformations() {
        final XSL xsl = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'",
                " version='2.0'>",
                "<xsl:template match='/'><done/>",
                "</xsl:template></xsl:stylesheet>"
            )
        );
        MatcherAssert.assertThat(
            xsl.transform(new XMLDocument("<a/>")),
            XhtmlMatchers.hasXPath("/done")
        );
        MatcherAssert.assertThat(
            xsl.transform(new XMLDocument("<a></a>")),
            XhtmlMatchers.hasXPath("/done ")
        );
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads")
    void makesXslTransformationsInThreads() throws Exception {
        final int loop = 50;
        final int timeout = 30;
        final XSL xsl = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet  ",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform' ",
                " version='2.0' >",
                "<xsl:template match='/'><works/>",
                "</xsl:template> </xsl:stylesheet>"
            )
        );
        final Runnable runnable = () -> MatcherAssert.assertThat(
            xsl.transform(new XMLDocument("<test/>")),
            XhtmlMatchers.hasXPath("/works")
        );
        final ExecutorService service = Executors.newFixedThreadPool(5);
        for (int count = 0; count < loop; count += 1) {
            service.submit(runnable);
        }
        service.shutdown();
        MatcherAssert.assertThat(
            service.awaitTermination(timeout, TimeUnit.SECONDS),
            Matchers.is(true)
        );
        service.shutdownNow();
    }

    @Test
    void transformsWithImports() {
        final XSL xsl = new XSLDocument(
            this.getClass().getResourceAsStream("first.xsl")
        ).with(new ClasspathSources(this.getClass()));
        MatcherAssert.assertThat(
            xsl.transform(new XMLDocument("<simple-test/>")),
            XhtmlMatchers.hasXPath("/result[.=6]")
        );
    }

    @Test
    void transformsWithImportsFromUrl() {
        final XSL xsl = XSLDocument.make(
            this.getClass().getResource("first.xsl")
        ).with(new ClasspathSources(this.getClass()));
        MatcherAssert.assertThat(
            xsl.transform(new XMLDocument("<simple-test/>")),
            XhtmlMatchers.hasXPath("/result[.=6]")
        );
    }

    @Test
    void transformsIntoText() {
        final XSL xsl = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet ",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'  ",
                " version='2.0'><xsl:output method='text'/>",
                "<xsl:template match='/'>hello</xsl:template></xsl:stylesheet>"
            )
        );
        MatcherAssert.assertThat(
            xsl.applyTo(new XMLDocument("<something/>")),
            Matchers.equalTo("hello")
        );
    }

    @Test
    void stripsXml() {
        MatcherAssert.assertThat(
            XSLDocument.STRIP.transform(
                new XMLDocument("<a>   <b/>  </a>")
            ).toString(),
            Matchers.containsString("<a>\n")
        );
    }

    @Test
    void transformsIntoTextWithParams() {
        final XSL xsl = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet   ",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'    ",
                " xmlns:xs='http://www.w3.org/2001/XMLSchema'",
                " version='2.0'><xsl:output method='text'  />",
                "<xsl:param name='boom' />",
                "<xsl:template match='/'>[<xsl:value-of select='$boom'/>]",
                "</xsl:template>   </xsl:stylesheet>"
            )
        );
        MatcherAssert.assertThat(
            xsl.with("boom", "Donny").applyTo(new XMLDocument("<ehe/>")),
            Matchers.equalTo("[Donny]")
        );
    }

    @Test
    void transformsIntoTextWithIntegerParams() {
        final XSL xsl = new XSLDocument(
            StringUtils.join(
                "<xsl:stylesheet     ",
                " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'       ",
                " version='2.0'><xsl:output method='text'    />",
                "<xsl:param name='faa' select='5'/>",
                "<xsl:template match='/'>+<xsl:value-of select='$faa'/>+",
                "</xsl:template>   </xsl:stylesheet>  "
            )
        );
        MatcherAssert.assertThat(
            xsl.with("faa", 1).applyTo(new XMLDocument("<r0/>")),
            Matchers.equalTo("+1+")
        );
    }

    @Test
    void catchesXslErrorMessages() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                RuntimeException.class,
                () -> new XSLDocument(
                    StringUtils.join(
                        " <xsl:stylesheet",
                        "  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'",
                        "  version='2.0'><xsl:template match='/'>",
                        "<xsl:message terminate='yes'>",
                        "<xsl:text>oopsie...</xsl:text>",
                        " </xsl:message></xsl:template></xsl:stylesheet>"
                    ),
                    "foo"
                ).with(new ClasspathSources()).transform(new XMLDocument("<zz1/>"))
            ).getLocalizedMessage(),
            Matchers.allOf(
                Matchers.containsString("Processing terminated by xsl:message"),
                Matchers.containsString("by xsl:message at line 1 in foo")
            )
        );
    }

    @Test
    void printsSystemIdInErrorMessages() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                RuntimeException.class,
                () -> new XSLDocument(
                    StringUtils.join(
                        " <xsl:stylesheet",
                        "  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'",
                        "  version='2.0'><xsl:template match='/'>",
                        "<xsl:value-of select='$xx'/></xsl:template>",
                        "  </xsl:stylesheet>"
                    ),
                    "some-fake-systemId"
                ).with(new ClasspathSources()).transform(new XMLDocument("<ooo/>"))
            ).getLocalizedMessage(),
            Matchers.containsString(
                "Failed to create transformer by net.sf.saxon.TransformerFactoryImpl"
            )
        );
    }

    @Test
    void catchesSaxonWarnings() {
        new XSLDocument(
            StringUtils.join(
                " <xsl:stylesheet",
                "  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'",
                "  version='2.0'>",
                "<xsl:template match='a'></xsl:template>",
                "<xsl:template match='a'></xsl:template>",
                "</xsl:stylesheet>"
            ),
            "https://example.com/hello.xsl"
        ).transform(new XMLDocument("<x><a/></x>"));
    }

    @Test
    void transformsInManyThreads() throws Exception {
        final XSL xsl = new XSLDocument(
            this.getClass().getResourceAsStream("first.xsl")
        ).with(new ClasspathSources());
        final XML xml = new XMLDocument(
            this.getClass().getResourceAsStream("simple.xml")
        );
        final AtomicInteger done = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        final int total = 50;
        final ExecutorService exec = Executors.newFixedThreadPool(total);
        for (int task = 0; task < total; ++task) {
            exec.submit(
                () -> {
                    latch.await();
                    xsl.transform(xml);
                    xsl.transform(xml);
                    done.incrementAndGet();
                    return 0;
                }
            );
        }
        latch.countDown();
        exec.shutdown();
        exec.awaitTermination(1L, TimeUnit.MINUTES);
        MatcherAssert.assertThat(done.get(), Matchers.equalTo(total));
    }

}
