# Production Database Deployment Guide

This document outlines the steps to add a production database (`FARMTRACKSPROD`) and dedicated user to your existing MariaDB instance, then configure your Docker production deployment to use it.

## Step 1: Access MariaDB Console

Access the MariaDB container console using one of these methods:

### Via Portainer
1. Open Portainer → Containers
2. Find your MariaDB container
3. Click on it, then select the **Console** tab
4. Click **Connect** to open the console
5. You'll be in the container shell; type: `mariadb -u root -p`
6. Provide your MariaDB root password when prompted

### Via Unraid
1. Open Unraid Dashboard → Docker
2. Find your MariaDB container
3. Click **Console** in the container details
4. Type: `mariadb -u root -p`
5. Provide your MariaDB root password when prompted

Once connected, you'll see the `MariaDB [<database>]>` prompt.

---

## Step 2: Create Database and User

With the MariaDB console open, execute these SQL commands in order:

### Create the production database
```sql
CREATE DATABASE FARMTRACKSPROD;
```

### Create the user and grant privileges
Replace `MY_PASSWORD` with a strong password of your choice:
```sql
CREATE USER 'farmtracksprod'@'%' IDENTIFIED BY 'MY_PASSWORD';
GRANT ALL PRIVILEGES ON FARMTRACKSPROD.* TO 'farmtracksprod'@'%';
FLUSH PRIVILEGES;
```

**Note:** The `%` wildcard allows connections from any host. For increased security in production, restrict to specific hosts:
```sql
CREATE USER 'farmtracksprod'@'farmtracks-app' IDENTIFIED BY 'MY_PASSWORD';
GRANT ALL PRIVILEGES ON FARMTRACKSPROD.* TO 'farmtracksprod'@'farmtracks-app';
FLUSH PRIVILEGES;
```

### Verify setup (optional)
```sql
SHOW DATABASES;
SELECT User, Host FROM mysql.user WHERE User='farmtracksprod';
```

### Change a user's password (if needed)
If you ever need to reset the `farmtracksprod` password after creation (e.g. if the wrong password was set initially):
```sql
ALTER USER 'farmtracksprod'@'%' IDENTIFIED BY 'your_new_password';
FLUSH PRIVILEGES;
```

### Exit MariaDB
```sql
EXIT;
```

---

## Step 3: Create `.env.prod` with Production Credentials

Create a `.env.prod` file in the project root (this file is gitignored — never committed).

