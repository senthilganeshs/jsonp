package io.github.senthilganeshs.parser.json;

import java.io.IOException;
import java.io.OutputStream;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.github.senthilganeshs.parser.json.Parser.JSONParserException;
import io.github.senthilganeshs.parser.json.Parser.Value;
import io.github.senthilganeshs.parser.json.ParserTest.Customer.Address.Builder.AddressBuilder;
import io.github.senthilganeshs.parser.json.ParserTest.Customer.Cargo.Builder.CargoBuilder;

public class ParserTest {
    
    @DataProvider(name = "parser",parallel = true)
    public Object[] inputs() {
        return new Parser[] {Parser.simple(), Parser.streamParser()};
    }
    
    @Test(dataProvider = "parser")
    public void testParserException(final Parser parser) throws Exception {
        parser.parse((String)null)
        .ifFailure(ParserTest::assertJSONParserException);
    }


    @Test(dataProvider = "parser")
    public void testNilValue(final Parser parser) throws Exception {
        parser.parse("null")
        .ifSuccess(v -> v
            .isNull(() -> Assert.assertTrue(true)));
    }

    @Test(dataProvider = "parser")
    public void testBoolValue(final Parser parser) throws Exception {
        parser.parse("true")
        .ifSuccess(v -> v
            .isBool(Assert::assertTrue));
        parser.parse("false")
        .ifSuccess(v -> v
            .isBool(Assert::assertFalse));
    }


    @Test(dataProvider = "parser")
    public void testStringValue(final Parser parser) throws Exception {
        parser.parse("\"hello\"")
        .ifSuccess(v -> v
            .isString(actual -> Assert.assertEquals("hello", actual))
            .isError(System.out::println))
        .ifFailure(e -> Assert.assertTrue(false, e.getLocalizedMessage()));
    }

    @Test(dataProvider = "parser")
    public void testIntegerValue(final Parser parser) throws Exception {
        parser.parse("1011")
        .ifSuccess(v -> v
            .isInteger(i -> Assert.assertEquals(1011, i.intValue())))
        .ifFailure(e -> Assert.assertFalse(true, "Expected int. Reason " + e.getLocalizedMessage()));
    }


    @Test(dataProvider = "parser")
    public void testArrayValue(final Parser parser) throws Exception {
        parser.parse("[null,\"string\",true]")
        .ifSuccess(v -> v
            .isArray(_v -> 
            _v.isString(str -> System.out.println(str + " is a string"))
            .isBool(bool -> System.out.println(bool + " is a boolean"))
            .isNull(() -> System.out.println("value is null"))
            .isError(System.out::println)))
        .ifFailure(System.out::println);
    }


    @Test(dataProvider = "parser")
    public void testNestedArrayValue(final Parser parser) throws Exception {
        parser.parse("[null,[true,false],\"string\"]")
        .ifSuccess(v -> v
            .isArray(_v -> _v
                .isNull(() -> System.out.println("null value"))
                .isArray(__v -> __v
                    .isBool(bool -> System.out.println(bool + " is a boolean")))
                .isString(str -> System.out.println(str + " is a string"))))
        .ifFailure(System.out::println);

        parser.parse("[[true, false],[null, false]]")
        .ifSuccess(v -> v
            .isArray(_v -> _v
                .isArray(__v -> __v
                    .isBool(bool -> System.out.println("boolean(" + bool + ")"))
                    .isNull(()-> System.out.println("empty")))));
    }

    
    @Test(dataProvider = "parser")
    public void testInvalidKey(final Parser parser) throws Exception {
        parser.parse("{"
            + "1011 : true"
            + "}").ifSuccess(v -> v
                .isJSON((k, _v) -> 
                k.isError(err -> Assert.assertTrue(
                    err.equals("key cannot be non string type."), 
                    "key should be string type"))))
        .ifFailure(System.out::println);
    }

