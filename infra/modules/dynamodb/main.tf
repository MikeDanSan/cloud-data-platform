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

  attribute {
    name = "jobPartition"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "jobPartition-createdAt-index"
    hash_key        = "jobPartition"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = var.tags
}