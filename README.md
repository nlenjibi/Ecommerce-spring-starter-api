# Smart E-Commerce System

A comprehensive E-Commerce API built using Spring Boot, featuring both RESTful and GraphQL endpoints for managing products, users, orders, categories, reviews, carts, and wishlists. This project demonstrates advanced Spring Boot concepts including validation, exception handling, AOP, OpenAPI documentation, and performance optimization.

## Project Overview

This phase of the Smart E-Commerce System transforms the existing database foundation into a web-based Spring Boot application and applies Spring Boot web development concepts to design and implement RESTful and GraphQL APIs that interact with the system's data while applying validation, exception handling, AOP, and OpenAPI documentation.

## Features

- **RESTful APIs**: Complete CRUD operations for all entities with pagination, sorting, and filtering
- **GraphQL Integration**: Flexible data fetching with queries and mutations
- **Authentication & Authorization**: User management with role-based access
- **Validation**: Bean Validation with custom validators
- **Exception Handling**: Centralized error management with `@ControllerAdvice`
- **AOP**: Logging, performance monitoring, and cross-cutting concerns
- **Caching**: Redis-based caching for improved performance
- **Documentation**: OpenAPI/Swagger documentation
- **Database**: JPA/Hibernate with Flyway migrations
- **Profiles**: Environment-specific configurations (dev, test, prod)

## Technologies Used

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 21 |
| Database | MySQL/PostgreSQL |
| ORM | Spring Data JPA |
| Migration | Flyway |
| GraphQL | Spring GraphQL |
| Caching | Redis |
| Validation | Bean Validation |
| Documentation | Springdoc OpenAPI |
| Testing | Spring Boot Test |
| Build Tool | Maven |
| Mapping | MapStruct |

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL or PostgreSQL
- Redis (optional, for caching)

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd Ecommerce-spring-starter-api
   ```

2. **Configure Database:**
   - Create a database (e.g., `ecommerce_dev`)
   - Update `src/main/resources/application-dev.yml` with your database credentials

3. **Install Dependencies:**
   ```bash
   mvn clean install
   ```

4. **Run Database Migrations:**
   ```bash
   mvn flyway:migrate
   ```

5. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## Configuration

The application supports multiple profiles:

- **dev**: Development environment with MySQL
- **test**: Testing environment
- **prod**: Production environment with PostgreSQL

To activate a profile, set the `spring.profiles.active` property or use the `--spring.profiles.active=dev` command-line argument.

### Environment Variables

Create a `.env` file in the root directory:

```env
DB_URL=jdbc:mysql://localhost:3306/ecommerce_dev
DB_USERNAME=root
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
```

## API Documentation

### Swagger UI
Access the REST API documentation at: `http://localhost:8080/swagger-ui.html`

### GraphiQL
Access the GraphQL playground at: `http://localhost:8080/graphiql`

## API Endpoints

### REST Endpoints

#### Products
- `GET /v1/products` - List products with filtering and pagination
- `GET /v1/products/{id}` - Get product by ID
- `POST /v1/products` - Create product
- `PUT /v1/products/{id}` - Update product
- `DELETE /v1/products/{id}` - Delete product

#### Users
- `GET /v1/users` - List users
- `GET /v1/users/{id}` - Get user by ID
- `POST /v1/users` - Create user
- `PUT /v1/users/{id}` - Update user
- `DELETE /v1/users/{id}` - Delete user

#### Categories, Orders, Reviews, Cart, Wishlist
Similar CRUD endpoints available for all entities.

### GraphQL Schema

The GraphQL schema includes the following main types:

#### Core Types
- `User`: User management with roles (ADMIN, CUSTOMER, MERCHANT)
- `Product`: Product catalog with inventory, pricing, and categories
- `Category`: Hierarchical product categories
- `Order`: Order management with status tracking
- `Review`: Product reviews and ratings
- `Cart`: Shopping cart functionality
- `Wishlist`: User wishlists

#### Pagination
All list queries support pagination with `PageInput`:
```graphql
input PageInput {
    page: Int = 0
    size: Int = 20
    sortBy: String = "id"
    direction: SortDirection = ASC
}
```

#### Sample Queries

**Get Products with Filtering:**
```graphql
query {
  products(pagination: {page: 0, size: 10}, filter: {categoryId: 1, minPrice: 10.0}) {
    content {
      id
      name
      price
      category {
        name
      }
    }
    pageInfo {
      totalElements
      currentPage
    }
  }
}
```

**Create Product:**
```graphql
mutation {
  createProduct(input: {
    name: "New Product"
    price: 29.99
    categoryId: 1
  }) {
    id
    name
    price
  }
}
```

## Performance Analysis

### REST API Performance

**Advantages:**
- Simple and straightforward implementation
- HTTP caching support (ETags, Cache-Control headers)
- Multiple endpoints allow for fine-grained caching strategies
- Lower learning curve for clients
- Better support for HTTP status codes and methods

**Disadvantages:**
- Over-fetching: Clients receive more data than needed
- Under-fetching: Multiple requests required for related data
- Versioning challenges with API evolution
- Fixed response structure limits flexibility

**Performance Metrics (Estimated):**
- Response Time: 50-100ms for simple requests
- Throughput: High for cached requests
- Bandwidth: Higher due to potential over-fetching
- N+1 Query Problem: Mitigated through proper DTO design

### GraphQL API Performance

**Advantages:**
- Single endpoint reduces network overhead
- Exact data fetching eliminates over/under-fetching
- Flexible queries adapt to client needs
- Strong typing with schema validation
- Real-time capabilities with subscriptions

