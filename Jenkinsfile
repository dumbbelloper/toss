// gotgan CI — backend·web 이미지 빌드(Kaniko) → Harbor → gotgan-gitops 태그 갱신 → Argo CD 동기화
// 주의: kaniko는 빌드 중 컨테이너 rootfs를 덮어쓰므로 이미지당 별도 kaniko 컨테이너를 쓴다.
pipeline {
  agent {
    kubernetes {
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kaniko-backend
    image: gcr.io/kaniko-project/executor:debug
    command: ["/busybox/cat"]
    tty: true
    resources:
      requests: { cpu: "500m", memory: "1Gi" }
      limits:   { cpu: "2",    memory: "3Gi" }
    volumeMounts:
    - name: harbor-docker
      mountPath: /kaniko/.docker
  - name: kaniko-web
    image: gcr.io/kaniko-project/executor:debug
    command: ["/busybox/cat"]
    tty: true
    resources:
      requests: { cpu: "250m", memory: "512Mi" }
      limits:   { cpu: "2",    memory: "2Gi" }
    volumeMounts:
    - name: harbor-docker
      mountPath: /kaniko/.docker
  - name: git
    image: alpine/git:latest
    command: ["cat"]
    tty: true
  volumes:
  - name: harbor-docker
    secret:
      secretName: harbor-dockerconfig
      items:
      - key: .dockerconfigjson
        path: config.json
'''
    }
  }
  environment {
    IMG = 'harbor.gotgan.live/gotgan'
  }
  stages {
    stage('Prepare') {
      steps {
        script { env.TAG = sh(returnStdout: true, script: 'git rev-parse --short=8 HEAD').trim() }
        echo "이미지 태그: ${env.TAG}"
      }
    }
    stage('Build backend') {
      steps {
        container('kaniko-backend') {
          sh '''/kaniko/executor \
            --context=dir://$WORKSPACE/backend \
            --dockerfile=Dockerfile \
            --destination=$IMG/backend:$TAG \
            --destination=$IMG/backend:latest'''
        }
      }
    }
    stage('Build web') {
      steps {
        container('kaniko-web') {
          sh '''/kaniko/executor \
            --context=dir://$WORKSPACE/web \
            --dockerfile=Dockerfile \
            --destination=$IMG/web:$TAG \
            --destination=$IMG/web:latest'''
        }
      }
    }
    stage('Update gitops') {
      steps {
        container('git') {
          withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
            sh '''set -e
              git config --global user.email "ci@gotgan.live"
              git config --global user.name "gotgan-ci"
              git clone https://x-access-token:$GH_TOKEN@github.com/dumbbelloper/gotgan-gitops.git /tmp/gitops
              cd /tmp/gitops
              sed -i "s|newTag:.*|newTag: \\"$TAG\\"|" apps/backend/kustomization.yaml
              sed -i "s|newTag:.*|newTag: \\"$TAG\\"|" apps/web/kustomization.yaml
              git add -A
              git commit -m "ci: gotgan backend/web -> $TAG" || { echo "변경 없음"; exit 0; }
              git push origin main
            '''
          }
        }
      }
    }
  }
}
