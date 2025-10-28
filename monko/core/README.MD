# ENV

Running ArangoDB in docker container

```bash
sudo docker run --name "de.peekandpoke.demos.karango" -e ARANGO_ROOT_PASSWORD= -p 8529:8529 -d arangodb 
sleep 30
sudo docker exec -t de.peekandpoke.demos.karango arangosh --server.username=root --server.password= --javascript.execute-string="db._createDatabase('kotlindev')"

```

# TODO

( ) Collection property configuration like indexed etc.

( ) remove dependency to me.eugeniomarletti.kotlin.processing

( ) Ref for List<T>

( ) Journal

( ) E2E-Tests
( ) DbEntityCollection ... storing, finding etc...
( ) Ref ... storing Ref and join queries
( ) Timestamped behaviour
( ) UserRecord behaviour

# DONE

(X) Statement have NO type parameter as they return nothing

(X) figure out a solution for the problem of 'name' vs 'identifier'

(X) Property path should have a 'parent'-chain

(X) return type issues with this here:

    - The type of 'result' currently is: Cursor<Author>  
    - but MUST Be Cursor<List<Author>>  

    val result = db.query {
        val let = LET("let") {
            listOf(
                Author("first", "last"),
                Author("first_2", "last_2")
            )
        }

        RETURN (let)
    }

