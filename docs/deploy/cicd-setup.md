# M8 — CI/CD setup runbook

Wires the two GitHub Actions workflows to AWS. Assumes the M7 deployment already exists
([aws-setup.md](aws-setup.md)): the EC2 host has `/opt/bookinn` with `docker-compose.prod.yml` +
a filled `.env`, ECR repos `bookinn/backend` and `bookinn/frontend` exist, and the host pulls from
ECR via its IAM instance role.

- **`.github/workflows/ci.yml`** — runs on every PR (and on pushes to master). Backend `mvn verify`
  (Checkstyle + SpotBugs + tests + JaCoCo 90% gate) ∥ frontend (ESLint + Vitest coverage + build).
- **`.github/workflows/deploy.yml`** — runs only after CI succeeds on master. Cross-builds both
  arm64 images, pushes them to ECR tagged with the commit SHA, then SSHes to EC2 and rolls the stack.

CI authenticates to AWS with **GitHub OIDC** — GitHub mints a short-lived token per run that assumes
an IAM role. No long-lived AWS access keys are stored in the repo or in GitHub (the "leaked key =
surprise bill" threat from [ADR 0005](../adr/0005-aws-deployment-cost-lifecycle.md)).

Shell variables (same ones as the M7 runbook):

```bash
export AWS_REGION=us-east-1
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

---

## 1. Create the GitHub OIDC identity provider (one-time per account)

Skip this if you already added `token.actions.githubusercontent.com` as an IAM OIDC provider.

```bash
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

> The thumbprint is no longer security-critical (STS validates the provider's certificate against a
> trusted CA), but the API still requires the field.

## 2. Create the deploy role GitHub assumes

**Trust policy** — only *this* repo's workflows, and only on the `master` branch, may assume the
role. This is the key scoping control: it stops any other repo (or a PR from a fork) from using it.

```bash
cat > /tmp/bookinn-ci-trust.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike":   { "token.actions.githubusercontent.com:sub": "repo:Jamiedz999/bookinn:ref:refs/heads/master" }
    }
  }]
}
EOF

aws iam create-role --role-name bookinn-ci-deploy \
  --assume-role-policy-document file:///tmp/bookinn-ci-trust.json
```

**Permissions policy** — just enough to push to the two ECR repos (the SSH step needs no AWS perms;
the host does the ECR pull with its own instance role):

```bash
cat > /tmp/bookinn-ci-perms.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "ecr:GetAuthorizationToken", "Resource": "*" },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart",
        "ecr:BatchGetImage",
        "ecr:GetDownloadUrlForLayer"
      ],
      "Resource": [
        "arn:aws:ecr:${AWS_REGION}:${ACCOUNT_ID}:repository/bookinn/backend",
        "arn:aws:ecr:${AWS_REGION}:${ACCOUNT_ID}:repository/bookinn/frontend"
      ]
    }
  ]
}
EOF

aws iam put-role-policy --role-name bookinn-ci-deploy \
  --policy-name bookinn-ecr-push --policy-document file:///tmp/bookinn-ci-perms.json

# Grab the role ARN for the GitHub secret:
aws iam get-role --role-name bookinn-ci-deploy --query 'Role.Arn' --output text
```

## 3. Set the GitHub secrets

Three secrets, no AWS keys among them:

```bash
# Role ARN from step 2:
gh secret set AWS_DEPLOY_ROLE_ARN --repo Jamiedz999/bookinn \
  --body "arn:aws:iam::${ACCOUNT_ID}:role/bookinn-ci-deploy"

# EC2 public IP (or DNS) — the same host from the M7 runbook:
gh secret set EC2_HOST --repo Jamiedz999/bookinn --body "<EC2_PUBLIC_IP>"

# The private half of the EC2 key pair (contents of the .pem, not the path):
gh secret set EC2_SSH_KEY --repo Jamiedz999/bookinn < ~/.ssh/bookinn.pem
```

## 4. Verify the acceptance criteria

- [ ] **Coverage gate** — open a PR that deletes a covered test or drops a line below 90% → the
      **Backend** check goes red and blocks merge. A lint violation reds the **Frontend** check.
- [ ] **Auto-deploy** — merge to master → CI runs → Deploy runs → within a few minutes the new build
      is live on `http://<EC2_PUBLIC_IP>/`. Watch it under the repo's **Actions** tab.
- [ ] **No long-lived keys** — `gh secret list` shows only the role ARN + SSH inputs; there is no
      `AWS_ACCESS_KEY_ID` anywhere.
- [ ] **CI badge** — green badge renders at the top of the README.

## Troubleshooting

- **`Not authorized to perform sts:AssumeRoleWithWebIdentity`** — the `sub` in the trust policy must
  match exactly. Pushes to master produce `repo:Jamiedz999/bookinn:ref:refs/heads/master`. Widen to
  `repo:Jamiedz999/bookinn:*` only if you deliberately want tags/other branches to deploy.
- **Deploy never starts after a green CI** — `workflow_run` only fires for workflow files present on
  the **default branch**; both workflows must be merged to master before the trigger is live.
- **arm64 build is slow** — the backend compiles under QEMU emulation (several minutes). buildx GHA
  layer caching (already configured) keeps subsequent runs fast.
