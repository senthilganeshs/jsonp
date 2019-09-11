package io.github.senthilganeshs.parser.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.senthilganeshs.object.java.lang.Either;
import io.github.senthilganeshs.parser.json.Parser.Value;

public interface StreamParser {
    
    Value consume (final ByteReader reader);
    
    
    public static Parser streamParser() {
         return new Parser2();
    }
    
    final static class Parser2 implements Parser {
        Parser2 () {
        }

        @Override
        public Either<Value, JSONParserException> parse(final String document) {
            
            if (document == null || document.isEmpty()) {
                return Either.fail(new JSONParserException("empty json"));
            }
            
            final ByteReader reader = new ByteReader.StringByteReader(document);
            return 
                Either.succ(new StreamParser() {
                    @Override
                    public Value consume(final ByteReader reader) {
                        final AtomicReference<Value> val = new AtomicReference<>();
                        reader.skipAll(Character::isWhitespace); //skip leading whitespace.
                        reader.read(i->true, ch -> val.set(all(ch).consume(reader)));
                        return val.get();
                    }
                }.consume(reader));
        }
    }
    
    interface ByteReader {
        
        ByteReader read(final Predicate<Character> cond, final Consumer<Character> action);
        
        ByteReader skipOne (final Predicate<Character> cond);
        
        ByteReader skipAll (final Predicate<Character> cond);
        
        final static class StringByteReader implements ByteReader {

            private final String document;
            private int cursor;

            StringByteReader(final String document) {
                this.document = document;
                this.cursor = 0;
            }
            
            @Override
            public ByteReader skipOne (final Predicate<Character> cond) {
                if (cond.test(document.charAt(cursor))) {
                    cursor ++;                    
                }
                return this;
            }
            
            @Override
            public ByteReader skipAll (final Predicate<Character> cond) {
                while (cursor < document.length() - 1) {
                    if (cond.test(document.charAt(cursor))) {
                        cursor ++;                    
                    } else {
                        break;
                    }
                }
                return this;
            }
            
            @Override
            public ByteReader read(final Predicate<Character> cond, final Consumer<Character> action) {
                while (cursor < document.length()) {
                    if (cond.test(document.charAt(cursor))) {
                        ((Consumer<Character>) ch -> {
                            cursor ++;
                            action.accept(ch);
                        }).accept(document.charAt(cursor));
                    } else {
                        break;
                    }
                }
                return this;
            }
        }
    }
    
    static StreamParser all (final char ch) {
        if (ch =='{') {
            return JSON_PARSER;
        } else if (ch =='[') {
            return ARRAY_PARSER;
        } else {
            return primitive(ch);
        }
    }
    
   
    
    static StreamParser NIL_PARSER = new NilParser();
   
    static StreamParser INTEGER_PARSER = new IntegerParser();
    
    static StreamParser NUMBER_PARSER = new NumberParser();
    
    static StreamParser STRING_PARSER = new StringParser();
    
    static StreamParser BOOLEAN_PARSER = new BooleanParser();
    
    static StreamParser ARRAY_PARSER = new ArrayParser();
    
    static StreamParser JSON_PARSER = new JSONParser();
    
    
    final static class IntOrNumberParser implements StreamParser {
        
        private final char first;

