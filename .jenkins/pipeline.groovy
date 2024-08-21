pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                updateGitHubStatus('pending', 'Test build.sh')
                echo 'Starting build'
                sh './build.sh'
            }
        }
    }

    post {
        success {
            script {
                echo 'Build succeed'
                updateGitHubStatus('success', 'Succeed build.sh')
            }
        }
        failure {
            script {
                echo 'Build failed'
                updateGitHubStatus('failure', 'Failed build.sh')
            }
        }
    }
}


def updateGitHubStatus(String status, String description) {
    withCredentials([usernamePassword(credentialsId: 'github', passwordVariable: 'GITHUB_TOKEN', usernameVariable: 'GITHUB_USER')]) {
        def commitSha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        echo "$commitSha"

        def scmUrl = scm.userRemoteConfigs[0].url
        echo "SCM URL: ${scmUrl}"
        def urlParts = scmUrl.split('/')
        orgName = urlParts[-2]
        repoName = urlParts[-1].split('\\.')[0]
        echo "Org name: ${orgName}"
        echo "Repo name: ${repoName}"

        sh """
        curl -u $GITHUB_USER:$GITHUB_TOKEN -X POST \
            -d '{"state": "$status", "target_url": "http://<jenkins_url>/job/<job_name>", "description": "$description", "context": "continuous-integration/jenkins"}' \
            https://api.github.com/repos/$orgName/$repoName/statuses/$commitSha
        """
    }
}
