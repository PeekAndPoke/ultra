# Monko core

MongoDB repositories on Vault: typed queries over KSP-generated property paths, an index DSL, and Kontainer wiring.

Docs: https://peekandpoke.io/ultra/monko/

## Tests

The tests here need no database. The funktor storage specs use `MongoDbConfig.forUnitTests`: a local MongoDB at
`mongodb://root:root@localhost:27017`, database `test` (`ULTRA_TEST_MONGO_DB` selects another).
