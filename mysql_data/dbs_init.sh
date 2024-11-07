#!/bin/bash

mysql -u root --password="$MYSQL_ROOT_PASSWORD" --execute="CREATE SCHEMA IF NOT EXISTS uof DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root --password="$MYSQL_ROOT_PASSWORD" --database="uof" < /tmp/infra-app.sql
