locals {
  tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "s3" {
  source       = "../../modules/s3"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags
}

module "dynamodb" {
  source       = "../../modules/dynamodb"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags
}

module "iam" {
  source               = "../../modules/iam"
  project_name         = var.project_name
  environment          = var.environment
  tags                 = local.tags
  raw_bucket_arn       = module.s3.raw_bucket_arn
  processed_bucket_arn = module.s3.processed_bucket_arn
  jobs_table_arn       = module.dynamodb.jobs_table_arn
}
