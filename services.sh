#!/bin/bash

# Define the password file location
JENKINS_PASSWORD_FILE="$(dirname "$0")/jenkins_initial_admin_password.txt"

# Check if the user provided an argument
if [ -z "$1" ]; then
  echo "Usage: services [up|down]"
  exit 1
fi

# Handle the "up" command
if [ "$1" = "up" ]; then
  echo "Starting services with Docker Compose..."
  docker-compose up -d

  # Jenkins
  # Retrieve the initial admin password from the Jenkins container
  echo "Retrieving the initial admin password..."
  docker-compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword >"$JENKINS_PASSWORD_FILE"
  echo "Initial admin password saved to $JENKINS_PASSWORD_FILE"

  echo "Jenkins is now running at http://localhost:8090"
  echo "Registry UI is now running at http://localhost:5001"
  echo "PMA is now running at http://localhost:9898"
  echo "Rancher is now running at https://localhost:433"


# Handle the "down" command
elif [ "$1" = "down" ]; then
  echo "Stopping services with Docker Compose..."
  docker-compose down

  # Jenkins
  # Optionally, remove the password file
  if [ -f "$JENKINS_PASSWORD_FILE" ]; then
    rm "$JENKINS_PASSWORD_FILE"
    echo "Removed $JENKINS_PASSWORD_FILE"
  fi

# Handle any other argument
else
  echo "Invalid command. Usage: services [up|down]"
  exit 1
fi
