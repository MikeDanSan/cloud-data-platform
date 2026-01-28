locals {
  app_name = "${var.project_name}-${var.environment}-spark-app"
  job_role_name = "${var.project_name}-${var.environment}-emr-job-role"
}

# EMR Serverless Application
resource "aws_emrserverless_application" "spark" {
  name          = local.app_name
  release_label = "emr-7.0.0"
  type          = "Spark"

  maximum_capacity {
    cpu    = "200 vCPU"
    memory = "400 GB"
  }

  auto_start_configuration {
    enabled = true
  }

  auto_stop_configuration {
    enabled              = true
    idle_timeout_minutes = 15
  }

  tags = var.tags
}

# Force stop before destroy
resource "null_resource" "stop_emr_app" {
  triggers = {
    app_id = aws_emrserverless_application.spark.id
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      aws emr-serverless stop-application --application-id ${self.triggers.app_id} || true
      sleep 10
    EOT
  }
}

# IAM role for EMR Serverless job execution
data "aws_iam_policy_document" "emr_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["emr-serverless.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "emr_job_role" {
  name               = local.job_role_name
  assume_role_policy = data.aws_iam_policy_document.emr_assume_role.json
  tags               = var.tags
}

# Policy for EMR job to access S3 buckets
data "aws_iam_policy_document" "emr_job_policy" {
  # Read from raw bucket
  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      var.raw_bucket_arn,
      "${var.raw_bucket_arn}/*"
    ]
  }

  # Write to processed bucket
  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      var.processed_bucket_arn,
      "${var.processed_bucket_arn}/*"
    ]
  }

  # Write EMR logs to raw bucket
  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:GetObject"
    ]
    resources = [
      "${var.raw_bucket_arn}/emr-logs/*"
    ]
  }

  # CloudWatch Logs
  statement {
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]
    resources = ["*"]
  }

  # Glue Data Catalog (optional)
  statement {
    effect = "Allow"
    actions = [
      "glue:GetDatabase",
      "glue:GetTable",
      "glue:GetPartitions"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "emr_job_policy" {
  name   = "${local.job_role_name}-policy"
  policy = data.aws_iam_policy_document.emr_job_policy.json
  tags   = var.tags
}

resource "aws_iam_role_policy_attachment" "emr_job_policy" {
  role       = aws_iam_role.emr_job_role.name
  policy_arn = aws_iam_policy.emr_job_policy.arn
}