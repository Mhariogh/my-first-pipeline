#  Cloud-Native Application on AWS

> A production-grade cloud-native application deployed on **Amazon EKS** with **Jenkins CI/CD**, path-based **ALB Ingress routing**, **PostgreSQL + Redis** storage layer, and a custom **Route 53** DNS configuration — built as a complete student lab assignment.

---

##  Live Application

| | |
|---|---|
| **Application URL** | `http://k8s-producti-appingre-725601d840-2144418538.us-east-1.elb.amazonaws.com` |
| **Health Check** | `/health` → `{"status":"ok","service":"frontend"}` |
| **API Endpoint** | `/api/data` → PostgreSQL data (cached by Redis) |
| **DNS Record** | `app.cloudnativelab.internal` → CNAME → ALB |
| **Jenkins UI** | `http://54.211.138.211:8080` |

---

##  Architecture

```
Internet Users
      │
      ▼
Amazon Route 53  ──  CNAME: app.cloudnativelab.internal → ALB DNS
      │
      ▼
Application Load Balancer (ALB)  ──  HTTP :80
      │
      ▼
AWS Load Balancer Controller  +  Kubernetes Ingress (app-ingress)
      │
      ├── path: /       ──▶  frontend-service (ClusterIP :80)
      │                           │
      │                           ▼
      │                     Frontend Pods ×2  (Nginx)
      │
      └── path: /api    ──▶  backend-service (ClusterIP :3000)
                                  │
                     ┌────────────┴────────────┐
                     ▼                         ▼
              PostgreSQL (RDS)         Redis (ElastiCache)
              db.t3.micro              cache.t3.micro
```

---

##  Technology Stack

| Layer | Technology | Detail |
|---|---|---|
| **Cloud** | Amazon Web Services | us-east-1 |
| **Orchestration** | Amazon EKS | Kubernetes v1.32 |
| **Registry** | Amazon ECR | 2 private repos |
| **Frontend** | Nginx + HTML/JS | Single-page app |
| **Backend** | Node.js 16 REST API | /health + /api/data |
| **Database** | PostgreSQL 15.12 | RDS db.t3.micro |
| **Cache** | Redis 7 | ElastiCache cache.t3.micro |
| **Load Balancer** | AWS ALB | Auto-provisioned by controller |
| **DNS** | Amazon Route 53 | Hosted zone + CNAME |
| **CI/CD** | Jenkins on EC2 | t3.medium, 7-stage pipeline |
| **IaC** | eksctl + Helm | Cluster + controller management |

---

## Repository Structure

```
my-first-pipeline/
│
├── cloud-native-lab/
│   ├── Jenkinsfile                  # 7-stage CI/CD pipeline
│   ├── cluster-config.yaml          # eksctl cluster definition
│   │
│   ├── backend/
│   │   ├── server.js                # Node.js API (PostgreSQL + Redis)
│   │   ├── package.json
│   │   └── Dockerfile
│   │
│   ├── frontend/
│   │   ├── index.html               # Single-page UI
│   │   ├── nginx.conf               # Proxies /api to backend
│   │   └── Dockerfile
│   │
│   └── k8s/
│       ├── backend-deployment.yaml
│       ├── frontend-deployment.yaml
│       ├── services.yaml
│       └── ingress.yaml
│
└── README.md
```

---

## Deployment Guide

### Prerequisites

```bash
aws --version        # AWS CLI v2
kubectl version      # kubectl
eksctl version       # eksctl
helm version         # Helm
docker --version     # Docker

aws configure        # set credentials
aws sts get-caller-identity   # verify
```

### Step 1 — Create EKS Cluster

```bash
eksctl create cluster -f cloud-native-lab/cluster-config.yaml
aws eks update-kubeconfig --region us-east-1 --name cloud-native-lab
kubectl get nodes
kubectl create namespace production
```

### Step 2 — Push Images to ECR

