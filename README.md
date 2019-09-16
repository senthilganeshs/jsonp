![build](https://travis-ci.org/senthilganeshs/jsonp.svg?branch=master)

## Overview

This project contains a simple JSON parser written in JAVA using only pure object oriented concepts. It was originally written for this [blog](https://Senthilganesh.hashnode.dev/yet-another-attempt-to-write-json-parser-cjzyzgxz5001nxls1sdx2cq56).

### Maven Dependency

Want to give a try using the purejson parser, add the following to your pom.xml 

```xml
<dependency>
    <groupId>io.github.senthilganeshs</groupId>
    <artifactId>purejson</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Parser
Parser accepts a json string and returns an Either which indicates presence of a value (ifSuccess) or an exception (ifFailure). 

```javascript
Parser.create().parse(
  "{" +
      "name : \"Senthil\"," + 
      "Employed : true," +
      "favourites : [\"TDD\", \"OOPS\"]" + 
  "}")
    .ifSuccess(v -> v
      .isJSON((k, _v) -> _v
        .isString(__v -> System.out.printf("String Attribute (%s, %s)\n", k, __v))
          .isBool(__v -> System.out.printf("Boolean Attribute(%s, %s)\n", k, __v))
          .isArray(__v -> __v
            .isString(___v -> System.out.printf("\t String Value (%s)", ___v)))))
    .ifFailure(System.out::println);
```

Converting a JSON string to an object (not POJO) can also be done declaratively with the fluent API's of Value object.

```javascript

Parser.create().parse(
  "{" + 
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
```

Objects encapsulate state and state is represented as JSON. Since parsing a JSON is a multi-step process, having a builder for the object construction is preferable. Any object will only expose behavior and in our example, the behavior is `render` to given `OutputStream`. This will help avoid cluttering the code with lots of POJO's.

Sample object construction via builder interface is follows:
```javascript
public static Customer fromJSON (final Value node) {
    Customer.Builder bld = new Customer.Builder.CustomerBuilder();
    node
    .isJSON((k, v) -> {
        k.isString(key -> {
            if (key.equals("name")) {
                v.isString(bld::name)
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
                })
            } else if (key.equals("cargo")) {
                v
                .isJSON((_k, _v) -> {
                    _k
                    .isString(_key -> {
                        if (_key.equals("trackingID")) {
                            _v.isString(bld.cargo()::trackingID)
                        } else if (_key.equals("from")) {
                            _v.isString(bld.cargo()::from)
                        } else if (_key.equals("to")) {
                            _v.isString(bld.cargo()::to)
                        }
                    })
                })
            }
        });
    })
    .isError(System.err::println);
    return bld.build();
}
```


### Generator
Generator accepts a Value returned by Parser and returns a JSON string as output.
```javascript
Generator.create().generate(
  Value.arr(Arrays.asList(
    Value.bool(true),
    Value.string("string"),
    Value.arr(Arrays.asList(
      Value.integer(1),
      Value.integer(2))))));
```

The above code will generate the following json `[true,"string",[1,2]]`

### Supported API's for Value.

|API                                        |Description                                                             |
|-------------------------------------------|------------------------------------------------------------------------|
|`isString`(final Consumer<String> action)    | Executes action supplying string value if the node represents a string |
|`isBool`(final Consumer<Boolean> action)     | Executes action supplying boolean value if the node represents a bool  |
|`isError`(final Consumer<String> action)     | Executes action supplying error message if the node represents an error|
|`isInteger`(final Consumer<Long> action)     | Executes action supplying long value if the node represents a long     |
|`isDouble`(final Consumer<Double> action)    | Executes action supplying double value if the node represents a double |
|`isArray`(final Consumer<Value> action)      | Executes action for every element in the array. Each element can again be used with other value apis to get to primitive values. |
|`isArrayAt`(final int index, final Consumer<Value> action)| Executes action for the value at specified index in the array|
|`isNull`(final Thunk action)                 | Executes provided code in case of null value                            |
|`isJSON`(final BiConsumer<Value, Value> action) | Executes action supplying key and value for each entries in the JSON object. Just like array data type, the value can further be used with other APIs to get to the primitive values. |
|`isJSONKey`(final Value key, final Consumer<Value> action) | Executes action supplying value matching the given key in the JSON object|

### Performance Benchmarks

[fabienrenauds java-json-benchmark](https://github.com/fabienrenaud/java-json-benchmark/pull/29) has results of purejson compared against top json parsers
