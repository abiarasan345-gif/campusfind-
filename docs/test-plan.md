# CampusFind Test Plan

## Automated tests
- Found item: reject AVAILABLE → RETURNED
- Found item: allow AVAILABLE → CLAIMED → RETURNED
- Found item: reject a status change by another user
- Matching: same category + same location produces a match

Run:

```powershell
mvn clean test
```

## Manual API test matrix
| ID | Scenario | Expected |
|---|---|---|
| T01 | Register new student | 201, row persisted in `users` |
| T02 | Duplicate username | 409 |
| T03 | Login with valid credentials | 200 + session |
| T04 | Create lost report | 201 |
| T05 | Create found item as student | 403 |
| T06 | Create found item as staff | 201 |
| T07 | List lost/found with filters | 200, DB-backed results |
| T08 | Matching same category/location | match returned |
| T09 | Confirm match | found CLAIMED + lost MATCHED |
| T10 | AVAILABLE → RETURNED | 400, no DB update |
| T11 | Staff A changes Staff B item | 403, no DB update |
| T12 | CLAIMED → RETURNED | 200 |
| T13 | Unknown lost/found ID | 404 |
| T14 | Missing required field | 400 + validationErrors |
| T15 | Admin dashboard | 200, counts from MySQL |
| T16 | Delete own report | 204 |

## UI test flow
1. Register or login.
2. Create a lost item.
3. Login as `staff1` and create a found item with the same category/location.
4. Open Matches and confirm the match.
5. Verify found item becomes CLAIMED and lost report becomes MATCHED.
6. Return the found item and verify RETURNED.
7. Login as Admin and verify dashboard counts change.
8. Try invalid status transition and confirm the UI shows the backend error without changing the table.
9. Use category/location/keyword filters and verify only backend results are displayed.
10. Refresh pages and verify data is still present because it is stored in MySQL.