```bash
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
ECR_URI=$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

aws ecr create-repository --repository-name cloud-native-app/backend
aws ecr create-repository --repository-name cloud-native-app/frontend

aws ecr get-login-password --region $REGION | \
  docker login --username AWS --password-stdin $ECR_URI

docker build -t $ECR_URI/cloud-native-app/backend:latest ./cloud-native-lab/backend
docker push $ECR_URI/cloud-native-app/backend:latest

docker build -t $ECR_URI/cloud-native-app/frontend:latest ./cloud-native-lab/frontend
docker push $ECR_URI/cloud-native-app/frontend:latest
```

### Step 3 — PostgreSQL & Redis

```bash
VPC_ID=$(aws eks describe-cluster --name cloud-native-lab \
  --query 'cluster.resourcesVpcConfig.vpcId' --output text)

SUBNETS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" "Name=mapPublicIpOnLaunch,Values=false" \
  --query 'Subnets[*].SubnetId' --output text | tr '\t' ' ')

# RDS PostgreSQL
aws rds create-db-subnet-group \
  --db-subnet-group-name cloud-native-db-subnet \
  --db-subnet-group-description "Lab DB subnet" \
  --subnet-ids $SUBNETS

aws rds create-db-instance \
  --db-instance-identifier cloud-native-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres --engine-version 15.12 \
  --master-username dbadmin \
  --master-user-password 'YourPassword' \
  --db-name appdb --allocated-storage 20 \
  --db-subnet-group-name cloud-native-db-subnet \
  --no-publicly-accessible --no-multi-az

# ElastiCache Redis
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name cloud-native-redis-subnet \
  --cache-subnet-group-description "Lab Redis subnet" \
  --subnet-ids $SUBNETS

aws elasticache create-cache-cluster \
  --cache-cluster-id cloud-native-redis \
  --cache-node-type cache.t3.micro \
  --engine redis --num-cache-nodes 1 \
  --cache-subnet-group-name cloud-native-redis-subnet
```

Store credentials as Kubernetes Secrets (no hardcoding):

```bash
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier cloud-native-postgres \
  --query 'DBInstances[0].Endpoint.Address' --output text)

REDIS_ENDPOINT=$(aws elasticache describe-cache-clusters \
  --cache-cluster-id cloud-native-redis --show-cache-node-info \
  --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' --output text)

kubectl create secret generic app-secrets \
  --from-literal=DATABASE_URL='postgresql://dbadmin:YourPassword@'"$RDS_ENDPOINT"':5432/appdb?sslmode=disable' \
  --from-literal=REDIS_URL='redis://'"$REDIS_ENDPOINT"':6379' \
  -n production
```

### Step 4 — ALB Controller

```bash
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

eksctl create iamserviceaccount \
  --cluster=cloud-native-lab --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::$ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts --approve

helm repo add eks https://aws.github.io/eks-charts && helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=cloud-native-lab \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 5 — Deploy to Kubernetes

```bash
kubectl apply -f cloud-native-lab/k8s/
kubectl get pods -n production -w     # wait for 4 Running pods
kubectl get ingress -n production     # get ALB address
```

### Step 6 — Route 53

```bash
aws route53 create-hosted-zone \
  --name cloudnativelab.internal \
  --caller-reference $(date +%s)

HOSTED_ZONE_ID=$(aws route53 list-hosted-zones \
  --query "HostedZones[?Name=='cloudnativelab.internal.'].Id" \
  --output text | cut -d/ -f3)

ALB_DNS=$(kubectl get ingress app-ingress -n production \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch "{
    \"Changes\": [{
      \"Action\": \"CREATE\",
      \"ResourceRecordSet\": {
        \"Name\": \"app.cloudnativelab.internal\",
        \"Type\": \"CNAME\",
        \"TTL\": 300,
        \"ResourceRecords\": [{\"Value\": \"$ALB_DNS\"}]
      }
    }]
  }"
```

### Step 7 — Jenkins CI/CD

```bash
# On Jenkins EC2 instance (Amazon Linux 2)
sudo rpm --import https://yum.corretto.aws/corretto.key
sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo
sudo yum install -y java-21-amazon-corretto-devel
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum install jenkins docker git -y
sudo systemctl start jenkins docker
sudo usermod -aG docker jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Jenkins Credentials to add (Secret text):

