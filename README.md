# 🐶 Puppies API

## 📘 Introduction

The Puppies API is a web-based application backend that powers a social-style platform where users can share pictures of their dogs, similar to Instagram. It’s built with:

- **Java 17**
- **Spring Boot**
- **Spring Security (JWT-based)**
- **Spring Data JPA**
- **PostgreSQL**
- **Liquibase**
- **Swagger / OpenAPI**

---

## ✅ Features

- 🔐 User registration (name, email, password)
- 🔑 JWT-based login & authentication
- 📝 Post creation (image URL, text content, date)
- ❤️ Like / Unlike functionality for posts
- 📰 Paginated user feed (newest posts first)
- 🔍 Post detail view (including like count)
- 👤 User profile (basic info + post & like counts)
- 👍 View liked posts (by authenticated user)
- 📸 View all posts made by a specific user
- 📄 Swagger UI documentation

---

## 🛠️ Project Setup

Follow the steps below to get the API running locally.

### 📋 Prerequisites

- **Java JDK 17+**
- **Maven 3.6+**
- **PostgreSQL (running locally or remotely)**

---

## 🧰 Setup Steps

### 1. Database Setup

Ensure PostgreSQL is running, then create the database:

```sql
CREATE DATABASE puppies;
```

Update the connection details in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/puppies
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
```

---

### 2. Generate JWT Secret Key

Generate a 512-bit (64 bytes) Base64-encoded secret key:

```java
import java.security.SecureRandom;
import java.util.Base64;

public class GenerateKey {
    public static void main(String[] args) {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        System.out.println(Base64.getEncoder().encodeToString(key));
    }
}
```

Paste the generated key in `application.properties`:

```properties
app.jwt.secret=YOUR_GENERATED_BASE64_KEY_HERE
app.jwt.expirationMs=3600000
```

> ⚠️ **Do not commit your secret key to version control.**

---

### 3. Build the Project

```bash
mvn clean install
```

This compiles the code, runs tests, and packages the app.

---

### 4. Run the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or with the JAR:

```bash
java -jar target/api-0.0.1-SNAPSHOT.jar
```

Liquibase will apply all DB migrations automatically.

---

### 5. Accessing the API

- Base URL: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Authenticate using `/api/auth/login` and use the JWT token via the **Authorize** button in Swagger UI.

---

### 6. Run Tests

```bash
mvn test
```

---

## 🌟 Highlights

- RESTful API design
- DTO-based response handling
- Secure authentication using JWT & BCrypt
- Liquibase-driven DB migrations
- Swagger UI for easy API testing
- Pagination for scalable data responses

---

## ⚠️ Known Caveats

- ❌ No JWT refresh token flow
- ❌ No server-side token revocation (logout handled client-side)
- 📛 Global error handling is basic; validation errors could be more descriptive
- 🐢 Possible N+1 query issues in post/like fetching
- 🔍 Input validation could be more comprehensive
- 🖼️ Only image URLs supported—no upload or file storage
----
