pipeline {
    agent any
    environment {
        // 仅保留简单字符串变量，移除复杂表达式
        GIT_URL = "https://github.com/cj3127/app-demo.git"
        GIT_BRANCH = "main"
        HARBOR_URL = "192.168.121.210"
        HARBOR_PROJECT = "app-demo"
        IMAGE_NAME = "app-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"
        APP_SERVERS = "192.168.121.80,192.168.121.81"  // 原始服务器列表字符串
        APP_BASE_DIR = "/opt/app-demo"
    }
    // 定义脚本级变量，让 post 阶段可访问
    def serverList = []  // 初始化空列表

    stages {
        stage("拉取 Git 代码") {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${GIT_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: "${GIT_URL}",
                        credentialsId: "git-cred"
                    ]]
                ])
                echo "✅ 代码拉取完成，分支：${GIT_BRANCH}，版本：${env.GIT_COMMIT.substring(0,8)}"
            }
        }

        stage("构建 Java 应用") {
            steps {
                sh "mvn clean package -Dmaven.test.skip=true"
                sh "ls -lh target/${IMAGE_NAME}.jar || exit 1"
                echo "✅ 应用构建完成，JAR路径：target/${IMAGE_NAME}.jar"
            }
        }

        stage("构建 Docker 镜像") {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",
                        usernameVariable: "HARBOR_USER",
                        passwordVariable: "HARBOR_PWD"
                    )]) {
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD}"
                        sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} ."
                        echo "✅ 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }
        }

        stage("推送镜像到 Harbor") {
            steps {
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker logout ${HARBOR_URL}"
                sh "docker image prune -f --filter 'until=720h'"
                echo "✅ 镜像推送完成，Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        stage("部署到 App 服务器") {
            steps {
                script {
                    // 在 script 块内处理复杂表达式（支持 Groovy 方法）
                    serverList = APP_SERVERS.split(',').collect { it.trim() }
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查 APP_SERVERS 配置")
                    }
                    echo "即将部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    def parallelTasks = [:]
                    for (def server : serverList) {
                        parallelTasks["部署到 ${server}"] = {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",
                                keyFileVariable: "SSH_KEY",
                                usernameVariable: "SSH_USER"  // 仅在当前 withCredentials 块内有效
                            )]) {
                                sh """
                                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${SSH_USER}@${server} "
                                        echo '拉取镜像：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}'
                                        docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} || exit 1
                                        
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            echo '停止旧容器：${IMAGE_NAME}'
                                            docker stop ${IMAGE_NAME} && docker rm ${IMAGE_NAME}
                                        fi
                                        
                                        echo '启动新容器：${IMAGE_NAME}:${IMAGE_TAG}'
                                        cd ${APP_BASE_DIR} || exit 1
                                        IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d
                                        
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            docker ps | grep ${IMAGE_NAME}
                                            echo '✅ 服务器 ${server} 部署成功'
                                        else
                                            echo '❌ 服务器 ${server} 部署失败，容器未启动'
                                            exit 1
                                        fi
                                    "
                                """
                            }
                        }
                    }

                    parallel parallelTasks
                    echo "✅ 所有服务器部署完成！"
                }
            }
        }
    }

    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "部署服务器：${serverList.join(', ')}"  // 访问脚本级变量
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "排查方向："
            echo "1. 检查凭证（git-cred/harbor-cred/app-server-ssh）是否有效"
            echo "2. 目标服务器是否可通（ssh 用户名@服务器IP，如 root@192.168.121.80）"  // 移除 SSH_USER 引用
            echo "3. Harbor镜像是否推送成功（http://${HARBOR_URL}）"
            echo "=================================================="
        }
    }
}
