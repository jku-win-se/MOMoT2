# 06 - MOMoT Integration

MOMoT bridges the gap between Henshin transformations and multi-objective search.

## Configuration (.momot file)

The `transformations` block specifies the Henshin modules:
```java
transformations {
    modules = ["model/myRules.henshin"]
    ignoreUnits = ["helperRule"]
}
```

## Search Variables
MOMoT creates search variables for:
1.  **Unit Selection**: Which rule/unit to apply at each step.
2.  **Parameter Values**: Input values for the selected rule.

## Fitness Functions
Henshin rules change the model state. Fitness functions then evaluate the new model:
```java
fitness {
    objectives {
        "Cost" : minimize { model -> calculateCost(model) }
    }
}
```

## Solution Length
The search algorithm decides how many rules to apply (the solution length). Each rule application is a "gene" in the chromosome.
