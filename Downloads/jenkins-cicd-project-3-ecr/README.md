# Project 3: Continuous Delivery to AWS ECR

This is the third CI/CD project, and it completes the loop. In Projects 1 and 2 you built the CI side and stopped at building a Docker image. Here you take that image, push it to a registry in the cloud (AWS ECR), and deploy it so the app is actually running and reachable in a browser. That is the CD, continuous delivery, in CI/CD.

The app is now a small web service, so once the pipeline deploys it you can open it and see it respond.

## What is new in this project

- A container registry: AWS ECR, the cloud store your built images are pushed to.
- Jenkins credentials: you will store an AWS access key securely in Jenkins instead of writing secrets into files. This is the most important new habit in this project.
- Two new pipeline stages: `Push to ECR` and `Deploy`.
- A reachable app: the deploy stage runs the container and publishes a port, so you can browse to it.

## Learning objectives

After completing this project you will be able to:

1. Explain what continuous delivery adds on top of continuous integration.
2. Describe what a container registry is and why teams push images to one.
3. Store a secret in Jenkins credentials and use it in a pipeline without exposing it.
4. Write `Push` and `Deploy` stages that authenticate to ECR, push an image, and run it.
5. Reach a deployed app in a browser and understand how it got there.

## Concepts recap (only the new parts)

### Continuous delivery

CI proved your change is good (it built and passed tests). CD takes the good build and ships it: it stores the image somewhere durable and runs it on a server. The result is that a push to GitHub can end with a new version running, with no manual steps.

### Container registry and ECR

A container registry is like a remote warehouse for Docker images. You push an image to it, and any server with access can pull and run it. ECR (Elastic Container Registry) is AWS's private registry. Your image gets a name like `123456789012.dkr.ecr.us-east-1.amazonaws.com/cicd-project-3:7`, where the number on the end is the build that produced it.

### Why credentials must be stored, not hardcoded

To push to ECR, Jenkins must prove who it is with an AWS access key. If you wrote that key into the `Jenkinsfile`, anyone who saw your repo would have your AWS account. Instead you store the key in Jenkins's credential store and reference it by an id. The secret never appears in your code or your logs.

## Important change from Projects 1 and 2

In the earlier projects, Jenkins used Docker-in-Docker. In this project Jenkins uses the host's Docker (the `docker-compose.yml` mounts the host Docker socket). The reason is the new `Deploy` stage: by using the host's Docker, the container the pipeline starts runs on the host and its port is reachable at the host's address. This is a deliberate trade-off and it is explained in the compose file.

## Prerequisites

- An AWS account.
- Docker and Docker Compose.
- A GitHub account and a repository for this project.
- Having done Projects 1 and 2 first is strongly recommended.

You can run this locally, but to deploy and reach the app the natural home is an AWS EC2 server. See `RUN-ON-AWS.md`.

## Project structure

```
jenkins-cicd-project-3-ecr/
├── app/
│   ├── pom.xml
│   ├── Dockerfile                           # Exposes port 8080 (the web app)
│   └── src/
│       ├── main/java/guru/elevatehub/
│       │   ├── App.java                     # Tiny HTTP server (no dependencies)
│       │   └── Calculator.java
│       └── test/java/guru/elevatehub/
│           └── CalculatorTest.java
├── Jenkinsfile                              # The 7-stage pipeline
├── jenkins/
│   ├── Dockerfile                           # Jenkins + Maven + Docker CLI + AWS CLI
│   └── docker-compose.yml                   # Host-socket Jenkins (see note above)
├── AWS-ECR-SETUP.md                         # Create the ECR repo, IAM user, access key
├── RUN-ON-AWS.md                            # Run the whole thing on EC2
├── INSTALL-MAVEN.md                         # Install Maven locally (optional)
├── BUILD-TOOLS.md                           # Build tools by language
├── ASSIGNMENT.md                            # What to submit
└── README.md                               # This file
```

## Step 1: Set up AWS (ECR repository, IAM user, access key)

Follow `AWS-ECR-SETUP.md`. When you finish you will have your region, your account id, your repository name, and an access key id and secret. Keep the key id and secret handy for Step 4.

## Step 2: Push the project to GitHub

```bash
git init
git add .
git commit -m "Initial commit: Project 3 continuous delivery to ECR"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

## Step 3: Start Jenkins

```bash
cd jenkins
docker compose up -d --build
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open `http://localhost:8080` (or your server address), unlock, install suggested plugins, and create your admin user.

## Step 4: Add your AWS access key to Jenkins credentials

This is the new skill. You are storing the secret in Jenkins, not in your code.

1. Go to Manage Jenkins, then Credentials.
2. Click the `(global)` domain, then Add Credentials.
3. Set Kind to Username and password.
4. Username: your AWS Access key ID.
5. Password: your AWS Secret access key.
6. ID: type exactly `aws-ecr`. The `Jenkinsfile` looks for this id.
7. Save.

The `Jenkinsfile` reads it with `credentials('aws-ecr')`, which gives the pipeline `AWS_CREDS_USR` (the key id) and `AWS_CREDS_PSW` (the secret) without ever printing them.

