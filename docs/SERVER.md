# Развёртывание на выделенном Linux-сервере без root и Docker

Целевая модель: собираем исполняемый JAR **на своём ПК**, копируем по SSH на сервер, подключаем к **установленной кафедральной PostgreSQL** `pg:5432/studs`, запускаем Java из своего пользовательского каталога. На сервере **не нужны** Gradle, Docker, root-права и system-wide пакеты.

## 1. Проверяем доступные возможности

```bash
ssh YOUR_LOGIN@YOUR_SERVER
java -version
getent hosts pg
# При наличии psql:
psql -h pg -d studs -U YOUR_LOGIN -c 'select current_user, current_database();'
```

Нужна Java 21+. Если серверная Java старее и политика вуза разрешает, положите пользовательский JDK 21 в `~/apps/jdk21` и добавьте его `bin` в PATH; root не нужен. Уточните политику кафедрального сервера относительно запуска долгоживущих процессов и входящих TCP-портов.

## 2. Очень важно: изоляция в общей БД

`studs` может содержать данные других студентов. **Не пользуйтесь таблицами общего `public`, если можно создать/использовать личную схему.** Приложение поддерживает `DB_SCHEMA` (например, `YOUR_LOGIN`). У пользователя должны быть `USAGE` и `CREATE` в существующей схеме, а также права на таблицы и последовательности, которые он создал.

Проверить схемы (psql):

```sql
SELECT schema_name FROM information_schema.schemata ORDER BY 1;
SELECT has_schema_privilege(current_user, 'YOUR_SCHEMA', 'USAGE,CREATE');
```

Если личной схемы нет, попробовать `CREATE SCHEMA YOUR_SCHEMA AUTHORIZATION YOUR_LOGIN;` **только если разрешено**. Если нет прав, попросите кафедру выделить схему. В проекте `spring.flyway.create-schemas=false`: он специально **не создаёт произвольную схему**. Внутри подготовленной схемы Flyway создаёт только объекты проекта с префиксом `person_lab_`.

При `DB_SCHEMA=YOUR_SCHEMA` Flyway и Hibernate используют одну схему. Для JDBC допускается добавить `?currentSchema=YOUR_SCHEMA` к `DB_URL`; если URL уже содержит `?`, добавлять с `&`. Этот параметр не обязателен при согласованной конфигурации Flyway/Hibernate, но полезен для однозначности search_path.

## 3. Собираем JAR локально

```powershell
gradle clean test bootJar
```

Файл: `build/libs/orbis.jar`.

## 4. Передаём файлы

В Windows PowerShell:

```powershell
ssh YOUR_LOGIN@YOUR_SERVER 'mkdir -p ~/orbis/config ~/orbis/scripts ~/orbis/run ~/orbis/logs'
scp build/libs/orbis.jar YOUR_LOGIN@YOUR_SERVER:~/orbis/orbis.jar
scp config/server.env.example YOUR_LOGIN@YOUR_SERVER:~/orbis/config/server.env.example
scp scripts/start-server.sh scripts/stop-server.sh YOUR_LOGIN@YOUR_SERVER:~/orbis/scripts/
```

## 5. Настраиваем секреты и профиль

На сервере:

```bash
cd ~/orbis
cp config/server.env.example config/server.env
nano config/server.env
chmod 600 config/server.env
chmod +x scripts/*.sh
```

Заполните **реальными** логином и паролем кафедрального PostgreSQL и личной схемой. **APP_USER1/2 больше не нужны:** учётные записи регистрируются через `/register` и сохраняются в PostgreSQL. Не публикуйте `server.env`, не копируйте пароль в Git. На сервере используется Spring profile `server`; `local` запрещено включать для реального доступа.

Пример:

```bash
DB_URL='jdbc:postgresql://pg:5432/studs'
DB_USER='YOUR_LOGIN'
DB_PASSWORD='SECRET'
DB_SCHEMA='YOUR_SCHEMA'
PORT=8080
BIND_ADDRESS=127.0.0.1
```

### Примечание о пароле и файле .env

`start-server.sh` читает файл через `source`: значения в кавычках — shell-строки. Если пароль содержит специальные shell-символы, заключите значение в одинарные кавычки и корректно экранируйте внутренние `'`. Файл должен быть доступен только вам (`chmod 600`).

## 6. Запускаем без root

```bash
cd ~/orbis
./scripts/start-server.sh
tail -f logs/orbis.log
```

Ожидаем сообщения Flyway об успешных миграциях (`V1` и `V2` для новой установки; только `V2` для ранее развернутой) и `Started OrbisApplication`. Откройте через туннель `/register`, создайте пользователя и войдите через `/login`. Без профиля `local` тестовые учётные записи не создаются.

Остановка:

```bash
./scripts/stop-server.sh
```

Этот вариант использует `nohup`, PID-файл и лог в `~/orbis/logs/orbis.log`. Не требует systemd и root. **Но** сервер может убивать фоновые процессы по окончании SSH-сессии. Для гарантированного постоянного запуска уточните, разрешены ли `tmux`, `systemd --user`/`loginctl enable-linger` либо кафедральный супервизор (последний вариант может потребовать администратора). Ротацию логов также нужно организовать отдельно, если процесс используется долго.

## 7. Как открыть страницу с локального ПК

Приложение по умолчанию слушает **только `127.0.0.1` на сервере**, без публикации в интернет. Откройте другой PowerShell на своём ПК:

```powershell
ssh -N -L 8080:127.0.0.1:8080 YOUR_LOGIN@YOUR_SERVER
```

В браузере ПК откройте `http://localhost:8080`.

Если порт 8080 на ПК занят, `ssh -N -L 8090:127.0.0.1:8080 ...` и `http://localhost:8090`.

Для публичного доступа потребуется согласованный с администратором HTTPS reverse proxy (или иная поддерживаемая вузом схема). Без разрешённого входящего порта **нельзя гарантировать**, что сайт будет доступен извне напрямую. Не меняйте `BIND_ADDRESS=0.0.0.0` без проверки правил сервера и TLS.

## 8. Обновление версии

```bash
~/orbis/scripts/stop-server.sh
# С ПК передайте свежий JAR через scp.
~/orbis/scripts/start-server.sh
```

Миграции Flyway применяются автоматически только после выполнения предыдущих миграций. Миграция V2 создаёт `person_lab_users`; ранее конфигурируемые in-memory пользователи не переносились (нужно зарегистрироваться заново). Перед обновлением делайте резервное копирование своих таблиц (при наличии разрешений), тестируйте локально, не редактируйте уже применённые V1/V2 SQL.
