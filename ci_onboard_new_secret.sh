#!/bin/bash

set -e          # Exit immediately if a command exits with a non-zero status
set -o pipefail # Ensure piped commands fail properly
REPO_DIR="$(pwd)"

echo "🔐 Syncing secrets..."
cd "$REPO_DIR/secrets"
./sync.sh
cd "$REPO_DIR"

echo "📊 Running DBT CI script..."
cd "$REPO_DIR/data_models"
python3 .dbt_ci_script.py
cd "$REPO_DIR"

echo "🐳 Building dbt Docker image..."
docker build data_models -t dbt:dev

echo "📦 Loading image into Minikube..."
minikube image load dbt:dev

echo "✅ CI pipeline completed successfully!"
