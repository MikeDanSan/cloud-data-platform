locals {
  role_name   = "${var.project_name}-${var.environment}-backend-role"
  policy_name = "${var.project_name}-${var.environment}-backend-policy"
}

# Trust policy is compute-specific. For now we use EC2 as a placeholder.
# Later, if you deploy on ECS tasks, you'll swap to ecs-tasks.amazonaws.com.
data "aws_iam_policy_document" "assume_role" {
  statement {
    effect = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "backend" {
  name               = local.role_name
  assume_role_policy = data.aws_iam_policy_document.assume_role.json
  tags               = var.tags
}

data "aws_iam_policy_document" "backend_policy" {
  # S3 access (raw + processed)
  statement {
    effect = "Allow"
    actions = [
      "s3:ListBucket"
    ]
    resources = [
      var.raw_bucket_arn,
      var.processed_bucket_arn
    ]
  }

  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject"
    ]
    resources = [
      "${var.raw_bucket_arn}/*",
      "${var.processed_bucket_arn}/*"
    ]
  }

  # DynamoDB access (jobs table)
  statement {
    effect = "Allow"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:UpdateItem",
      "dynamodb:DeleteItem",
      "dynamodb:Query",
      "dynamodb:Scan"
    ]
    resources = [var.jobs_table_arn]
  }
}

resource "aws_iam_policy" "backend" {
  name   = local.policy_name
  policy = data.aws_iam_policy_document.backend_policy.json
  tags   = var.tags
}

resource "aws_iam_role_policy_attachment" "backend_attach" {
  role       = aws_iam_role.backend.name
  policy_arn = aws_iam_policy.backend.arn
}