**Disadvantages:**
- Complexity in implementation and optimization
- Potential N+1 query problem without proper batching
- Caching challenges due to dynamic queries
- Security concerns with complex queries

**Performance Metrics (Estimated):**
- Response Time: 30-80ms for optimized queries
- Throughput: Variable based on query complexity
- Bandwidth: Lower due to precise data fetching
- Caching: Implemented with Redis for common queries

### Comparative Analysis

| Aspect | REST | GraphQL |
|--------|------|---------|
| Data Fetching | Multiple endpoints | Single endpoint |
| Over-fetching | Common | Eliminated |
| Under-fetching | Common | Eliminated |
| Caching | HTTP-level | Application-level |
| Learning Curve | Low | Medium |
| Flexibility | Low | High |
| Performance | Good for simple ops | Better for complex data needs |

**Recommendations:**
- Use REST for simple CRUD operations and external integrations
- Use GraphQL for complex data requirements and frontend applications
- Implement caching and monitoring (AOP) for both
- Optimize database queries to prevent N+1 problems

## Project Objectives

By the end of this project, learners will be able to:

1. Apply Spring Boot configuration principles, IoC, and Dependency Injection to build modular and maintainable enterprise web applications.
2. Develop RESTful APIs using layered architecture (Controller → Service → Repository) with environment-based configurations and clean response handling.
3. Implement validation, exception handling, and API documentation using Bean Validation, @ControllerAdvice, and Springdoc OpenAPI.
4. Integrate GraphQL schemas, queries, and mutations to enable flexible and optimized data retrieval alongside REST endpoints.
5. Apply Aspect-Oriented Programming (AOP) and algorithmic techniques for logging, monitoring, and efficient sorting, searching, and pagination within APIs.

## Epics and User Stories

### Epic 1: Application Setup and Dependency Management
**User Story 1.1** As a developer, I want to configure and structure a Spring Boot project so that it runs efficiently across multiple environments.
- Acceptance Criteria: Spring Boot project initialized with required dependencies, profiles configured for dev, test, and prod environments, constructor-based dependency injection used consistently.

### Epic 2: RESTful API Development
**User Story 2.1** As an administrator, I want to manage users, products, and categories through REST endpoints so that I can maintain the e-commerce system.
- Acceptance Criteria: CRUD APIs implemented following REST conventions, responses structured with status, message, and data, controllers communicate through service and repository layers.

**User Story 2.2** As a customer, I want to view, sort, and filter products so that I can find items easily.
- Acceptance Criteria: Pagination, sorting, and filtering parameters supported, efficient algorithms used for sorting and data retrieval, response performance documented and analyzed.

### Epic 3: Validation, Exception Handling, and Documentation
**User Story 3.1** As a developer, I want to validate and document all API endpoints so that they remain consistent and reliable.
- Acceptance Criteria: Bean Validation annotations applied to request DTOs, custom validators used for complex rules, OpenAPI documentation generated automatically with annotations.

### Epic 4: GraphQL Integration
**User Story 4.1** As a frontend developer, I want to fetch data using GraphQL queries and mutations so that I can retrieve only the information I need.
- Acceptance Criteria: GraphQL schema defined for key entities, queries and mutations implemented successfully, REST and GraphQL endpoints coexist without conflict.

### Epic 5: Cross-Cutting Concerns (AOP)
**User Story 5.1** As a developer, I want to use AOP for logging and monitoring so that common concerns are handled centrally.
- Acceptance Criteria: AOP aspects implemented using @Before, @After, and @Around, logging and monitoring applied to critical service methods, implementation explained in project documentation.

## Technical Requirements

| Area | Description |
|---|---|
| Framework | Spring Boot 3.x (Spring Web, Validation, AOP, GraphQL, Springdoc OpenAPI) |
| Language | Java 21 |
| Database | Relational database designed in Module 4 |
| Architecture | Layered (Controller → Service → Repository) |
| Validation | Bean Validation annotations and custom validators |
| Documentation | Springdoc OpenAPI for Swagger documentation |
| Cross-Cutting Concerns | Logging and performance monitoring with AOP |
| Testing & Interaction | APIs tested with Postman, JavaFX, or any web frontend |
| DSA Integration | Sorting, searching, and pagination algorithms used in API logic |

## Deliverables

| Deliverable | Description |
|---|---|
| Spring Boot Web Application | Backend exposing REST and GraphQL APIs connected to the database. |
| Validation and Exception Handling | DTOs with validation rules and centralized error management. |
| API Documentation | Interactive Swagger/OpenAPI documentation. |
| AOP Implementation | Logging and performance aspects using AOP. |
| GraphQL Schema and Queries | Defined schema with queries and mutations. |
| Performance Report | Analysis comparing REST and GraphQL performance. |
| README File | Setup instructions and API testing guide. |

## Evaluation Criteria

| Category | Description | Points |
|---|---|---|
| Spring Boot Configuration & IoC | Proper setup, DI usage, and IoC application. | 15 |
| REST API Development | CRUD functionality and RESTful architecture correctly implemented. | 20 |
| Validation & Documentation | Validation, exception handling, and OpenAPI documentation in place. | 20 |
| GraphQL & Data Integration | GraphQL queries, mutations, and performance comparisons implemented. | 15 |
| AOP & Algorithmic Optimization | Logging, monitoring, and algorithmic improvements applied effectively. | 15 |
| Code Quality & Reporting | Well-structured code, readability, and analytical reporting. | 15 |
| **Total** |  | **100 pts** |

## Testing

Run tests with:
```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
