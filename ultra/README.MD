# ULTRA

ULTRA is a set of Kotlin libraries and helpers. Each module below is published on its own.

Mutator lives in [/mutator](../mutator/README.MD), not in this folder.

## Modules

- [cache](https://peekandpoke.io/ultra/cache) - In-memory caches with eviction and refresh
- codegen - TypeScript SDK and model generation from Kotlin API definitions
- [common](common/README.MD) - Multiplatform helpers for collections, hashing, files and utility types
- [datetime](https://peekandpoke.io/ultra/datetime) - Multiplatform date and time types, ranges and timezones
- depot - File storage repositories (file system, encrypting)
- depot-s3 - Depot repository backed by AWS S3
- fixture - Test data generation (Lorem Ipsum and similar)
- html - Multiplatform HTML helpers (image source sets, Cloudinary, DOM events)
- [i18n](https://peekandpoke.io/ultra/i18n) - Internationalisation with message catalogs and locales
- [kontainer](kontainer/README.MD) - Multi stage dependency injection
- [log](log/README.MD) - A lean logging framework that integrates with the Kontainer
- [maths](https://peekandpoke.io/ultra/maths) - Easing, smoothing and stochastic helpers
- [meta](meta/README.MD) - Meta programming tools for annotation processing and code generation
- model - Shared model types (audit log, object keys, messages)
- reflection - Type references and reification of `KType`
- remote - Multiplatform API client and request types
- [security](security/README.MD) - Everyday security features (JWT, CSRF, users)
- semanticui - Semantic UI colors, icons and flags as Kotlin types
- [slumber](slumber/README.MD) - Object serialization with nullability awareness
- [streams](streams/README.MD) - Reactive streams for Kotlin Multiplatform
- [vault](https://peekandpoke.io/ultra/vault) - Repository layer on top of a database

## TODOs

- ( ) Mutator
    - ( ) support better code generation for nested classes
