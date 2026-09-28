# CampusFind ER Diagram

```text
USERS                       CATEGORIES
-----                       ----------
id PK                       id PK
username UNIQUE             category_name UNIQUE
password_hash                    | 
role                             | 1:N
full_name                        +---------------------+
email                            |                     |
created_at                       ↓                     ↓
                           LOST_REPORTS          FOUND_ITEMS
                           ------------          -----------
                           id PK                 id PK
                           user_id FK            staff_user_id FK
                           category_id FK        category_id FK
                           matched_found_item_id item_name
                           item_name             description
                           description           location
                           location              date_found
                           date_lost             status
                           status                created_at
                           created_at
                                |
                                | 0..1:1
                                +-----------> FOUND_ITEMS.id
```
