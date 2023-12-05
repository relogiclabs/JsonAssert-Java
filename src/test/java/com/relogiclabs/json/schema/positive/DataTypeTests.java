package com.relogiclabs.json.schema.positive;

import com.relogiclabs.json.schema.JsonAssert;
import org.junit.jupiter.api.Test;

public class DataTypeTests {
    @Test
    public void When_DataTypeMultiple_ValidTrue() {
        var schema = "#string #integer #null ";
        var json = "10";
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypeMultipleInObject_ValidTrue() {
        var schema =
            """
            {
                "key1": #string #null,
                "key2": #string #boolean,
                "key3": #string #number
            }
            """;
        var json =
            """
            {
                "key1": null,
                "key2": false,
                "key3": 500000
            }
            """;
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypeMultipleInArray_ValidTrue() {
        var schema =
            """
            [#integer #null, #integer #null, #integer #boolean]
            """;
        var json =
            """
            [10, null, false]
            """;
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypeMultipleWithNestedFunctionInObject_ValidTrue() {
        var schema =
            """
            {
                "key1": @range*(1, 100) #array #null,
                "key2": @range*(1, 100) #array #null
            }
            """;
        var json =
            """
            {
                "key1": [10, 20, 30],
                "key2": null
            }
            """;
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypeAnyInArray_ValidTrue() {
        var schema =
            """
            #any* #array
            """;
        var json =
            """
            [[], {}, null, false, "test", 0.5, 1E-10, 0]
            """;
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypePrimitiveInArray_ValidTrue() {
        var schema =
            """
            #primitive* #array
            """;
        var json =
            """
            ["test", 0, false, null]
            """;
        JsonAssert.isValid(schema, json);
    }

    @Test
    public void When_DataTypeCompositeInArray_ValidTrue() {
        var schema =
            """
            #composite* #array
            """;
        var json =
            """
            [[], {}, [10, 20], {"key": 100}]
            """;
        JsonAssert.isValid(schema, json);
    }
}