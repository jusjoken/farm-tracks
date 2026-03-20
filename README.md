# Farm Tracks

Track your stock with confidence.

## Deploying to Production

Use the project script to run the validated production build flow.

### Build production JAR only

```
./build-prod.sh
```

Output artifact:

```
target/farm-tracks-1.0-SNAPSHOT.jar
```

### Build and deploy to local Docker SWAG stack

```
./build-prod.sh --deploy
```

What this does:

- Builds frontend with the required workaround profile.
- Packages the production JAR.
- Copies JAR to `/home/birch/appdata/farmtracks/app.jar`.
- Recreates Docker services using `docker-compose.yml` and `docker-compose.local-swag.yml`.

### Optional: run the built JAR directly

```
java -jar target/farm-tracks-1.0-SNAPSHOT.jar
```

## Deploying using Docker

If you need to deploy to the local SWAG stack from an already-built JAR, run

```
cp -f target/farm-tracks-1.0-SNAPSHOT.jar /home/birch/appdata/farmtracks/app.jar
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml up -d --force-recreate farmtracks swag
```

To view logs:

```
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml logs --tail=200 farmtracks
```
