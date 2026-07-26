# M7 — AWS deployment runbook

One-time setup to deploy BookInn to AWS: **EC2 (t4g.small / arm64)** running the production
`docker compose` stack behind nginx, with a **self-hosted MySQL container** co-located on the same
host, images stored in **ECR**, and **billing guardrails** in place. Cost/lifecycle rationale is in
[ADR 0005](../adr/0005-aws-deployment-cost-lifecycle.md); the DB is self-hosted rather than RDS per
[ADR 0006](../adr/0006-self-host-mysql-container-drop-rds.md) to take the running cost to $0.

> Secrets never enter the repo. The EC2 host pulls from ECR using an **IAM instance role** (no access
> keys on the box), and DB/JWT secrets live only in `/opt/bookinn/.env` on the host.

Shell variables used throughout (set these once in your local shell):

```bash
export AWS_REGION=us-east-1                 # N. Virginia
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export ECR=${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
export MY_IP=$(curl -s https://checkip.amazonaws.com)/32   # your current public IP, for SSH
```

---

## 0. Prerequisites (do in the AWS Console)

- [ ] Root account: enable **MFA**, then stop using root.
- [ ] Create an IAM admin user (or SSO), enable MFA, and `aws configure` locally with **its** keys.
- [ ] Never create long-lived access keys for anything else — the EC2 host uses an instance role.

## 1. Billing guardrails (do this FIRST)

The real "surprise bill" is a leaked key mining crypto, not the demo. Zero committed secrets is the
primary control; Budgets are the backstop.

Console → **Billing → Budgets → Create budget** (Budgets are only available in `us-east-1`, that's
normal):

- [ ] **Zero-spend budget** (template) — alerts the moment anything bills.
- [ ] **Monthly cost budgets** at **$1 / $5 / $10** with email alerts at 80% / 100% actual + forecast.

The auto-stop **Budget Action** needs the EC2 instance ID, so it is configured in step 6.

## 2. ECR — two image repos

```bash
aws ecr create-repository --repository-name bookinn/backend  --region "$AWS_REGION"
aws ecr create-repository --repository-name bookinn/frontend --region "$AWS_REGION"
```

## 3. Security groups (least privilege)

Find your default VPC id:

```bash
export VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true \
  --query 'Vpcs[0].VpcId' --output text --region "$AWS_REGION")
```

**EC2 SG** — public HTTP, SSH from your IP only:

```bash
export EC2_SG=$(aws ec2 create-security-group --group-name bookinn-ec2 \
  --description "BookInn EC2" --vpc-id "$VPC_ID" \
  --query GroupId --output text --region "$AWS_REGION")

aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" \
  --protocol tcp --port 80 --cidr 0.0.0.0/0 --region "$AWS_REGION"
aws ec2 authorize-security-group-ingress --group-id "$EC2_SG" \
  --protocol tcp --port 22 --cidr "$MY_IP" --region "$AWS_REGION"
```

No separate database security group is needed: the DB is a MySQL container internal to the compose
network with no host port published (ADR 0006), so nothing on the host or the internet can reach
port 3306. This is what satisfies the "DB unreachable from the internet" AC (verified in step 9).

## 4. Database — self-hosted MySQL container (no AWS provisioning)

There is no RDS instance to create. MySQL runs as the `mysql` service in
`docker-compose.prod.yml` on the same EC2 host, on the compose-internal network only, with its data
in a named volume (`bookinn-mysql-data`). It is provisioned automatically when the stack comes up in
step 8; Flyway migrations + demo seeding populate it on the backend's first boot, and a destroyed
volume self-heals the same way. Its footprint is memory-tuned (128M buffer pool, performance_schema
off) and capped by `mem_limit` so it fits alongside the JVM and nginx in 2 GB.

