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

## License

This project is licensed under the MIT License - see the LICENSE file for details.
