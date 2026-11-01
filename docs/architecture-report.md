# Architecture Report

## 1. Frontend structure (React + Vite)
```
frontend/
├─ src/
│   ├─ assets/                     # static images (hero.png, react.svg, vite.svg)
│   ├─ components/                # UI primitives
│   │   ├─ Navbar.jsx               # top bar (currently minimal)
│   │   ├─ Navbar.module.css
│   │   ├─ Sidebar.jsx              # deep‑nav sidebar
│   │   ├─ Sidebar.module.css
│   │   ├─ SummaryCard.jsx          # card used on Dashboard
│   │   └─ SummaryCard.module.css
│   ├─ data/                       # mock data (currently only mockDashboard.js)
│   │   └─ mockData.js
│   ├─ pages/                      # route‑level screens
│   │   ├─ Dashboard.jsx            # main dashboard (still uses mock data)
│   │   ├─ Dashboard.module.css
│   │   ├─ Login.jsx                # placeholder login form
│   │   ├─ Login.module.css
│   │   ├─ Register.jsx             # placeholder register form
│   │   ├─ Register.module.css
│   │   ├─ Subscriptions.jsx        # placeholder list page
│   │   ├─ Subscriptions.module.css
│   │   ├─ Transactions.jsx         # placeholder list page
│   │   └─ Transactions.module.css
│   ├─ styles/                     # global CSS variables and resets
│   │   ├─ global.css
│   │   └─ variables.css
│   ├─ App.jsx                      # router configuration (React‑Router v6)
│   ├─ main.jsx                     # entry point (now wraps <App/> with BrowserRouter)
│   └─ index.css
└─ package.json                    # dependencies list (React 19, React‑Router 6, Vite 8)
```

- **Styling**: Plain CSS Modules only; no Tailwind. Global variables (colors, spacing) are defined in `styles/variables.css`.
- **Routing**: Defined in `App.jsx` using `<Routes>` for Dashboard, Subscriptions, Transactions, Login, Register.
- **State management**: Currently none – the UI relies on local component state and mock data.

## 2. Backend overview (Spring Boot)
```
backend/src/main/java/com/subscriptiontracker/
├─ config/
│   ├─ JwtProperties.java           # JWT secret and expiry configuration
│   ├─ OpenApiConfig.java           # Swagger UI config
│   └─ SecurityConfig.java          # CORS (allowed‑origins from ${FRONTEND_URL:http://localhost:5173})
├─ controller/
│   ├─ AuthController.java          # /api/auth/** (register, login, /me)
│   ├─ SubscriptionController.java # /api/subscriptions/** (CRUD)
│   └─ SmsController.java           # /api/sms/** (parse, import, list, suggestions)
├─ dto/request/
│   ├─ RegisterRequest.java
│   ├─ LoginRequest.java
│   ├─ SubscriptionRequest.java
│   └─ SmsParseRequest.java
├─ dto/response/
│   ├─ ApiResponse.java (wrapper with success flag, message, data)
│   ├─ AuthResponse.java (jwt, expiresAt)
│   ├─ UserResponse.java
│   ├─ SubscriptionResponse.java
│   ├─ SmsParseResponse.java
│   ├─ SmsTransactionResponse.java
│   └─ SubscriptionSuggestionResponse.java
├─ model/entity/
│   ├─ User.java
│   ├─ Subscription.java
│   ├─ SmsTransaction.java
│   ├─ NotificationLog.java
│   └─ … (price history, enums)
├─ security/*  (JWT filter, provider, entry point)
├─ service/*   (AuthService, SubscriptionService, SmsParserService, SmsTransactionService, EmailService, NotificationService)
└─ scheduler/DailyReminderScheduler.java (email reminders)
```

- **CORS**: `SecurityConfig` builds a `CorsConfigurationSource` using the `cors.allowed-origins` property (defaults to `http://localhost:5173`).
- **Authentication**: JWT is created by `JwtTokenProvider` and validated in `JwtAuthenticationFilter`. The token is sent back in `AuthResponse` and must be added as `Authorization: Bearer <jwt>` on all protected endpoints.
- **Global response format**: Every controller returns `ApiResponse<T>` – `{ success: true, message: "…", data: <payload> }`.

