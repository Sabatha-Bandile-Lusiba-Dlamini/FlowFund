# FlowFund API

The backend for FlowFund, a stokvel (community savings group) and trading-journal app. It is an ASP.NET Core Web API (.NET 10) with Entity Framework Core on Azure SQL, serving an offline-first Android client built with Kotlin, Jetpack Compose, Retrofit and Room.

## Highlights
- **Single identity flow:** Google/Firebase sign-in is verified once with the Firebase Admin SDK, then exchanged for the API's own JWT access token (15 minutes) and refresh token (30 days). A `token_use` claim stops refresh tokens being used on protected routes.
- **Per-user data isolation:** trades and journal entries are always queried by the caller's user id, so another user's id returns `404` rather than revealing it exists.
- **Role-based group access:** Admin, Treasurer and Member permissions are checked in the service layer for contributions, confirmations, missed-payment alerts and payouts.
- **Idempotent writes:** client-generated GUIDs with unique database indexes mean a retried offline sync returns the stored row (`200`) instead of creating a duplicate (`201`).
- **Calculated statistics:** trade profit/loss and win rate come from a pure `TradeCalculator` with no database or clock, so they are directly unit tested.
- **Operational basics:** request logging and exception-to-HTTP mapping in one middleware, transient-failure retries for serverless Azure SQL, and secrets kept out of source control.

## Tech stack
ASP.NET Core Web API (net10.0), Entity Framework Core (SQL Server provider, code-first migrations), Azure SQL, Firebase Admin SDK, JWT bearer authentication, Swagger, xUnit with Moq and EF Core InMemory, Azure App Service.

| Layer | Role |
|---|---|
| Controllers | Routes and status codes only |
| Services | Business rules, validation, ownership checks, logging |
| Calculators | Pure maths, unit tested directly |
| Data | `FlowFundDbContext`, migrations, unique indexes, decimal precision |

## API overview
Base path `/api/v1`. Send `Authorization: Bearer <accessToken>` on everything except `/auth/*`. Trade and journal dates are Unix epoch milliseconds (UTC).

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/sso/verify`, `POST /auth/refresh` |
| Trades | `GET/POST /trades`, `GET/PUT/DELETE /trades/{id}`, `GET /trades/statistics` |
| Journal | `GET/POST /journal`, `GET/PUT/DELETE /journal/{id}` |
| Groups | `POST /groups`, `GET /groups/mine`, member add/remove/role endpoints |
| Contributions | group contributions, payment confirmation, `GET /groups/{id}/alerts/missed-payments`, `GET /users/me/contributions` |
| Payouts | `GET /groups/{id}/payout-schedule`, `/next`, `/generate` |
| Users | `GET/PATCH /users/me`, settings, notifications, statements |

Errors: `400` validation, `401` missing or invalid token, `403` not permitted, `404` not found (or not yours), `409` conflict, `500` unexpected.

HOW TO RUN THE APP:

## Prerequisites

| Tool | Notes |
|---|---|
| .NET SDK 10.0 | Check with `dotnet --version` |
| EF Core CLI | `dotnet tool install --global dotnet-ef` |
| Android Studio | Current stable version (includes its own JDK) |
| Azure SQL database | Serverless databases can take about a minute to wake up |
| Firebase project | Google sign-in enabled; Email/Password enabled for a test user |
| Phone or emulator | Android 6.0+; use a **Google Play** emulator image |

## 1. Configure the API

Real secrets are never committed. `appsettings.json`, the Firebase service-account key and the JWT secret are git-ignored.

1. Copy `FlowFund.Api/appsettings.Example.json` to `FlowFund.Api/appsettings.json` and fill in:
   - `ConnectionStrings:DefaultConnection`: your Azure SQL connection string
   - `Jwt:Secret`: a new base-64 secret (for example `openssl rand -base64 64`)
   - `Jwt:Issuer` and `Jwt:Audience`: any consistent values
   - `Firebase:ServiceAccountPath`: path to your Firebase service-account JSON file
2. Connection string format (replace the placeholders):
   ```
   Server=tcp:<your-server>.database.windows.net,1433;Initial Catalog=<database>;Persist Security Info=False;User ID=<sql-user>;Password=<sql-password>;MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=90;
   ```
3. In the Azure portal, open your SQL server > Networking, add your current public IPv4 address as a firewall rule, and Save. Add it again whenever your network or IP changes.

## 2. Run the API locally

Open PowerShell in the repository root.

1. Set the connection string for the database commands (current terminal only):
   ```powershell
   $env:FLOWFUND_CONNECTION_STRING = "<your connection string>"
   ```
2. Restore, build and create the database tables:
   ```powershell
   dotnet restore
   dotnet build
   dotnet ef database update --project FlowFund.Api
   ```
   If the database is paused, wait a minute and run the update again.
3. Start the API so that the emulator or a phone on your Wi-Fi can reach it:
   ```powershell
   dotnet run --project FlowFund.Api --launch-profile http --urls http://0.0.0.0:5078
   ```
4. Allow the port through Windows Firewall (admin PowerShell, once):
   ```powershell
   netsh advfirewall firewall add rule name="FlowFund API" dir=in action=allow protocol=TCP localport=5078
   ```
5. Open Swagger at `http://localhost:5078/swagger`.

