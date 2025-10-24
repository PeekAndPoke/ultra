#!/bin/bash

# ArangoDB container ##########################################################################################################################

ARANGO_CONTAINER="arangodb.local"

echo ""
echo "Checking if ArangoDB docker container '$ARANGO_CONTAINER' is running"
echo "====================================================================================================="
echo ""

checkArangoRunning() {
  curl http://localhost:8529 && echo "yes" || echo "no"
}

result=$(checkArangoRunning)

echo "ArangoDB container running: '${result}'"

if [ "$result" == "yes" ]; then
  echo "Container is not running. Starting it..."
  docker start $ARANGO_CONTAINER

else

  echo "Container does not exist. Creating it..."
  docker run --name "$ARANGO_CONTAINER" -e ARANGO_ROOT_PASSWORD= -p 127.0.0.1:8529:8529 --memory="1024m" -d arangodb:3.12.2

  while ! docker exec -t $ARANGO_CONTAINER arangosh --server.username=root --server.password= --javascript.execute-string="db._databases()" && true; do
    echo "Waiting for database to start"
    sleep 1
  done

fi

docker update --restart unless-stopped "$ARANGO_CONTAINER"

DATABASES=(
  "funktor-demo-dev"
  "funktor-demo-test"
)

for DATABASE in "${DATABASES[@]}"; do
  echo -e "\e[32m""Ensuring database $DATABASE"
  docker exec -t $ARANGO_CONTAINER arangosh --server.username=root --server.password= --javascript.execute-string="db._createDatabase('$DATABASE')" || true
done

