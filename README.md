# Farm Tracks

Track your stock with confidence.

## Local HTTPS (Desktop + Mobile)

This project is configured to run HTTPS locally by default.

1. Generate local certificates (PEM for SWAG + PKCS12 for Spring Boot):

```bash
./deploy/local-proxy/generate-local-cert.sh /home/birch/appdata/farmtracks/local-certs 192.168.0.99
```

Use your machine's LAN IP as the second argument so mobile clients can validate the certificate SAN.

2. Debug directly with Spring Boot over HTTPS:

- Use `Debug Application (stable)` in VS Code.
- Open `https://<your-lan-ip>:8443` from desktop/mobile.

3. Run through local SWAG (HTTPS edge):

```bash
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml up -d --force-recreate farmtracks swag
```

- Open `https://<your-lan-ip>:44302` from desktop/mobile.

Notes:

- For mobile, trust/install the generated cert if your browser/OS does not trust it automatically.
- HTTP is not required for normal local development flows.

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
