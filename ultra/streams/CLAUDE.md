# ultra/streams

- **A configurable operator takes its config as a stream** (maintainer, 2026-10-29; ULTRA-00271): settings that may
  change over time are `Stream<X>` parameters, and the constant overload delegates through `steady(x)`. The config
  stream is listened to only while someone listens to the operator, and the KDoc says when a change takes effect.
  A mode (`distinct(strict)`) stays a constant.
