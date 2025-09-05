pipeline {
    agent any  # 任意 Jenkins 节点执行（此处为 ci-server）
    environment {
        // 全局变量（适配您的环境）
        GIT_URL = "https://github.com/cj3127/app-demo.git"  # 您的 Git 仓库地址
        GIT_BRANCH = "main"  # 代码分支
        HARBOR_URL = "192.168.121.210"  # Harbor 地址
        HARBOR_PROJECT = "app-demo"  # Harbor 项目名
        IMAGE_NAME = "app-demo"  # 镜像名
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"  # 镜像标签（构建号+Git 短提交ID）
        APP_SERVERS = ["192.168.121.80", "192.168.121.81"]  # 目标部署节点（app-server1/2）
    }
    stages {
        // 阶段1：拉取 Git 代码
        stage("拉取 Git 代码") {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${GIT_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: "${GIT_URL}",
                        credentialsId: "git-cred"  # 对应 Jenkins 中 Git 凭证 ID
                    ]]
                ])
                echo "代码拉取完成，分支：${GIT_BRANCH}"
            }
        }

        // 阶段2：构建 Java 应用（生成 JAR 包）
        stage("构建 Java 应用") {
            steps {
                // 若为 Maven 项目，执行 mvn package；若为 Gradle 项目，执行 ./gradlew build
                sh "mvn clean package -Dmaven.test.skip=true"  # 跳过测试（测试可单独加阶段）
                echo "应用构建完成，JAR 包路径：target/${IMAGE_NAME}.jar"
            }
        }

        // 阶段3：构建 Docker 镜像
        stage("构建 Docker 镜像") {
            steps {
                script {
                    // 登录 Harbor（使用 Jenkins 凭证）
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",
                        usernameVariable: "HARBOR_USER",
                        passwordVariable: "HARBOR_PWD"
                    )]) {
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD}"
                    }

                    // 构建镜像（标签包含 Harbor 地址）
                    sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} ."
                    echo "Docker 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        // 阶段4：推送镜像到 Harbor
        stage("推送镜像到 Harbor") {
            steps {
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker logout ${HARBOR_URL}"  # 登出 Harbor
                echo "镜像推送完成，可在 Harbor 查看：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        // 阶段5：部署到 App 服务器（多节点并行部署）
        stage("部署到 App 服务器") {
            steps {
                script {
                    // 遍历所有 App 服务器，并行部署
                    parallel APP_SERVERS.collect { server ->
                        [
                            "部署到 ${server}": {
                                withCredentials([sshUserPrivateKey(
                                    credentialsId: "app-server-ssh",
                                    keyFileVariable: "SSH_KEY",
                                    usernameVariable: "SSH_USER"
                                )]) {
                                    // SSH 连接目标服务器，执行部署命令
                                    sh """
                                        ssh -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                            # 1. 拉取最新镜像
                                            docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}
                                            
                                            # 2. 停止并删除旧容器（若存在）
                                            if [ \$(docker ps -q -f name=app-demo) ]; then
                                                docker stop app-demo && docker rm app-demo
                                            fi
                                            
                                            # 3. 用 docker-compose 启动新容器（传递镜像标签）
                                            cd /opt/app-demo/  # 假设 app-server 上的应用目录
                                            IMAGE_TAG=${IMAGE_TAG} docker-compose up -d
                                            
                                            # 4. 验证容器状态
                                            docker ps | grep app-demo
                                        '
                                    """
                                }
                                echo "部署完成：${server}"
                            }
                        ]
                    }
                }
            }
        }
    }

    // 流水线结束后操作（成功/失败通知）
    post {
        success {
            echo "======================================"
            echo "CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "应用访问地址：http://192.168.121.88（Nginx VIP）"
            echo "======================================"
            // 可选：添加邮件/企业微信通知（需安装对应插件）
        }
        failure {
            echo "======================================"
            echo "CI/CD 流水线执行失败！请查看日志排查问题。"
            echo "======================================"
            // 可选：失败通知
        }
    }
}
