pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
        nodejs 'Node-20'
    }

    environment {
        BACKEND_DIR = 'iss-predictor-service'
        FRONTEND_DIR = 'iss-predictor-ui'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend: Build & Test') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B clean verify'
                }
            }
            post {
                always {
                    junit testResults: "${BACKEND_DIR}/target/surefire-reports/*.xml", allowEmptyResults: true
                }
            }
        }

        stage('Frontend: Build') {
            steps {
                dir("${FRONTEND_DIR}") {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker: Build Images') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh "docker build -t iss-predictor-service:${IMAGE_TAG} ."
                }
                dir("${FRONTEND_DIR}") {
                    sh "docker build -t iss-predictor-ui:${IMAGE_TAG} ."
                }
            }
        }

        stage('Docker: Push') {
            when { branch 'main' }
            steps {
                echo 'Add registry credentials via Jenkins Credentials Binding and docker push here.'
                // Example:
                // withCredentials([usernamePassword(credentialsId: 'docker-registry', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                //     sh "docker login -u $USER -p $PASS"
                //     sh "docker push iss-predictor-service:${IMAGE_TAG}"
                // }
            }
        }
    }

    post {
        success { echo "Build #${env.BUILD_NUMBER} succeeded." }
        failure { echo "Build #${env.BUILD_NUMBER} failed - check logs." }
    }
}