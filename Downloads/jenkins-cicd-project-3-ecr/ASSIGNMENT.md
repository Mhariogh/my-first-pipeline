# Project 3 Assignment: Continuous Delivery to AWS ECR

**Due: [set your deadline]**

Follow the `README.md` to build, push, and deploy the app, then complete the tasks below and submit the evidence listed.

## What to do

1. Complete the AWS setup in `AWS-ECR-SETUP.md` (ECR repository, IAM user, access key).
2. Add your AWS access key to Jenkins as a credential with id `aws-ecr` (README Step 4).
3. Set your `AWS_REGION`, `ECR_ACCOUNT`, and `ECR_REPO` in the `Jenkinsfile` and push.
4. Run the pipeline until all seven stages pass and the app deploys.
5. Open the deployed app in your browser and confirm it responds.
6. Complete Exercise 1: change the app's message, push, and watch the new version deploy.

## What to submit

1. The link to your GitHub repository.
2. A screenshot of a green pipeline run showing all seven stages passing, including `Push to ECR` and `Deploy`.
3. A screenshot of your image in the ECR repository in the AWS Console (showing the build-number tag).
4. A screenshot of the deployed app open in your browser, showing the URL with the host and port 8081.
5. Exercise 1: a screenshot of the app in the browser showing your changed message, proving the new version was deployed by the pipeline.
6. One or two sentences in your own words: what does the `Push to ECR` stage do, and why is the AWS key stored in Jenkins credentials instead of in the `Jenkinsfile`?

## Grading (100 points)

- AWS set up and `aws-ecr` credential configured: 20
- Green seven-stage pipeline including Push and Deploy: 30
- Image visible in ECR with a build-number tag: 15
- Deployed app reachable in the browser: 20
- Exercise 1 (changed message deployed) plus the written explanation: 15

## Reminder

Delete the ECR images, the IAM access key, and any EC2 instance when you are done, so you are not charged and your account stays secure.

## How to submit

Reply to the assignment email with your repository link and the screenshots attached.
