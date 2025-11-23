# Identity Service

This is the custom OAuth2/JWT Identity Service used by the Stall Service for authentication.

## Source Code

The complete source code for this service is available in the `custom-IAM.txt` file at the root of this repository.

## Quick Setup

1. Extract the source code from `custom-IAM.txt`
2. Place the code in this directory following the structure shown in the file
3. Build with: `mvn clean package`
4. Run via Docker Compose (already configured)

## Key Files Needed

- `pom.xml` - Maven configuration
- `Dockerfile` - Container build instructions
- `src/main/java/**` - Application source code
- `src/main/resources/**` - Configuration files
- `.env` - Environment variables (see env.example in custom-IAM.txt)

## Default User

- Username: `admin`
- Password: `admin123`
- Role: ADMIN

## Endpoints

- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/users/me` - Get current user
- `GET /api/v1/actuator/health` - Health check

See custom-IAM.txt for complete API documentation.
