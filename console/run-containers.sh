#!/bin/bash

# get latest version
docker pull mongo

CONTAINER_NAME=mongodb.local
IMAGE=mongo:latest

wait_for_mongo() {
    echo "Waiting for MongoDB inside '$CONTAINER_NAME' to be ready..."

    # Loop until 'ping' returns ok:1
    while true; do
        # Try to ping Mongo and extract the 'ok' field.
        # If mongosh fails (not up yet, auth not ready, etc), capture "0" instead of killing the script.
        local ping_ok
        ping_ok=$(docker exec "$CONTAINER_NAME" mongosh --quiet --eval "db.runCommand({ ping: 1 }).ok" 2>/dev/null || echo "0")

        if [ "$ping_ok" = "1" ]; then
            echo "MongoDB is ready ✅"
            break
        fi
    done
}


# Does a container with this name exist but is stopped?
if docker ps -a --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
    echo "Container '$CONTAINER_NAME' exists. Starting it…"
    docker start "$CONTAINER_NAME"
fi

if ! docker ps --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
    echo "Container '$CONTAINER_NAME' does not exists. Creating it…"

    # install mongodb
    docker run \
      -d \
      --name $CONTAINER_NAME \
      -p 127.0.0.1:27017:27017 \
      -e MONGO_INITDB_ROOT_USERNAME=root \
      -e MONGO_INITDB_ROOT_PASSWORD=root \
      $IMAGE
fi

# waiting for the container to be up and running
wait_for_mongo

# ensure databases
docker exec $CONTAINER_NAME mongosh -u root -p root --eval '
show dbs;
'

docker exec $CONTAINER_NAME mongosh -u root -p root --eval '
db = db.getSiblingDB("funktor-dev");
db.createCollection("__init__");
db.getCollection("__init__").updateOne(
  { _id: "keepalive" },
  { $setOnInsert: { createdAt: new Date() } },
  { upsert: true }
);
'
docker exec $CONTAINER_NAME mongosh -u root -p root --eval '
db = db.getSiblingDB("funktor-test");
db.createCollection("__init__");
db.getCollection("__init__").updateOne(
  { _id: "keepalive" },
  { $setOnInsert: { createdAt: new Date() } },
  { upsert: true }
);
'