        IntOrNumberParser (final char first) {
            this.first = first;
        }
        
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder bld = new StringBuilder();
            bld.append(first);
            final AtomicBoolean isNumber = new AtomicBoolean(false);
            reader.read(
                (ch -> Character.isDigit(ch) || ch =='.' || ch == 'e' || ch == '+'),
                ch -> {
                    if (Character.isDigit(ch)) {
                        bld.append(ch);
                    } else if (ch == '.' || ch == 'e' ||ch == '+' ) {
                        isNumber.set(true);
                        bld.append(ch);
                    }
                });
            if (isNumber.get()) {
                return Value.number(Double.parseDouble(bld.toString()));
            } else 
                return Value.integer(Long.parseLong(bld.toString()));
        }        
    }
    
    static StreamParser primitive (final char ch) {
        if (ch =='"') {
            return STRING_PARSER;
        } else if (ch == 't' || ch == 'f') {
            return BOOLEAN_PARSER;
        } else if (ch == 'n') {
            return NIL_PARSER;
        } else 
            return new IntOrNumberParser(ch); //should not lose first digit
    }
    
    static Value primitive (final char ch, final ByteReader reader) {
        return primitive (ch).consume(reader);
    }
    
    final static class ArrayParser implements StreamParser {

        @Override
        public Value consume(final ByteReader reader) {
            final List<Value> values = new ArrayList<>();
            reader.skipAll(Character::isWhitespace);
            
            reader.read(ch -> ch != ']', ch -> {
                if (Character.isWhitespace(ch)) {
                    //skip whitespace
                } else if (ch == ',') {
                    //skip comma.
                } else if (ch == '{') {                    
                    values.add(JSON_PARSER.consume(reader));
                } else if (ch == '[') {
                    values.add(ARRAY_PARSER.consume(reader));                    
                } else {
                    values.add(primitive(ch, reader));
                }
            });
            
            reader.skipOne(ch -> ch == ']');
            
            return Value.arr(values);
        }     
    }
    
    final static class JSONParser implements StreamParser {

        @Override
        public Value consume(final ByteReader reader) {
            final Map<Value, Value> map = new LinkedHashMap<>();
            final AtomicBoolean isKey = new AtomicBoolean(true);
            final AtomicReference<Value> key = new AtomicReference<>();
            final AtomicReference<Value> value = new AtomicReference<>();
            
            reader.skipAll(Character::isWhitespace);
            
            reader.read(ch -> ch != '}', ch -> {
               if (Character.isWhitespace(ch)) {
                   //skip whitespace
               } else if (ch == ':') {
                   isKey.set(false);
               } else if (ch == ',') {
                   isKey.set(true);
                   map.put(key.get(), value.get());
               } else if (ch == '"') {
                   if(isKey.get()) {
                       key.set(primitive(ch, reader));
                   } else {
                       value.set(primitive(ch, reader));
                   }
               } else if (ch =='[') {
                   if (!isKey.get()) {
                       value.set(ARRAY_PARSER.consume(reader));
                   } else {
                       value.set(Value.err("array detected in place of key. Allowed types are [string]"));
                   }
               } else if (ch == '{') {
                   if (!isKey.get()) {
                       value.set(JSON_PARSER.consume(reader)); //FIXME: can avoid recursion by explicitly managing the stack.
                   } else {
                       //error. json at key position.
                       key.set(Value.err("json detected in place of key. Allowed types are [string]"));
                   }
               } else {
                   if (isKey.get()) {
                       key.set(Value.err("key cannot be non string type."));
                   } else {
                       value.set(primitive(ch, reader));
                   }
               }                  
            });
            
            map.put(key.get(), value.get());
            
            reader.skipOne(ch -> ch == '}'); //skip closing brace.
            
            return Value.json(map);
        }
    }
    
    final static class StringParser implements StreamParser {
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder value = new StringBuilder();
            reader.read(ch -> ch != '"', value::append);
            reader.skipOne(ch -> ch == '"'); //skip one double quote
            return Value.string(value.toString());
        }
    }
    
    final static class IntegerParser implements StreamParser {
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder longValue = new StringBuilder();
            reader.read(Character::isDigit, longValue::append);
            return Value.integer(Long.parseLong(longValue.toString()));
        }        
    }
    
    final static class BooleanParser implements StreamParser {
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder boolValue = new StringBuilder();
            final AtomicInteger index = new AtomicInteger(-1);
          
            final StringBuilder expected = new StringBuilder();
            
            reader.read(ch -> {
                if (expected.length() == 0) {
                    if (ch == 'r') {
                        boolValue.append('t');
                        expected.append("rue");
                    }else if (ch == 'a') {
                        boolValue.append('f');
                        expected.append("alse");
                    }
                }
                index.incrementAndGet();

                if ((index.get() < expected.length() && (ch == expected.charAt(index.get())))) {
                    return true;
                }
                return false;
            }, boolValue::append);
            return Value.bool(Boolean.parseBoolean(boolValue.toString()));
        }
    }
    
    final static class NumberParser implements StreamParser {
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder numberValue = new StringBuilder();
            reader.read(
                ch ->(Character.isDigit(ch) || 
                              ch == '.' ||
                              ch == 'e' ||
                              ch == '+' ), 
                numberValue::append);
            
            return Value.number(Double.parseDouble(numberValue.toString()));
        }       
    }
    
    final static class NilParser implements StreamParser {
        @Override
        public Value consume(final ByteReader reader) {
            final StringBuilder bld = new StringBuilder();
            final char[] allowed = new char[] {'u','l','l'};
            final AtomicInteger index = new AtomicInteger();
            
            reader.read(ch -> index.get() < 3 && allowed[index.getAndIncrement()] == ch, 
                bld::append);
            if (index.get() != 2) 
                return Value.err("null is expected");
            return Value.nil();
        }
    }
}