pipeline {

    agent {
        // label "" also could have been 'agent any' - that has the same meaning.
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        APP_NAME = "authorizationapp"
        JOB_NAME = "${JOB_NAME}".replace("/", "-").replace("%2F", "-")
        JENKINS_TAG = "${JOB_NAME}.${BUILD_NUMBER}"

        GIT_SSL_NO_VERIFY = true
        //GIT_CREDENTIALS = credentials('labs-ci-cd-jenkins-git-password')
        //NEXUS_CREDS = credentials('labs-ci-cd-nexus-password')
        
        
 

        // GITLAB_DOMAIN = "gitlab-labs-ci-cd.apps.somedomain.com"
        GITLAB_PROJECT = "tie"
        SCHEMA_NAME = "${APP_NAME}".replace("-", "_")
    }

    // The options directive is for configuration that applies to the whole job.
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
        ansiColor('xterm')
        timestamps()
    }

    stages {

        stage("prepare environment for master deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
              expression { GIT_BRANCH ==~ /(.*master)/ }
            }
            steps {
                script {

                  //  env.MAVEN_VERSION="${BUILD_NUMBER}-RELEASE"
                    //env.MAVEN_DEPLOY_REPO = "labs-releases"
                    // Arbitrary Groovy Script executions can do in script tags
                    //env.PROJECT_NAMESPACE = "labs-test"
                    //env.NODE_ENV = "test"
                    //env.E2E_TEST_ROUTE = "oc get route/${APP_NAME} --template='{{.spec.host}}' -n ${PROJECT_NAMESPACE}".execute().text.minus("'").minus("'")
                 
                       def server = Artifactory.server 'art-1'
                 def uploadSpec = """{
                    "files": [{
                       "pattern": "path/",
                       "target": "path/"
                    }]
                 }"""

                 server.upload(uploadSpec) 
               
                    
                }
            }
        }
        stage("prepare environment for develop deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
              expression { GIT_BRANCH ==~ /(.*develop)/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.MAVEN_VERSION="${BUILD_NUMBER}-SNAPSHOT"
                    env.MAVEN_DEPLOY_REPO = "labs-snapshots"
                    env.PROJECT_NAMESPACE = "labs-dev"
                    env.NODE_ENV = "dev"
                    env.E2E_TEST_ROUTE = "oc get route/${APP_NAME} --template='{{.spec.host}}' -n ${PROJECT_NAMESPACE}".execute().text.minus("'").minus("'")
                }
            }
        }

        stage("prepare maven version for feature branch") {
            agent {
                node {
                    label "master"
                }
            }
            when {
              not {
                  expression { GIT_BRANCH ==~ /(.*master|.*develop)/ }
              }
            }
            steps {
                script {
                    env.MAVEN_VERSION="${BUILD_NUMBER}-${GIT_BRANCH}-SNAPSHOT".replace("/", "-")
                    env.MAVEN_DEPLOY_REPO = "labs-snapshots"
               }
            }
        }

        stage("Apply cluster configs") {
            agent {
                node {
                label "jenkins-slave-ansible"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /(.*master|.*develop)/ }
            }
            steps {
                echo '### Apply cluster configs ###'
                sh  '''
                printenv
                '''
                sh  '''
                cd .openshift-applier
                ansible-galaxy install -r requirements.yml --roles-path=roles
                ansible-playbook apply.yml -e target=app -i inventory/
                '''
            }
            post {
                always {
                archiveArtifacts "**"
                }
            }
        }

        stage("mvn-build") {
            agent {
                node {
                    label "jenkins-slave-mvn"
                }
            }
            steps {
                // git branch: 'develop',
                //     credentialsId: 'jenkins-git-creds',
                //     url: 'https://gitlab-labs-ci-cd.apps.somedomain.com/labs/tie-api.git'

                sh 'printenv'

                echo '### setting version ###'
                sh 'mvn -B versions:set -DnewVersion=${MAVEN_VERSION}'

                echo '### SKIPPING Checking binaries for security problems ###'
                // sh "mvn -B dependency-check:check"

                echo '### compiling  ###'
                sh "mvn -B clean install"

                echo '### testing ###'
                sh "mvn -B test "

                echo '### sonaring ###'
                sh "mvn -B sonar:sonar"

                echo '### Packaging App for Nexus ###'
                sh 'mvn -B deploy -DskipTests=true'
            }
            // Post can be used both on individual stages and for the entire build.
            post {
                always {
                    archiveArtifacts "**"
                    junit 'authorization-server/target/surefire-reports/*.xml'
                    // publish html
                }
                success {
                    echo "Git tagging"
                    sh'''
                        git config --global user.email "jenkins@example.com"
                        git config --global user.name "jenkins-ci"
                        git tag -a ${JENKINS_TAG} -m "JENKINS automated commit"
                        # git push https://${GIT_CREDENTIALS_USR}:${GIT_CREDENTIALS_PSW}@${GITLAB_DOMAIN}/${GITLAB_PROJECT}/${APP_NAME}.git --tags
                    '''
                }
                failure {
                    echo "FAILURE"
                }
            }
        }


        stage("liquidbase-test") {
            agent {
                node {
                    label "jenkins-slave-mssql"
                }
            }
            steps {

                sh 'printenv'

                echo '### testing access to DB ###'

                script {
                    openshift.withCluster() {
                        openshift.withProject(env.PIPELINES_NAMESPACE ) {
                            def dcObj = openshift.selector("dc", "mssql").object()
                            def podSelector = openshift.selector("pod", [deployment: "mssql-${dcObj.status.latestVersion}"])
                            if(podSelector.exists()){
                                podSelector.untilEach {
                                    echo "pod: ${it.name().substring(4)}"
                                    env.MSSQL_POD_NAME = it.name().substring(4)
                                    return true
                                }
                            }
                            else{
                              error("failed to find an mssql instance in namespace ${env.PIPELINES_NAMESPACE}")
                            }
                        }
                        env.LIQUIBASE_TEST_SCHEMA_NAME = "${JOB_NAME}_${BUILD_NUMBER}".replace("-","_")
                    }
                }
                sh 'oc exec $MSSQL_POD_NAME "echo" "hello world" -n $PIPELINES_NAMESPACE'
                sh 'oc exec $MSSQL_POD_NAME "/usr/local/bin/create_db.sh" -n $PIPELINES_NAMESPACE'

                sh 'oc exec $MSSQL_POD_NAME "/usr/local/bin/create_schema.sh" "${LIQUIBASE_TEST_SCHEMA_NAME}" "Pass1234" -n $PIPELINES_NAMESPACE'

                // run maven liquibase verification
                sh "mvn -B -f authorization-server/pom.xml liquibase:updateTestingRollback -Dliquibase.username=${LIQUIBASE_TEST_SCHEMA_NAME} -Dliquibase.password=Pass1234"
                //cleanup schema
                sh 'oc exec $MSSQL_POD_NAME "/usr/local/bin/drop_schema.sh" "${LIQUIBASE_TEST_SCHEMA_NAME}" -n $PIPELINES_NAMESPACE'
            }
        }

        stage("db-create") {
            agent {
                node {
                    label "jenkins-slave-mssql"
                }
            }

            when {
                expression { GIT_BRANCH ==~ /(.*master|.*develop)/ }
            }
            steps {

                sh 'printenv'

                echo '### testing access to DB ###'

                script {



                    openshift.withCluster() {
                        openshift.withProject(env.PROJECT_NAMESPACE ) {
                            def dcObj = openshift.selector("dc", "mssql").object()
                            def podSelector = openshift.selector("pod", [deployment: "mssql-${dcObj.status.latestVersion}"])
                            if(podSelector.exists()){
                                podSelector.untilEach {
                                    echo "pod: ${it.name().substring(4)}"
                                    env.MSSQL_POD_NAME = it.name().substring(4)
                                    return true
                                }
                            }
                            else{
                              error("failed to find an mssql instance in namespace ${env.PIPELINES_NAMESPACE}")
                            }
                        }
                    }
                }

                sh 'oc exec $MSSQL_POD_NAME "echo" "hello world" -n $PROJECT_NAMESPACE'
                sh 'oc exec $MSSQL_POD_NAME "/usr/local/bin/create_db.sh" -n $PROJECT_NAMESPACE'
                sh 'oc exec $MSSQL_POD_NAME "/usr/local/bin/create_schema.sh" "${SCHEMA_NAME}" "Re4llySecretPasswd!_" -n $PROJECT_NAMESPACE'
            }
        }

        stage("app-bake") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /(.*master|.*develop)/ }
            }
            steps {
                echo '### Get Binary from Nexus ###'
                sh  '''

                curl -v "http://nexus-labs-ci-cd.apps.vcc.emea-1.rht-labs.com/service/siesta/rest/beta/search/assets/download?repository=${MAVEN_DEPLOY_REPO}&maven.groupId=com.vcc.tie.auth&maven.artifactId=authorization-server&maven.baseVersion=${MAVEN_VERSION}&maven.extension=jar" -L -o authorizationapp.jar
                '''
                echo '### Create Linux Container Image from package ###'
                sh  '''
                        oc project ${PIPELINES_NAMESPACE} # probs not needed
                        oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"ImageStreamTag\\",\\"name\\":\\"${APP_NAME}:${JENKINS_TAG}\\"}}}}"
                        oc start-build ${APP_NAME} --from-file=authorizationapp.jar --follow
                    '''
            }
            post {
                always {
                    archiveArtifacts "**"
                }
            }
        }

        stage("java-deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /(.*master|.*develop)/ }
            }
            steps {
                echo '### tag image for namespace ###'
                sh  '''
                    oc project ${PROJECT_NAMESPACE}
                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:${JENKINS_TAG} ${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                    '''
                echo '### set env vars and image for deployment ###'
                sh '''
                    oc set image dc/${APP_NAME} ${APP_NAME}=docker-registry.default.svc:5000/${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                    oc rollout latest dc/${APP_NAME}
                '''
                echo '### Verify OCP Deployment ###'
                openshiftVerifyDeployment depCfg: env.APP_NAME,
                    namespace: env.PROJECT_NAMESPACE,
                    replicaCount: '1',
                    verbose: 'false',
                    verifyReplicaCount: 'true',
                    waitTime: '',
                    waitUnit: 'sec'
            }
        }
    }
}
