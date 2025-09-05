pipeline {
    agent any  // 可指定特定执行节点，如 label 'jenkins-agent'
    environment {
        // 全局环境变量：根据实际环境修改以下值
        GIT_URL = "https://github.com/cj3127/app-demo.git"  // Git 仓库地址
        GIT_BRANCH = "main"  // 目标分支（可改为参数化构建，见注释）
        HARBOR_URL = "192.168.121.210"  // Harbor 镜像仓库地址（无 http/https）
        HARBOR_PROJECT = "app-demo"  // Harbor 中的项目名（需提前创建）
        IMAGE_NAME = "app-demo"  // Docker 镜像名称
        // 镜像标签：Jenkins 构建号 + Git 短SHA（前8位），便于版本追溯
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"
        // 部署目标服务器列表（逗号分隔，trim() 会自动处理空格）
        APP_SERVERS = "192.168.121.80,192.168.121.81"
        // 应用基础路径（目标服务器上的 docker-compose 所在目录）
        APP_BASE_DIR = "/opt/app-demo"
    }
    // 可选：参数化构建（支持切换分支/环境，需勾选"此项目是参数化的"）
    // parameters {
    //     string(name: "GIT_BRANCH", defaultValue: "main", description: "Git 分支（如 dev/test/main）")
    //     string(name: "APP_SERVERS", defaultValue: "192.168.121.80,192.168.121.81", description: "部署服务器列表（逗号分隔）")
    // }
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

        // 阶段2：构建 Java 应用（生成 JAR 包）- 含 Maven 依赖缓存
        stage("构建 Java 应用") {
            steps {
                // 缓存 Maven 本地仓库（加速后续构建，需安装 Pipeline Cache Plugin）
                cache(path: "${HOME}/.m2/repository", key: "maven-{{ checksum 'pom.xml' }}") {
                    // Maven 打包：清理旧产物 + 跳过测试（如需测试删除 -Dmaven.test.skip=true）
                    sh "mvn clean package -Dmaven.test.skip=true"
                }
                // 验证 JAR 包是否生成（避免后续步骤无文件报错）
                sh "ls -lh target/${IMAGE_NAME}.jar || exit 1"
                echo "✅ 应用构建完成，JAR 包路径：target/${IMAGE_NAME}.jar"
            }
        }

        // 阶段3：构建 Docker 镜像（含 Harbor 登录）
        stage("构建 Docker 镜像") {
            steps {
                script {
                    // 登录 Harbor（使用 Jenkins 存储的 Harbor 凭证，避免密码明文）
                    withCredentials([usernamePassword(
                        credentialsId: "harbor-cred",  // Jenkins 中存储的 Harbor 账号密码
                        usernameVariable: "HARBOR_USER",  // 用户名变量（内部使用）
                        passwordVariable: "HARBOR_PWD"    // 密码变量（内部使用）
                    )]) {
                        // 登录 Harbor（若 Harbor 用 http，需配置 Docker 允许 insecure-registries）
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD}"
                        // 构建 Docker 镜像（标签符合 Harbor 规范：仓库地址/项目/镜像名:标签）
                        sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} ."
                        echo "✅ Docker 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }
        }

        // 阶段4：推送 Docker 镜像到 Harbor
        stage("推送镜像到 Harbor") {
            steps {
                // 推送镜像到 Harbor（需确保 Harbor 项目有推送权限）
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                // 登出 Harbor（清理登录状态，减少安全风险）
                sh "docker logout ${HARBOR_URL}"
                // 可选：清理 Jenkins 节点旧镜像（避免磁盘占满）
                sh "docker image prune -f --filter 'reference=${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:*' --filter 'until=720h'"
                echo "✅ 镜像推送完成，Harbor 查看地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        // 阶段5：多节点并行部署到 App 服务器（核心修正点）
        stage("部署到 App 服务器") {
            steps {
                script {
                    // 1. 拆分服务器列表：处理逗号分隔，去除空格（解决原报错核心问题）
                    def serverList = APP_SERVERS.split(',').collect { it.trim() }
                    // 验证服务器列表非空
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查 APP_SERVERS 配置")
                    }
                    echo "即将并行部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    // 2. 构建并行任务 Map（parallel 需接收键值对：任务名→执行逻辑）
                    def parallelTasks = [:]
                    for (def server : serverList) {
                        // 为每台服务器创建一个并行任务（任务名：部署到 [IP]）
                        parallelTasks["部署到 ${server}"] = {
                            // 使用 SSH 私钥登录目标服务器（需 Jenkins 存储对应凭证）
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",  // Jenkins 中存储的目标服务器 SSH 私钥
                                keyFileVariable: "SSH_KEY",       // 私钥文件路径变量（内部使用）
                                usernameVariable: "SSH_USER"      // SSH 登录用户名（如 root）
                            )]) {
                                // SSH 连接目标服务器，执行部署命令（三重引号保留换行，便于阅读）
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                        # 1. 拉取最新镜像（确保目标服务器能访问 Harbor）
                                        echo "正在拉取镜像：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                                        docker pull ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}
                                        
                                        # 2. 停止并删除旧容器（若容器存在）
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            echo "停止旧容器：${IMAGE_NAME}"
                                            docker stop ${IMAGE_NAME} && docker rm ${IMAGE_NAME}
                                        fi
                                        
                                        # 3. 切换到应用目录，通过 docker-compose 启动新容器
                                        echo "启动新容器：${IMAGE_NAME}:${IMAGE_TAG}"
                                        cd ${APP_BASE_DIR}
                                        # 传递镜像标签到 docker-compose（需 compose 文件引用此变量）
                                        IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d
                                        
                                        # 4. 验证部署结果（检查容器是否运行）
                                        echo "验证容器状态："
                                        if [ \$(docker ps -q -f name=${IMAGE_NAME}) ]; then
                                            docker ps | grep ${IMAGE_NAME}
                                            echo "✅ 服务器 ${server} 部署成功"
                                        else
                                            echo "❌ 服务器 ${server} 部署失败，容器未启动"
                                            exit 1  # 失败终止当前任务
                                        fi
                                    '
                                """
                            }
                        }
                    }

                    // 3. 执行并行部署（所有服务器同时执行，任一失败则整个阶段失败）
                    parallel parallelTasks
                    echo "✅ 所有服务器部署完成！"
                }
            }
        }
    }

    // 流水线后置操作：成功/失败通知
    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor 地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "应用访问地址：http://192.168.121.88（Nginx VIP，需替换为实际地址）"
            echo "部署服务器：${APP_SERVERS.split(',').collect { it.trim() }.join(', ')}"
            echo "=================================================="
            

        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "排查建议：1. 查看对应阶段日志 2. 检查凭证/服务器网络 3. 验证 Harbor 可访问"
            echo "=================================================="
            
            
        }
    }
}
