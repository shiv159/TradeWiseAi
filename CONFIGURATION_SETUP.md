# Configuration Setup

## Application Properties

The application uses `application.properties` for configuration, which is ignored by Git for security reasons.

### Setup Instructions:

1. Copy the template file:
   ```bash
   cp src/main/resources/application.properties.template src/main/resources/application.properties
   ```

2. Edit `application.properties` and replace the placeholder values:
   - `YOUR_API_KEY_HERE` with your actual AlphaVantage API key
   - MongoDB connection details if different from defaults
   - Other configuration values as needed

### Configuration Properties:

#### MongoDB Configuration
- `spring.data.mongodb.uri`: MongoDB connection string
- `spring.data.mongodb.database`: Database name

#### AlphaVantage API Configuration
- `tradewise.alpha-vantage.api-key`: Your AlphaVantage API key
- `tradewise.alpha-vantage.base-url`: API base URL
- `tradewise.alpha-vantage.symbol-suffix`: Symbol suffix for BSE stocks
- `tradewise.alpha-vantage.timeout-seconds`: API timeout in seconds

#### Cache Configuration
- `tradewise.cache.ttl-minutes`: Cache time-to-live in minutes
- `tradewise.cache.enabled`: Enable/disable caching

#### Cosmos DB Configuration (Optional)
- `tradewise.cosmos-db.enabled`: Enable/disable Cosmos DB
- `tradewise.cosmos-db.endpoint`: Cosmos DB endpoint
- `tradewise.cosmos-db.key`: Cosmos DB access key
- `tradewise.cosmos-db.database-name`: Database name
- `tradewise.cosmos-db.container-name`: Container name

## Security Notes

- Never commit `application.properties` with real API keys
- Use environment variables for production deployments
- The template file can be safely committed to version control
