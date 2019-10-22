#!/usr/bin/env groovy

def unittests
def checkout_code
def commiter_name

node('katfv5') {
    properties(
        [
            parameters(
              [
                string(defaultValue: 'http://bitbucket/scm/atf/k.n.a.f.e.git', name: 'MYREPO'),
                string(defaultValue: 'master', name: 'MYBRANCH')
              ]
            )
        ]
    )


    try {
        echo "${params.MYREPO}, ${params.MYBRANCH}"
        notifyBuild('STARTED')
        // Load inlcude files
        deleteDir()
        // bring the repository to the slave node
        git branch: 'master', url: 'https://github.com/eyalm/knafe.git'
        
        // myPath = "${env.WORKSPACE}" + "/" + "pipeline"
        // echo myPath
        // sh "ls -la ${pwd()}"
        
        // def  FILES_LIST = sh (script: "ls   '${pwd()}'/pipeline", returnStdout: true).trim()
        // echo "FILES_LIST : ${FILES_LIST}"

        checkout_code = load 'pipeline/pipeline.checkout'
        unittests = load 'pipeline/pipeline.unittests'

    // Checkout 
        stage ('Checkout') {
            commiter_name = checkout_code.checkout_code(params.MYREPO, params.MYBRANCH)
        }

    // Run UT
        echo commiter_name
        stage ('UnitTests') {
            unittests.runtests(params.MYREPO, params.MYBRANCH)
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

