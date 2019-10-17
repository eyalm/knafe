#!groovy

def repo = "ssh://xiv-gerrit.haifa.ibm.com:22122/autosde"
def unittests
def build
def download
def clean_deployment
def system_tests


node('rhel75') {
    try {
        notifyBuild('STARTED')
        // Load inlcude files
        deleteDir()
        git branch: env.BRANCH, url: 'ssh://xiv-gerrit.haifa.ibm.com:22122/autosde'
        unittests = load 'pipeline/pipeline.unittests'
        build = load 'pipeline/pipeline.build'
        download = load 'pipeline/pipeline.download_ova'
        clean_deployment = load 'pipeline/pipeline.clean_deploy'
        system_tests = load 'pipeline/pipeline.systemtests'

    // Run UT
        stage ('UnitTests') {
            unittests.runtests(repo)
        }

    // Run Build
        stage ('Build') {
            build.runbuild(repo)
        }

    // Run Download OVA from artifactory
        stage ('Download OVA from Artifactory') {
            download.download_from_artifactory()
        }

    // Deploy Clean OVA
        stage ('Clean Deployment') {
            def autosde_server_name = getProperty('AUTOSDE_SERVER_NAME')
            clean_deployment.clean_deploy(autosde_server_name)
        }

    // Run System Tests
        stage ('SystemTests') {
            def autosde_server_name = getProperty('AUTOSDE_SERVER_NAME')
            system_tests.runtests(autosde_server_name, "sanity")
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
  slackSend (color: colorCode, message: summary)

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

