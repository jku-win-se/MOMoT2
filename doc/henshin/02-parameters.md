# 02 - Parameters

Parameters allow rules to be dynamic and are the primary way MOMoT interacts with Henshin rules.

## Parameter Types

-   **IN**: Provided by the caller (or MOMoT search). Required for the rule to execute.
-   **OUT**: Set by the rule during execution. Can be used by subsequent units.
-   **INOUT**: Both provided and potentially modified.
-   **VAR**: Internal variable used for binding values between sub-units in a `SequentialUnit`. Not exposed to MOMoT.

## MOMoT Integration

MOMoT automatically detects `IN` and `INOUT` parameters and treats them as search variables.

### ignoreParameters
In your `.momot` file, you can hide parameters from the search:
```java
search {
    ignoreParameters = ["paramName"]
}
```

### unitParameters
You can also manually define how parameters map to search variables:
```java
search {
    unitParameters = [
        "ruleName.paramName" : ["Value1", "Value2"]
    ]
}
```

## Pitfalls
-   **Unbound IN parameters**: If MOMoT doesn't provide a value for an `IN` parameter, the rule will likely fail to match.
