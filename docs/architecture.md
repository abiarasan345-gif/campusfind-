# CampusFind Architecture

```text
HTML/CSS/Vanilla JS
        │
        ▼
REST Controllers
        │
        ▼
Service Layer
(Business Rules / Matching / Authorization)
        │
        ▼
Repository Layer
(Spring Data JPA)
        │
        ▼
MySQL
```

## Main entities
- User
- Category
- LostReport
- FoundItem

## Relationships
- User 1:N LostReport
- User 1:N FoundItem
- Category 1:N LostReport
- Category 1:N FoundItem
- LostReport 0..1:1 Matched FoundItem
