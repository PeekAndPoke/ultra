```ascii-art
                                                             
                          _/    _/  _/_/_/      _/_/    _/_/_/_/  _/_/_/_/_/   
                         _/  _/    _/    _/  _/    _/  _/            _/        
                        _/_/      _/_/_/    _/_/_/_/  _/_/_/        _/         
                       _/  _/    _/    _/  _/    _/  _/            _/          
                      _/    _/  _/    _/  _/    _/  _/            _/           
                                                             
```

# K.R.A.F.T. = Kotlin Rapid App Framework & Toolkit

Build single page apps with pure Kotlin.

Kraft bridges the gap between Kotlin code and the DOM by using Javascript VDom-engines like
the very light-weight [Preact](https://preactjs.com).

It uses [kotlinx/html](https://github.com/Kotlin/kotlinx.html) for markup-rendering, so no template engine is needed.

It comes with a handy DSL for the [FomanticUI Css Framework](https://fomantic-ui.com/).

# Check out the examples

## Hello-World example

[Try it out](https://raw.githack.com/PeekAndPoke/ultra/master/docs/kraft/examples/hello-world/index.html) |
[See the code](./examples/hello-world) |
Or run it locally
> ./gradlew -t --parallel :examples:hello-world:run

## SemanticUi / FomanticUi examples

Writing beautiful web-apps without a css framework is very hard. Kraft comes with a handy DSL
from the [FomanticUI Css Framework](https://fomantic-ui.com/).

Check out the examples, with lots of example code waiting to be copied and pasted.

[Try it out](https://raw.githack.com/PeekAndPoke/ultra/master/docs/kraft/examples/fomanticui/index.html) |
[See the code](./examples/fomanticui) |
Or run it locally
> ./gradlew -t --parallel :examples:fomanticui:run

## Addon examples

The addon examples show the usage of other Javascript libraries within a Kraft-App.

[Try it out](https://raw.githack.com/PeekAndPoke/ultra/master/docs/kraft/examples/addons/index.html) |
[See the code](./examples/addons) |
Or run it locally
> ./gradlew -t --parallel :examples:addons:run

## Example projects / repos

[Hello World Demo](https://github.com/PeekAndPoke/kraft-example-helloworld)  
A minimal project.

[Router Demo](https://github.com/PeekAndPoke/kraft-example-router)  
A minimal example of how to write an app with multiple pages.

[Remote Demo](https://github.com/PeekAndPoke/kraft-example-remote)  
An example project that shows how to consume rest apis and display the results.

# Motivations for this project

## Why Kraft ?

Why Kraft when there already is [KotlinJS/React](https://github.com/JetBrains/create-react-kotlin-app)?

This project takes a different approach. It does not try to wrap around React.
Instead, it uses very light weight VDom-engines like [Preact](https://preactjs.com).

This reduces the Javascript dependencies to a minimum. In fact, Preact has zero dependencies of its own!
This makes the whole project much more stable in the long run.

## Why FomanticUI ?

This project currently focuses on FomanticUI (previously SemanticUI) as a css framework.
FomanticUI / SemanticUI uses a very "semantic" approach to writing styled markup. And as it turns out,
this perfectly maps to a Kotlin DSL:

```kotlin
ui.segment {
    ui.button {
        +"Click me"
    }
}
```

Working with **kotlinx/html** happily removes the necessity for a template engine! _Great!_  
**Kraft::Fomantic** adds another typesafe and IDE-assisted layer on top of that. _Sweet!_    