> Migrating from a prior RDS-based deploy? Tear the instance down so the meter stops — see
> [Teardown](#teardown-stop-the-meter).

## 5. EC2 — t4g.small (Graviton / arm64)

IAM instance role so the host can pull from ECR with **no static keys**:

```bash
aws iam create-role --role-name bookinn-ec2 \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
aws iam attach-role-policy --role-name bookinn-ec2 \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
aws iam create-instance-profile --instance-profile-name bookinn-ec2
aws iam add-role-to-instance-profile --instance-profile-name bookinn-ec2 --role-name bookinn-ec2
```

Create an SSH key pair, then launch the instance on the **arm64** Amazon Linux 2023 AMI:

```bash
aws ec2 create-key-pair --key-name bookinn --query KeyMaterial --output text \
  --region "$AWS_REGION" > ~/.ssh/bookinn.pem && chmod 400 ~/.ssh/bookinn.pem

# Latest AL2023 arm64 AMI via SSM public parameter:
export AMI=$(aws ssm get-parameters \
  --names /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64 \
  --query 'Parameters[0].Value' --output text --region "$AWS_REGION")

aws ec2 run-instances \
  --image-id "$AMI" --instance-type t4g.small \
  --key-name bookinn --security-group-ids "$EC2_SG" \
  --iam-instance-profile Name=bookinn-ec2 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=bookinn}]' \
  --region "$AWS_REGION"
```

Get the public IP and instance id:

```bash
aws ec2 describe-instances --filters Name=tag:Name,Values=bookinn Name=instance-state-name,Values=running \
  --query 'Reservations[0].Instances[0].[InstanceId,PublicIpAddress]' --output text --region "$AWS_REGION"
```

## 6. Budget Action — auto-stop EC2 at the threshold

Console → **Budgets → your $10 budget → Add action** → action type **Stop EC2 instances**, target the
`bookinn` instance id from step 5, trigger at 100% actual. This is the automated backstop from ADR 0005.

## 7. Build & push arm64 images (from your laptop)

`docker buildx` cross-builds arm64 on an amd64 laptop via QEMU (already verified locally):

```bash
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR"

docker buildx build --platform linux/arm64 -t "$ECR/bookinn/backend:latest"  --push ./backend
docker buildx build --platform linux/arm64 -t "$ECR/bookinn/frontend:latest" --push ./frontend
```

## 8. Configure & start the stack on EC2

SSH in (`ssh -i ~/.ssh/bookinn.pem ec2-user@<PUBLIC_IP>`), then on the host:

```bash
# Docker + compose plugin
sudo dnf update -y && sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user     # log out/in for this to take effect
sudo mkdir -p /usr/libexec/docker/cli-plugins && sudo curl -sSL \
  https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64 \
  -o /usr/libexec/docker/cli-plugins/docker-compose && sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose

sudo mkdir -p /opt/bookinn && sudo chown ec2-user /opt/bookinn && cd /opt/bookinn
```

Add a **2 GB swapfile** — the host now runs nginx + JVM + MySQL together in 2 GB, so swap is the
OOM safety net that lets a transient spike degrade to slow instead of killing a container (ADR 0006):

```bash
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab   # persists across reboots
free -h                                                       # confirm Swap: 2.0Gi
```

Copy the deploy files up **from your laptop** (new terminal, in the repo root — note `.env.example`
is a dotfile, so `ls -a` to see it landed):

```bash
scp -i ~/.ssh/bookinn.pem deploy/docker-compose.prod.yml deploy/.env.example \
    ec2-user@<PUBLIC_IP>:/opt/bookinn/
```

Then back on the host:

```bash
cp .env.example .env
# Fill .env: ECR_REGISTRY=$ECR; leave BOOKINN_DB_URL at jdbc:mysql://mysql:3306/bookinn (the
# internal container, not an endpoint); pick a BOOKINN_DB_PASSWORD and MYSQL_ROOT_PASSWORD (the
# mysql container is created with these on first boot); BOOKINN_JWT_SECRET=$(openssl rand -hex 32).
nano .env

# ECR login uses the instance role — no keys needed:
aws ecr get-login-password --region <REGION> | docker login --username AWS --password-stdin <ECR_REGISTRY>

docker compose -f docker-compose.prod.yml --env-file .env pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
# Boot order is staggered: mysql → healthy, then backend runs Flyway/seeding → healthy, then nginx.
docker compose -f docker-compose.prod.yml ps      # all three should read healthy/up
```

## 9. Verify the acceptance criteria

- [ ] **App usable via public IP** — open `http://<PUBLIC_IP>/`, browse listings, click "Try as host".
- [ ] **No secrets in repo** — `git grep -nEi 'password|secret|BEGIN .*PRIVATE KEY' -- . ':!*.example' ':!docs/**'` returns nothing real.
- [ ] **DB unreachable publicly** — from your laptop `nc -vz <PUBLIC_IP> 3306` must time out (the
      mysql container publishes no host port, so there is nothing listening on 3306).
- [ ] **Stack fits the 2 GB memory budget** — on the host, `docker stats --no-stream` sums to
      ~1.3 GB across the three services with headroom, and a cold boot leaves no OOM-kills:
      `dmesg | grep -i oom` and `docker events --since 5m --filter event=oom` are empty.
- [ ] **Billing alarm active** — budgets visible in Billing → Budgets; zero-spend + $1/$5/$10.

## Teardown (stop the meter)

```bash
aws ec2 terminate-instances --instance-ids <INSTANCE_ID> --region "$AWS_REGION"
```

If you are migrating off a prior RDS-based deploy, delete the now-unused instance to stop its meter
(one-time; new deploys never create it — ADR 0006):

```bash
aws rds delete-db-instance --db-instance-identifier bookinn-db --skip-final-snapshot --region "$AWS_REGION"
```

Everything is containerized, so the same `docker-compose.prod.yml` runs on Lightsail or any Docker
host later (ADR 0005 lifecycle).
