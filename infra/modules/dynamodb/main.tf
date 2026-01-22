locals {
  table_name = "${var.project_name}-${var.environment}-jobs"
}

resource "aws_dynamodb_table" "jobs" {
  name         = local.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "jobId"

  attribute {
    name = "jobId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = var.tags
}
