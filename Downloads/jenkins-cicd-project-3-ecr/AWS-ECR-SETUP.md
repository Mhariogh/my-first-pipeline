# AWS Setup for ECR (repository, IAM user, access key)

Do these steps once in your AWS account before running the pipeline. They create the registry your image will be pushed to, and a set of credentials Jenkins will use. You can use the AWS Console or the AWS CLI; both are shown.

You will need three values from here for the `Jenkinsfile`: your AWS region, your 12-digit account id, and the repository name.

## 1. Find your account id and pick a region

- Account id: top right of the AWS Console, under your account menu, or run `aws sts get-caller-identity --query Account --output text`.
- Region: pick one close to you, for example `us-east-1`. Use the same region everywhere.

## 2. Create the ECR repository

The pipeline creates the repository automatically on first push, so this step is optional. To do it yourself:

Console: open ECR, click Create repository, choose Private, name it `cicd-project-3`, create.

CLI:

```bash
aws ecr create-repository --repository-name cicd-project-3 --region us-east-1
```

## 3. Create an IAM user for Jenkins

Jenkins needs credentials that are allowed to push to ECR. The cleanest beginner approach is a dedicated IAM user with only ECR permissions.

Console:

1. Open IAM, go to Users, click Create user.
2. Name it `jenkins-ecr`. Do not give it console access.
3. On permissions, choose Attach policies directly and attach `AmazonEC2ContainerRegistryPowerUser`.
4. Create the user.

CLI:

```bash
aws iam create-user --user-name jenkins-ecr
aws iam attach-user-policy --user-name jenkins-ecr \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser
```

## 4. Create an access key for that user

This is the username and password Jenkins will store.

Console: open the `jenkins-ecr` user, Security credentials tab, Create access key, choose Application running outside AWS, create, and copy both the Access key ID and the Secret access key. You only see the secret once.

CLI:

```bash
aws iam create-access-key --user-name jenkins-ecr
```

Copy the `AccessKeyId` and `SecretAccessKey` from the output. Keep them safe; treat the secret like a password.

## 5. Put the values into the project

- Open the `Jenkinsfile` and set `AWS_REGION`, `ECR_ACCOUNT`, and `ECR_REPO` at the top.
- The access key id and secret go into Jenkins as a credential, not into any file. That is covered in the README, Step 4.

## Security and cleanup

- Never commit the access key to GitHub. It belongs only in the Jenkins credential store.
- When you finish the project, delete the access key (`aws iam delete-access-key`), and delete the ECR images so you are not charged for storage.
