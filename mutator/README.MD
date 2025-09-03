# Mutator

Mutator generates code that helps you to mutate nested immutable data structures without hassle.

Constructs like this are no longer necessary:

```
copy(
  copy(
    copy(
      copy(
        ... etc
      )
    )
  )
)
```

It also has handy helper functions for manipulation immutable collections.

Mutator uses KSP and works for Kotlin multi-platform projects.

## Examples

Code says more than a thousand words. So let's look at the [Examples](core/docs/mutator::docs/index.md)!

For more inspiration you can also have a look at the [Tests](core/src/jvmTest/kotlin)!

## TODO

### Vetoing Plugins

Plugins must be able to veto the generation of mutators.

This is useful for data classes like date time class (f.e. MpDateTime), where
it is not desirable to have a mutator for the year, month, day, hour, minute, second, millisecond, etc. directly.
Instead, the user should use the usual methods to mutate the date time class..

### Support for different collection libraries

- https://github.com/hrldcpr/pcollections
- https://github.com/usethesource/capsule
- https://github.com/andrewoma/dexx
- https://github.com/GlenKPeterson/Paguro

### Mutability for non data class objects

- should we do this?
