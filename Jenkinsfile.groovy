DEPLOYMENT_ENV = "${env.DEPLOYMENT_ENV}"
K8S_NAMESPACE = "" // TODO SET
K8S_CLUSTER = "infra"
DOCKER_REGISTRY = "localhost:5000"  // Your local Docker registry URL
DOCKER_REGISTRY_SECRET = "" // TODO SET
RANCHER_TOKEN_CREDENTIALS_ID = "" // TODO SET
APP_NAME = "infra-app"
// Unique tag for each build // TODO Check if it can be used somehow
IMAGE_TAG = "${env.BUILD_NUMBER}"
KUBECONFIG = "/var/jenkins_home/kubeconfig.yaml"  // Kubeconfig location for Jenkins

BUILD_JAR_PATH = "target/infra-app-0.0.1-SNAPSHOT.jar"

DOCKER_IMAGE_NEEDS_REBUILD = true
// Name of last deployment. Required to execute rollback if it fails
LAST_DEPLOYMENT_NAME = ""

pipeline {
    agent any

    stages {
        stage('Setup Docker Permissions') {
            steps {
                script {
                    // Get the UID and GID of the jenkins user
                    def jenkinsUserGroupId = sh(script: "id jenkins | awk '{print \$1}' | sed 's/.*\\(\\([0-9]*\\):\\([0-9]*\\)\\)/\\1:\\2/'", returnStdout: true).trim()

                    // Set ownership to Jenkins user and group IDs
                    sh "chown -R ${jenkinsUserGroupId} ."
                }
            }
        }
        stage("Determine Environment") {
            steps {
                determineEnv(GIT_BRANCH)
            }
        }
        stage("Determine should docker image be rebuilt") {
            steps {
                script {
                    def imageExists = sh script: "docker images -q ${DOCKER_REGISTRY}/${APP_NAME}:${DEPLOYMENT_ENV}-${GIT_COMMIT} | wc -l", returnStdout: true
                    // might include newline so just take first char as it should be enough
                    imageExists = imageExists == "" ? "0" : imageExists.charAt(0)
                    if (imageExists == "1") {
                        DOCKER_IMAGE_NEEDS_REBUILD = false
                    }
                    echo "Docker image needs rebuild: ${DOCKER_IMAGE_NEEDS_REBUILD}"
                }
            }
        }
        stage('Build Docker Image') {
            when {
                expression {
                    return DOCKER_IMAGE_NEEDS_REBUILD == true
                }
            }
            steps {
                script {
                    // Build the Docker image
                    sh "docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:${DEPLOYMENT_ENV}-${GIT_COMMIT} ."
                    // Push the Docker image to the local Docker registry
                    sh "docker push ${DOCKER_REGISTRY}/${APP_NAME}:${DEPLOYMENT_ENV}-${GIT_COMMIT}"
                }
            }
        }
//        stage('Deploy to Kubernetes') {
//            steps {
//                script {
//                    // Set the image in the Kubernetes deployment to the new version
//                    sh """
//                    kubectl --kubeconfig=${KUBECONFIG} set image deployment/${APP_NAME} ${APP_NAME}=${REGISTRY}/${APP_NAME}:${IMAGE_TAG}
//                    """
//                }
//            }
//        }
    }

    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}

def determineEnv(git_branch) {
    if (DEPLOYMENT_ENV == "production") {
        // SET PRODUCTION INFO
    } else {
        DEPLOYMENT_ENV = "staging"
    }
}