Run this command from the project root to create it (only if it doesn't already exist):

```bash
[ -f .env.prod ] && echo ".env.prod already exists — edit it directly" || cat > .env.prod << 'EOF'
# .env.prod — production credentials (gitignored)
# Variable names must match docker-compose.yml substitution variables
DM_DB_URL=jdbc:mariadb://192.168.0.30:3306/FARMTRACKSPROD
DM_DB_USER=farmtracksprod
DM_DB_PASSWORD=YOUR_PASSWORD
PATH_TO_PROFILE_IMAGE=/home/birch/Documents
JAR_HOST_DIR=/home/birch/appdata/farmtracks
JAR_FILE=app.jar
SSL_CERT_HOST_DIR=/home/birch/appdata/farmtracks/certs
SERVER_SSL_KEY_STORE_PASSWORD=

# Bootstrap admin — only needed on FIRST START when app_user table is empty.
# Remove these two lines (or leave blank) after the first successful login.
APP_SECURITY_BOOTSTRAP_ADMIN_USERNAME=
APP_SECURITY_BOOTSTRAP_ADMIN_PASSWORD=
EOF
```

Then secure the file permissions:

```bash
chmod 600 .env.prod
```

Replace:
- `192.168.0.30` with your MariaDB hostname/IP if different
- `YOUR_PASSWORD` with the password you set in Step 2

**Important:** The variable names must match the `${VAR}` references in `docker-compose.yml` (e.g. `DM_DB_URL`, not `SPRING_DATASOURCE_URL`). `--env-file .env.prod` replaces the default `.env` entirely for compose variable substitution, so all required variables must be present.

The dev `.env` file (also gitignored) continues to hold your dev (`FARMTRACKS`) credentials unchanged. The `build-prod.sh --deploy` script passes `--env-file .env.prod` to docker compose so production credentials are never stored in any committed file.

---

## Step 4: Optional—Copy Existing Data (Dev to Prod)

If you want to copy schema and data from the dev database:

```sql
-- Create prod database with same schema as dev
CREATE DATABASE FARMTRACKSPROD LIKE FARMTRACKS;

-- Copy data (if schema matches)
-- Option 1: Via mysqldump for full backup/restore
-- Option 2: Via INSERT...SELECT if compatible
```

Or use mysqldump from your host:
```bash
mysqldump -u farmtracks -p -h 192.168.0.30 FARMTRACKS | \
  mysql -u farmtracksprod -p -h 192.168.0.30 FARMTRACKSPROD
```

---

## Step 5: Test Production Deployment

After updating your `.env.prod`, restart the application:

```bash
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml --env-file .env.prod up -d farmtracks
```

> **Note:** `--env-file .env.prod` is required whenever you run compose manually. Without it, Docker Compose defaults to `.env` (dev credentials). The `build-prod.sh --deploy` script handles this automatically.

### First-time start: bootstrapping the initial admin user

On a fresh production database the `app_user` table is empty — there is no account to log in with. The app has a built-in bootstrap mechanism: if `APP_SECURITY_BOOTSTRAP_ADMIN_USERNAME` and `APP_SECURITY_BOOTSTRAP_ADMIN_PASSWORD` are set **and** the user table is empty at startup, it will create a full ADMIN+USER account automatically.

**To bootstrap:**

1. Edit `.env.prod` and fill in the bootstrap credentials:
   ```
   APP_SECURITY_BOOTSTRAP_ADMIN_USERNAME=admin
   APP_SECURITY_BOOTSTRAP_ADMIN_PASSWORD=ChangeMe123!
   ```

2. Start (or restart) the container:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.local-swag.yml --env-file .env.prod up -d farmtracks
   ```

3. Log in with the bootstrap credentials and **immediately change the password** through the application's user management.

4. Clear the bootstrap vars from `.env.prod` (set them to blank or remove the lines entirely) so they are not set on future restarts:
   ```
   APP_SECURITY_BOOTSTRAP_ADMIN_USERNAME=
   APP_SECURITY_BOOTSTRAP_ADMIN_PASSWORD=
   ```
   Then restart once more to confirm normal startup:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.local-swag.yml --env-file .env.prod up -d farmtracks
   ```

> **Why this is safe:** The service checks `appUserRepository.count() > 0` first. If any users exist it exits immediately, so leaving the vars set after the first login is harmless — but clearing them is good practice.

### Verify database connection in logs
```bash
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml logs --tail=50 farmtracks | grep -i "database\|hikari\|jdbc"
```

Look for output like:
```
HikariPool-1 - Start completed
Database JDBC URL [jdbc:mariadb://192.168.0.30/FARMTRACKSPROD...]
```

If you see connection errors, verify:
1. MariaDB is reachable at the hostname/IP
2. Credentials (`farmtracksprod` / `MY_PASSWORD`) are correct
3. Firewall allows connection to port 3306
4. User has `ALL PRIVILEGES` on the database

---

## Step 6: Verify Application Health

Once the container is running:

```bash
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml ps
docker compose -f docker-compose.yml -f docker-compose.local-swag.yml logs --tail=50 farmtracks | grep -E "Started Application|ERROR"
```

You should see:
```
Started Application in X.XXX seconds
```

---

## Summary of Variables

| Variable | Example Value | Notes |
|----------|---------------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:mariadb://192.168.0.30/FARMTRACKSPROD?user=farmtracksprod&password=mypass123` | JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | `farmtracksprod` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `mypass123` | User password (store securely) |

---

## Security Best Practices

1. **Use strong passwords** – Minimum 16 characters, mixed case, numbers, symbols
2. **Restrict host access** – Use specific hostnames instead of `%` if possible
3. **Store credentials securely** – Use `.env` files with proper permissions (`chmod 600`)
4. **Backup database** – Before switching to production, backup the entire database
5. **Monitor connections** – Regularly review user access logs

---

## Troubleshooting

### Connection Refused
- Verify MariaDB is running: `docker ps | grep mariadb`
- Check hostname/IP and port (default: 3306)

### Authentication Failed
- Verify user exists: `SELECT User FROM mysql.user WHERE User='farmtracksprod';`
- Reset password: `ALTER USER 'farmtracksprod'@'%' IDENTIFIED BY 'newpassword';`

### Database Not Found
- Verify database exists: `SHOW DATABASES; | grep FARMTRACKSPROD`
- Recreate if needed: `CREATE DATABASE FARMTRACKSPROD;`

### Application Still Using Old Database
- Check Spring Boot logs for which database it's connecting to: `docker logs farmtracks-app --tail=50 | grep -i 'JDBC URL\|catalog'`
- Ensure variable names in `.env.prod` match `docker-compose.yml` (`DM_DB_URL`, `DM_DB_USER`, `DM_DB_PASSWORD` — **not** `SPRING_DATASOURCE_*`)
- Always pass `--env-file .env.prod` when running compose manually — without it, compose uses `.env` (dev credentials)
- Restart with correct env file: `docker compose -f docker-compose.yml -f docker-compose.local-swag.yml --env-file .env.prod up -d farmtracks`

---

## Next Steps

After successful deployment:
1. Run application health checks
2. Test key functionality (login, data queries)
3. Monitor application and database logs
4. Set up regular backups of `FARMTRACKSPROD`
5. Document any custom configurations

