package io.github.senthilganeshs.parser.json;

import junit.framework.TestCase;

public class StreamParserTest extends TestCase {

    public void testStreamParserPrimitives() throws Exception {
        StreamParser.streamParser()
        .parse("\"test\"")
        .ifSuccess(v -> v
            .isString(System.out::println));
    }
}
