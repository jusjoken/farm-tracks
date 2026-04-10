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

Recommended staged flow:

1. Build and test JAR directly in VS Code.
2. Deploy the same built JAR to local Portainer-managed container and test again.
3. Deploy the same tested JAR to remote host.

### Build production JAR only

```
./build-prod.sh
```

Output artifact:

```
target/farm-tracks-1.0-SNAPSHOT.jar
```

### Build and deploy to local Portainer-style container

```
./build-prod.sh --deploy
```

If you already built/tested the JAR and only want to redeploy locally:

```bash
./build-prod.sh --skip-build --deploy
```

What this does:

- Builds frontend with the required workaround profile.
- Packages the production JAR.
- Copies JAR to `/home/birch/appdata/farmtracks/app.jar`.
- Restarts the local app container (`deploy/local/deploy-local-jar.sh`).

Configure local container/JAR path:

```bash
cp deploy/local/local-deploy.conf.example deploy/local/local-deploy.conf
# edit deploy/local/local-deploy.conf
```

If startup validation times out, tune `LOCAL_STARTUP_TIMEOUT_SEC` and
`LOCAL_STARTUP_LOG_REGEX` in `deploy/local/local-deploy.conf`.

### Optional: run the built JAR directly

```
java -jar target/farm-tracks-1.0-SNAPSHOT.jar
```

### Deploy tested JAR to remote host

Preferred: use a dedicated remote deploy config file.

```bash
cp deploy/remote/remote-deploy.conf.example deploy/remote/remote-deploy.conf
# edit deploy/remote/remote-deploy.conf for your server
./deploy/remote/deploy-remote-jar.sh
```

Portainer-only remote stack (no `docker-compose.yml` on server):

- Set `REMOTE_APP_CONTAINER` to the running app container name on remote host.

This copies the JAR to the remote host and runs `docker restart <container>`.

Optional and recommended for rollback safety: publish the exact JAR to GitHub
Releases before remote deployment.

1. Ensure `gh` is installed and authenticated (`gh auth login`).
2. Set release repo in `deploy/remote/remote-deploy.conf`:

```bash
GITHUB_RELEASE_REPO=owner/repo
```

3. Deploy with pre-publish enabled:

```bash
./deploy/remote/deploy-remote-jar.sh --publish-release --release-tag v1.2.3
```

If `--release-tag` is omitted, the default tag follows the app's displayed
version scheme: `v<app.version.major>.<git total commit count>`.
`GITHUB_RELEASE_TARGET` is optional; if unset, GitHub uses the default branch.
If the release publish step fails, remote deployment is aborted.

If the remote user cannot access Docker directly, set:

```bash
export REMOTE_DOCKER_CMD="sudo -n docker"
```

Or put `REMOTE_DOCKER_CMD="sudo -n docker"` in
`deploy/remote/remote-deploy.conf`.

If you want to use a custom config path:

```bash
./deploy/remote/deploy-remote-jar.sh --config /path/to/remote-deploy.conf
```

Legacy env-variable mode also works:

```bash
export REMOTE_HOST=your.server.example
export REMOTE_USER=youruser
./deploy/remote/deploy-remote-jar.sh
```

Optional variables:

```bash
export SSH_PORT=22
export SSH_KEY_PATH=~/.ssh/id_ed25519
export REMOTE_APP_JAR=/home/birch/appdata/farmtracks/app.jar
export REMOTE_APP_CONTAINER=farmtracks-app
export REMOTE_DOCKER_CMD="sudo -n docker"
```

## Legacy Local SWAG (optional)

If you still need the old docker-compose SWAG flow from an already-built JAR, run

```
cp -f target/farm-tracks-1.0-SNAPSHOT.jar /home/birch/appdata/farmtracks/app.jar
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml up -d --force-recreate farmtracks swag
```

To view logs:

```
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml logs --tail=200 farmtracks
```
