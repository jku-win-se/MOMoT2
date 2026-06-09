# 05 - Metamodel Binding

For a Henshin module to be valid, it must be correctly bound to one or more Ecore metamodels.

## nsURI Declaration
The `Module` element must include the `nsURI` of the target metamodels:
```xml
<henshin:Module xmi:version="2.0" ...>
  <imports href="path/to/your.ecore#/"/>
</henshin:Module>
```

## Type References
Every node in your rules must reference an `EClass` from the imported metamodel:
```xml
<nodes name="n1" type="path/to/your.ecore#//YourClass"/>
```

## Dynamic vs. Registered Metamodels
-   **Dynamic**: Henshin loads the `.ecore` file from the path specified.
-   **Registered**: Henshin looks up the metamodel in the Eclipse EMF registry using its `nsURI`.

## Common Errors
-   **Unresolved Proxy**: If the `href` to the Ecore file is wrong, Henshin will throw a `NullPointerException` or `UnresolvableProxyException`.
-   **Namespace Mismatch**: Ensure the `nsURI` in the Henshin file matches the `nsURI` defined in the Ecore file.
