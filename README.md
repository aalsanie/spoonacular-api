# Spoonacular Service
a comprehensive set of endpoints focused on recipes, food products, nutrition data, and meal planning relies on Spoonacular API.

### Table of Contents
- [Setup & Run Instructions](#Setup-&-Run-Instructions)
- [API documentation](#API-Documentation)
- [Architecture](#Architecture)
- [Design Decisions](#Choice-of-technology)
- [Future Improvements](#Future-Improvements)

### Setup & Run Instructions
Detailed steps to get your development environment running:

#### Prerequisites:
- Java JDK 17 or higher
- Maven
- Docker
- API Key [API docs](https://spoonacular.com/food-api/docs#Authentication)

#### Clone the Repository
```shell
git clone https://github.com/Ahmad-alsanie/atypon-food-api.git
cd atypon-food-api
```

#### Tests
```shell
./mvnw clean test
```

![Jacoco](./jacoco.png)

#### Without Docker
#### Build & Run the Service
```shell
mvn clean install
```

setup your API KEY

```yaml
spoonacular:
  base-url: https://api.spoonacular.com
  api-key: ${API_KEY} # Set API key via environment variables
```

```shell
 mvn spring-boot:run
```

#### With Docker
#### Build & Run the Service
```shell
docker build -t ahmad/atypon-food-api .
```

```shell
docker run -p 8080:8080 -d ahmad/atypon-food-api
```

### API Documentation
Navigate to [Swagger API documentation](http://localhost:8080) to view swagger documentation of our endpoints.

| API       | supported methods | onSuccess | onFailure  |
|-----------|-------------------|-----------|------------|
| /search   | GET               | 200       | empty list | 
| /info     | GET               | 200       | 404        | 
| /calories | POST              | 202       | 400        | 


### Architecture
Key components of our service:
- Controller: serves as the entry point for HTTP requests and handles Restful endpoints ```search```,```info``` and ```calories```
- Service: contains the core business logic, manages process events and handles interactions requests with Spoonacular
- Model: contains a representation of our main entities
- Configuration: manages service properties and configure the service
- Tests: we have both unit tests and integration tests covering all edge cases and providing assurance with >80% coverage

### End points in plain english
- Search Recipes: Query recipes using parameters like name and cuisine.

- Recipe Information: Provide details about a specific recipe including total calories.

- Customized Calorie Information: Calculate calories by excluding specific ingredients.

### Choice of Technology
- Spring Boot for rapid development features and simplifies the creation of stand-alone, production ready microservices with its autoconfigured components & IOC container.
- API Client: RestTemplate (for calling Spoonacular APIs)
- Tests: JUnit, Mockito for unit and integration tests >80% coverage
- Tests: stress test
- Docker for containerization allowing us to package and run our application in a loosely isolated environment

### Future Improvements
API limits were a challenge during development, so caching strategies might help in production.
Mapping complex JSON responses requires careful testing.

- Add caching with Redis for frequently searched recipes.
- Add authentication and rate-limiting if this will be a public-facing API.
