pipeline {
    agent any
    environment {
        GIT_URL = "https://github.com/cj3127/app-demo.git"
        GIT_BRANCH = "main"
        HARBOR_URL = "192.168.121.210"
        HARBOR_PROJECT = "app-demo"
        IMAGE_NAME = "app-demo"
        IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.substring(0,8)}"
        APP_SERVERS = "192.168.121.80,192.168.121.81"
        APP_BASE_DIR = "/opt/app-demo"
        CONTAINER_NAME = "app-demo"
        TARGET_PORT = "8080"
        serverListStr = ""
        SSH_USER_GLOBAL = "root"
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
                sh "ls -lh target/${IMAGE_NAME}.jar || { echo '❌ JAR包不存在'; exit 1; }"
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
                        sh "docker login ${HARBOR_URL} -u ${HARBOR_USER} -p ${HARBOR_PWD} || { echo '❌ Harbor登录失败'; exit 1; }"
                        sh "docker build -t ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} . || { echo '❌ 镜像构建失败'; exit 1; }"
                        echo "✅ 镜像构建完成：${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }
        }

        stage("推送镜像到 Harbor") {
            steps {
                sh "docker push ${HARBOR_URL}/${HARBOR_PROJECT}/${IMAGE_NAME}:${IMAGE_TAG} || { echo '❌ 镜像推送失败'; exit 1; }"
                sh "docker logout ${HARBOR_URL}"
                sh "docker image prune -f --filter 'until=720h'"
                echo "✅ 镜像推送完成，Harbor地址：http://${HARBOR_URL}/${HARBOR_PROJECT}"
            }
        }

        stage("部署到 App 服务器") {
            steps {
                script {
                    def serverList = APP_SERVERS.split(',').collect { it.trim() }
                    if (serverList.isEmpty()) {
                        error("❌ 部署服务器列表为空，请检查 APP_SERVERS 配置")
                    }
                    env.serverListStr = serverList.join(',')
                    echo "即将部署到 ${serverList.size()} 台服务器：${serverList.join(', ')}"

                    def parallelTasks = [:]
                    for (def server : serverList) {
                        parallelTasks["部署到 ${server}"] = {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: "app-server-ssh",
                                keyFileVariable: "SSH_KEY",
                                usernameVariable: "SSH_USER"
                            )]) {
                                // 核心：用单引号包裹SSH命令，仅对Groovy变量用${}，Shell变量直接用$
                                sh '''
                                    ssh -i ''' + SSH_KEY + ''' -o StrictHostKeyChecking=no ''' + SSH_USER + '''@''' + server + ''' '
                                        # 1. 拉取镜像
                                        echo '===== 拉取镜像：''' + HARBOR_URL + '''/''' + HARBOR_PROJECT + '''/''' + IMAGE_NAME + ''':''' + IMAGE_TAG + ''' ====='
                                        if ! docker pull ''' + HARBOR_URL + '''/''' + HARBOR_PROJECT + '''/''' + IMAGE_NAME + ''':''' + IMAGE_TAG + '''; then
                                            echo '❌ 镜像拉取失败'; exit 1;
                                        fi

                                        # 2. 检查端口占用
                                        echo '===== 检查端口 ''' + TARGET_PORT + ''' 是否占用 ====='
                                        if ss -tulnp | grep -q :''' + TARGET_PORT + '''; then
                                            echo '❌ 端口 ''' + TARGET_PORT + ''' 已被占用：';
                                            ss -tulnp | grep :''' + TARGET_PORT + ''';
                                            exit 1;
                                        fi

                                        # 3. 清理旧容器（Shell变量直接用$，无需转义）
                                        echo '===== 清理旧容器 ''' + CONTAINER_NAME + ''' ====='
                                        if docker ps -a | grep -q ''' + CONTAINER_NAME + '''; then
                                            echo '发现旧容器，强制删除...';
                                            docker ps -a | grep ''' + CONTAINER_NAME + ''' | awk '{print $1}' | xargs -I {} docker rm -f {};
                                        fi
                                        cd ''' + APP_BASE_DIR + ''' || { echo '❌ 部署目录不存在'; exit 1; }
                                        docker-compose down --remove-orphans >/dev/null 2>&1;
                                        if docker ps -a | grep -q ''' + CONTAINER_NAME + '''; then
                                            echo '❌ 旧容器清理失败'; exit 1;
                                        fi

                                        # 4. 启动新容器
                                        echo '===== 启动新容器：''' + CONTAINER_NAME + ''':''' + IMAGE_TAG + ''' ====='
                                        IMAGE_TAG=''' + IMAGE_TAG + ''' HARBOR_URL=''' + HARBOR_URL + ''' HARBOR_PROJECT=''' + HARBOR_PROJECT + ''' IMAGE_NAME=''' + IMAGE_NAME + ''' docker-compose up -d || {
                                            echo '❌ docker-compose启动失败'; exit 1;
                                        }

                                        # 5. 验证容器状态（所有变量直接拼接，避免转义冲突）
                                        echo '===== 验证容器状态 ====='
                                        RETRY=5
                                        INTERVAL=3
                                        for ((i=1; i<=RETRY; i++)); do
                                            echo "验证第 \$i/\$RETRY 次（间隔\$INTERVAL秒）";
                                            docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "''' + CONTAINER_NAME + '''|NAMES";
                                            
                                            if docker ps --format "{{.Names}}|{{.Status}}" | grep -q "^''' + CONTAINER_NAME + '''|Up"; then
                                                echo '✅ 容器启动成功';
                                                docker ps | grep ''' + CONTAINER_NAME + ''';
                                                echo '✅ 服务器 ''' + server + ''' 部署成功';
                                                exit 0;
                                            fi
                                            
                                            if docker ps -a --format "{{.Names}}|{{.Status}}" | grep -q "^''' + CONTAINER_NAME + '''|Exited"; then
                                                echo '❌ 容器启动后退出，日志：';
                                                docker logs ''' + CONTAINER_NAME + ''' --tail 30;
                                                exit 1;
                                            fi
                                            
                                            sleep \$INTERVAL;
                                        done

                                        # 6. 验证失败处理
                                        echo '❌ 多次重试后容器仍未启动';
                                        docker logs ''' + CONTAINER_NAME + ''' --tail 50 2>/dev/null;
                                        exit 1;
                                    '
                                '''
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
            echo "部署服务器：${env.serverListStr.split(',').join(', ')}"
            echo "=================================================="
        }
        failure {
            echo "=================================================="
            echo "❌ CI/CD 流水线执行失败！"
            echo "失败阶段：${currentBuild.currentResult}"
            echo "排查方向："
            echo "1. 检查凭证（git-cred/harbor-cred/app-server-ssh）是否有效"
            echo "2. 检查端口占用：ssh ${SSH_USER_GLOBAL}@192.168.121.80 'ss -tulnp | grep :${TARGET_PORT}'"
            echo "3. 检查容器日志：ssh ${SSH_USER_GLOBAL}@故障服务器 'docker logs ${CONTAINER_NAME} --tail 100'"
            echo "4. 确认 ${APP_BASE_DIR}/docker-compose.yml 配置了 container_name: ${CONTAINER_NAME}"
            echo "=================================================="
        }
    }
}
