# Subly – Subscription Tracker

**Simple subscriptions. Smarter spending.**

Subly is a full-stack subscription management application that helps users organize their recurring subscriptions, monitor monthly spending, track upcoming renewals, and review transaction history in one place.

The application provides a centralized dashboard and subscription management system with secure user authentication.

## ✨ Features

### 🔐 Authentication
- User registration and login.
- JWT-based authentication.
- Protected application routes.
- User-specific subscription data.

### 📊 Dashboard
- Overview of monthly subscription spending.
- Active subscription count.
- Upcoming renewals.
- Active free trial count.
- Spending breakdown by category.
- Recent transaction overview.

### 📅 Subscription Management
- Add, view, update, and delete subscriptions.
- Organize subscriptions by category.
- Track billing cycles and payment methods.
- Monitor subscription renewal dates.
- Manage free trials and trial end dates.
- Search, filter, and sort subscriptions.

### 💳 Transaction Management
- View imported transaction history.
- Search transactions by merchant or account.
- Filter by transaction type and payment method.
- Filter transactions by date range.
- Sort transactions by date or amount.
- Review transaction details.

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React.js, JavaScript, HTML, CSS |
| Build Tool | Vite |
| Backend | Java, Spring Boot |
| Database | MySQL |
| Authentication | Spring Security, JWT |
| API Communication | REST APIs, Axios |
| Routing | React Router |
| Version Control | Git, GitHub |

## 📁 Project Structure

```text
SubscriptionTracker/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   ├── styles/
│   │   └── utils/
│   ├── package.json
│   └── vite.config.js
│
├── docs/
│   └── architecture-report.md
│
└── README.md
```

## ⚙️ Getting Started

Follow these instructions to run Subly locally.

### Prerequisites

Make sure you have installed:

- Java 19 or compatible project-supported JDK.
- Maven.
- Node.js and npm.
- MySQL Server.
- Git.

### 1. Clone the Repository

```bash
git clone https://github.com/shenbagakasthuri/SubscriptionTracker.git
```

```bash
cd SubscriptionTracker
```

### 2. Configure the Database

1. Start your MySQL server.
2. Create a database for Subly.
3. Configure the database connection in the backend's local application configuration.
4. Set the required database username and password.

Keep local credentials and secrets outside Git.

### 3. Run the Backend

Open a terminal:

```bash
cd backend
```

Run the Spring Boot application:

```bash
mvn spring-boot:run
```

The backend runs locally at:

```text
http://localhost:8080
```

### 4. Run the Frontend

Open another terminal from the project root:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Create a `.env` file inside the frontend directory:

```env
VITE_API_URL=http://localhost:8080
```

Start the development server:

```bash
npm run dev
```

Open the local URL displayed in your terminal, usually:

```text
http://localhost:5173
```

## 🔗 Main API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate a user |
| GET | `/api/auth/me` | Get authenticated user |
| GET | `/api/subscriptions` | Get subscriptions |
| POST | `/api/subscriptions` | Create subscription |
| GET | `/api/subscriptions/{id}` | Get subscription details |
| PUT | `/api/subscriptions/{id}` | Update subscription |
| DELETE | `/api/subscriptions/{id}` | Delete subscription |
| POST | `/api/sms/parse` | Parse transaction message |
| POST | `/api/sms/import` | Import transaction |
| GET | `/api/sms/imports` | Get imported transactions |
| GET | `/api/sms/subscription-suggestions` | Get subscription suggestions |

Protected endpoints require a valid JWT bearer token.

## 🔒 Security

- JWT-based authentication.
- Protected API endpoints.
- User-specific data access.
- Environment-based configuration for sensitive credentials.
- Local environment files excluded from version control.

## 🚀 Future Enhancements

- Cloud deployment and public access.
- Subscription renewal notifications.
- Enhanced spending analytics and reports.
- Improved subscription recommendations.
- Additional payment and transaction integrations.

## 👨‍💻 Author

**Shenbaga Kasthuri R**

Computer Science and Engineering – Internet of Things

- GitHub: [@shenbagakasthuri](https://github.com/shenbagakasthuri)
- Project Repository: [SubscriptionTracker](https://github.com/shenbagakasthuri/SubscriptionTracker)

---

<p align="center">
  **Subly – Simple subscriptions. Smarter spending.**
</p>
