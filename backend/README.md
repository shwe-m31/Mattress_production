# PEPS production backend

This Spring Boot service supplies the existing React dashboard at `GET /api/dashboard` on port `8080`.

## Start

1. Ensure MySQL is running. The service creates `peps_production_db` automatically when the configured user has permission. Alternatively, run `peps_production_db.sql` first.
2. Configure credentials if MySQL requires them:

   ```powershell
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your-password"
   ```

3. From this folder, start the backend:

   ```powershell
   mvn spring-boot:run
   ```

The first startup seeds current-day production records. A new completed event is persisted every 10 seconds by default; adjust `simulation.interval` in `src/main/resources/application.properties` if desired.

Then start the existing React app from the project root with `npm start`. It continues to use its existing controls and requests `http://localhost:8080/api/dashboard`.
