# Docker Setup for TradeWise AI

This document provides instructions for setting up MongoDB using Docker Compose for the TradeWise AI application.

## Prerequisites

- Docker and Docker Compose installed on your system
- Java 21 and Maven (for running the application)

## Quick Start

### 1. Start MongoDB with Docker Compose

```bash
# Start MongoDB and MongoDB Express
docker-compose up -d

# Check if containers are running
docker-compose ps
```

### 2. Configure Application

Copy the Docker-specific configuration:
```bash
cp src/main/resources/application-docker.properties src/main/resources/application.properties
```

**Important**: Replace `YOUR_API_KEY_HERE` with your actual AlphaVantage API key in `application.properties`.

### 3. Run the Application

```bash
# Build and run the application
./mvnw spring-boot:run
```

## Services

### MongoDB
- **Container Name**: `tradewise-mongodb`
- **Port**: `27017`
- **Database**: `tradewise-db`
- **Admin User**: `admin` / `password123`
- **App User**: `tradewise-user` / `tradewise-password`

### MongoDB Express (Web UI)
- **Container Name**: `tradewise-mongo-express`
- **URL**: http://localhost:8081
- **Username**: `admin`
- **Password**: `admin123`

## Database Structure

The initialization script creates:

### Collections
- `stock_data` - Main collection for storing stock data

### Indexes
- **Compound Index**: `stockSymbol + dataType` (unique)
- **Single Index**: `stockSymbol` (for general queries)
- **Single Index**: `lastUpdated` (for TTL operations)

## Volume Management

### Persistent Data
MongoDB data is stored in Docker volumes:
- `mongodb_data` - Database files
- `mongodb_config` - Configuration files

### Backup Data
```bash
# Create backup
docker exec tradewise-mongodb mongodump --db tradewise-db --out /backup

# Copy backup from container
docker cp tradewise-mongodb:/backup ./mongodb-backup
```

### Restore Data
```bash
# Copy backup to container
docker cp ./mongodb-backup tradewise-mongodb:/backup

# Restore backup
docker exec tradewise-mongodb mongorestore --db tradewise-db /backup/tradewise-db
```

## Docker Commands

### Start Services
```bash
# Start all services
docker-compose up -d

# Start only MongoDB
docker-compose up -d mongodb

# View logs
docker-compose logs -f mongodb
```

### Stop Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: This deletes all data)
docker-compose down -v
```

### Restart Services
```bash
# Restart MongoDB
docker-compose restart mongodb

# Rebuild and restart
docker-compose up -d --build
```

## Connecting to MongoDB

### From Application
The application connects using the configuration in `application-docker.properties`:
```properties
spring.data.mongodb.uri=mongodb://tradewise-user:tradewise-password@localhost:27017/tradewise-db?authSource=tradewise-db
```

### Using MongoDB Shell
```bash
# Connect as admin
docker exec -it tradewise-mongodb mongosh -u admin -p password123 --authenticationDatabase admin

# Connect as application user
docker exec -it tradewise-mongodb mongosh -u tradewise-user -p tradewise-password --authenticationDatabase tradewise-db
```

### Using MongoDB Compass
- **Connection String**: `mongodb://tradewise-user:tradewise-password@localhost:27017/tradewise-db?authSource=tradewise-db`

## Environment Variables

You can override default values using environment variables:

```bash
# Set environment variables
export MONGO_ROOT_PASSWORD=your-secure-password
export MONGO_APP_PASSWORD=your-app-password

# Start with custom environment
docker-compose up -d
```

## Troubleshooting

### Container Won't Start
```bash
# Check container logs
docker-compose logs mongodb

# Check if port is in use
netstat -an | findstr :27017
```

### Connection Issues
```bash
# Test MongoDB connection
docker exec tradewise-mongodb mongosh --eval "db.runCommand('ping')"

# Check network connectivity
docker network ls
docker network inspect tradewise_tradewise-network
```

### Reset Database
```bash
# Stop services and remove volumes
docker-compose down -v

# Start fresh
docker-compose up -d
```

## Security Notes

### For Development
- Default passwords are used for convenience
- MongoDB authentication is enabled

### For Production
- Change all default passwords
- Use environment variables for secrets
- Enable SSL/TLS encryption
- Configure proper firewall rules
- Use MongoDB Atlas or managed service

## Configuration Files

- `docker-compose.yml` - Docker Compose configuration
- `mongo-init/init-tradewise-db.js` - MongoDB initialization script
- `application-docker.properties` - Application configuration for Docker

## API Testing

Once the application is running, you can test the APIs:

```bash
# Test current price endpoint
curl "http://localhost:8080/api/v1/getCurrentPrice?symbol=AAPL"

# Test historical price endpoint
curl "http://localhost:8080/api/v1/historicalPrice?symbol=AAPL"
```

The first request will fetch data from AlphaVantage API and cache it in MongoDB. Subsequent requests will return cached data if it's fresh (within TTL).
