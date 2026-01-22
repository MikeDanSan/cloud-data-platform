locals {
  role_name         = "${var.project_name}-${var.environment}-github-deploy-role"
  oidc_provider_url = "https://token.actions.githubusercontent.com"
}

# --- OIDC Provider (safe to create once; Terraform will no-op if it already exists in state) ---
resource "aws_iam_openid_connect_provider" "github" {
  url = local.oidc_provider_url

  client_id_list = [
    "sts.amazonaws.com"
  ]

  # GitHub's OIDC thumbprint can change over time. Terraform can manage it, but
  # if you want AWS to fetch it automatically you can also create this in console.
  # This value is commonly used; if AWS rejects it, use the AWS console "Get thumbprint"
  # and paste the current value.
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# Trust policy: allow ONLY this repo + main branch to assume the role
data "aws_iam_policy_document" "trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # sub format: repo:OWNER/REPO:ref:refs/heads/main
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:${var.github_ref}"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = local.role_name
  assume_role_policy = data.aws_iam_policy_document.trust.json
  tags               = var.tags
}

# Permissions policy for build/push + deploy
data "aws_iam_policy_document" "permissions" {
  # ECR push permissions (scoped to your repo)
  statement {
    effect = "Allow"
    actions = [
      "ecr:GetAuthorizationToken"
    ]
    resources = ["*"]
  }

  statement {
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:DescribeRepositories",
      "ecr:ListImages"
    ]
    resources = [
      "arn:aws:ecr:${var.aws_region}:${var.aws_account_id}:repository/${var.ecr_repository_name}"
    ]
  }

  # ECS deploy permissions (cluster/service scoped as much as possible)
  statement {
    effect = "Allow"
    actions = [
      "ecs:DescribeClusters",
      "ecs:DescribeServices",
      "ecs:DescribeTaskDefinition",
      "ecs:RegisterTaskDefinition",
      "ecs:UpdateService"
    ]
    resources = ["*"]
  }

  # Allow passing the ECS roles referenced by task definition
  statement {
    effect  = "Allow"
    actions = ["iam:PassRole"]
    resources = [
      var.ecs_task_role_arn,
      var.ecs_execution_role_arn
    ]
  }
}

resource "aws_iam_policy" "github_deploy" {
  name   = "${var.project_name}-${var.environment}-github-deploy-policy"
  policy = data.aws_iam_policy_document.permissions.json
  tags   = var.tags
}

resource "aws_iam_role_policy_attachment" "attach" {
  role       = aws_iam_role.github_deploy.name
  policy_arn = aws_iam_policy.github_deploy.arn
}
