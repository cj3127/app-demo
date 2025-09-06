pipeline {
    agent any
    environment {
        // 基础环境变量
        GIT_URL = "https://github.com/cj3127/app-demo.git"
        GIT_BRANCH = "main"
        HARBOR_URL = "192.168.121.210"
        HARBOR_PROJECT = "app-demo"
        IMAGE_NAME = "app-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"
        APP_SERVERS = "192.168.121.80,192.168.121.81"
        APP_BASE_DIR = "/opt/app-demo"
    }
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
                    // 拆分服务器列表
                    def serverList = APP_SERVERS.split(',').collect { it.trim() }
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查 APP_SERVERS 配置")
                    }
                    
                    echo "即将部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    // 构建并行部署任务
                    def parallelTasks = [:]
                    for (def server : serverList) {
                        parallelTasks["部署到 ${server}"] = {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",
                                keyFileVariable: "SSH_KEY",
                                usernameVariable: "SSH_USER"
                            )]) {
                                // 使用更可靠的部署脚本
                                sh """
                                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                        # 拉取镜像
                                        echo "拉取镜像：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                                        docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} || exit 1
                                        
                                        # 检查并停止旧容器
                                        CONTAINER_ID=\$(docker ps -q -f name=${IMAGE_NAME})
                                        if [ ! -z "\$CONTAINER_ID" ]; then
                                            echo "停止旧容器：${IMAGE_NAME} (\$CONTAINER_ID)"
                                            docker stop \$CONTAINER_ID && docker rm \$CONTAINER_ID
                                        fi
                                        
                                        # 启动新容器
                                        echo "启动新容器：${IMAGE_NAME}:${IMAGE_TAG}"
                                        cd ${APP_BASE_DIR} || exit 1
                                        IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d
                                        
                                        # 等待几秒让容器启动
                                        sleep 5
                                        
                                        # 验证部署结果
                                        NEW_CONTAINER_ID=\$(docker ps -q -f name=${IMAGE_NAME})
                                        if [ ! -z "\$NEW_CONTAINER_ID" ]; then
                                            echo "容器状态:"
                                            docker ps -f name=${IMAGE_NAME}
                                            echo "✅ 服务器 ${server} 部署成功"
                                        else
                                            echo "❌ 服务器 ${server} 部署失败，容器未启动"
                                            echo "尝试查看日志:"
                                            docker logs ${IMAGE_NAME} 2>/dev/null || echo "无法获取日志"
                                            exit 1
                                        fi
                                    '
                                """
                            }
                        }
                    }

                    // 执行并行部署
                    parallel parallelTasks
                    echo "✅ 所有服务器部署完成！"
                }
            }
        }
    }

    // 流水线后置通知
    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "部署服务器：${APP_SERVERS}"  // 直接使用环境变量
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "排查方向："
            echo "1. 检查凭证（git-cred/harbor-cred/app-server-ssh）是否有效"
            echo "2. 目标服务器是否可通（示例：ssh root@192.168.121.80）"
            echo "3. Harbor镜像是否推送成功（访问：http://${HARBOR_URL}）"
            echo "4. 目标服务器是否有 ${APP_BASE_DIR} 目录和docker-compose.yml"
            echo "5. 检查目标服务器上的Docker Compose配置"
            echo "=================================================="
        }
    }
}