### Sign in to Swagger

1. Get a Firebase ID token for a test user:
   ```powershell
   $apiKey = "<your Firebase Web API key>"
   $body = @{ email = "<test-user-email>"; password = "<test-user-password>"; returnSecureToken = $true } | ConvertTo-Json
   $r = Invoke-RestMethod -Method Post -ContentType "application/json" -Body $body `
     -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey"
   $r.idToken
   ```
2. In Swagger, call `POST /api/v1/auth/sso/verify` with `{ "idToken": "<the token>" }`.
3. Copy the returned **accessToken** only, click **Authorize** and paste it. The access token lasts 15 minutes; `POST /api/v1/auth/refresh` with the refresh token issues a new one.

## 3. Run the Android app

1. Open the `FLOWFUNDPART2` folder in Android Studio and let Gradle sync.
2. Put `google-services.json` from the same Firebase project in the `app/` folder.
3. Register your debug SHA-1 in Firebase console > Project settings > your Android app > Add fingerprint. Find it in Android Studio's Gradle panel under `app > Tasks > android > signingReport` (variant **debug**, the `SHA1` line), then download `google-services.json` again.
4. Set the API address in `app/build.gradle.kts`. It must end with `/api/v1/`:
   - Emulator: `http://10.0.2.2:5078/api/v1/`
   - Real phone on the same Wi-Fi: `http://<your-PC-IPv4-address>:5078/api/v1/` (find the address with `ipconfig`)
   - Hosted API: `https://<default-domain>/api/v1/`

   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5078/api/v1/\"")
   ```
   Click Sync Now after changing it.
5. For plain-HTTP addresses (the first two above), the manifest needs `android:usesCleartextTraffic="true"` on the `<application>` tag. Remove it when you use the hosted HTTPS address.
6. To use a real phone, enable Developer options (Settings > About phone > tap Build number 7 times), turn on USB debugging, connect with a data cable and accept the prompt on the phone.
7. Select the emulator or phone in Android Studio and press **Run**.
8. Sign in with Google, unlock the app, and open **Trades** or **Journal** from the dashboard.

## 4. Use the hosted API (Azure)

The API is hosted as an Azure Web App. To run against it instead of your PC:

1. Make sure the Web App has these settings (Settings > Environment variables > App settings): `ConnectionStrings__DefaultConnection`, `Jwt__Secret`, `Jwt__Issuer`, `Jwt__Audience` and `Firebase__ServiceAccountJson` (the contents of the Firebase service-account key).
2. Set `ASPNETCORE_ENVIRONMENT` to `Development` only if you need Swagger on the hosted site.
3. Open `https://<default-domain>/swagger` (the Default domain is on the Web App's Overview page) and sign in as described above.
4. Set `API_BASE_URL` in the Android app to `https://<default-domain>/api/v1/`, sync Gradle, reinstall the app and run it.

The application also uses Firebase Authentication and JWT-based authorisation to protect access to backend resources.