## 3. Existing backend API surface
| Controller | Method | URL | Request body | Response payload (inside `data`) | Auth required |
|------------|--------|-----|--------------|----------------------------------|---------------|
| **AuthController** | POST | `/api/auth/register` | `RegisterRequest` (name, email, password) | `AuthResponse` (jwt, expiresAt) | No |
| | POST | `/api/auth/login` | `LoginRequest` (email, password) | `AuthResponse` | No |
| | GET | `/api/auth/me` | – | `UserResponse` (id, name, email, createdAt) | Yes (Bearer) |
| **SubscriptionController** | POST | `/api/subscriptions` | `SubscriptionRequest` (name, category, amount, billingCycle, paymentMethod, nextRenewalDate, isTrial, trialEndDate, status) | `SubscriptionResponse` (full subscription DTO) | Yes |
| | GET | `/api/subscriptions` | – | `List<SubscriptionResponse>` | Yes |
| | GET | `/api/subscriptions/{id}` | – | `SubscriptionResponse` | Yes |
| | PUT | `/api/subscriptions/{id}` | `SubscriptionRequest` | `SubscriptionResponse` | Yes |
| | DELETE | `/api/subscriptions/{id}` | – | – (204 No Content) | Yes |
| **SmsController** | POST | `/api/sms/parse` | `SmsParseRequest` (smsText) | `SmsParseResponse` (merchant, amount, date, confidence, paymentMethod, direction) | Yes |
| | POST | `/api/sms/import` | `SmsParseRequest` (smsText) | `SmsTransactionResponse` (saved transaction DTO) | Yes |
| | GET | `/api/sms/imports` | – | `List<SmsTransactionResponse>` | Yes |
| | GET | `/api/sms/imports/{id}` | – | `SmsTransactionResponse` | Yes |
| | GET | `/api/sms/subscription-suggestions` | – | `List<SubscriptionSuggestionResponse>` (possible recurring services) | Yes |

## 4. CORS & Frontend‑Backend Compatibility
- `SecurityConfig` reads `${FRONTEND_URL:http://localhost:5173}`; the Vite dev server runs on that default, so CORS is already open for the frontend.
- No additional proxy configuration is required; the frontend can call the backend directly (e.g., `http://localhost:8080/api/...`).

## 5. Missing backend endpoints (required for the planned UI)
| Feature | Expected data | Backend support |
|---------|---------------|-----------------|
| Dashboard summary (total monthly spend, upcoming renewals count, active free‑trial count) | Aggregated numbers for the logged‑in user | **Not present** – there is no `/api/dashboard/summary` or similar aggregation endpoint. The UI will need to compute these values client‑side from the list of subscriptions and transactions, or the backend must be extended (out of scope). |
| Notification history | List of notification logs (email sent, type, date) | No controller exposing `NotificationLog`. The scheduler writes logs but no REST endpoint exists. |
| Spending trend chart data (spending per month) | Time‑series of spend per month | No dedicated endpoint; can be derived from transactions/subscriptions client‑side. |
| Category‑wise spending chart | Aggregated amount per `Category` enum | Not provided directly; can be computed client‑side from transactions. |

All other UI pieces (login, registration, subscription CRUD, SMS parse/import) have matching backend endpoints.

## 6. Dependencies (frontend `package.json`)
```json
"dependencies": {
  "react": "^19.3.1",
  "react-dom": "^19.3.1",
  "react-router-dom": "^6.30.6"
},
"devDependencies": {
  "@types/react": "^19.3.0",
  "@types/react-dom": "^19.3.0",
  "@vitejs/plugin-react": "^6.1.2",
  "oxlint": "^1.88.0",
  "vite": "^8.3.4"
}
```
- No charting library is currently installed. If we later add Recharts we will need to install it (outside the scope of this inspection).

---
**Conclusion** – The frontend has a clean component layout but still relies on mock data. The backend provides all CRUD and SMS APIs needed for the functional UI, except for aggregated dashboard/notification endpoints. Those missing pieces will have to be computed on the client or flagged as blockers for later backend extension.
