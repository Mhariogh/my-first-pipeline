pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Cloning repository...'
                git branch: 'main',
                    credentialsId: 'github-creds',
                    url: 'https://github.com/Mhariogh/my-first-pipeline.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh 'mvn test'
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'
                sh 'docker build -t myapp:${BUILD_NUMBER} .'
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                sh '''
                    docker stop myapp-container || true
                    docker rm myapp-container || true
                    docker run -d \
                      --name myapp-container \
                      --restart unless-stopped \
                      -p 9090:8080 \
                      myapp:${BUILD_NUMBER}
                    echo "✅ App deployed!"
                    docker ps | grep myapp-container
                '''
            }
        }
    }

    post {
        success { echo '✅ Build Successful!' }
        failure { echo '❌ Build Failed!' }
    }
}
