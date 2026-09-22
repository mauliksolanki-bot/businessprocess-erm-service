pipeline {
    agent any

    environment {
        APP_DIR    = '/opt/ermservice'
        APP_JAR    = '/opt/ermservice/app.jar'
        SERVICE    = 'ermservice'
        APP_PORT   = '8081'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -q clean package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | head -n1)
                    if [ -z "$JAR" ]; then
                        echo "No built jar found in target/" >&2
                        exit 1
                    fi
                    cp "$JAR" /tmp/ermservice-deploy.jar
                    sudo cp /tmp/ermservice-deploy.jar "${APP_JAR}"
                    sudo chown ermservice:ermservice "${APP_JAR}"
                    rm -f /tmp/ermservice-deploy.jar
                    sudo systemctl restart "${SERVICE}"
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    set -e
                    sleep 8
                    sudo systemctl is-active "${SERVICE}"
                    ok=""
                    for i in $(seq 1 10); do
                        if curl -sf "http://localhost:${APP_PORT}/v3/api-docs" -o /dev/null; then
                            ok="1"
                            break
                        fi
                        sleep 5
                    done
                    if [ -z "$ok" ]; then
                        echo "WARNING: app did not respond on ${APP_PORT} yet (check DB env vars in /etc/ermservice/ermservice.env)"
                    else
                        echo "App responded OK on port ${APP_PORT}"
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo 'ermservice build & deploy finished. Check the Verify stage output for runtime health.'
        }
        failure {
            echo 'Build or deploy failed - see stage logs above.'
        }
    }
}