    @Test(dataProvider = "parser")
    public void testJSONValue(final Parser parser) throws Exception { 
        parser.parse(
            "{" +
                "\"name\" : \"Senthil\"," + 
                "\"Employed\" : true," +
                "\"favourites\" : [\"TDD\", \"OOPS\"]" + 
            "}")
        .ifSuccess(v -> v
            .isJSON((k, _v) -> _v
                .isString(__v -> System.out.printf("String Attribute (%s, %s)\n", k, __v))
                .isBool(__v -> System.out.printf("Boolean Attribute(%s, %s)\n", k, __v))
                .isArray(__v -> __v
                    .isString(___v -> System.out.printf("\t String Value (%s)", ___v))
                    .isBool(___v -> System.out.printf("\t Boolean Value (%s)", ___v)))))
        .ifFailure(System.out::println);
    }

    @Test(dataProvider = "parser")
    public void testJSONArray(final Parser parser) throws Exception {
        parser.parse("["
            + "{ \"name\" :\"Arun\","
            + "  \"age\"  : 10"
            + "}, {"
            + "\"name\"   : \"Akash\","
            + "\"age\"    : 14"
            + "}"
            + "]")
        .ifSuccess(v -> v
            .isArray(_v -> _v
                .isJSON((key, __v) -> 
                key.isString(_k -> {
                    if (_k.equals("name")) {
                        __v.isString(System.out::println);
                    } else if (_k.equals("age")) {
                        __v.isInteger(System.out::println);
                    }
                }))))
        .ifFailure(System.out::println);
    }


    @Test(dataProvider = "parser")
    public void testJSONToObject(final Parser parser) throws Exception {
        parser.parse("{" + 
            "  \"name\" : \"Senthil\"," + 
            "  \"address\" : {" + 
            "    \"flatNumber\" : \"#1011\"," + 
            "    \"building\"   : \"PureOO\"," + 
            "    \"city\"       : \"Objectvile\"" + 
            "  }," + 
            "  \"cargo\"   : {" + 
            "    \"trackingID\" : \"AOL123\"," + 
            "    \"from\"       : \"Impericity\"," + 
            "    \"to\"         : \"Objectvile\"" + 
            "  }" + 
            "}")
        .ifSuccess(v -> Customer.fromJSON(v).render(System.out))
        .ifFailure(System.out::println);
    }

    @FunctionalInterface
    interface Renderable {
        void render (final OutputStream out);
    }

    interface Customer extends Renderable {

        public static Customer fromJSON (final Value node) {
            Customer.Builder bld = new Customer.Builder.CustomerBuilder();
            node
            .isJSON((k, v) -> {
                k.isString(key -> {
                    if (key.equals("name")) {
                        v.isString(bld::name).isError(System.out::println);
                    } else if (key.equals("address")) {
                        v.isJSON((_k, _v) -> {
                            _k
                            .isString(_key -> {
                                if (_key.equals("flatNumber")) {
                                    _v.isString(bld.address()::flatNumber);
                                } else if (_key.equals("building")) {
                                    _v.isString(bld.address()::building);
                                } else if (_key.equals("city")) {
                                    _v.isString(bld.address()::city);
                                }
                            })
                            .isError(System.out::println);
                        })
                        .isError(System.out::println);
                    } else if (key.equals("cargo")) {
                        v
                        .isJSON((_k, _v) -> {
                            _k
                            .isString(_key -> {
                                if (_key.equals("trackingID")) {
                                    _v.isString(bld.cargo()::trackingID)
                                    .isError(System.out::println);
                                } else if (_key.equals("from")) {
                                    _v.isString(bld.cargo()::from)
                                    .isError(System.out::println);
                                } else if (_key.equals("to")) {
                                    _v.isString(bld.cargo()::to)
                                    .isError(System.out::println);
                                }
                            })
                            .isError(System.out::println);
                        })
                        .isError(System.out::println);
                    }
                });
            })
            .isError(System.out::println);
            return bld.build();
        }

        interface Address extends Renderable {

            final static class JSONAddress implements Address {

