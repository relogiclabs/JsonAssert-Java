package com.relogiclabs.jschema.test.negative;

import com.relogiclabs.jschema.JsonAssert;
import com.relogiclabs.jschema.JsonSchema;
import com.relogiclabs.jschema.exception.DataTypeValidationException;
import com.relogiclabs.jschema.exception.FunctionValidationException;
import org.junit.jupiter.api.Test;

import static com.relogiclabs.jschema.message.ErrorCode.DTYPMS01;
import static com.relogiclabs.jschema.test.extension.GeneralExtension1.EX_ERRACCESS01;
import static com.relogiclabs.jschema.test.extension.GeneralExtension1.EX_ERRORIP01;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AggregatedTests {
    @Test
    public void When_AggregatedTestWithWrongDataType_ExceptionThrown() {
        var schema =
            """
            %title: "User Profile API Request"
            %version: "1.0.0-basic"
            %schema:
            {
                "user": {
                    "id": @range(1, 10000) #integer,
                    /*username does not allow special characters*/
                    "username": @regex("[a-z_]{3,30}") #string,
                    /*currently only one role is allowed by system*/
                    "role": "user" #string,
                    "isActive": #boolean, //user account current status
                    "registeredAt": #time,
                    "profile": {
                        "firstName": @regex("[A-Za-z ]{3,50}") #string,
                        "lastName": @regex("[A-Za-z ]{3,50}") #string,
                        "dateOfBirth": #date,
                        "age": @range(18, 130) #integer,
                        "email": @email #string,
                        "pictureURL": @url #string,
                        "address": {
                            "street": @length(10, 200) #string,
                            "city": @length(3, 50) #string,
                            "country": @regex("[A-Za-z ]{3,50}") #string
                        } #object #null
                    }
                }
            }
            """;
        var json =
            """
            {
                "user": {
                    "id": "not number",
                    "username": "john doe",
                    "role": "user",
                    "isActive": true,
                    "registeredAt": "2023-09-06T15:10:30.639Z",
                    "profile": {
                        "firstName": "John",
                        "lastName": "Doe",
                        "dateOfBirth": "1993-06-17",
                        "age": 30,
                        "email": "john.doe@example.com",
                        "pictureURL": "https://example.com/picture.jpg",
                        "address": {
                            "street": "123 Some St",
                            "city": "Some town",
                            "country": "Some Country"
                        }
                    }
                }
            }
            """;
        JsonSchema.isValid(schema, json);
        var exception = assertThrows(DataTypeValidationException.class,
            () -> JsonAssert.isValid(schema, json));
        assertEquals(DTYPMS01, exception.getCode());
        exception.printStackTrace();
    }

    @Test
    public void When_ExtendedAggregatedTestWithInvalidAccess_ExceptionThrown() {
        var schema =
            """
            %title: "Extended User Profile Dashboard API Response"
            %version: "2.0.0-extended"
            %import: com.relogiclabs.jschema.test.extension.GeneralExtension1
            %pragma IgnoreUndefinedProperties: true

            %define $post: {
                "id": @range(1, 1000) #integer,
                "title": @length(10, 100) #string,
                "content": @length(30, 1000) #string,
                "tags": $tags
            } #object
            %define $product: {
                "id": @length(2, 10) @regex("[a-z][a-z0-9]+") #string,
                "name": @length(5, 30) #string,
                "brand": @length(5, 30) #string,
                "price": @range(0.1, 1000000),
                "inStock": #boolean,
                "specs": {
                    "cpu": @length(5, 30) #string,
                    "ram": @regex("[0-9]{1,2}GB") #string,
                    "storage": @regex("[0-9]{1,4}GB (SSD|HDD)") #string
                } #object #null
            }
            %define $tags: @length(1, 10) #string*($tag) #array
            %define $tag: @length(3, 20) @regex("[A-Za-z_]+") #string
            %schema:
            {
                "user": {
                    "id": @range(1, 10000) #integer,
                    /*username does not allow special characters*/
                    "username": @regex("[a-z_]{3,30}") #string,
                    "role": @enum("user", "admin") #string &role,
                    "isActive": #boolean, //user account current status
                    "registeredAt": @time("DD-MM-YYYY hh:mm:ss") #string,
                    "dataAccess": @checkDataAccess(&role) #integer,
                    "profile": {
                        "firstName": @regex("[A-Za-z]{3,50}") #string,
                        "lastName": @regex("[A-Za-z]{3,50}") #string,
                        "dateOfBirth": @date("DD-MM-YYYY") #string,
                        "age": @range(18, 128) #integer,
                        "email": @email #string,
                        "pictureURL": @url #string,
                        "address": {
                            "street": @length(10, 200) #string,
                            "city": @length(3, 50) #string,
                            "country": @regex("[A-Za-z ]{3,50}") #string
                        } #object #null,
                        "hobbies": !?
                    },
                    "posts": @length(0, 1000) #object*($post) #array,
                    "preferences": {
                        "theme": @enum("light", "dark") #string,
                        "fontSize": @range(9, 24) #integer,
                        "autoSave": #boolean
                    }
                },
                "products": #object*($product) #array,
                "weather": {
                    "temperature": @range(-50, 60) #integer #float,
                    "isCloudy": #boolean
                }
            }
            """;
        var json =
            """
            {
                "user": {
                    "id": 1234,
                    "username": "johndoe",
                    "role": "user",
                    "isActive": true,
                    "registeredAt": "06-09-2023 15:10:30",
                    "dataAccess": 6,
                    "profile": {
                        "firstName": "John",
                        "lastName": "Doe",
                        "dateOfBirth": "17-06-1993",
                        "age": 30,
                        "email": "john.doe@example.com",
                        "pictureURL": "https://example.com/picture.jpg",
                        "address": {
                            "street": "123 Some St",
                            "city": "Some town",
                            "country": "Some Country"
                        }
                    },
                    "posts": [
                        {
                            "id": 1,
                            "title": "Introduction to JSON",
                            "content": "JSON (JavaScript Object Notation) is a lightweight data interchange format...",
                            "tags": [
                                "JSON",
                                "tutorial",
                                "data"
                            ]
                        },
                        {
                            "id": 2,
                            "title": "Working with JSON in Java",
                            "content": "Java provides great support for working with JSON...",
                            "tags": [
                                "Java",
                                "JSON",
                                "tutorial"
                            ]
                        },
                        {
                            "id": 3,
                            "title": "Introduction to JSON Schema",
                            "content": "A JSON schema defines the structure and data types of JSON objects...",
                            "tags": [
                                "Schema",
                                "JSON",
                                "tutorial"
                            ]
                        }
                    ],
                    "preferences": {
                        "theme": "dark",
                        "fontSize": 14,
                        "autoSave": true
                    }
                },
                "products": [
                    {
                        "id": "p1",
                        "name": "Smartphone",
                        "brand": "TechGiant",
                        "price": 599.99,
                        "inStock": true,
                        "specs": null
                    },
                    {
                        "id": "p2",
                        "name": "Laptop",
                        "brand": "SuperTech",
                        "price": 1299.99,
                        "inStock": false,
                        "specs": {
                            "cpu": "Intel i11",
                            "ram": "11GB",
                            "storage": "11GB SSD"
                        }
                    }
                ],
                "weather": {
                    "temperature": 25.5,
                    "isCloudy": false,
                    "conditions": null
                }
            }
            """;
        JsonSchema.isValid(schema, json);
        var exception = assertThrows(FunctionValidationException.class,
            () -> JsonAssert.isValid(schema, json));
        assertEquals(EX_ERRACCESS01, exception.getCode());
        exception.printStackTrace();
    }

    @Test
    public void When_AggregatedTestWithWrongIP_ExceptionThrown() {
        var schema =
            """
            %title: "Profile Dashboard Request"
            %version: "1.0.0-IPTest"
            %import: com.relogiclabs.jschema.test.extension.GeneralExtension1

            %schema:
            {
                "user": {
                    /*username does not allow special characters*/
                    "username": @regex("[a-z_]{3,30}") #string,
                    /*currently only one role is allowed by system*/
                    "role": "user" #string &role,
                    "dataAccess": @checkDataAccess(&role) #integer,
                    "ipAddress": @checkIPAddress #string,
                    "profile": {
                        "firstName": @regex("[A-Za-z ]{3,50}") #string,
                        "lastName": @regex("[A-Za-z ]{3,50}") #string,
                        "dateOfBirth": #date
                    }
                }
            }
            """;
        var json =
            """
            {
                "user": {
                    "username": "johndoe",
                    "role": "user",
                    "dataAccess": 5,
                    "ipAddress": "0.192.168.1",
                    "profile": {
                        "firstName": "John",
                        "lastName": "Doe",
                        "dateOfBirth": "1911-06-17"
                    }
                }
            }
            """;
        JsonSchema.isValid(schema, json);
        var exception = assertThrows(FunctionValidationException.class,
            () -> JsonAssert.isValid(schema, json));
        assertEquals(EX_ERRORIP01, exception.getCode());
        exception.printStackTrace();
    }

    @Test
    public void When_ExtendedAggregatedScriptTestWithInvalidAccess_ExceptionThrown() {
        var schema =
            """
            %title: "Extended User Profile Dashboard API Response"
            %version: "2.0.0-extended"
            %import: com.relogiclabs.jschema.test.extension.GeneralExtension1

            %pragma DateDataTypeFormat: "DD-MM-YYYY"
            %pragma TimeDataTypeFormat: "DD-MM-YYYY hh:mm:ss"
            %pragma IgnoreUndefinedProperties: true

            %define $post: {
                "id": @range(1, 1000) #integer,
                "title": @length(10, 100) #string,
                "content": @length(30, 1000) #string,
                "tags": $tags
            } #object

            %define $product: {
                "id": @length(2, 10) @regex("[a-z][a-z0-9]+") #string,
                "name": @length(5, 30) #string,
                "brand": @length(5, 30) #string,
                "price": @range(0.1, 1000000),
                "inStock": #boolean,
                "specs": {
                    "cpu": @length(5, 30) #string,
                    "ram": @regex("[0-9]{1,2}GB") #string,
                    "storage": @regex("[0-9]{1,4}GB (SSD|HDD)") #string
                } #object #null
            }

            %define $tags: @length(1, 10) #string*($tag) #array
            %define $tag: @length(3, 20) @regex("[A-Za-z_]+") #string

            %schema:
            {
                "user": {
                    "id": @range(1, 10000) #integer,
                    /*username does not allow special characters*/
                    "username": @regex("[a-z_]{3,30}") #string,
                    "role": @enum("user", "admin") #string &role,
                    "isActive": #boolean, //user account current status
                    "registeredAt": @after("01-01-2010 00:00:00") #time,
                    "dataAccess": @checkAccess(&role) #integer,
                    "ipAddress": @checkIPAddress #string,
                    "profile": {
                        "firstName": @regex("[A-Za-z]{3,50}") #string,
                        "lastName": @regex("[A-Za-z]{3,50}") #string,
                        "dateOfBirth": @before("01-01-2006") #date,
                        "age": @range(18, 128) #integer,
                        "email": @email #string,
                        "pictureURL": @url #string,
                        "address": {
                            "street": @length(10, 200) #string,
                            "city": @length(3, 50) #string,
                            "country": @regex("[A-Za-z ]{3,50}") #string
                        } #object #null,
                        "hobbies": !?
                    },
                    "posts": @length(0, 1000) #object*($post) #array,
                    "preferences": {
                        "theme": @enum("light", "dark") #string,
                        "fontSize": @range(9, 24) #integer,
                        "autoSave": #boolean
                    }
                },
                "products": #object*($product) #array,
                "weather": {
                    "temperature": @range(-50, 60) #integer #float,
                    "isCloudy": #boolean
                }
            }

            %script: {
                future constraint checkAccess(role) {
                    // Auto-unpacking turns the single-value '&role' array into the value itself
                    // 'target' keyword refers to the target JSON value
                    if(role == "user" && target > 5) return fail(
                        "EX_ERRACCESS01", "Data access incompatible with 'user' role",
                        expected("an access at most 5 for 'user' role"),
                        actual("found access " + target + " that is greater than 5"));
                }
            }
            """;
        var json =
            """
            {
                "user": {
                    "id": 1234,
                    "username": "johndoe",
                    "role": "user",
                    "isActive": true,
                    "registeredAt": "06-09-2023 15:10:30",
                    "dataAccess": 6,
                    "ipAddress": "127.0.0.1",
                    "profile": {
                        "firstName": "John",
                        "lastName": "Doe",
                        "dateOfBirth": "17-06-1993",
                        "age": 30,
                        "email": "john.doe@example.com",
                        "pictureURL": "https://example.com/picture.jpg",
                        "address": {
                            "street": "123 Some St",
                            "city": "Some town",
                            "country": "Some Country"
                        }
                    },
                    "posts": [
                        {
                            "id": 1,
                            "title": "Introduction to JSON",
                            "content": "JSON (JavaScript Object Notation) is a lightweight data interchange format...",
                            "tags": [
                                "JSON",
                                "tutorial",
                                "data"
                            ]
                        },
                        {
                            "id": 2,
                            "title": "Working with JSON in Java",
                            "content": "Java provides great support for working with JSON...",
                            "tags": [
                                "Java",
                                "JSON",
                                "tutorial"
                            ]
                        },
                        {
                            "id": 3,
                            "title": "Introduction to JSON Schema",
                            "content": "A JSON schema defines the structure and data types of JSON objects...",
                            "tags": [
                                "Schema",
                                "JSON",
                                "tutorial"
                            ]
                        }
                    ],
                    "preferences": {
                        "theme": "dark",
                        "fontSize": 14,
                        "autoSave": true
                    }
                },
                "products": [
                    {
                        "id": "p1",
                        "name": "Smartphone",
                        "brand": "TechGiant",
                        "price": 1.99,
                        "inStock": true,
                        "specs": null
                    },
                    {
                        "id": "p2",
                        "name": "Laptop",
                        "brand": "SuperTech",
                        "price": 1299.99,
                        "inStock": false,
                        "specs": {
                            "cpu": "Ryzen",
                            "ram": "11GB",
                            "storage": "11GB SSD"
                        }
                    }
                ],
                "weather": {
                    "temperature": 25.5,
                    "isCloudy": true,
                    "conditions": null
                }
            }
            """;
        JsonSchema.isValid(schema, json);
        var exception = assertThrows(FunctionValidationException.class,
            () -> JsonAssert.isValid(schema, json));
        assertEquals("EX_ERRACCESS01", exception.getCode());
        exception.printStackTrace();
    }
}