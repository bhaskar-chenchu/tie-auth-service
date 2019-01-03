def call(body) {
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
	def label = "gradle-build-${UUID.randomUUID().toString()}"
	podTemplate(
		label: label,
		name: label,
		imagePullSecrets: ['prodregistry'],
		containers: [
			containerTemplate(name: label, image: 'neugcstieacr01.azurecr.io/baseimg/gradle_microservices_xenial:v1', command: 'cat', ttyEnabled: true)
		],
		volumes: [
			hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock')
		]
	){
		node(label) {
			ws ('/home/jenkins/workspace/build'){
				container(label) {
					def application = 'tie';
					def module = config.MicroserviceName;
					def tempImageTag = "neugcstieacr01.azurecr.io/tie/temp/${module}";
					def imageTag = "neugcstieacr01.azurecr.io/${application}/${module}:${env.BUILD_NUMBER}";
					properties([
						[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10']],
						disableConcurrentBuilds()
					])
					stage('Clone') {
						dir(config.MicroserviceName) {
							try {								
								git branch: params.BranchName, credentialsId: 'git_creds', url: 'https://volvocargroup.visualstudio.com/Workshop%20Management/_git/tie-authorizatoin-service'
							} catch (exc) {
								currentBuild.result = "FAILURE"
								throw exc
							} finally {
							}
						}
					}
					stage('Unit Test - Java') {
						dir(config.MicroserviceName) {
							try {
								sh 'gradle test'
								if(config.cdcConsumer != '' && config.cdcConsumer == true) {
									sh 'gradle pactPublish'
								}
							} catch (exc) {
								currentBuild.result = "FAILURE"
								if(config.TeamDL) {
									mail body: "${config.MicroserviceName} - Unit test java error is here: ${env.BUILD_URL}" ,
									from: 'cld-TIE-Team-TOT@grp.volvocars.com',
									replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
									subject: "${config.MicroserviceName} - Unit test java failed #${env.BUILD_NUMBER}",
									to: "${config.TeamDL}"
								}
								throw exc
							} finally {
								jacoco()
								publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'build/reports/tests/test', reportFiles: 'index.html', reportName: 'Report - Java Unit Test', reportTitles: 'Report - Java Unit Test'])
							}
						}
					}
					stage('SonarQube Analysis - Java') {
						dir(config.MicroserviceName) {
							try {
								sh 'gradle sonarqube -x test -Dsonar.host.url=http://tiesonar.gcs-dev-cloud.volvocars.biz:9000/sonarqube -Dsonar.verbose=false --scan'
							} catch (exc) {
								currentBuild.result = "FAILURE"
								if(config.TeamDL) {
									mail body: "${config.MicroserviceName} - SonarQube analysis java error is here: ${env.BUILD_URL}" ,
									from: 'cld-TIE-Team-TOT@grp.volvocars.com',
									replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
									subject: "${config.MicroserviceName} - SonarQube analysis java failed #${env.BUILD_NUMBER}",
									to: "${config.TeamDL}"
								}
								throw exc
							} finally {
							}
						}
					}
					stage('Build - Java') {
						dir(config.MicroserviceName) {
							try {
								sh 'gradle -b build.gradle build -x test'
							} catch (exc) {
								currentBuild.result = "FAILURE"
								if(config.TeamDL) {
									mail body: "${config.MicroserviceName} - Build java error is here: ${env.BUILD_URL}" ,
									from: 'cld-TIE-Team-TOT@grp.volvocars.com',
									replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
									subject: "${config.MicroserviceName} - Build java failed #${env.BUILD_NUMBER}",
									to: "${config.TeamDL}"
								}
								throw exc
							} finally {
							}
						}
					}
					if(config.cdcProvider != '' && config.cdcProvider == true) {
						stage ('CDC - Verify Provider') {
							dir(config.MicroserviceName) {
								try {
									sh 'gradle pactVerify'
								} catch (exc) {
									currentBuild.result = "FAILURE"
									if(config.TeamDL) {
										mail body: "${config.MicroserviceName} - CDC verify provide error is here: ${env.BUILD_URL}" ,
										from: 'cld-TIE-Team-TOT@grp.volvocars.com',
										replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
										subject: "${config.MicroserviceName} - CDC verify provide failed #${env.BUILD_NUMBER}",
										to: "${config.TeamDL}"
									}
									throw exc
								} finally {
								}
							}
						}
					}
					stage('Docker Build') {
						dir(config.MicroserviceName) {
							try {
								sh 'gradle -b build.gradle createDockerfile'
								withDockerRegistry([credentialsId: 'acr_cred', url: 'https://neugcstieacr01.azurecr.io']) {
									sh "sudo docker build --no-cache -t ${module} ."
									// sh "docker tag ${module} '${tempImageTag}'"
									// sh "docker push '${tempImageTag}'"
								}
							} catch (exc) {
								currentBuild.result = "FAILURE"
								if(config.TeamDL) {
									mail body: "${config.MicroserviceName} - Docker build error is here: ${env.BUILD_URL}" ,
									from: 'cld-TIE-Team-TOT@grp.volvocars.com',
									replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
									subject: "${config.MicroserviceName} - Docker build failed #${env.BUILD_NUMBER}",
									to: "${config.TeamDL}"
								}
								throw exc
							} finally {
							}
						}
					}
					stage('Push to ACR') {
						dir(config.MicroserviceName) {
							try {
								sh "docker tag ${module} '${imageTag}'"
								withDockerRegistry([credentialsId: 'acr_cred', url: 'https://neugcstieacr01.azurecr.io']) {
									sh "docker push '${imageTag}'"
								}
							} catch (exc) {
								currentBuild.result = "FAILURE"
								if(config.TeamDL) {
									mail body: "${config.MicroserviceName} - Push to ACR error is here: ${env.BUILD_URL}" ,
									from: 'cld-TIE-Team-TOT@grp.volvocars.com',
									replyTo: 'cld-TIE-Team-TOT@grp.volvocars.com',
									subject: "${config.MicroserviceName} - Push to ACR failed #${env.BUILD_NUMBER}",
									to: "${config.TeamDL}"
								}
								throw exc
							} finally {
								withDockerRegistry([credentialsId: 'acr_prod', url: 'https://wagdigital.azurecr.io']) {
									sh "docker rmi '${imageTag}' -f"
									sh "docker rmi '${module}' -f"
								}
							}
						}
					}
				}
			}
		}
	}
}