                private final String city;
                private final String building;
                private final String flatNumber;

                JSONAddress (final String flatNumber, final String building, final String city) {
                    this.flatNumber = flatNumber;
                    this.building = building;
                    this.city = city;
                }

                @Override
                public void render(final OutputStream out) {
                    try {
                        out.write(String.format("Flat %s, Building %s, City = %s", flatNumber, building, city).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            interface Builder {
                Builder flatNumber (final String flatNumber);
                Builder building (final String building);
                Builder city (final String city);
                Address build();

                final static class AddressBuilder implements Builder {

                    private String flatNumber;
                    private String building;
                    private String city;

                    @Override
                    public Builder flatNumber(String flatNumber) {
                        this.flatNumber = flatNumber;
                        return this;
                    }

                    @Override
                    public Builder building(String building) {
                        this.building = building;
                        return this;
                    }

                    @Override
                    public Builder city(String city) {
                        this.city = city;
                        return this;
                    }

                    @Override
                    public Address build() {
                        return new JSONAddress(flatNumber, building, city);
                    }

                }
            }
        }

        interface Cargo extends Renderable {

            final static class JSONCargo implements Cargo {

                private final String to;
                private final String from;
                private final String trackingID;

                JSONCargo (final String trackingID, final String from, final String to) {
                    this.trackingID = trackingID;
                    this.from = from;
                    this.to = to;
                }

                @Override
                public void render(OutputStream out) {
                    try {
                        out.write(String.format("Cargo Tracking ID %s, From Location %s, To Location %s\n", trackingID, from, to).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            interface Builder {
                Builder trackingID (final String trackingID);
                Builder from (final String from);
                Builder to (final String to);

                Cargo build();

                final static class CargoBuilder implements Builder {

                    private String trackingID;
                    private String from;
                    private String to;

                    @Override
                    public Builder trackingID(String trackingID) {
                        this.trackingID = trackingID;
                        return this;
                    }

                    @Override
                    public Builder from(String from) {
                        this.from = from;
                        return this;
                    }

                    @Override
                    public Builder to(String to) {
                        this.to = to;
                        return this;
                    }

                    @Override
                    public Cargo build() {
                        return new JSONCargo(trackingID, from, to);
                    }
                }
            }
        }

        final static class JSONCustomer implements Customer {

            private final Cargo cargo;
            private final Address address;
            private final String name;

            JSONCustomer (final String name, final Address address, final Cargo cargo) {
                this.name = name;
                this.address = address;
                this.cargo = cargo;
            }

            @Override
            public void render(OutputStream out) {
                try {
                    out.write(String.format("Customer Name : %s\n", name).getBytes());
                    out.write("\n Address Details\n ".getBytes());
                    address.render(out);
                    out.write("\n Cargo Details \n".getBytes());
                    cargo.render(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        interface Builder {
            Builder name (final String name);
            Address.Builder address();
            Cargo.Builder cargo();

            Customer build();

            final static class CustomerBuilder implements Builder {

                private String name;
                private AddressBuilder addressBuilder;
                private CargoBuilder cargoBuilder;

                CustomerBuilder() {
                    this.addressBuilder = new Address.Builder.AddressBuilder(); 
                    this.cargoBuilder = new Cargo.Builder.CargoBuilder();
                }

                @Override
                public Builder name(String name) {
                    this.name = name;
                    return this;
                }

                @Override
                public io.github.senthilganeshs.parser.json.ParserTest.Customer.Address.Builder address() {
                    return addressBuilder;
                }

                @Override
                public io.github.senthilganeshs.parser.json.ParserTest.Customer.Cargo.Builder cargo() {
                    return cargoBuilder;
                }

                @Override
                public Customer build() {
                    return new JSONCustomer(name, addressBuilder.build(), cargoBuilder.build());
                }

            }
        }		
    }

    private static void assertJSONParserException(final Exception e) {
        AssertJUnit.assertTrue(e.getClass().equals(JSONParserException.class));
    }
}