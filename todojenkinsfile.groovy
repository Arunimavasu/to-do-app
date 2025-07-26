node {
    stage ('Prepare') {
        checkout scm
    }
    stage ('Docker build') {
        dir ("$WORKSPACE/k8s/web-app") {
            sh 'docker build -t php-webapp:latest .'
        }
    }
    stage ('scan image') {
        sh 'trivy image --severity CRITICAL,HIGH --exit-code 1 --ignore-unfixed php-webapp:latest'
    }
    stage ('push image') {
        withCredentials([usernamePassword(credentialsId: 'dockerhub.com-personal', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh """
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                docker tag php-webapp:latest $DOCKER_USER/php-webapp:latest
                docker push $DOCKER_USER/php-webapp:latest
            """
        }
    }
    withCredentials([file(credentialsId: 'u22-k8s', variable: 'KUBECONFIG_PATH')]) {
        env.KUBECONFIG = KUBECONFIG_PATH
        
        stage ('mysql deploy'){
            sh """
                kubectl apply -f k8s/db/dbpv.yaml
                kubectl apply -f k8s/db/dbpvc.yaml
                kubectl apply -f k8s/db/dbconfigmap.yaml
                kubectl apply -f k8s/db/dbsecrets.yaml
                kubectl apply -f k8s/db/mysqlpod.yaml
                kubectl apply -f k8s/db/mysqlservice.yaml
            """
        }
        stage ('php-admin deploy') {
            sh """
                
                kubectl apply -f k8s/app/appconfigmap.yaml
                kubectl apply -f k8s/app/appsecrets.yaml
                kubectl apply -f k8s/app/phppod.yaml
                kubectl apply -f k8s/app/phpservice.yaml
            """
        }
        stage ('php-wedapp deploy') {
            sh """
                kubectl apply -f k8s/web-app/appdeployment.yaml
                kubectl apply -f k8s/web-app/phpwebappservice.yaml
            """
        }
        stage ('verification') {
        }
    }
}
