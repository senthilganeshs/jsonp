package parser.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import object.java.lang.Either;

public interface Parser {

    Either<Value, JSONParserException> parse(final String document);

    public static Parser create() {
        return new Simple();
    }

    final static class Simple implements Parser {

        @Override
        public Either<Value, JSONParserException> parse(final String document) {
            return Parser.ALL.parse(document);
        }
    }

    public static final Parser ALL = empty(
        trimmed(nil(bool(string(integer(number(array(json(fail("unknown Parser"))))))))));

    public static Parser json(final Parser other) {
        return new JSONParser(other);
    }

    public static Parser array(final Parser other) {
        return new ArrayParser(other);
    }

    public static Parser integer(final Parser other) {
        return new IntParser(other);
    }

    public static Parser number(final Parser other) {
        return new NumberParser(other);
    }

    public static Parser string(final Parser other) {
        return new StringParser(other);
    }

    public static Parser nil(final Parser other) {
        return new NilParser(other);
    }

    public static Parser bool(final Parser other) {
        return new BoolParser(other);
    }

    public static Parser fail(final JSONParserException e) {
        return token -> Either.fail(e);
    }

    public static Parser fail(final String msg) {
        return fail(new JSONParserException(msg));
    }

    public static Parser empty(final Parser other) {
        return token -> {
            if (token == null || token.isEmpty()) {
                return Either.fail(new JSONParserException("empty document"));
            }
            return other.parse(token);
        };
    }

    public static Parser trimmed(final Parser other) {
        return token -> other.parse(token.trim());
    }

    final static class NilParser implements Parser {
        private final Parser other;

        NilParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(final String token) {
            if (token.equals("null")) {
                return Either.succ(Value.nil());
            }
            return other.parse(token);
        }
    }

    final static class BoolParser implements Parser {
        private final Parser other;

        BoolParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(final String token) {
            if (token.equals("true") || token.equals("false")) {
                return Either.succ(Value.bool(Boolean.parseBoolean(token)));
            }
            return other.parse(token);
        }
    }

    final static class IntParser implements Parser {
        private final Parser other;

        IntParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(String document) {
            try {
                return Either.succ(Value.integer(Integer.parseInt(document)));
            } catch (NumberFormatException e) {
                return other.parse(document);
            }
        }
    }

    final static class NumberParser implements Parser {
        private final Parser other;

        NumberParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(String document) {
            try {
                return Either.succ(Value.number(Double.parseDouble(document)));
            } catch (NumberFormatException e) {
                return other.parse(document);
            }
        }
    }

    final static class StringParser implements Parser {

        private final Parser other;

        StringParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(final String token) {
            if (token.startsWith("\"")) {
                final int start = token.indexOf('"');
                final int end = token.lastIndexOf('"');
                if (start == 0 && end == token.length() - 1)
                    return Either.succ(Value.string(token.substring(start, end + 1)));
                else {
                    return Either.succ(Value.err(String.format("Parser error at %d\n%s\n%s\n%s",
                        end + 1, token, dashes(end + 1),
                        "Expecting 'EOF', '}', ':', ']', got " + token.substring(end + 1))));
                }
            }
            return other.parse(token);
        }

        private final String dashes(final int pos) {
            final char dash = '-';
            final StringBuilder bld = new StringBuilder();
            for (int i = 0; i < pos; i++) {
                bld.append(dash);
            }
            bld.append("^");
            return bld.toString();
        }
    }

    final static class ArrayParser implements Parser {

        private final Parser other;

        ArrayParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(final String token) {
            if (token.charAt(0) == '[' && token.charAt(token.length() - 1) == ']') {
                final List<Value> values = new ArrayList<>();
                final String inner = token.substring(1, token.length() - 1);
                int nb = 0;
                int nbr = 0;
                int s = 0;
                for (int i = 0; i < inner.length(); i++) {
                    if (Character.isWhitespace(inner.charAt(i))) {
                        continue; // skip whitespace.
                    } else if (inner.charAt(i) == '[') {
                        nb++;
                    } else if (inner.charAt(i) == ']') {
                        nb--;
                    } else if (inner.charAt(i) == '{') {
                        nbr++;
                    } else if (inner.charAt(i) == '}') {
                        nbr--;
                    } else if (inner.charAt(i) == ',') {
                        if (nb == 0 && nbr == 0) {
                            values.add(asValue(inner.substring(s, i)));
                            s = i + 1;
                        }
                    }

                    if (i == inner.length() - 1) {
                        if (nbr == 0 && nb == 0) {
                            Parser.ALL.parse(inner.substring(s, i + 1).trim()).ifSuccess(
                                values::add);
                        }
                    }
                }
                return Either.succ(Value.arr(values));
            }

            return other.parse(token);
        }

        private Value asValue(final String value) {
            final AtomicReference<Value> result = new AtomicReference<>();
            final AtomicReference<JSONParserException> excep = new AtomicReference<>();
            Parser.ALL.parse(value).ifSuccess(result::set).ifFailure(excep::set);
            if (excep.get() != null) {
                return Value.err(excep.get().getLocalizedMessage());
            }
            return result.get();
        }
    }

    final static class JSONParser implements Parser {

        private final Parser other;

        JSONParser(final Parser other) {
            this.other = other;
        }

