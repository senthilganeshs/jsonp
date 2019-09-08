package io.github.senthilganeshs.parser.json;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.senthilganeshs.parser.json.Generator;
import io.github.senthilganeshs.parser.json.Parser.Value;
import junit.framework.Assert;
import junit.framework.TestCase;

public class GeneratorTest extends TestCase {
    
    public void testEmptyJSON() throws Exception {
        Assert.assertEquals("null", Generator.create().generate(Value.nil()));
    }
    
    public void testStringJSON() throws Exception {
        Assert.assertEquals("\"hello\"", Generator.create().generate(Value.string("hello")));
    }
    
    public void testIntegerJSON() throws Exception {
        Assert.assertEquals("1011", Generator.create().generate(Value.integer(1011)));
    }
    
    public void testDoubleJSON() throws Exception {
        Assert.assertEquals("10.234", Generator.create().generate(Value.number(10.234)));
    }
    
    public void testBoolJSON() throws Exception {
        Assert.assertEquals("false", Generator.create().generate(Value.bool(false)));
    }
    
    public void testArrayJSON() throws Exception {
        Assert.assertEquals(
            "[true,\"string\",[1,2]]",
            Generator.create().generate(
                Value.arr(Arrays.asList(
                    Value.bool(true),
                    Value.string("string"),
                    Value.arr(Arrays.asList(
                        Value.integer(1),
                        Value.integer(2)))))));
    }
    
    public void testObjectJSON() throws Exception {
        
        final Map<Value, Value> map = new LinkedHashMap<>();
        final Map<Value, Value> address = new LinkedHashMap<>();
        final Map<Value, Value> cargo = new LinkedHashMap<>();
        
        address.put(Value.string("flatNumber"), Value.integer(1011));
        address.put(Value.string("building"), Value.string("PureOO"));
        address.put(Value.string("city"), Value.string("Objectvile"));
        
        cargo.put(Value.string("trackingID"), Value.string("AOL1234"));
        cargo.put(Value.string("from"), Value.string("Impericity"));
        cargo.put(Value.string("to"), Value.string("Objectvile"));
        
        map.put(Value.string("name"), Value.string("Senthil"));
        map.put(Value.string("address"), Value.json(address));
        map.put(Value.string("cargo"), Value.json(cargo));
        
        Assert.assertEquals(
             "[{\"name\":\"Senthil\","
            + "\"address\":{"
            + "\"flatNumber\":1011,"
            + "\"building\":\"PureOO\","
            + "\"city\":\"Objectvile\""
            + "},"
            + "\"cargo\":{"
            + "\"trackingID\":\"AOL1234\","
            + "\"from\":\"Impericity\","
            + "\"to\":\"Objectvile\""
            + "}"
            + "},"
            + "12,"
            + "true"
            + "]", 
            
            Generator.create().generate(
                Value.arr(
                    Arrays.asList(
                        Value.json(map), 
                        Value.integer(12), 
                        Value.bool(true)))));
    }
    
    

}
