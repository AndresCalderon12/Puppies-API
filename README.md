Puppies API
Introduction
This project is a web API designed to power a "Puppies" application, allowing users to share pictures of their dogs in a style similar to Instagram. It's built using Java, Spring Boot, Spring Security (with JWT), Spring Data JPA, PostgreSQL, and Liquibase.
Features Implemented
User creation (name, email, password)
User authentication (sign-in via JWT)
Post creation (image URL, text content, date)
Liking/Unliking posts
Fetching a user's feed (paginated list of all posts, newest first)
Fetching details of an individual post (including like count)
Fetching a user's profile (basic info + post/like counts)
Fetching a list of posts liked by the authenticated user
Fetching a list of posts made by a specific user
API documentation via Swagger/OpenAPI
Project Setup Instructions
Follow these steps to get the Puppies API running locally.
Prerequisites
Java Development Kit (JDK): Version 17 or later.
Maven: Version 3.6 or later (for building and running).
PostgreSQL: A running instance of PostgreSQL database server.
Git: (Optional, for cloning the repository).
1. Clone the Repository (Optional)
git clone <repository_url>
cd puppies-api


2. Database Setup
Ensure your PostgreSQL server is running.
Create a database named puppies (or choose another name).
-- Example using psql
CREATE DATABASE puppies;


Configure the database connection details in src/main/resources/application.properties:
spring.datasource.url=jdbc:postgresql://localhost:5432/puppies # Adjust host/port/db name if needed
spring.datasource.username=your_db_username # Replace with your PostgreSQL username
spring.datasource.password=your_db_password # Replace with your PostgreSQL password


3. Generate JWT Secret Key
The application uses a secret key for signing JWTs. You need to generate a secure, Base64-encoded key that is at least 64 bytes (512 bits) long for the HS512 algorithm.
You can use the provided helper class GenerateKey.java (if included) or an online generator.
Run the generator (if using the class): Compile and run the GenerateKey class.
Update application.properties: Copy the generated Base64 key and paste it as the value for app.jwt.secret:
# In src/main/resources/application.properties
app.jwt.secret=YOUR_GENERATED_BASE64_KEY_HERE
app.jwt.expirationMs=3600000 # Example: 1 hour

Important: Keep this key secure and do not commit it to public repositories in real-world scenarios.
4. Build the Project
Open a terminal or command prompt in the project's root directory.
Run the Maven build command:
mvn clean install

This will compile the code, run tests, and package the application.
5. Run the Application
You can run the application using the Spring Boot Maven plugin:
mvn spring-boot:run


Alternatively, you can run the packaged JAR file (created in the target/ directory after mvn install):
java -jar target/api-0.0.1-SNAPSHOT.jar # Adjust JAR filename if needed


The API should start, and Liquibase will automatically apply the necessary database schema changes based on the files in src/main/resources/db/changelog/.
6. Accessing the API
The API will typically be available at http://localhost:8080.
Swagger UI (API Documentation): Access the interactive documentation at http://localhost:8080/swagger-ui.html. You can use this UI to test endpoints. Remember to log in via /api/auth/login first and use the "Authorize" button with the obtained JWT (Bearer <token>) for secured endpoints.
7. Running Tests
To run the unit and integration tests included in the project, use the Maven test command:
mvn test

This command executes all classes ending in *Test.java found in the src/test/java directory.
Highlights
Clean API Structure: Follows RESTful principles with clear resource paths and HTTP methods.
DTO Pattern: Uses Data Transfer Objects (PostResponseDTO, UserResponseDTO, UserProfileDTO, etc.) to decouple the API contract from the internal domain models and control data exposure.
Security: Implements JWT-based authentication and authorization using Spring Security, including password hashing (BCrypt) and endpoint protection.
Database Migrations: Uses Liquibase for managing database schema changes reliably and version-controllably.
Testing: Includes unit tests for service layers (@ExtendWith(MockitoExtension.class)) and slice tests for controllers (@WebMvcTest), demonstrating good testing practices.
API Documentation: Integrated springdoc-openapi for automatic Swagger UI generation, providing interactive API documentation.
Pagination: Implemented pagination (Pageable, Page) for list endpoints (/feed, /likes, /posts) to handle potentially large datasets efficiently.
Caveats and Limitations
JWT Implementation:
Uses stateless JWTs without refresh tokens. Tokens have a fixed expiration time defined in application.properties.
No server-side token blacklist is implemented for logout; logout relies on the client discarding the token.
Error Handling: Basic global exception handling is in place (GlobalExceptionHandler), but could be made more granular for different specific error types (e.g., validation errors could return more detailed field information).
Performance (N+1 Queries): The current implementation for fetching like counts when mapping lists of posts (e.g., in getUserFeed, getUserPosts, getLikedPosts) fetches the count for each post individually. This can lead to N+1 query performance issues on large lists and should be optimized in a production scenario (e.g., using JPQL subqueries, projections, or batch fetching).
Input Validation: Basic validation (Assert, @Valid on DTOs) is used, but could be expanded (e.g., stricter password complexity rules, more specific validation annotations).
Image Handling: The API currently only stores image URLs (imageUrl). It does not handle actual image file uploads, storage, or serving.
Testing Scope: Primarily focused on unit tests for services and slice tests for controllers. More comprehensive integration tests (@SpringBootTest) covering the full request lifecycle could be added.
Database Secrets: Database credentials and the JWT secret are currently stored directly in application.properties. In a production environment, these should be externalized using environment variables, configuration servers, or secrets management tools.
