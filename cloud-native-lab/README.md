# Cloud-Native Application on AWS

A production-grade cloud-native application deployed on Amazon EKS with Jenkins CI/CD, ALB Ingress routing, PostgreSQL and Redis storage, and Route 53 DNS.

## Live Application

- App URL: http://k8s-producti-appingre-725601d840-2144418538.us-east-1.elb.amazonaws.com
- Health: /health returns {status:ok, service:frontend}
- API: /api/data returns PostgreSQL data cached by Redis
- DNS: app.cloudnativelab.internal CNAME to ALB
- Jenkins: http://54.211.138.211:8080

## Technology Stack

| Layer | Technology |
|---|---|
| Orchestration | Amazon EKS Kubernetes v1.32 |
| Registry | Amazon ECR 2 private repos |
| Frontend | Nginx + HTML/JS |
| Backend | Node.js 16 REST API |
| Database | PostgreSQL 15.12 on RDS db.t3.micro |
| Cache | Redis 7 on ElastiCache cache.t3.micro |
| Load Balancer | AWS ALB auto-provisioned by controller |
| DNS | Amazon Route 53 hosted zone + CNAME |
| CI/CD | Jenkins on EC2 t3.medium 7-stage pipeline |

## Repository Structure

- backend/ - Node.js API server.js + Dockerfile
- frontend/ - Nginx UI index.html + nginx.conf + Dockerfile
- k8s/ - Kubernetes manifests deployments services ingress
- Jenkinsfile - 7-stage CI/CD pipeline
- cluster-config.yaml - eksctl EKS cluster definition

## CI/CD Pipeline

Jenkins 7-stage pipeline Build 10 SUCCESS in 54 seconds

1. Checkout - clone repository
2. Test - npm install verify
3. Login ECR - authenticate via IAM Role no keys
4. Build Backend - docker build and push to ECR
5. Build Frontend - docker build and push to ECR
6. Deploy EKS - kubectl set image rolling update
7. Verify - kubectl get pods and ingress

## Secrets Management

No credentials are hardcoded anywhere in this codebase.

| Secret | Storage |
|---|---|
| DATABASE_URL | Kubernetes Secret app-secrets |
| REDIS_URL | Kubernetes Secret app-secrets |
| AWS credentials | EC2 IAM Role no access keys stored |
| ALB Controller auth | IRSA via OIDC |
| Pipeline values | Jenkins Credentials Store |

## Requirements Checklist

- Route 53 hosted zone and DNS record - Done
- Application accessible via URL - Done
- EKS cluster and kubectl configured - Done
- Namespaces used production - Done
- Multiple replicas 2 per service - Done
- Frontend UI and Backend API deployed - Done
- ALB Ingress Controller installed - Done
- Two microservices deployed - Done
- PostgreSQL for persistent data - Done
- Redis for caching - Done
- Jenkins and Docker CI/CD pipeline - Done
- No hardcoded secrets - Done
- Autoscaling HPA Bonus - Done

## Cleanup

Run after submission to avoid AWS charges

    eksctl delete cluster --name cloud-native-lab --region us-east-1
    aws rds delete-db-instance --db-instance-identifier cloud-native-postgres --skip-final-snapshot
    aws elasticache delete-cache-cluster --cache-cluster-id cloud-native-redis
    aws ecr delete-repository --repository-name cloud-native-app/backend --force
    aws ecr delete-repository --repository-name cloud-native-app/frontend --force

## Author

Stephen Mensah - Cloud-Native Application on AWS - Student Laboratory Assignment
EKS - Jenkins - PostgreSQL - Redis - Route 53 - ALB - ECR - Docker
