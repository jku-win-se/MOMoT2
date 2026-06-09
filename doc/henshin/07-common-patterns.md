# 07 - Common Patterns

Reusable templates for common transformation tasks.

## 1. Create Node with Attribute
```xml
<units xsi:type="henshin:Rule" name="createItem">
  <parameters name="itemName" kind="IN"/>
  <rhs name="Rhs">
    <nodes name="newItem" type="...#//Item">
      <attributes value="itemName" feature="...#//Item/name"/>
    </nodes>
  </rhs>
</units>
```

## 2. Delete Node (Safe — with NAC guard)
The item is deleted only when it has no children. The NAC prevents the rule from firing if a
`children` edge exists from `toDelete` to any other node.
```xml
<units xsi:type="henshin:Rule" name="deleteItem">
  <lhs name="Lhs">
    <nodes xmi:id="_lhs_item" name="toDelete" type="...#//Item"/>
    <!-- NAC: do NOT fire if toDelete has any children -->
    <formula xsi:type="henshin:Not">
      <child xsi:type="henshin:NestedCondition">
        <conclusion>
          <nodes xmi:id="_nac_parent" outgoing="_nac_edge">
            <type href="...#//Item"/>
          </nodes>
          <nodes xmi:id="_nac_child" incoming="_nac_edge">
            <type href="...#//Item"/>
          </nodes>
          <edges xmi:id="_nac_edge" source="_nac_parent" target="_nac_child">
            <type href="...#//Item/children"/>
          </edges>
        </conclusion>
        <!-- Bind the LHS node into the NAC pattern -->
        <mappings origin="_lhs_item" image="_nac_parent"/>
      </child>
    </formula>
  </lhs>
  <rhs name="Rhs"/>
  <!-- No mappings: _lhs_item has no RHS counterpart → it is DELETED -->
</units>
```

## 3. Move/Re-parent
The item is preserved (mapped LHS→RHS) but the containment edge is deleted from `source`
and a new containment edge is created to `target`. Both containers are also preserved.
```xml
<units xsi:type="henshin:Rule" name="moveItem">
  <parameters name="itemId" kind="IN"/>
  <lhs name="Lhs">
    <nodes xmi:id="_lhs_src"  name="source" type="...#//Container" outgoing="_lhs_edge"/>
    <nodes xmi:id="_lhs_item" name="item"   type="...#//Item"      incoming="_lhs_edge">
      <attributes value="itemId"><type href="...#//Item/id"/></attributes>
    </nodes>
    <nodes xmi:id="_lhs_tgt"  name="target" type="...#//Container"/>
    <!-- old containment edge — not mapped to RHS, so it is DELETED -->
    <edges xmi:id="_lhs_edge" source="_lhs_src" target="_lhs_item">
      <type href="...#//Container/items"/>
    </edges>
  </lhs>
  <rhs name="Rhs">
    <nodes xmi:id="_rhs_src"  name="source" type="...#//Container"/>
    <nodes xmi:id="_rhs_item" name="item"   type="...#//Item">
      <attributes value="itemId"><type href="...#//Item/id"/></attributes>
    </nodes>
    <nodes xmi:id="_rhs_tgt"  name="target" type="...#//Container" outgoing="_rhs_edge"/>
    <!-- new containment edge — not in LHS, so it is CREATED -->
    <edges xmi:id="_rhs_edge" source="_rhs_tgt" target="_rhs_item">
      <type href="...#//Container/items"/>
    </edges>
  </rhs>
  <!-- All three nodes are preserved -->
  <mappings origin="_lhs_src"  image="_rhs_src"/>
  <mappings origin="_lhs_item" image="_rhs_item"/>
  <mappings origin="_lhs_tgt"  image="_rhs_tgt"/>
</units>
```

## 4. Pass Value Between Sequential Rules (VAR)
`VAR` parameters are internal to a `SequentialUnit`. They are set by one rule and consumed by
the next. MOMoT never exposes them as search variables.

The SequentialUnit declares no parameters itself; the VAR is shared by name across sub-rules.
```xml
<!-- Composite unit: read from source, write to target -->
<units xsi:type="henshin:SequentialUnit" name="copyValue">
  <subUnits href="#readSourceValue"/>
  <subUnits href="#writeTargetValue"/>
</units>

<!-- Rule 1: Read the value from the source node into a VAR -->
<units xsi:type="henshin:Rule" xmi:id="readSourceValue" name="readSourceValue">
  <parameters name="sourceId"    kind="IN"/>
  <parameters name="capturedVal" kind="VAR"/>  <!-- written here, consumed by next rule -->
  <lhs name="Lhs">
    <nodes xmi:id="_r_lhs" type="...#//Node">
      <attributes value="sourceId"><type href="...#//Node/id"/></attributes>
    </nodes>
  </lhs>
  <rhs name="Rhs">
    <nodes xmi:id="_r_rhs" type="...#//Node">
      <attributes value="sourceId"><type href="...#//Node/id"/></attributes>
      <!-- capturedVal is set to the current value attribute of this node -->
      <attributes value="capturedVal"><type href="...#//Node/value"/></attributes>
    </nodes>
  </rhs>
  <mappings origin="_r_lhs" image="_r_rhs"/>
</units>

<!-- Rule 2: Write the VAR value onto a target node -->
<units xsi:type="henshin:Rule" xmi:id="writeTargetValue" name="writeTargetValue">
  <parameters name="targetId"    kind="IN"/>
  <parameters name="capturedVal" kind="VAR"/>  <!-- same name: receives value from rule 1 -->
  <lhs name="Lhs">
    <nodes xmi:id="_w_lhs" type="...#//Node">
      <attributes value="targetId"><type href="...#//Node/id"/></attributes>
    </nodes>
  </lhs>
  <rhs name="Rhs">
    <nodes xmi:id="_w_rhs" type="...#//Node">
      <attributes value="targetId"><type href="...#//Node/id"/></attributes>
      <!-- overwrite the value attribute with what rule 1 captured -->
      <attributes value="capturedVal"><type href="...#//Node/value"/></attributes>
    </nodes>
  </rhs>
  <mappings origin="_w_lhs" image="_w_rhs"/>
</units>
```
> **MOMoT note**: `sourceId` and `targetId` (kind `IN`) become search variables. `capturedVal`
> (kind `VAR`) is invisible to the search. Use `ignoreParameters = ["sourceId", "targetId"]`
> in `.momot` if you want to exclude them too.

## 5. Counter Increment
```xml
<lhs name="Lhs">
  <nodes name="c" type="...#//Counter">
    <attributes value="val" feature="...#//Counter/value"/>
  </nodes>
</lhs>
<rhs name="Rhs">
  <nodes name="c" type="...#//Counter">
    <attributes value="val + 1" feature="...#//Counter/value"/>
  </nodes>
</rhs>
```
*(Note: Henshin supports basic expressions in attribute values)*.
