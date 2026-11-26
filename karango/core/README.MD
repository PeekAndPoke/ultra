# ENV

Running ArangoDB in a docker container for the tests

```bash
sudo docker run --name "io.peekandpoke.demos.karango" -e ARANGO_ROOT_PASSWORD= -p 8529:8529 -d arangodb
```

The tests connect to `localhost:8529` as `root` without a password and use the database `_system`, or the one named
in `ULTRA_TEST_ARANGO_DB` (`ArangoDbConfig.forUnitTests`). In a worktree, `console/wt-gradle.sh` sets it.
