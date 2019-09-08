package com.github.senthilganeshs.parser.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.senthilganeshs.parser.json.Parser.Value;

public interface Generator {

    String generate (final Value value);

    public static Generator create() {
        return Simple.INSTANCE;
    }
    
    enum Simple implements Generator {

        INSTANCE
        ;
        
        @Override
        public String generate(final Value value) {
            final StringBuilder bld = new StringBuilder();
            value.isNull(() -> bld.append("null"));
            value.isString(str -> bld.append("\"" + str + "\""));
            value.isInteger(bld::append);
            value.isDouble(bld::append);
            value.isBool(bld::append);
            
            if (!bld.toString().isEmpty())
                return bld.toString();
            
            final List<String> array = new ArrayList<>();
            final boolean isArray = (value instanceof Value.ArrayValue);
            value.isArray(v -> {
                array.add(generate(v));   
            });
            
            if (isArray) {
                bld.append("[");
                array.stream().reduce((lhs, rhs) -> lhs + "," + rhs).ifPresent(bld::append);
                bld.append("]");
                return bld.toString();
            }
            
            final Map<String, String> map = new LinkedHashMap<>();
            final boolean isMap = (value instanceof Value.JSONValue);
            value.isJSON((k, v) -> {
                map.put(generate(k), generate(v));                
            });
            
            if (isMap) {
                bld.append("{");
                map.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .reduce((lhs, rhs) -> lhs + "," + rhs).ifPresent(bld::append);
                bld.append("}");
            }            
            return bld.toString();
        }   
    }   
}