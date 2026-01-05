pipeline {
    agent any

    tools {
        jdk 'java-21'
        maven 'maven-3.9.12'
    }

    environment {
        DOCKERHUB_REPO = "sboomisnow/analytics-service"
        IMAGE_TAG = "latest"

        EC2_IP      = "18.118.149.115"
        EC2_HOST    = "ubuntu@18.118.149.115"
        SERVER_URL  = "http://18.118.149.115:8082/swagger-ui/index.html"
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/pms-org/pms-analytics.git'
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -t $DOCKERHUB_REPO:$IMAGE_TAG .
                """
            }
        }

        stage('Login & Push to DockerHub') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials-analytics',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push $DOCKERHUB_REPO:$IMAGE_TAG
                    """
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(['analytics-ec2-server']) {
                    withCredentials([
                        file(credentialsId: 'analytics-env-file', variable: 'ENV_FILE')
                    ]) {

                        // Ensure target directories exist
                        sh """
                        ssh -o StrictHostKeyChecking=no $EC2_HOST "
                            mkdir -p /home/ubuntu/redis
                        "
                        """

                        // Copy docker compose file
                        sh """
                        scp -o StrictHostKeyChecking=no \
                            compose.yaml \
                            $EC2_HOST:/home/ubuntu/compose.yaml
                        """

                        // Copy redis config directory
                        // 1. Clean & prepare directory
                        sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@$EC2_HOST \
                          'sudo rm -rf /home/ubuntu/redis &&
                           mkdir -p /home/ubuntu/redis &&
                           sudo chown -R ubuntu:ubuntu /home/ubuntu'
                        """

                        // 2. Copy redis configs
                        sh """
                        scp -o StrictHostKeyChecking=no -r redis \
                            ubuntu@$EC2_HOST:/home/ubuntu/
                        """

                        // 3. Fix permissions again (safety)
                        sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@$EC2_HOST \
                          'sudo chown -R ubuntu:ubuntu /home/ubuntu/redis'
                        """

                        // Copy .env file from Jenkins credentials
                        sh """
                        scp -o StrictHostKeyChecking=no \
                            "$ENV_FILE" \
                            $EC2_HOST:/home/ubuntu/.env
                        """

                        // Deploy containers
                        sh """
                        ssh -o StrictHostKeyChecking=no $EC2_HOST "
                            cd /home/ubuntu &&
                            docker compose down -v --remove-orphans &&
                            docker pull $DOCKERHUB_REPO:$IMAGE_TAG &&
                            docker compose up -d &&
                            docker ps
                        "
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployment Successful"
            echo "🌍 EC2 Host: $EC2_IP"
            echo "🚀 App URL: $SERVER_URL"
        }
        failure {
            echo "❌ Deployment Failed"
        }
    }
}