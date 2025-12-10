#!/usr/bin/env bash
set -e

BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Заменяем все опасные символы в ветке на дефис
SAFE_BRANCH=$(echo "$BRANCH" | sed 's/[^a-zA-Z0-9_.-]/-/g')

# Unix timestamp (epoch seconds)
TS=$(date +%s)

# Формат тега: <branch>-<timestamp>
IMAGE_TAG="${SAFE_BRANCH}-${TS}"

echo "Using image tag: xbot:${IMAGE_TAG}"

IMAGE_TAG="$IMAGE_TAG" docker compose up --build -d
