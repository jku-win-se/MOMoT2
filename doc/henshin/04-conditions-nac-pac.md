# 04 - Application Conditions

Application conditions allow you to refine when a rule matches beyond simple graph patterns.

## NAC (Negative Application Condition)
Defines a pattern that **must NOT** exist for the rule to apply.
-   Common use: "Create a node only if it doesn't already exist" or "Delete a node only if it has no children".

## PAC (Positive Application Condition)
Defines a pattern that **must** exist, but is not part of the transformation itself (i.e., it's not deleted or modified).

## Attribute Conditions
You can add logic to parameters and attributes:
```xml
<attributeConditions conditionText="x > 10" />
```

## XMI Structure
Conditions are nested within the `lhs` of a rule:
```xml
<lhs name="Lhs">
  <formula xsi:type="henshin:Not">
    <child xsi:type="henshin:NestedCondition">
      <conclusion name="Nac">
        <nodes name="forbiddenNode" type="..."/>
      </conclusion>
    </child>
  </formula>
</lhs>
```
