#!/usr/bin/env groovy

def repo = "ssh://eyal.meltzer@bitbucket:7999/atf/k.n.a.f.e"
def unittests


node('rhel75') {
    try {
        notifyBuild('STARTED')
        // Load inlcude files
        deleteDir()
        git branch: env.BRANCH, url: 'ssh://eyal.meltzer@bitbucket:7999/atf/k.n.a.f.e'
        unittests = load 'pipeline/pipeline.unittests'

    // Run UT
        stage ('UnitTests') {
            unittests.runtests(repo)
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

  if (buildStatus == 'FAILED') {
    emailext (
        attachLog: true,
        from: "Jenkins",
        subject: subject,
        body: details,
        recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
        to: 'erantz@il.ibm.com'
    )
  }
  else {
    emailext (
        attachLog: true,
        from: "Jenkins",
        subject: subject,
        body: details,
        recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    )
  }
}
