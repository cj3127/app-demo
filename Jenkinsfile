pipeline {
    agent any  // 任意 Jenkins 节点执行（此处为 ci-server）
    agent any  # 任意 Jenkins 节点执行（此处为 ci-server）
    environment {
        // 全局变量（适配您的环境）
        GIT_URL = "https://github.com/cj3127/app-demo.git"  // 您的 Git 仓库地址
        GIT_BRANCH = "main"  // 代码分支
        HARBOR_URL = "192.168.121.210"  // Harbor 地址
        HARBOR_PROJECT = "app-demo"  // Harbor 项目名
        IMAGE_NAME = "app-demo"  // 镜像名
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"  // 镜像标签（构建号+Git 短提交ID）
        APP_SERVERS = "192.168.121.80,192.168.121.81" // 目标部署节点（移除逗号后空格，避免解析问题）
        APP_DEPLOY_DIR = "/opt/app-demo"  // 应用部署目录，统一变量方便维护
        GIT_URL = "https://github.com/cj3127/app-demo.git"  # 您的 Git 仓库地址
        GIT_BRANCH = "main"  # 代码分支
        HARBOR_URL = "192.168.121.210"  # Harbor 地址
        HARBOR_PROJECT = "app-demo"  # Harbor 项目名
        IMAGE_NAME = "app-demo"  # 镜像名
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"  # 镜像标签（构建号+Git 短提交ID）
        APP_SERVERS = ["192.168.121.80", "192.168.121.81"]  # 目标部署节点（app-server1/2）
    }
    stages {
        // 前置检查阶段：验证必要工具是否安装
        stage("Pre-check") {
            steps {
                script {
                    echo "检查必要工具是否安装..."
                    // 检查关键工具是否存在
                    sh "git --version || { echo '❌ git 未安装'; exit 1; }"
                    sh "mvn --version || { echo '❌ maven 未安装'; exit 1; }"
                    sh "docker --version || { echo '❌ docker 未安装'; exit 1; }"
                    sh "docker-compose --version || { echo '❌ docker-compose 未安装'; exit 1; }"
                    sh "ssh -V || { echo '❌ ssh 未安装'; exit 1; }"
                    echo "✅ 所有必要工具检查通过"
                }
            }
        }

        // 阶段1：拉取 Git 代码
        stage("Checkout Code") {
        stage("拉取 Git 代码") {
            steps {
                script {
                    echo "开始拉取代码，仓库：${GIT_URL}，分支：${GIT_BRANCH}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${GIT_BRANCH}"]],
                        userRemoteConfigs: [[
                            url: "${GIT_URL}",
                            credentialsId: "git-cred"  // 对应 Jenkins 中 Git 凭证 ID
                        ]]
                    ])
                    // 输出当前代码版本信息
                    sh "git log -1 --pretty=format:'📌 最新提交: %h - %an, %ad : %s'"
                    echo "✅ 代码拉取完成"
                }
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
        stage("Build Application") {
        stage("构建 Java 应用") {
            steps {
                script {
                    echo "开始构建应用..."
                    // 执行 Maven 构建，添加错误处理
                    sh "mvn clean package -Dmaven.test.skip=true || { echo '❌ Maven 构建失败'; exit 1; }"
                    
                    // 验证构建结果
                    def jarFile = "target/${IMAGE_NAME}.jar"
                    sh "[ -f ${jarFile} ] || { echo '❌ JAR 包不存在，构建失败'; exit 1; }"
                    
                    // 输出 JAR 包信息
                    sh "ls -lh ${jarFile}"
                    echo "✅ 应用构建完成，JAR 包路径：${jarFile}"
                }
                // 若为 Maven 项目，执行 mvn package；若为 Gradle 项目，执行 ./gradlew build
                sh "mvn clean package -Dmaven.test.skip=true"  # 跳过测试（测试可单独加阶段）
                echo "应用构建完成，JAR 包路径：target/${IMAGE_NAME}.jar"
            }
        }

        // 阶段3：构建 Docker 镜像
        stage("Build Docker Image") {
        stage("构建 Docker 镜像") {
            steps {
                script {
                    echo "开始构建 Docker 镜像..."
                    def fullImageName = "${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    
                    // 登录 Harbor（使用 Jenkins 凭证）
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",
                        usernameVariable: "HARBOR_USER",
                        passwordVariable: "HARBOR_PWD"
                    )]) {
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD} || { echo '❌ Harbor 登录失败'; exit 1; }"
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD}"
                    }

                    // 构建镜像
                    sh "docker build -t ${fullImageName} . || { echo '❌ Docker 镜像构建失败'; exit 1; }"
                    
                    // 查看构建的镜像信息
                    sh "docker images | grep ${IMAGE_NAME}"
                    echo "✅ Docker 镜像构建完成：${fullImageName}"
                    // 构建镜像（标签包含 Harbor 地址）
                    sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} ."
                    echo "Docker 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        // 阶段4：推送镜像到 Harbor
        stage("Push Docker Image") {
        stage("推送镜像到 Harbor") {
            steps {
                script {
                    echo "开始推送镜像到 Harbor..."
                    def fullImageName = "${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    
                    sh "docker push ${fullImageName} || { echo '❌ 镜像推送失败'; exit 1; }"
                    
                    // 登出 Harbor 并清理本地镜像，释放空间
                    sh "docker logout ${HARBOR_URL}"
                    sh "docker rmi ${fullImageName} || true"  // 忽略删除失败
                    
                    echo "✅ 镜像推送完成，可在 Harbor 查看：http://${HARBOR_URL}/${HARBOR_PROJECT}"
                }
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                sh "docker logout ${HARBOR_URL}"  # 登出 Harbor
                echo "镜像推送完成，可在 Harbor 查看：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        // 阶段5：部署到 App 服务器（多节点并行部署）
        stage("Deploy to App Servers") {
        stage("部署到 App 服务器") {
            steps {
                script {
                    echo "开始部署到应用服务器..."
                    // 分割服务器列表为数组并去除空格
                    def servers = APP_SERVERS.split(',').collect { it.trim() }
                    
                    // 遍历所有 App 服务器，并行部署
                    parallel servers.collect { server ->
                    parallel APP_SERVERS.collect { server ->
                        [
                            "部署到 ${server}": {
                                echo "开始部署到 ${server}..."
                                withCredentials([sshUserPrivateKey(
                                    credentialsId: "app-server-ssh",
                                    keyFileVariable: "SSH_KEY",
                                    usernameVariable: "SSH_USER"
                                )]) {
                                    // SSH 连接目标服务器，执行部署命令
                                    sh """
                                        ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                            echo "在 ${server} 上执行部署操作..."
                                            
                                            // 1. 登录 Harbor
                                            docker login ${HARBOR_URL} -u ${env.HARBOR_USER} -p ${env.HARBOR_PWD} || { echo "❌ Harbor 登录失败"; exit 1; }
                                            
                                            // 2. 拉取最新镜像
                                            IMAGE=${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}
                                            echo "拉取镜像: \${IMAGE}"
                                            docker pull \${IMAGE} || { echo "❌ 镜像拉取失败"; exit 1; }
                                        ssh -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                            # 1. 拉取最新镜像
                                            docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}
                                            
                                            // 3. 停止并删除旧容器（若存在）
                                            echo "停止并删除旧容器..."
                                            # 2. 停止并删除旧容器（若存在）
                                            if [ \$(docker ps -q -f name=app-demo) ]; then
                                                docker stop app-demo && docker rm app-demo || { echo "❌ 停止/删除旧容器失败"; exit 1; }
                                                docker stop app-demo && docker rm app-demo
                                            fi
                                            
                                            // 4. 确保部署目录存在
                                            echo "准备部署目录..."
                                            mkdir -p ${APP_DEPLOY_DIR} || { echo "❌ 创建部署目录失败"; exit 1; }
                                            cd ${APP_DEPLOY_DIR} || { echo "❌ 进入部署目录失败"; exit 1; }
                                            
                                            // 5. 启动新容器
                                            echo "启动新容器..."
                                            IMAGE_TAG=${IMAGE_TAG} docker-compose up -d || { echo "❌ 启动容器失败"; exit 1; }
                                            
                                            // 6. 验证容器状态
                                            echo "验证容器状态..."
                                            if [ \$(docker ps -q -f name=app-demo) ]; then
                                                echo "✅ 容器启动成功"
                                                docker ps | grep app-demo
                                            else
                                                echo "❌ 容器启动失败，查看日志:"
                                                docker logs app-demo
                                                exit 1
                                            fi
                                            # 3. 用 docker-compose 启动新容器（传递镜像标签）
                                            cd /opt/app-demo/  # 假设 app-server 上的应用目录
                                            IMAGE_TAG=${IMAGE_TAG} docker-compose up -d
                                            
                                            // 7. 清理操作
                                            docker logout ${HARBOR_URL}
                                            docker system prune -f || true  // 清理无用镜像，释放空间
                                            # 4. 验证容器状态
                                            docker ps | grep app-demo
                                        '
                                    """
                                }
                                echo "✅ 部署完成：${server}"
                                echo "部署完成：${server}"
                            }
                        ]
                    }
@@ -187,32 +110,17 @@ pipeline {
    post {
        success {
            echo "======================================"
            echo "🎉 CI/CD 流水线执行成功！"
            echo "CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "应用访问地址：http://192.168.121.88（Nginx VIP）"
            echo "======================================"
            // 可选：添加邮件/企业微信通知（需安装对应插件）
            // emailext to: 'dev-team@example.com', subject: '✅ 构建成功: app-demo #${BUILD_NUMBER}', body: '构建详情: ${BUILD_URL}'
        }
        failure {
            echo "======================================"
            echo "❌ CI/CD 流水线执行失败！"
            echo "构建编号：${BUILD_NUMBER}"
            echo "构建地址：${BUILD_URL}"
            echo "请查看日志排查问题"
            echo "CI/CD 流水线执行失败！请查看日志排查问题。"
            echo "======================================"
            // 可选：失败通知
            // emailext to: 'dev-team@example.com', subject: '❌ 构建失败: app-demo #${BUILD_NUMBER}', body: '构建详情: ${BUILD_URL}'
        }
        always {
            echo "======================================"
            echo "❌ CI/CD 流水线执行失败！"
            echo "构建编号：${currentBuild.number}"
            echo "构建地址：${env.BUILD_URL}"
            echo "构建结果：${currentBuild.result}"  // 修正为合法字段
            echo "构建开始时间：${currentBuild.startTime}"  // 可选，按需添加
            echo "请查看日志排查问题"
            echo "======================================"
        }
    }
}