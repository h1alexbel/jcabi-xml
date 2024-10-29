/*
 * Copyright (c) 2012-2024, jcabi.com
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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.cactoos.io.ResourceOf;
import org.cactoos.io.TeeInput;
import org.cactoos.scalar.LengthOf;
import org.cactoos.text.FormattedText;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test case for {@link XMLDocument}.
 *
 * @since 0.1
 * @checkstyle AbbreviationAsWordInNameCheck (20 lines)
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.DoNotUseThreads"})
final class XMLDocumentTest {

    @Test
    void findsDocumentNodesWithXpath() {
        final XML doc = new XMLDocument(
            "<r><a>\u0443\u0440\u0430!</a><a>B</a></r>"
        );
        MatcherAssert.assertThat(
            doc.xpath("//a/text()"),
            Matchers.hasSize(2)
        );
        MatcherAssert.assertThat(
            doc.xpath("/r/a/text()"),
            Matchers.hasItem("\u0443\u0440\u0430!")
        );
    }

    @Test
    void findWithXpathListEqualsToJavaUtilList() {
        MatcherAssert.assertThat(
            new XMLDocument(
                "<does><not><matter/></not></does>"
            ).xpath("//missing/text()"),
            new IsEqual<>(
                Collections.<String>emptyList()
            )
        );
        MatcherAssert.assertThat(
            new XMLDocument(
                "<root><item>first</item><item>second</item></root>"
            ).xpath("//root/item[1]/text()"),
            new IsEqual<>(
                Collections.singletonList("first")
            )
        );
        MatcherAssert.assertThat(
            new XMLDocument(
                "<root><item>abc</item><item>def</item></root>"
            ).xpath("/root/item/text()"),
            new IsEqual<>(
                Arrays.asList("abc", "def")
            )
        );
    }

    @Test
    void findsWithXpathAndNamespaces() {
        final XML doc = new XMLDocument(
            "<html xmlns='http://www.w3.org/1999/xhtml'><div>\u0443\u0440\u0430!</div></html>"
        );
        MatcherAssert.assertThat(
            doc.nodes("/xhtml:html/xhtml:div"),
            Matchers.hasSize(1)
        );
        MatcherAssert.assertThat(
            doc.nodes("//xhtml:div[.='\u0443\u0440\u0430!']"),
            Matchers.hasSize(1)
        );
    }

    @Test
    void findsWithXpathWithCustomNamespace() throws Exception {
        final File file = Files.createTempDirectory("")
            .resolve("x.xml").toFile();
        new LengthOf(
            new TeeInput(
                "<a xmlns='urn:foo'><b>\u0433!</b></a>",
                file
            )
        ).value();
        final XML doc = new XMLDocument(file).registerNs("f", "urn:foo");
        MatcherAssert.assertThat(
            doc.nodes("/f:a/f:b[.='\u0433!']"),
            Matchers.hasSize(1)
        );
        MatcherAssert.assertThat(
            doc.xpath("//f:b/text()").get(0),
            Matchers.equalTo("\u0433!")
        );
    }

    @Test
    void findsDocumentNodesWithXpathAndReturnsThem() throws Exception {
        final XML doc = new XMLDocument(
            new ByteArrayInputStream(
                "<root><a><x>1</x></a><a><x>2</x></a></root>".getBytes()
            )
        );
        MatcherAssert.assertThat(
            doc.nodes("//a"),
            Matchers.hasSize(2)
        );
        MatcherAssert.assertThat(
            doc.nodes("/root/a").get(0).xpath("x/text()").get(0),
            Matchers.equalTo("1")
        );
    }

    @Test
    void convertsItselfToXml() {
        final XML doc = new XMLDocument("<hello><a/></hello>");
        MatcherAssert.assertThat(
            doc.toString(),
            Matchers.hasToString(XhtmlMatchers.hasXPath("/hello/a"))
        );
    }

    @Test
    void retrievesDomNode() throws Exception {
        final XML doc = new XMLDocument(
            this.getClass().getResource("simple.xml")
        );
        MatcherAssert.assertThat(
            doc.nodes("/root/simple").get(0).node().getNodeName(),
            Matchers.equalTo("simple")
        );
        MatcherAssert.assertThat(
            doc.nodes("//simple").get(0).node().getNodeType(),
            Matchers.equalTo(Node.ELEMENT_NODE)
        );
    }

    @Test
    void throwsCustomExceptionWhenXpathNotFound() {
        try {
            new XMLDocument("<root/>").xpath("/absent-node/text()").get(0);
            MatcherAssert.assertThat("exception expected here", false);
        } catch (final IndexOutOfBoundsException ex) {
            MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.allOf(
                    Matchers.containsString("/absent-node/text("),
                    Matchers.containsString("<root/")
                )
            );
        }
    }

    @Test
    void throwsWhenXpathQueryIsBroken() {
        try {
            new XMLDocument("<root-99/>").xpath("/*/hello()");
            MatcherAssert.assertThat("exception expected", false);
        } catch (final IllegalArgumentException ex) {
            MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("XPathFactoryImpl")
            );
        }
    }

    @Test
    void preservesProcessingInstructions() {
        MatcherAssert.assertThat(
            new XMLDocument("<?xml version='1.0'?><?x test?><a/>"),
            Matchers.hasToString(Matchers.containsString("<?x test?>"))
        );
    }

    @Test
    void preservesDomStructureWhenXpath() {
        final XML doc = new XMLDocument(
            "<root><item1/><item2/><item3/></root>"
        );
        final XML item = doc.nodes("//root/item2").get(0);
        MatcherAssert.assertThat(
            item.nodes("..").get(0).xpath("name()").get(0),
            Matchers.equalTo("root")
        );
    }

    @Test
    void printsWithAndWithoutXmlHeader() {
        final XML doc = new XMLDocument("<hey/>");
        MatcherAssert.assertThat(
            doc,
            Matchers.hasToString(Matchers.startsWith("<?xml "))
        );
        MatcherAssert.assertThat(
            doc.nodes("/*").get(0),
            Matchers.hasToString(Matchers.startsWith("<hey"))
        );
    }

    @Test
    void parsesInMultipleThreads() throws Exception {
        final int timeout = 10;
        final int loop = 100;
        final Runnable runnable = () -> MatcherAssert.assertThat(
            new XMLDocument("<root><hey/></root>"),
            XhtmlMatchers.hasXPath("/root/hey")
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
    void xpathInMultipleThreads() throws Exception {
        final int timeout = 30;
        final int repeat = 1000;
        final int loop = 50;
        final XML xml = new XMLDocument(
            String.format(
                "<a><b>test text</b><c>%s</c></a>",
                StringUtils.repeat(
                    "<beta>some text \u20ac</beta> ",
                    repeat
                )
            )
        );
        final Runnable runnable = () -> {
            MatcherAssert.assertThat(
                xml.xpath("/a/b/text()").get(0),
                Matchers.equalTo("test text")
            );
            MatcherAssert.assertThat(
                xml.nodes("/a").get(0).nodes("c"),
                Matchers.iterableWithSize(1)
            );
        };
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
    void printsInMultipleThreads() throws Exception {
        final int repeat = 1000;
        final int loop = 50;
        final XML xml = new XMLDocument(
            String.format(
                "<root><data>%s</data></root>",
                StringUtils.repeat(
                    "<alpha>some text \u20ac</alpha> ",
                    repeat
                )
            )
        );
        final AtomicInteger done = new AtomicInteger();
        final Runnable runnable = () -> {
            MatcherAssert.assertThat(
                xml.toString(),
                XhtmlMatchers.hasXPath("/root/data/alpha")
            );
            done.incrementAndGet();
        };
        final ExecutorService service = Executors.newFixedThreadPool(5);
        for (int count = 0; count < loop; count += 1) {
            service.submit(runnable);
        }
        service.shutdown();
        while (true) {
            if (done.get() == loop) {
                break;
            }
            if (service.awaitTermination(1L, TimeUnit.MILLISECONDS)) {
                break;
            }
        }
        MatcherAssert.assertThat(
            service.awaitTermination(1L, TimeUnit.SECONDS),
            Matchers.is(true)
        );
        service.shutdownNow();
    }

    @Test
    void performsXpathCalculations() {
        final XML xml = new XMLDocument("<x><a/><a/><a/></x>");
        MatcherAssert.assertThat(
            xml.xpath("count(//x/a)"),
            Matchers.iterableWithSize(1)
        );
        MatcherAssert.assertThat(
            xml.xpath("count(//a)").get(0),
            Matchers.equalTo("3")
        );
    }

    @Test
    void buildsDomNode() {
        final XML doc = new XMLDocument("<?xml version='1.0'?><f/>");
        MatcherAssert.assertThat(
            doc.node(),
            Matchers.instanceOf(Document.class)
        );
        MatcherAssert.assertThat(
            doc.nodes("/f").get(0).node(),
            Matchers.instanceOf(Element.class)
        );
    }

    @Test
    void comparesToAnotherDocument() {
        MatcherAssert.assertThat(
            new XMLDocument("<hi>\n<dude>  </dude></hi>"),
            Matchers.equalTo(new XMLDocument("<hi><dude>  </dude></hi>"))
        );
        MatcherAssert.assertThat(
            new XMLDocument("<hi><man></man></hi>"),
            Matchers.not(
                Matchers.equalTo(new XMLDocument("<hi><man>  </man></hi>"))
            )
        );
    }

    @Test
    @Disabled
    void comparesDocumentsWithDifferentIndentations() {
        // @checkstyle MethodBodyCommentsCheck (4 lines)
        // @todo #1:90min Implement comparison of XML documents with different indentations.
        //  The current implementation of XMLDocument does not ignore different indentations
        //  when comparing two XML documents. We need to implement a comparison that ignores
        //  different indentations. Don't forget to remove the @Disabled annotation from this test.
        MatcherAssert.assertThat(
            "Different indentations should be ignored",
            new XMLDocument("<program>\n <indentation/>\n</program>"),
            Matchers.equalTo(
                new XMLDocument("<program>\n  <indentation/>\n</program>\n")
            )
        );
    }

    @Test
    void preservesXmlNamespaces() {
        final String xml = "<a xmlns='http://www.w3.org/1999/xhtml'><b/></a>";
        MatcherAssert.assertThat(
            new XMLDocument(xml),
            XhtmlMatchers.hasXPath("/xhtml:a/xhtml:b")
        );
    }

    @Test
    void preservesImmutability() {
        final XML xml = new XMLDocument("<r1><a/></r1>");
        final Node node = xml.nodes("/r1/a").get(0).node();
        node.appendChild(node.getOwnerDocument().createElement("h9"));
        MatcherAssert.assertThat(
            xml,
            XhtmlMatchers.hasXPath("/r1/a[not(h9)]")
        );
    }

    @Test
    void appliesXpathToClonedNode() {
        final XML xml = new XMLDocument("<t6><z9 a='433'/></t6>");
        final XML root = xml.nodes("/t6").get(0);
        MatcherAssert.assertThat(
            root.xpath("//z9/@a").get(0),
            Matchers.equalTo("433")
        );
    }

    @Test
    void extractsNodesFromPom() throws Exception {
        final XML xml = new XMLDocument(new ResourceOf("com/jcabi/xml/small-pom.xml").stream());
        final List<XML> properties = xml
            .registerNs("ns1", "http://maven.apache.org/POM/4.0.0")
            .nodes("/ns1:project/ns1:properties/*");
        MatcherAssert.assertThat(
            new FormattedText(
                "%s should contain 2 property nodes\n but was %s\n in %s",
                xml,
                properties.size(),
                properties
            ).toString(),
            properties,
            Matchers.hasSize(2)
        );
    }

    @Test
    void stripsUnnecessaryWhiteSpacesWhileParsing() {
        MatcherAssert.assertThat(
            "Two XML documents are equal to each other",
            new XMLDocument("<x><y>hello</y></x>"),
            Matchers.equalTo(
                new XMLDocument(
                    "<x>  \n\n\n      <y>hello</y  >  \n    </x >"
                )
            )
        );
    }

}
