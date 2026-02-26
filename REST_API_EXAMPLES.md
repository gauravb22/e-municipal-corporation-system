# Spring Boot REST API Examples

This project now exposes JSON APIs for `Complaint`, `User`, and `Ward` under `/api`.

## Base Notes
- `Content-Type: application/json`
- Use JWT bearer token for protected endpoints:
  - `Authorization: Bearer <token>`

## 1) Auth API

### POST `/api/auth/login`
Request:
```json
{
  "role": "CITIZEN",
  "phone": "9876543210",
  "password": "secret123"
}
```

Success (`200 OK`):
```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "expiresAt": "2026-02-26T18:30:00Z",
  "expiresInMinutes": 120,
  "userId": 12,
  "loginId": "9876543210",
  "role": "CITIZEN",
  "principalType": "CITIZEN",
  "fullName": "Gaurav Kumar"
}
```

Error (`400 BAD REQUEST`):
```json
{
  "error": "Invalid password."
}
```

Error (`404 NOT FOUND`):
```json
{
  "error": "Citizen account not found."
}
```

## 2) Complaint APIs

### GET `/api/complaints`
Success (`200 OK`):
```json
[
  {
    "id": 101,
    "userId": 12,
    "complaintType": "Garbage",
    "location": "Main Road",
    "houseNo": "A-12",
    "wardNo": 9,
    "wardZone": "A",
    "wardId": 4,
    "description": "Garbage not collected",
    "notComingFromDate": null,
    "photoTimestamp": "2026-02-25T08:45:00",
    "photoLocation": "Main Road",
    "photoLatitude": "21.1458",
    "photoLongitude": "79.0882",
    "photoPath": "/uploads/complaints/101/before.jpg",
    "donePhotoTimestamp": null,
    "donePhotoLocation": null,
    "donePhotoLatitude": null,
    "donePhotoLongitude": null,
    "donePhotoPath": null,
    "status": "submitted",
    "createdAt": "2026-02-25T08:46:10",
    "updatedAt": null,
    "feedbackSubmitted": false,
    "feedbackDescription": null,
    "feedbackRating": null,
    "feedbackSolved": null,
    "feedbackSolvedBy": null,
    "feedbackSubmittedAt": null
  }
]
```

### GET `/api/complaints/{id}`
Success (`200 OK`) returns one complaint object.

Error (`404 NOT FOUND`):
```json
{
  "error": "Complaint not found."
}
```

### POST `/api/complaints`
Request:
```json
{
  "userId": 12,
  "complaintType": "Garbage",
  "location": "Main Road",
  "houseNo": "A-12",
  "description": "Garbage not collected from 3 days",
  "wardNo": 9,
  "wardZone": "A",
  "photoTimestamp": "2026-02-25T08:45:00",
  "photoLocation": "Main Road",
  "photoLatitude": "21.1458",
  "photoLongitude": "79.0882",
  "photoBase64": null,
  "notComingFromDate": null
}
```

Success (`201 CREATED`) returns created complaint object.

Error (`400 BAD REQUEST`):
```json
{
  "error": "complaintType is required."
}
```

### PUT `/api/complaints/{id}`
Request:
```json
{
  "status": "approved",
  "description": "Assigned to ward team",
  "wardNo": 9,
  "wardZone": "A"
}
```

Success (`200 OK`) returns updated complaint object.

Error (`404 NOT FOUND`):
```json
{
  "error": "Complaint not found."
}
```

### DELETE `/api/complaints/{id}`
Success (`200 OK`):
```json
{
  "message": "Complaint deleted successfully."
}
```

Error (`404 NOT FOUND`):
```json
{
  "error": "Complaint not found."
}
```

## 3) User APIs

### GET `/api/users`
Success (`200 OK`) returns list of users.

### GET `/api/users/{id}`
Success (`200 OK`) returns one user.

Error (`404 NOT FOUND`):
```json
{
  "error": "User not found."
}
```

### POST `/api/users`
Request:
```json
{
  "fullName": "Ravi Sharma",
  "email": "ravi@example.com",
  "password": "secret123",
  "phone": "9876543210",
  "address": "Civil Lines",
  "houseNo": "B-44",
  "wardNo": 9,
  "wardZone": "A",
  "photoBase64": null,
  "active": true
}
```

Success (`201 CREATED`) returns created user (without password).

Error (`400 BAD REQUEST`):
```json
{
  "error": "Phone is already registered."
}
```

### PUT `/api/users/{id}`
Request:
```json
{
  "fullName": "Ravi S. Sharma",
  "address": "New Colony",
  "active": false
}
```

Success (`200 OK`) returns updated user object.

### DELETE `/api/users/{id}`
Success (`200 OK`):
```json
{
  "message": "User deleted successfully."
}
```

Error (`404 NOT FOUND`):
```json
{
  "error": "User not found."
}
```

## 4) Ward APIs

### GET `/api/wards`
Success (`200 OK`) returns list of wards.

### GET `/api/wards/{id}`
Success (`200 OK`) returns one ward.

Error (`404 NOT FOUND`):
```json
{
  "error": "Ward not found."
}
```

### POST `/api/wards`
Request:
```json
{
  "wardNo": 9,
  "wardZone": "A",
  "name": "Ward 9A"
}
```

Success (`201 CREATED`) returns created ward.

Error (`400 BAD REQUEST`):
```json
{
  "error": "Ward already exists for wardNo 9 and wardZone A."
}
```

### PUT `/api/wards/{id}`
Request:
```json
{
  "name": "Ward 9A - Central"
}
```

Success (`200 OK`) returns updated ward object.

### DELETE `/api/wards/{id}`
Success (`200 OK`):
```json
{
  "message": "Ward deleted successfully."
}
```

Error (`404 NOT FOUND`):
```json
{
  "error": "Ward not found."
}
```
