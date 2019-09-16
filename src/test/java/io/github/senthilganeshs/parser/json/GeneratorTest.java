package io.github.senthilganeshs.parser.json;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import io.github.senthilganeshs.parser.json.Parser.Value;

public class GeneratorTest {
    
    @Test
    public void testEmptyJSON() throws Exception {
        AssertJUnit.assertEquals("null", Generator.create().generate(Value.nil()));
    }
    
    @Test
    public void testStringJSON() throws Exception {
        AssertJUnit.assertEquals("\"hello\"", Generator.create().generate(Value.string("hello")));
    }
    
    @Test
    public void testIntegerJSON() throws Exception {
        AssertJUnit.assertEquals("1011", Generator.create().generate(Value.integer(1011)));
    }
    
    @Test
    public void testDoubleJSON() throws Exception {
        AssertJUnit.assertEquals("10.234", Generator.create().generate(Value.number(10.234)));
    }
    
    @Test
    public void testBoolJSON() throws Exception {
        AssertJUnit.assertEquals("false", Generator.create().generate(Value.bool(false)));
    }
    
    @Test
    public void testArrayJSON() throws Exception {
        AssertJUnit.assertEquals(
            "[true,\"string\",[1,2]]",
            Generator.create().generate(
                Value.arr(Arrays.asList(
                    Value.bool(true),
                    Value.string("string"),
                    Value.arr(Arrays.asList(
                        Value.integer(1),
                        Value.integer(2)))))));
    }
    
    @Test
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
        
        AssertJUnit.assertEquals(
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
