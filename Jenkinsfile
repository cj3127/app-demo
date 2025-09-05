pipeline {
    agent any  // 可指定执行节点，如 label 'jenkins-agent'
    environment {
        // 全局环境变量：根据实际环境修改
        GIT_URL = "https://github.com/cj3127/app-demo.git"  // Git仓库地址
        GIT_BRANCH = "main"  // 目标分支
        HARBOR_URL = "192.168.121.210"  // Harbor地址（无协议头）
        HARBOR_PROJECT = "app-demo"  // Harbor项目名（需提前创建）
        IMAGE_NAME = "app-demo"  // 镜像名称
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"  // 镜像标签
        APP_SERVERS = "192.168.121.80,192.168.121.81"  // 部署目标服务器
        APP_BASE_DIR = "/opt/app-demo"  // 目标服务器部署目录
        // 全局服务器列表（解决Post阶段引用问题）
        serverList = APP_SERVERS.split(',').collect { it.trim() }
    }
    stages {
        // 阶段1：拉取Git代码
        stage("拉取 Git 代码") {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${GIT_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: "${GIT_URL}",
                        credentialsId: "git-cred"  // Git凭证ID
                    ]]
                ])
                echo "✅ 代码拉取完成，分支：${GIT_BRANCH}，版本：${env.GIT_COMMIT.substring(0,8)}"
            }
        }

        // 阶段2：构建Java应用
        stage("构建 Java 应用") {
            steps {
                sh "mvn clean package -Dmaven.test.skip=true"
                sh "ls -lh target/${IMAGE_NAME}.jar || exit 1"  // 验证JAR包存在
                echo "✅ 应用构建完成，JAR路径：target/${IMAGE_NAME}.jar"
            }
        }

        // 阶段3：构建Docker镜像
        stage("构建 Docker 镜像") {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",  // Harbor凭证ID
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

        // 阶段4：推送镜像到Harbor
        stage("推送镜像到 Harbor") {
            steps {
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker logout ${HARBOR_URL}"
                sh "docker image prune -f --filter 'until=720h'"  // 清理30天前镜像
                echo "✅ 镜像推送完成，Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        // 阶段5：多节点并行部署（已修复SSH登录和变量解析）
        stage("部署到 App 服务器") {
            steps {
                script {
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查APP_SERVERS配置")
                    }
                    echo "即将部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    // 构建并行部署任务
                    def parallelTasks = [:]
                    for (def server : serverList) {
                        parallelTasks["部署到 ${server}"] = {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",  // SSH凭证ID（需配置用户名）
                                keyFileVariable: "SSH_KEY",
                                usernameVariable: "SSH_USER"
                            )]) {
                                // 修复1：添加-i ${SSH_KEY}指定私钥
                                // 修复2：远程命令用双引号，转义远程变量\$
                                sh """
                                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${SSH_USER}@${server} "
                                        # 拉取镜像
                                        echo '拉取镜像：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}'
                                        docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} || exit 1
                                        
                                        # 停止旧容器
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            echo '停止旧容器：${IMAGE_NAME}'
                                            docker stop ${IMAGE_NAME} && docker rm ${IMAGE_NAME}
                                        fi
                                        
                                        # 启动新容器
                                        echo '启动新容器：${IMAGE_NAME}:${IMAGE_TAG}'
                                        cd ${APP_BASE_DIR} || exit 1
                                        IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d
                                        
                                        # 验证部署
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

                    // 执行并行部署
                    parallel parallelTasks
                    echo "✅ 所有服务器部署完成！"
                }
            }
        }
    }

    // 流水线通知（成功/失败）
    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "部署服务器：${serverList.join(', ')}"
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "排查方向："
            echo "1. 检查凭证（git-cred/harbor-cred/app-server-ssh）是否有效"
            echo "2. 目标服务器是否可通（ssh ${SSH_USER}@服务器IP）"
            echo "3. Harbor镜像是否推送成功（http://${HARBOR_URL}）"
            echo "=================================================="
        }
    }
}
    
