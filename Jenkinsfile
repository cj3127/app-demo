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
        // 部署验证重试参数
        DEPLOY_RETRY = 3
        DEPLOY_INTERVAL = 2
        // 全局存储服务器列表
        serverListStr = ""
    }
    
    // 移除未配置的工具声明（或替换为Jenkins中已存在的工具名称）
    // 若需保留，需在Jenkins"全局工具配置"中添加对应名称的Maven和JDK
    // tools {
    //     maven 'Maven-3.8.6'
    //     jdk 'JDK-8'
    // }

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
            post {
                failure {
                    echo "❌ 代码拉取失败，请检查Git仓库地址和凭证有效性"
                }
            }
        }

        stage("构建 Java 应用") {
            steps {
                sh "mvn clean package -Dmaven.test.skip=true"
                // 验证JAR包是否生成
                sh "ls -lh target/${IMAGE_NAME}.jar || { echo '❌ JAR包构建失败'; exit 1; }"
                echo "✅ 应用构建完成，JAR路径：target/${IMAGE_NAME}.jar"
            }
            post {
                failure {
                    echo "❌ Java应用构建失败，请检查代码编译错误或依赖问题"
                }
                // 修复：将归档步骤放入always块（post块必须包含条件）
                always {
                    archiveArtifacts artifacts: "target/${IMAGE_NAME}.jar", fingerprint: true
                }
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
                        sh """
                            # 登录Harbor，失败则终止
                            docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD} || {
                                echo '❌ Harbor登录失败，请检查凭证';
                                exit 1;
                            }
                            
                            # 构建镜像，失败则终止
                            docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} . || {
                                echo '❌ 镜像构建失败';
                                exit 1;
                            }
                        """
                        echo "✅ 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }
            post {
                failure {
                    echo "❌ Docker镜像构建失败，请检查Dockerfile或构建上下文"
                }
            }
        }

        stage("推送镜像到 Harbor") {
            steps {
                sh """
                    # 推送镜像，失败则终止
                    docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} || {
                        echo '❌ 镜像推送失败';
                        exit 1;
                    }
                    
                    # 登出并清理旧镜像
                    docker logout ${HARBOR_URL}
                    docker image prune -f --filter 'until=720h'  # 清理30天前的镜像
                """
                echo "✅ 镜像推送完成，Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
            post {
                failure {
                    echo "❌ 镜像推送至Harbor失败，请检查网络连接或仓库权限"
                }
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
                    // 存入全局变量供post阶段使用
                    env.serverListStr = serverList.join(',')
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
                                sh """
                                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${SSH_USER}@${server} '
                                        # 定义变量
                                        IMAGE="${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                                        CONTAINER="${IMAGE_NAME}"
                                        DEPLOY_DIR="${APP_BASE_DIR}"
                                        RETRY=${DEPLOY_RETRY}
                                        INTERVAL=${DEPLOY_INTERVAL}
                                        
                                        echo "===== 1/4 拉取镜像：\${IMAGE} ====="
                                        if ! docker pull \${IMAGE}; then
                                            echo "❌ 镜像拉取失败（检查Harbor地址和网络）";
                                            exit 1;
                                        fi
                                        
                                        echo "===== 2/4 清理旧容器和网络 ====="
                                        # 检查并删除所有同名容器（包括已停止的）
                                        if [ -n "\$(docker ps -a -q -f name=^/\${CONTAINER}\$)" ]; then
                                            echo "发现旧容器，执行停止和删除..."
                                            docker stop \${CONTAINER} 2>/dev/null || true  # 允许停止失败（容器可能已退出）
                                            if ! docker rm -f \${CONTAINER}; then
                                                echo "❌ 旧容器删除失败（可能被占用）";
                                                exit 1;
                                            fi
                                        else
                                            echo "未发现旧容器，跳过清理"
                                        fi
                                        
                                        # 清理可能存在的同名网络（解决网络冲突问题）
                                        if [ -n "\$(docker network ls -q -f name=^\${CONTAINER}_default\$)" ]; then
                                            echo "发现残留网络，执行删除..."
                                            docker network rm \${CONTAINER}_default 2>/dev/null || true
                                        fi
                                        
                                        echo "===== 3/4 启动新容器 ====="
                                        # 验证部署目录和配置文件
                                        if [ ! -d "\${DEPLOY_DIR}" ]; then
                                            echo "❌ 部署目录 \${DEPLOY_DIR} 不存在";
                                            exit 1;
                                        fi
                                        if [ ! -f "\${DEPLOY_DIR}/docker-compose.yml" ]; then
                                            echo "❌ docker-compose.yml 文件不存在";
                                            exit 1;
                                        fi
                                        
                                        # 启动容器
                                        cd \${DEPLOY_DIR} || exit 1
                                        if ! IMAGE_TAG=${IMAGE_TAG} HARBOR_URL=${HARBOR_URL} HARBOR_PROJECT=${HARBOR_PROJECT} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d; then
                                            echo "❌ docker-compose启动失败，日志：";
                                            docker-compose logs --tail 20;  # 输出最后20行日志便于排查
                                            exit 1;
                                        fi
                                        
                                        echo "===== 4/4 验证部署结果（最多\${RETRY}次重试） ====="
                                        for ((i=1; i<=\${RETRY}; i++)); do
                                            echo "验证第 \${i}/\${RETRY} 次..."
                                            
                                            # 检查容器是否正在运行
                                            RUNNING=\$(docker ps -q -f name=^/\${CONTAINER}\$ -f status=running)
                                            if [ -n "\${RUNNING}" ]; then
                                                echo "✅ 容器启动成功！状态："
                                                docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep \${CONTAINER}
                                                echo "✅ 服务器 ${server} 部署成功"
                                                exit 0
                                            fi
                                            
                                            # 检查容器是否已退出
                                            EXITED=\$(docker ps -a -q -f name=^/\${CONTAINER}\$ -f status=exited)
                                            if [ -n "\${EXITED}" ]; then
                                                echo "❌ 容器启动后退出，最后30行日志："
                                                docker logs \${CONTAINER} --tail 30  # 输出应用日志定位问题
                                                exit 1
                                            fi
                                            
                                            # 未就绪，等待重试
                                            if [ \${i} -lt \${RETRY} ]; then
                                                sleep \${INTERVAL}
                                            fi
                                        done
                                        
                                        # 多次重试后仍未成功
                                        echo "❌ 经过\${RETRY}次重试，容器仍未正常启动"
                                        echo "当前容器状态："
                                        docker ps -a --format "table {{.Names}}\t{{.Status}}" | grep \${CONTAINER}
                                        echo "最后50行日志："
                                        docker logs \${CONTAINER} --tail 50 2>/dev/null
                                        exit 1
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
            post {
                failure {
                    echo "❌ 应用部署到服务器失败，请查看具体服务器的错误日志"
                }
            }
        }
    }

    post {
        success {
            echo "=================================================="
            echo "🎉 CI/CD 流水线执行成功！"
            echo "构建编号：${env.BUILD_NUMBER}"
            echo "镜像标签：${IMAGE_TAG}"
            echo "Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            echo "部署服务器：${env.serverListStr.split(',').join(', ')}"
            echo "访问地址：http://${env.serverListStr.split(',')[0]}:8080"
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "构建编号：${env.BUILD_NUMBER}"
            echo "镜像标签：${IMAGE_TAG}"
            echo "排查方向："
            echo "1. 检查所有凭证（git-cred/harbor-cred/app-server-ssh）是否有效"
            echo "2. 检查目标服务器连通性：ssh ${env.SSH_USER}@目标IP"
            echo "3. 检查Harbor镜像是否存在：http://${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}/tags"
            echo "4. 检查目标服务器部署目录：ssh ${env.SSH_USER}@目标IP 'ls -ld ${APP_BASE_DIR}'"
            echo "5. 查看容器日志：ssh ${env.SSH_USER}@目标IP 'docker logs ${IMAGE_NAME} --tail 100'"
            echo "=================================================="
        }
        // 移除空的always块（或添加实际步骤如通知）
        // always {
        //     echo "流水线执行结束"
        // }
    }
}
