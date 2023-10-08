pipeline
        {
            agent {
                docker {
                    image 'ubuntu:latest'
                    args ' -v /var/run/docker.sock:/var/run/docker.sock ' +
                            ' -v /usr/bin/docker:/usr/bin/docker '
                }
            }
            environment{
                volume='artifact'
                gitImage='aslamimages/alpine-git:2'
                buildImage="aslamimages/mvn_jdk_git:latest"
                gitProjectUrl="https://github.com/aslamcontact/ecom_product_images.git"
                deployImage="aslamimages/product_images"
                proFolder="ecom_product_images"

            }



            options {
                skipDefaultCheckout()
            }
            stages {

                stage('cloning')
                        {
                            steps {

                                sh  "docker volume create ${volume}"

                                sh  "docker run --rm  --name test2 "+
                                        "-v ${volume}:/app "+
                                        "-w /app  ${gitImage} "+
                                        "git clone ${gitProjectUrl}"

                            }
                        }





                stage('validate')
                        {
                            steps {
                                sh "docker run --rm --name validate "+
                                        "-v ${volume}:/app "+
                                        " -w /app ${buildImage} "+
                                        "mvn -f ${proFolder}/ validate"
                            }
                        }

                stage('compile')
                        {
                            steps {
                                sh "docker run --rm  --name compile "+
                                        "-v ${volume}:/app "+
                                        "-w /app ${buildImage} " +
                                        "mvn -f ${proFolder}/ compile"

                            }
                        }

                stage('package')
                        {
                            steps {
                                sh "docker run --rm  --name package "+
                                        "-v ${volume}:/app "+
                                        "-w /app ${buildImage} " +
                                        "mvn -f ${proFolder}/ package"
                            }
                        }




                stage('build')
                        {
                            steps {
                                sh "docker run --rm --name build "+
                                        " -v ${volume}:/app "+
                                        " -v /var/run/docker.sock:/var/run/docker.sock "+
                                        " -v /usr/bin/docker:/usr/bin/docker "+
                                        " -w /app  ubuntu:latest "+
                                        "docker build -t ${deployImage}:${BUILD_NUMBER} ${proFolder}/."
                            }
                        }


                stage('deploy')
                        {

                            steps {

                                withCredentials([usernamePassword(credentialsId: 'dockerhub_key',
                                        usernameVariable: 'USERNAME',
                                        passwordVariable: 'PASSWORD')]
                                ) {



                                    sh 'docker login --username $USERNAME --password $PASSWORD'
                                    sh "docker tag ${deployImage}:${BUILD_NUMBER} ${deployImage}:latest"
                                    sh "docker push ${deployImage}:${BUILD_NUMBER}"
                                    sh "docker push ${deployImage}:latest"

                                }



                            }
                        }
                stage('docker compose up')
                        {
                            steps {



                             /*  sh  "docker run --rm  --name compose_sys "+
                                        "-v ${volume}:/app "+
                                        "-v /var/run/docker.sock:/var/run/docker.sock "+
                                        "-v /usr/bin/docker:/usr/bin/docker "+
                                        "-v /usr/bin/compose:/usr/bin/compose "+
                                        "-v /usr/libexec/docker/cli-plugins/docker-compose:"+
                                        "/usr/libexec/docker/cli-plugins/docker-compose "+
                                        "-w /app/${proFolder}  ubuntu:latest "+
                                        "docker compose up -d "  */



                            }
                        }



                stage('docker compose down')
                        {
                            steps {



                             /*   sh  "docker run --rm  --name test3 "+
                                        "-v ${volume}:/app "+
                                        "-v /var/run/docker.sock:/var/run/docker.sock "+
                                        "-v /usr/bin/docker:/usr/bin/docker "+
                                        "-v /usr/bin/compose:/usr/bin/compose "+
                                        "-v /usr/libexec/docker/cli-plugins/docker-compose:"+
                                        "/usr/libexec/docker/cli-plugins/docker-compose "+
                                        "-w /app/${proFolder}  ubuntu:latest "+
                                        "docker compose down "

                                    */

                            }
                        }


            }

            post{

                always{
                    sh "docker volume rm ${volume}"

                }
                success{
                    sh " docker image rm "+
                            "\$(docker images | awk '{print \$1 \" \" \$2 \" \" \$3}' "+
                            "| grep ${deployImage} | grep -v latest "+
                            "| grep -v '${deployImage} ${BUILD_NUMBER}' | awk '{print \$3}')"
                }

            }
        }