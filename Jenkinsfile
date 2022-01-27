node() {
    try {
        String ANSI_GREEN = "\u001B[32m"
        String ANSI_NORMAL = "\u001B[0m"
        String ANSI_BOLD = "\u001B[1m"
        String ANSI_RED = "\u001B[31m"
        String ANSI_YELLOW = "\u001B[33m"

        ansiColor('xterm') {
            stage('Checkout') {
                cleanWs()
                checkout scm
                commit_hash = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                env.commit_id = sh(script: "echo " + "gateway" + "_" + commit_hash + "_" + env.BUILD_NUMBER, returnStdout: true).trim()
                echo "${env.commit_id}"
                }
        }
           stage('docker-pre-build') {
             sh '''

              docker build --no-cache -f ./Dockerfile.build -t $docker_pre_build .
              docker run --name $docker_pre_build $docker_pre_build:latest && docker cp $docker_pre_build:/opt/target/zuul-0.0.1-SNAPSHOT.jar .
              docker rm -f $docker_pre_build
              docker rmi -f $docker_pre_build
                '''
        }

           stage('docker-build') {
                sh '''
                   docker build --no-cache -f Dockerfile -t $docker_server/$docker_repo:$commit_id .
                   '''
        }
         stage('docker-push') {

               sh '''
                  docker push $docker_server/$docker_repo:$commit_id
                  docker rmi -f $docker_server/$docker_repo:$commit_id
                  '''
       }
       stage('ArchiveArtifacts') {
	       	   sh ("echo ${commit_id} > commit_id.txt")	     
               archiveArtifacts "commit_id.txt" 
               currentBuild.description = "${commit_id}"
       }


}
    catch (err) {
        currentBuild.result = "FAILURE"
        throw err
    }
}
