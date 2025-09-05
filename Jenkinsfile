pipeline {
    agent any  // 可指定特定执行节点，如 label 'jenkins-agent'
    environment {
        // 全局环境变量：根据实际环境修改以下值
        GIT_URL = "https://github.com/cj3127/app-demo.git"  // Git 仓库地址
        GIT_BRANCH = "main"  // 目标分支（可改为参数化构建）
        HARBOR_URL = "192.168.121.210"  // Harbor 地址（无 http/https）
        HARBOR_PROJECT = "app-demo"  // Harbor 项目名（需提前创建）
        IMAGE_NAME = "app-demo"  // Docker 镜像名称
        // 镜像标签：Jenkins 构建号 + Git 短SHA（前8位），便于版本追溯
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"
        // 部署目标服务器列表（逗号分隔，自动处理空格）
        APP_SERVERS = "192.168.121.80,192.168.121.81"
        // 目标服务器上的 docker-compose 所在目录
        APP_BASE_DIR = "/opt/app-demo"
    }
    stages {
        // 阶段1：拉取 Git 代码
        stage("拉取 Git 代码") {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${GIT_BRANCH}"]],  // 拉取远程分支（如 origin/main）
                    userRemoteConfigs: [[
                        url: "${GIT_URL}",
                        credentialsId: "git-cred"  // Jenkins 中存储的 Git 凭证（账号/SSH私钥）
                    ]]
                ])
                echo "✅ 代码拉取完成，当前分支：${GIT_BRANCH}，Git 版本：${env.GIT_COMMIT.substring(0,8)}"
            }
        }

        // 阶段2：构建 Java 应用（移除 cache 步骤，避免插件依赖）
        stage("构建 Java 应用") {
            steps {
                // 直接执行 Maven 打包（无缓存，适合未安装 Pipeline Cache Plugin 的场景）
                sh "mvn clean package -Dmaven.test.skip=true"
                // 验证 JAR 包是否生成（避免后续步骤无文件报错）
                sh "ls -lh target/${IMAGE_NAME}.jar || exit 1"
                echo "✅ 应用构建完成，JAR 包路径：target/${IMAGE_NAME}.jar"
            }
        }

        // 阶段3：构建 Docker 镜像（含 Harbor 登录）
        stage("构建 Docker 镜像") {
            steps {
                script {
                    // 登录 Harbor（使用 Jenkins 存储的凭证，避免密码明文）
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",  // Harbor 账号密码凭证 ID
                        usernameVariable: "HARBOR_USER",
                        passwordVariable: "HARBOR_PWD"
                    )]) {
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD}"
                        // 构建镜像（符合 Harbor 命名规范）
                        sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} ."
                        echo "✅ Docker 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }
        }

        // 阶段4：推送镜像到 Harbor
        stage("推送镜像到 Harbor") {
            steps {
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker logout ${HARBOR_URL}"  // 登出 Harbor，清理登录状态
                // 清理 Jenkins 节点旧镜像（避免磁盘占满）
                sh "docker image prune -f --filter 'until=720h'"  // 只清理 30 天前的无用镜像
                echo "✅ 镜像推送完成，Harbor 查看地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        // 阶段5：多节点并行部署（已修正参数格式问题）
        stage("部署到 App 服务器") {
            steps {
                script {
                    // 1. 正确拆分服务器列表（处理逗号+空格）
                    def serverList = APP_SERVERS.split(',').collect { it.trim() }
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查 APP_SERVERS 配置")
                    }
                    echo "即将并行部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    // 2. 构建并行任务 Map（parallel 需键值对格式）
                    def parallelTasks = [:]
                    for (def server : serverList) {
                        parallelTasks["部署到 ${server}"] = {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",  // 目标服务器 SSH 私钥凭证
                                keyFileVariable: "SSH_KEY",
                                usernameVariable: "SSH_USER"  // SSH 登录用户名（如 root）
                            )]) {
                                // SSH 执行部署命令
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                        # 拉取最新镜像
                                        echo "拉取镜像：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                                        docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}
                                        
                                        # 停止并删除旧容器
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            echo "停止旧容器：${IMAGE_NAME}"
                                            docker stop ${IMAGE_NAME} && docker rm ${IMAGE_NAME}
                                        fi
                                        
                                        # 启动新容器（通过 docker-compose）
                                        echo "启动新容器：${IMAGE_NAME}:${IMAGE_TAG}"
                                        cd ${APP_BASE_DIR}
                                        IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d
                                        
                                        # 验证部署结果
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            docker ps | grep ${IMAGE_NAME}
                                            echo "✅ 服务器 ${server} 部署成功"
                                        else
                                            echo "❌ 服务器 ${server} 部署失败，容器未启动"
                                            exit 1
                                        fi
                                    '
                                """
                            }
                        }
                    }

                    // 3. 执行并行部署
                    parallel parallelTasks
                    echo "✅ 所有服务器部署完成！"
                }
            }
        }
    }

    // 流水线后置通知（成功/失败）
    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor 地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "应用访问地址：http://192.168.121.88（替换为实际 Nginx VIP/IP）"
            echo "部署服务器：${serverList.join(', ')}"
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败原因：请查看「构建日志」中报错阶段的详细信息"
            echo "排查方向：1. 凭证是否存在 2. 服务器网络是否通 3. Harbor 是否可访问"
            echo "=================================================="
        }
    }
}