## Step 5: Set your AWS values in the Jenkinsfile

Open `Jenkinsfile` and edit the three marked values at the top: `AWS_REGION`, `ECR_ACCOUNT` (your 12-digit account id), and `ECR_REPO`. Commit and push the change.

## Step 6: Create the pipeline job

1. New Item, name it `cicd-project-3`, choose Pipeline, OK.
2. Pipeline section: Definition is Pipeline script from SCM.
3. SCM is Git, paste your repository URL.
4. Branch Specifier `*/main`, Script Path `Jenkinsfile`.
5. Save.

## Step 7: Run it and open the app

Click Build Now. Watch all seven stages run. On success, the deploy stage has started the container. Open it in a browser:

- Locally: `http://localhost:8081/`
- On AWS: `http://<your-ec2-ip>:8081/` (open port 8081 in the security group)

You should see a JSON response. Try `/health` and `/add?a=2&b=3` too. The app you are looking at was built, tested, pushed to ECR, pulled back, and started entirely by the pipeline.

## Understanding the two new stages

### Push to ECR

```groovy
stage('Push to ECR') {
    steps {
        sh '''
            export AWS_ACCESS_KEY_ID=$AWS_CREDS_USR
            export AWS_SECRET_ACCESS_KEY=$AWS_CREDS_PSW
            aws ecr describe-repositories --region ${AWS_REGION} --repository-names ${ECR_REPO} \
              || aws ecr create-repository --region ${AWS_REGION} --repository-name ${ECR_REPO}
            aws ecr get-login-password --region ${AWS_REGION} \
              | docker login --username AWS --password-stdin ${ECR_REGISTRY}
            docker push ${IMAGE}:${IMAGE_TAG}
            docker push ${IMAGE}:latest
        '''
    }
}
```

The two `export` lines hand the stored credentials to the AWS CLI. The describe-or-create line makes the repository on the first run. `aws ecr get-login-password` produces a temporary password that `docker login` uses. Then the two `docker push` commands upload the image, tagged both with this build number and with `latest`.

### Deploy

```groovy
stage('Deploy') {
    steps {
        sh '''
            docker rm -f cicd-app || true
            docker run -d --name cicd-app -p ${APP_PORT}:8080 ${IMAGE}:${IMAGE_TAG}
            docker ps --filter name=cicd-app
        '''
    }
}
```

The first line removes any previous version of the app, ignoring the error if there is none. The second line runs the new image in the background, publishing the container's port 8080 to the host's port 8081. The last line shows it running.

## How to write a Push and Deploy stage yourself

The pattern for any registry-and-deploy is the same three moves:

1. Authenticate. Log Docker in to the registry using a stored credential. For ECR that is `aws ecr get-login-password | docker login`. For Docker Hub it would be `docker login -u $USER -p $TOKEN`.
2. Push. `docker push` the tagged image.
3. Deploy. On the target, `docker run` the image with a published port. Stop the old one first so the name is free.

Once you see those three moves, you can deploy to any registry or server by swapping the authenticate command.

## Exercises

1. **Make a visible change and watch it deploy.** Edit the message string in `App.java`, push, run the pipeline, and refresh the browser. Your change is live with no manual deploy.
2. **Roll back by build number.** Note an earlier `IMAGE_TAG` from a previous run, then manually run `docker run -d --name cicd-app -p 8081:8080 <image>:<older-tag>` on the host. This shows why tagging each build matters.
3. **Prove credentials stay secret.** Look through the `Push to ECR` console log. Confirm your secret key never appears. That is the credential store doing its job.

## Troubleshooting

- **`Unable to locate credentials` or an auth error in Push.** The credential id is wrong. It must be exactly `aws-ecr`, Kind Username and password, with the key id as username and the secret as password.
- **`no basic auth credentials` on push.** The `docker login` to ECR did not run or the region or account id is wrong. Check the three edited values in the `Jenkinsfile`.
- **`name is already in use by container cicd-app`.** A previous deploy is still running. The `docker rm -f cicd-app` line handles this; if you hit it manually, run that command yourself.
- **App will not open in the browser.** On AWS, port 8081 is not open in the security group, or your IP changed. Open it from your IP.
- **`permission denied` talking to Docker.** The compose file mounts the host socket and runs as root to avoid this; make sure you started Jenkins with the provided `docker-compose.yml`.

## Cleanup

```bash
cd jenkins
docker compose down -v        # stop Jenkins and remove its data
docker rm -f cicd-app || true # stop the deployed app
```

Then, to avoid charges and protect your account: delete the ECR images (or the whole repository) in the console, delete the `jenkins-ecr` access key in IAM, and if you used EC2, terminate the instance.

## What comes next

You now have a full CI/CD pipeline: a push to GitHub ends with a tested image in a registry and a running app. The next step, Project 4, is to make it automatic, so a push triggers the pipeline on its own using a GitHub webhook. After that, deploying to a managed service or a Kubernetes cluster instead of a single container is the natural progression.
