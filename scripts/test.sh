# Register a new user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "wendy@example.com",
    "password": "wendy@example.com",
    "name": "wendy@example.com"
  }'

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "wendy@example.com",
    "password": "wendy@example.com"
  }'

# Get current user profile
curl -X GET http://localhost:8081/api/auth/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg"

# Create a new travel project
curl -X POST http://localhost:8081/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg" \
  -d '{
    "name": "Tokyo Adventure",
    "description": "3-day trip to Tokyo",
    "startDate": "2024-03-15",
    "endDate": "2024-03-17",
    "budget": 1500.00
  }'

# Get all projects
curl -X GET http://localhost:8081/api/projects \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg"

# Get specific project
curl -X GET http://localhost:8081/api/projects/PROJECT_ID \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg"

# Update project
curl -X PUT http://localhost:8081/api/projects/PROJECT_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg" \
  -d '{
    "name": "Updated Tokyo Adventure",
    "description": "Updated 3-day trip to Tokyo",
    "budget": 2000.00
  }'

# Delete project
curl -X DELETE http://localhost:8081/api/projects/PROJECT_ID \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInN1YiI6IndlbmR5QGV4YW1wbGUuY29tIiwiaWF0IjoxNzYwNDAxOTIyLCJleHAiOjE3NjA0ODgzMjJ9.brm9EYk6s9mcQCQHtEw1eNSpAK6_vXg3mP__QYu9pTg"