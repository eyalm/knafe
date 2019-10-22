#!/usr/bin/env groovy

def runtests(repo){
    echo 'in tests'
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
        deleteDir()
        checkout([$class: 'GitSCM',
                branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false,  submoduleCfg: [], userRemoteConfigs: [[url: 'http://bitbucket/scm/atf/k.n.a.f.e.git']]])



//        checkout([$class: 'GitSCM',
//                branches: [[name: env.BRANCH]],
//                doGenerateSubmoduleConfigurations:false,
//                extensions: [],
//                submoduleCfg: [],
//                userRemoteConfigs: [[url:repo]]])
        sh '/usr/bin/python autosde.py generate-all --backend_only'
        sh '/usr/bin/python autosde.py unit-tests'
        archiveArtifacts 'unit_tests_reports/TEST*.xml'
        junit allowEmptyResults: true, keepLongStdio: true, testResults: 'unit_tests_reports/TEST*.xml'
        // archiveArtifacts artifacts: '**/nosetests.xml'
        // publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'cover/', reportFiles: 'index.html', reportName: 'Autosde Coverage Report'])
        // junit '**/nosetests.xml'
    }
}

return this;