        @Override
        public Either<Value, JSONParserException> parse(final String token) {
            if (token.startsWith("{") && token.endsWith("}")) {
                final Map<Value, Value> map = new HashMap<>();
                final String inner = token.substring(1, token.length() - 1);
                int nbr = 0;
                int nb = 0;
                int vs = 0;
                int ks = 0;
                String key = "";
                for (int i = 0; i < inner.length(); i++) {
                    if (Character.isWhitespace(inner.charAt(i))) {
                        // skip reference
                    } else if (inner.charAt(i) == '{') {
                        nbr++;
                    } else if (inner.charAt(i) == '}') {
                        nbr--;
                    } else if (inner.charAt(i) == '[') {
                        nb++;
                    } else if (inner.charAt(i) == ']') {
                        nb--;
                    } else if (inner.charAt(i) == ',') {
                        if (nbr == 0 && nb == 0) {
                            ks = i + 1;
                            final String _key = key;
                            Parser.ALL.parse(inner.substring(vs, i).trim()).ifSuccess(
                                value -> map.put(Value.string(_key), value));
                        }
                    } else if (inner.charAt(i) == ':') {
                        if (nbr == 0 && nb == 0) {
                            key = inner.substring(ks, i).trim();
                            vs = i + 1;
                        }
                    }
                    if (i == inner.length() - 1) {
                        if (nbr == 0 && nb == 0) {
                            final String _key = key;
                            Parser.ALL.parse(inner.substring(vs, i + 1).trim()).ifSuccess(
                                value -> map.put(Value.string(_key), value));
                        }
                    }
                }
                return Either.succ(Value.json(map));
            }
            return other.parse(token);
        }

    }

    interface Thunk {
        void code();
    }

    interface Value {
        default Value isString(final Consumer<String> action) {
            return this;
        }

        default Value isBool(final Consumer<Boolean> action) {
            return this;
        }

        default Value isError(final Consumer<String> action) {
            return this;
        }

        default Value isInteger(final Consumer<Integer> action) {
            return this;
        }

        default Value isDouble(final Consumer<Double> action) {
            return this;
        }

        default Value isArray(final Consumer<Value> action) {
            return this;
        }

        default Value isNull(final Thunk action) {
            return this;
        }

        default Value isJSON(final BiConsumer<Value, Value> action) {
            return this;
        }

        public static Value nil() {
            return new NilValue();
        }

        public static Value string(final String value) {
            return new StringValue(value);
        }

        public static Value bool(final boolean value) {
            return new BoolValue(value);
        }

        public static Value integer(final int value) {
            return new IntValue(value);
        }

        public static Value number(final double value) {
            return new DoubleValue(value);
        }

        public static Value err(final String value) {
            return new ErrorValue(value);
        }

        public static Value arr(final List<Value> values) {
            return new ArrayValue(values);
        }

        public static Value json(final Map<Value, Value> values) {
            return new JSONValue(values);
        }

        final static class IntValue implements Value {
            private final int value;

            IntValue(final int value) {
                this.value = value;
            }

            @Override
            public Value isInteger(final Consumer<Integer> action) {
                action.accept(value);
                return this;
            }
        }

        final static class DoubleValue implements Value {
            private final double value;

            DoubleValue(final double value) {
                this.value = value;
            }

            @Override
            public Value isDouble(final Consumer<Double> action) {
                action.accept(value);
                return this;
            }
        }

        final static class JSONValue implements Value {

            private final Map<Value, Value> map;

            JSONValue(final Map<Value, Value> map) {
                this.map = map;
            }

            @Override
            public Value isJSON(final BiConsumer<Value, Value> action) {
                map.entrySet().forEach(e -> action.accept(e.getKey(), e.getValue()));
                return this;
            }
        }

        final static class ArrayValue implements Value {
            private final List<Value> values;

            ArrayValue(final List<Value> values) {
                this.values = values;
            }

            @Override
            public Value isArray(final Consumer<Value> action) {
                values.forEach(action);
                return this;
            }
        }

        final static class NilValue implements Value {
            @Override
            public Value isNull(final Thunk action) {
                action.code();
                return this;
            }
        }

        final static class BoolValue implements Value {
            private final boolean value;

            BoolValue(final boolean value) {
                this.value = value;
            }

            @Override
            public Value isBool(final Consumer<Boolean> action) {
                action.accept(value);
                return this;
            }
        }

        final static class StringValue implements Value {
            private final String value;

            StringValue(final String value) {
                this.value = withoutQuotes(value);
            }

            private String withoutQuotes(final String value) {
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                return value;
            }

            @Override
            public Value isString(final Consumer<String> action) {
                action.accept(value);
                return this;
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }

            @Override
            public boolean equals(final Object other) {
                if (other == null)
                    return false;
                if (other == this)
                    return true;
                if (other instanceof StringValue) {
                    return ((StringValue) other).value.equals(value);
                }
                return false;
            }
        }

        final static class ErrorValue implements Value {
            private final String msg;

            ErrorValue(final String msg) {
                this.msg = msg;
            }

            @Override
            public Value isError(final Consumer<String> action) {
                action.accept(msg);
                return this;
            }
        }
    }

    final static class JSONParserException extends Exception {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        JSONParserException(final Throwable e) {
            super(e);
        }

        JSONParserException(final String msg) {
            super(msg);
        }
    }
}