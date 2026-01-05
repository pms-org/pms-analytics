pipeline {
    agent any
    tools {
        jdk 'java-21'
        maven 'maven-3.9.12'
    }
    environment {
        DOCKERHUB_REPO = "sboomisnow/analytics-service"
        IMAGE_TAG = "latest"
        EC2_IP="18.118.149.115"
        SERVER_URL="http://18.118.149.115:8082/swagger-ui/index.html"
        EC2_HOST = "ubuntu@18.118.149.115"
    }

    stages {

        stage('Clean Workspace') {
            steps { cleanWs() }
        }

        stage('Git Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/pms-org/pms-analytics.git'
            }
        }

        stage('Maven build'){
            steps{
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -f Dockerfile -t $DOCKERHUB_REPO:$IMAGE_TAG .
                """
            }
        }

        stage('Login & Push to DockerHub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials-analytics',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push $DOCKERHUB_REPO:$IMAGE_TAG
                    '''
                }
            }
        }

    //     stage('Deploy to EC2') {
    //         steps {
    //             sshagent(['analytics-ec2-server']) {
    //                 withCredentials([file(credentialsId: 'analytics-env-file', variable: 'ENV_FILE')]) {

    //                     // Copy compose file to instance
    //                     sh '''
    //                     scp -o StrictHostKeyChecking=no \
    //                         compose.yaml \
    //                         $EC2_HOST:/home/ubuntu/compose.yaml
    //                     '''

    //                     // Copy .env inside EC2 from Jenkins secret file
    //                     // Give permissions 
    //                     sh '''
    //                     scp -o StrictHostKeyChecking=no "$ENV_FILE" "$EC2_HOST:/home/ubuntu/.env"
    //                     '''

    //                     // Deploy containers
    //                     // Made docker as user for ubuntu
    //                     sh """
    //                     ssh -o StrictHostKeyChecking=no $EC2_HOST "
    //                         docker pull $DOCKERHUB_REPO:$IMAGE_TAG &&
    //                         docker compose down &&
    //                         docker compose up -d &&
    //                         docker ps
    //                     "
    //                     """
    //                 }
    //             }
    //         }
    //     }
    // }

        stage('Deploy to EC2') {
            steps {
                sshagent(['analytics-ec2-server']) {
                    withCredentials([file(credentialsId: 'analytics-env-file', variable: 'ENV_FILE')]) {

                        // Copy compose file
                        sh '''
                        scp -o StrictHostKeyChecking=no \
                            compose.yaml \
                            $EC2_HOST:/home/ubuntu/compose.yaml
                        '''

                        // Copy redis config folder (THIS FIXES YOUR ISSUE)
                        sh '''
                        scp -o StrictHostKeyChecking=no -r \
                            redis \
                            $EC2_HOST:/home/ubuntu/redis
                        '''

                        // Copy .env
                        sh '''
                        scp -o StrictHostKeyChecking=no \
                            "$ENV_FILE" \
                            "$EC2_HOST:/home/ubuntu/.env"
                        '''

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
            echo "Deployment Successful" 
            echo "Deployed EC2 Host: $EC2_IP"
            echo "Deployed App URL: $SERVER_URL"
        }
        failure { echo "Deployment Failed" }
    }
}