locals {
  task_role_name   = "${var.project_name}-${var.environment}-backend-task-role"
  exec_role_name   = "${var.project_name}-${var.environment}-backend-exec-role"
  task_policy_name = "${var.project_name}-${var.environment}-backend-task-policy"
}

data "aws_iam_policy_document" "assume_ecs_tasks" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task" {
  name               = local.task_role_name
  assume_role_policy = data.aws_iam_policy_document.assume_ecs_tasks.json
  tags               = var.tags
}

resource "aws_iam_role" "execution" {
  name               = local.exec_role_name
  assume_role_policy = data.aws_iam_policy_document.assume_ecs_tasks.json
  tags               = var.tags
}

data "aws_iam_policy_document" "task_policy" {
  # S3 bucket list permissions
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

  # S3 object permissions
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

  # DynamoDB permissions
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

resource "aws_iam_policy" "task" {
  name   = local.task_policy_name
  policy = data.aws_iam_policy_document.task_policy.json
  tags   = var.tags
}

resource "aws_iam_role_policy_attachment" "task_attach" {
  role       = aws_iam_role.task.name
  policy_arn = aws_iam_policy.task.arn
}

resource "aws_iam_role_policy_attachment" "execution_attach" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
