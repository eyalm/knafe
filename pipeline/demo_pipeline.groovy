#!/usr/bin/env groovy

def unittests
def checkout_code
def systemtests
def testblockcheck
def systemistall
def userguide
def deployment
def error_file_location_path = "/tmp/paramCheckError.txt"

def send_mail(last_commiter_name, error_file_location_path){
    // Use SUCCESS FAILURE or ABORTED
    currentBuild.result = "FAILURE"
    errors_dump = readFile("${error_file_location_path}")
    to_blame = last_commiter_name + "@kaminario.com"
    mail to: to_blame, cc: "alex.tarasiuk@kaminario.com", from:"ATF@kaminario.com", subject: "errors were found during Jenkines pipeline execution", body: errors_dump
    throw new Exception("Throw to stop pipeline")

}

// Best to leave these at default unless you have a reason to change them
node('katfv5') {
    properties(
        [
            parameters(
              [
                string(defaultValue: 'http://bitbucket/scm/atf/k.n.a.f.e.git', name: 'MYREPO'),
                string(defaultValue: 'master', name: 'MYBRANCH'),
                string(defaultValue: 'jenkins_pool', name: 'CI_POOL'),
                string(defaultValue: 'jenkins_ci', name: 'CI_USER'),
                string(defaultValue: '/test_cases/features/knafe_ci/knafe_ci.feature', name: 'CI_SUITE'),
                string(defaultValue: '/ATF/ATF_DISTS/knafe', name: 'DIST_DIR'),
              ]
            )
        ]
    )

    //env.WORKSPACE = pwd()
    try 
    {
        notifyBuild('STARTED')
        // Load inlcude files
        deleteDir()
        // bring the repository to the slave node
        git branch: 'master', url: 'https://github.com/eyalm/knafe.git'
        
        checkout_code = load 'pipeline/pipeline.checkout'
        testblockcheck = load 'pipeline/pipeline.testblockscheck'
        unittests = load 'pipeline/pipeline.unittests'
        systemistall = load 'pipeline/pipeline.systeminstall'
        systemtests = load 'pipeline/pipeline.systemtests'
        userguide = load 'pipeline/pipeline.userguide'
        deployment = load 'pipeline/pipeline.deployment'

        stage ('Checkout') {
            last_commiter_name = checkout_code.checkout_code(params.MYREPO, params.MYBRANCH)
        
            if (!last_commiter_name) {
                last_commiter_name = "alex.tarasiuk"
            }
            echo "last commiter name is:" + last_commiter_name + "mail:" + last_commiter_name + "@kaminario.com"
        }
       
        stage ('Unit Tests') {
            unittests.runtests(params.MYREPO, params.MYBRANCH)
        }

        stage ('Test Block\'s Check') {
            res = testblockcheck.check(error_file_location_path)
            if (res != 0) {
                send_mail(last_commiter_name, error_file_location_path)
            }
        } 

        stage ('System Install') {
            
            systemistall.install()
        }

        stage ('System Tests') {
            def suite = "${env.WORKSPACE}" + params.CI_SUITE
            systemtests.runtests(CI_POOL, CI_USER, suite)
        }

        stage ('User Guide') {
            res = userguide.generate(error_file_location_path)
            if (res != 0) {
                send_mail(last_commiter_name, error_file_location_path)
            }

        }

        stage ('Deploy') {
            deployment.deploy()
        }
    }
    catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        // Success or failure, always send notifications
        notifyBuild(currentBuild.result)
    }
}


def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'PLUM'
    colorCode = '#DDA0DD'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else if (buildStatus == 'UNSTABLE') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  //slackSend (color: colorCode, message: summary)

  // if (buildStatus == 'FAILED') {
  //   emailext (
  //       attachLog: true,
  //       from: "Jenkins",
  //       subject: subject,
  //       body: details,
  //       recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
  //       to: 'erantz@il.ibm.com'
  //   )
  // }
  // else {
  //   emailext (
  //       attachLog: true,
  //       from: "Jenkins",
  //       subject: subject,
  //       body: details,
  //       recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
  //   )
  // }
}

