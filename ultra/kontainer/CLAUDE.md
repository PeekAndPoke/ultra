# ultra/kontainer

- **Scoping:** a singleton that transitively injects a `dynamic` becomes `SemiDynamic`, i.e. one instance per request.
  Shared-state services must take zero constructor dependencies — and "constructor runs once at boot" is usually false
  for realms, which are rebuilt per request.
