# ultra::mutator

## TODO

- ( ) TESTS: all java.time.* must be treated like primitive types
- ( ) create FallbackRenderer that accepts everything, like primitive renderer (with comment "sorry no better way known" message in generated code)


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
