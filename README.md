# x-bot

### Перед запуском скопируйте пример файла переменных:
```
cp .env.example .env
```
И заполните значения:
```
BOT_USERNAME=your_bot_username
BOT_TOKEN=your_bot_token
```

### Сборка и запуск

Проект содержит скрипт compose.sh, который:

* определяет текущую git-ветку;
* генерирует Unix-timestamp;
* формирует тег <branch>:<timestamp>;
* собирает Docker-образ;
* запускает контейнер через Docker Compose.

Запуск
```
./compose.sh
```

### Как устроена сборка

Используется multi-stage Dockerfile:
* Stage 1: Maven + JDK 21 (сборка JAR)
* Stage 2: Лёгкий JRE 21 для запуска

Оптимизации:
* кеш Maven-зависимостей
* .dockerignore исключает .git, target, IDE-файлы
* параллельная компиляция (mvn -T 1C)
* минимальный runtime-образ