# Локальный запуск на Windows (IntelliJ IDEA)

## 1. Требования

- **JDK 21** (проверьте `java -version`).
- **IntelliJ IDEA** с плагинами Kotlin/Gradle.
- **Gradle 8.14+ или Gradle 9.x**. Wrapper не включён в этот архив, поскольку его бинарный `gradle-wrapper.jar` нельзя было получить в сборочной среде. В IDEA откройте Gradle Settings → Distribution → *Specified version*; либо установите Gradle отдельно. После первичной загрузки можно выполнить `gradle wrapper --gradle-version 8.14.3` и добавить сгенерированные файлы в Git.
- PostgreSQL 15+ (локальный Docker **или** установленный Postgres без Docker).

## 2A. PostgreSQL через Docker (локально)

PowerShell в корне проекта:

```powershell
docker compose up -d
docker compose ps
```

Compose использует прежнее техническое имя проекта `person-lab`, чтобы повторно подключить существующий volume с данными после переименования интерфейса в Orbis. Не удаляйте volume (`docker compose down -v` приведёт к потере локальных данных).

Включён только Postgres на `127.0.0.1:5432`, данные сохраняются в Docker volume.

## 2B. PostgreSQL без Docker

Создайте пользователя и БД удобным способом (pgAdmin либо psql от администратора PostgreSQL):

```sql
CREATE ROLE person_lab LOGIN PASSWORD 'local_password';
CREATE DATABASE person_lab OWNER person_lab;
```

Если порт другой, установите `DB_URL`, `DB_USER`, `DB_PASSWORD` в переменных окружения или в Run Configuration.

## 3. Запуск

В PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
gradle bootRun
```

Или в IDEA запустите `OrbisApplication.kt` и в Run Configuration укажите `SPRING_PROFILES_ACTIVE=local`.

Откройте `http://localhost:8080/register`, введите логин, пароль и повтор пароля. После успешной регистрации войдите через `/login`.

В профиле `local` также автоматически создаются демонстрационные пользователи `alice / local_alice_password` и `bob / local_bob_password`. Используйте их в разных браузерах для проверки WebSocket. На сервере их **нет**.

## 4. Внутри БД

При первом запуске Flyway создаст `person_lab_locations`, `person_lab_people`, `person_lab_audit`, `person_lab_idempotency`, `person_lab_users`, `person_lab_schema_history`. Hibernate только проверяет схему (`ddl-auto: validate`).

```sql
SELECT * FROM person_lab_people ORDER BY id;
SELECT * FROM person_lab_locations ORDER BY id;
SELECT * FROM person_lab_audit ORDER BY id DESC;
SELECT * FROM person_lab_schema_history;
SELECT id, username, created_at FROM person_lab_users ORDER BY id; -- не выводите password_hash без нужды
```

## 5. Тесты и сборка

```powershell
gradle clean test bootJar
```

Итог: `build/libs/orbis.jar`.

## 6. Если не запускается

- `Connection refused`: PostgreSQL не запущен или порт занят.
- `password authentication failed`: проверьте DB_USER/DB_PASSWORD и то, что старый Docker volume не хранит предыдущий пароль.
- `missing table`: проверьте, что активна Flyway автонастройка (`spring-boot-starter-flyway`), миграция `V1__person_lab.sql`, и у пользователя есть CREATE в схеме.
- `schema-validation`: сравните структуру таблиц с миграцией; не применяйте `ddl-auto=update` для исправления наугад.
- порт 8080 занят: `$env:PORT='8090'` перед запуском.
- `Could not resolve dependencies`: необходимо первое подключение к Maven Central / Gradle Plugin Portal.

## Стандартная английская форма входа вместо Orbis

Если видите «Please sign in» / «Sign in», это не шаблон `templates/auth/login.html` из Orbis.
Остановите все запущенные приложения в IntelliJ, проверьте, кто занимает порт `8080` командой
`netstat -ano | findstr :8080`, затем запустите `OrbisApplicationKt` с
`SPRING_PROFILES_ACTIVE=local`. В логе должно быть `Starting OrbisApplicationKt`,
активный профиль `local` и `Orbis local demo accounts ready: alice, bob`.

Готовая диагностика из PowerShell в корне проекта:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1
```

Скрипт проверяет, что по `/login` и `/register` реально отдаются страницы Orbis. Если ранее
у тестового `bob` был другой пароль, профиль `local` восстановит стандартный демо-пароль
при старте. Обычных пользователей и данные `Person` это не изменяет.
