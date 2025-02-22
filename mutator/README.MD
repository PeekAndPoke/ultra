# ultra::mutator

Mutator generates code that helps you to mutate nested immutable data structures. So you will not have code like this:

```
copy(
  copy(
    copy(
       ... etc
    )
  )
)
```

It also has handy helper functions for manipulation immutable collections.

## Examples

Code says more than a thousand words. So let's look at the [Examples](docs/ultra::docs/index.md)!

For more inspiration you can also have a look at the [Test](src/test/kotlin)!

## TODO

### Multiplatform support

Check this out:

https://github.com/Foso/MpApt/blob/master/Readme.md

### Support for different collection libraries

- https://github.com/hrldcpr/pcollections
- https://github.com/usethesource/capsule
- https://github.com/andrewoma/dexx
- https://github.com/GlenKPeterson/Paguro

### fix the problem in the annotation-processor TypeElement.variables when a property has a name collision with java type
 
- like "int", "byte"  etc.
- In these cases the bytecode for the class is "strange" and the annotation processor will not find the variables
- Solution would be to look for specific getters "getInt()", "getByte()" ...

- there a are collisions as well between data class properties and companion objects

- OR: at least write something in the docs (Gotchas section)

### make java.lang.Object mutable

Examples:

- List<Any>
- data class X(val v: Any)
