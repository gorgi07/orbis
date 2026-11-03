# REST API и сценарии проверки

Все `/api/**` требуют пользовательской cookie-сессии. Все изменяющие HTTP-запросы требуют CSRF token, поэтому для простого тестирования сначала используйте браузерный UI; для Postman получите сессионную cookie и токен через HTML страницы. В примерах тела ниже приведён JSON (HTTP Header `Content-Type: application/json`).

## Person

- `GET /api/people?page=0&size=15&sortBy=name&direction=asc&name=Анна` — **строгое** `name = 'Анна'`, не LIKE; фильтры `eyeColor`, `hairColor`, `nationality`, `locationName` поддерживают точное равенство. Нет параметра = нет ограничения.
- `GET /api/people/1` — подробности и связанная Location.

Ответ списка имеет **стабильный DTO страницы** (не сериализуется `PageImpl`):

```json
{
  "content": [{ "id": 1, "name": "Анна", "...": "остальные атрибуты" }],
  "number": 0,
  "size": 15,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Поля оставлены совместимыми с прежним интерфейсом; это устраняет предупреждение Spring Data о прямой сериализации `PageImpl` и фиксирует явный контракт API.
- `POST /api/people` + `Idempotency-Key: unique-key-01` — создание, ответ `201 {"id": 1, "replayed": false}`.
- `PUT /api/people/1` — изменение с обязательной текущей версией; при конфликте `409`.
- `DELETE /api/people/1` — `204`, включая повторный DELETE.

Пример POST:

```json
{
  "name":"Анна", "coordinateX":10, "coordinateY":20.5,
  "eyeColor":"BLUE", "hairColor":"RED", "locationId":null,
  "height":171.5, "weight":60.3, "nationality":"ITALY"
}
```

## Location

- `GET /api/locations` — список локаций.
- `GET /api/locations/1` — карточка локации.
- `GET /api/locations/1/people` — люди, привязанные к конкретной локации по ID (не по совпадению названий).
- `POST /api/locations`: `{"x":15,"y":20.5,"name":"Двор"}`
- `PUT /api/locations/1`: то же тело + `"version":0` (актуальное значение с GET).
- `DELETE /api/locations/1?replacementLocationId=2`: перенос **всех** людей с Location 1 на Location 2 и удаление Location 1, атомарно. Без replacementLocationId разрешено лишь при отсутствии связанных Person.

## Специальные операции

- `POST /api/operations/delete-by-weight?weight=70` + уникальный `Idempotency-Key` — удаляет **одного** Person с точным весом, возвращает его ID или `null`, при повторах не удаляет других.
- `GET /api/operations/max-id` — один Person с максимальным ID либо `null`.
- `GET /api/operations/count-height?height=180` — количество Person с `height < 180`.
- `GET /api/operations/count-hair?color=RED` — количество с RED волосами.
- `GET /api/operations/count-eyes?color=BLUE` — количество с BLUE глазами.

## Аудит / realtime

- `GET /api/audit` — последние 60 событий.
- `ws(s)://<host>/ws/events` — уведомление `{ "entity":"PERSON", "id":"1", "action":"UPDATE" }` после коммита. Откройте `alice` и `bob` в двух браузерах и сравните обновление таблицы.

## Ручной функциональный чек-лист

1. Создать 2 Location; связать 3 Person с первой.
2. Убедиться, что Person видны на странице списка и в подробностях по ID.
3. Проверить создание с пустым name, нулевым height/weight, пустым location.name, несуществующим locationId — показать внятную ошибку.
4. Точная фильтрация: `Анна` не должна совпасть с `Анна Мария`.
5. Сортировка, перелистывание более чем 15 записей.
6. Открыть 2 сессии; изменить человека в первой — таблица второй обновится автоматически.
7. Открыть одного Person в двух вкладках, обе открыть для редактирования, обновить первую и сохранить вторую — ожидается 409.
8. Открыть карточку Location 1 из таблицы; проверить связанные Person и переход по ссылке. Удалить Location 1 с переназначением на Location 2; проверить, что все Person остались, ссылки сменились.
9. Удалить одного человека по весу; повторить **тот же HTTP запрос с тем же Idempotency-Key** — следующий человек с этим весом не удалится.
10. Проверить все count и maxId; проверить аудит, затем послать неверный запрос и убедиться, что данные не изменились.
11. Проверить вход в систему с неправильным паролем, logout и запрет `/api/people` без cookie.

## Что дополнительно протестировать перед сдачей

- Конкурентные действия `delete-by-weight` и `delete location` с 2 браузеров.
- Переподключение WebSocket после перезапуска сервера.
- Количество строк при пустых и нулевых результатах.
- Сохранение даты и корректную временную зону (PostgreSQL TIMESTAMPTZ, UTC).
