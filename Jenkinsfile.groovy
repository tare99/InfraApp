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
        stage('Build and publish Docker Image') {
            when {
                expression {
                    return DOCKER_IMAGE_NEEDS_REBUILD == true
                }
            }
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-23-alpine'
                    /**
                     * We want to cache java packages between jobs
                     * In case of maven that can be achieved by mounting workspace .m2 dir into docker agent .m2
                     * Official docs state that this config should work
                     * agent {
                     *   docker {
                     *     image 'maven:3.9.9-eclipse-temurin-23-alpine'
                     *     args '-v /root/.m2:/root/.m2'
                     *   }
                     * }
                     * However I didn't manage to get it working without:
                     * - Specifying `-u root` in args. Without it image runs with unknown user
                     * - `:z` flag which sets volume in share mode (so that other containers can build in the same time)
                     */
                    //noinspection GroovyAssignabilityCheck
                    args '-v /root/.m2:/root/.m2:z -u root'
                    reuseNode true
                }
            }
            steps {
                sh "unset MAVEN_CONFIG && ./mvnw dependency:purge-local-repository"
                sh "unset MAVEN_CONFIG && ./mvnw -U -P release -Dmaven.test.skip=true clean install"
                /**
                 * Jenkins user/group are 1000/1000
                 * We need to switch permissions after the build to make sure that jenkins workspace can read them
                 */
                sh "chown -R 1000:1000 ."
                /**
                 * Stashing only fat jar as it the only thing that gets into build
                 */
                stash name: "mavenbuild", includes: "${BUILD_JAR_PATH}"
                unstash "mavenbuild"
                sh "docker build --file=deploy/Dockerfile --no-cache --build-arg app_environment=${DEPLOYMENT_ENV} -t ${DOCKER_REGISTRY}/${APP_NAME}:${DEPLOYMENT_ENV}-${GIT_COMMIT} ."
                sh "docker push ${DOCKER_REGISTRY}/${APP_NAME}:${DEPLOYMENT_ENV}-${GIT_COMMIT} "
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