| ID | Value |
|---|---|
| `AWS_ACCOUNT_ID` | Your 12-digit account ID |
| `AWS_REGION` | `us-east-1` |
| `ECR_REGISTRY` | `<account>.dkr.ecr.us-east-1.amazonaws.com` |
| `EKS_CLUSTER_NAME` | `cloud-native-lab` |
| `KUBE_NAMESPACE` | `production` |

Create Pipeline job → SCM: Git → Script Path: `cloud-native-lab/Jenkinsfile`

### Step 8 — Autoscaling (Bonus)

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml --validate=false

kubectl autoscale deployment backend --cpu-percent=50 --min=2 --max=6 -n production
kubectl autoscale deployment frontend --cpu-percent=50 --min=2 --max=4 -n production
kubectl get hpa -n production
```

---

##  CI/CD Pipeline

Jenkins pipeline runs automatically on every push to `main` — **7 stages, 54 seconds:**

```
1. Checkout      — clone repository
2. Test          — npm install, verify server starts
3. Login to ECR  — authenticate via EC2 IAM Role (no keys)
4. Build Backend — docker build + push to ECR with BUILD_NUMBER tag
5. Build Frontend— docker build + push to ECR with BUILD_NUMBER tag
6. Deploy to EKS — kubectl set image + rollout status wait
7. Verify        — kubectl get pods/ingress confirmation
```

**Build #1 — SUCCESS **

---

##  Secrets Management

>  No credentials are hardcoded anywhere in this repository.

| Secret | Storage Method |
|---|---|
| `DATABASE_URL` | Kubernetes Secret (`app-secrets`) |
| `REDIS_URL` | Kubernetes Secret (`app-secrets`) |
| AWS credentials (Jenkins) | EC2 IAM Role — no access keys |
| ALB Controller AWS auth | IRSA (IAM Role for Service Account / OIDC) |
| Pipeline values | Jenkins Credentials Store |

---

##  Verification

```bash
kubectl get nodes                          # 2 nodes Ready
kubectl get pods -n production             # 4 pods Running
kubectl get services -n production         # 2 ClusterIP services
kubectl get ingress -n production          # ALB address assigned
kubectl get hpa -n production              # HPA active
kubectl get secrets -n production          # app-secrets Opaque 2

aws route53 list-hosted-zones              # hosted zone exists
curl http://$ALB_DNS/health                # {"status":"ok","service":"frontend"}
curl http://$ALB_DNS/api/data              # {"source":"database","data":{...}}
```

---

##  Requirements Checklist

| Requirement | Status |
|---|---|
| Route 53 hosted zone + DNS record |  |
| Application accessible via URL |  |
| EKS cluster + kubectl configured |  |
| Namespaces (`production`) |  |
| Multiple replicas (2 per service) |  |
| Frontend UI + Backend API |  |
| ALB Ingress Controller |  |
| Traffic: ALB → Ingress → Service → Pods |  |
| Two microservices deployed |  |
| PostgreSQL for persistent data |  |
| Redis for caching |  |
| Jenkins + Docker CI/CD |  |
| No hardcoded secrets |  |
| HTTPS / Autoscaling (Bonus) |  |

---

##  Cleanup

```bash
eksctl delete cluster --name cloud-native-lab --region us-east-1
aws rds delete-db-instance --db-instance-identifier cloud-native-postgres --skip-final-snapshot
aws elasticache delete-cache-cluster --cache-cluster-id cloud-native-redis
aws ecr delete-repository --repository-name cloud-native-app/backend --force
aws ecr delete-repository --repository-name cloud-native-app/frontend --force
aws ec2 terminate-instances --instance-ids <jenkins-instance-id>
```

---

##  Author

**Stephen Mensah**  
Cloud-Native Application on AWS — Student Laboratory Assignment  
`EKS` · `Jenkins` · `PostgreSQL` · `Redis` · `Route 53` · `ALB` · `ECR` · `Docker`
