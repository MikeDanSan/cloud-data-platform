locals {
  tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

module "s3" {
  source                 = "../../modules/s3"
  project_name           = var.project_name
  environment            = var.environment
  force_destroy          = true
  raw_expire_days        = 14
  processed_expire_days  = 30
  noncurrent_expire_days = 7
  tags                   = local.tags

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
  emr_job_role_arn     = module.emr_serverless.job_role_arn
}

module "emr_serverless" {
  source               = "../../modules/emr_serverless"
  project_name         = var.project_name
  environment          = var.environment
  tags                 = local.tags
  raw_bucket_arn       = module.s3.raw_bucket_arn
  processed_bucket_arn = module.s3.processed_bucket_arn
}

module "ecr" {
  source       = "../../modules/ecr"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags
}

module "ecs_backend" {
  source = "../../modules/ecs_fargate_alb"

  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags
  aws_region   = var.aws_region

  ecr_repository_url = module.ecr.repository_url
  task_role_arn      = module.iam.task_role_arn
  execution_role_arn = module.iam.execution_role_arn

  raw_bucket_name       = module.s3.raw_bucket_name
  processed_bucket_name = module.s3.processed_bucket_name
  jobs_table_name       = module.dynamodb.jobs_table_name

  emr_application_id = module.emr_serverless.application_id
  emr_job_role_arn   = module.emr_serverless.job_role_arn

  domain_name    = "api.cloudpipes.net"
  hosted_zone_id = "Z0390926EZVT7BYZ3CY6"
}

module "github_oidc" {
  source       = "../../modules/github_oidc_deploy_role"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags

  github_repo = "MikeDanSan/cloud-data-platform"
  github_ref  = "refs/heads/main"

  aws_account_id = "615340784692"
  aws_region     = var.aws_region

  ecr_repository_name = module.ecr.repository_name

  ecs_cluster_name = "cloud-data-platform-dev-cluster"
  ecs_service_name = "cloud-data-platform-dev-backend-service"

  ecs_task_role_arn      = module.iam.task_role_arn
  ecs_execution_role_arn = module.iam.execution_role_arn
}

module "waf" {
  source       = "../../modules/waf_alb"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags

  alb_arn    = module.ecs_backend.alb_arn
  waf_mode   = "COUNT"
  rate_limit = 2000
}

module "budget" {
  source       = "../../modules/budget"
  project_name = var.project_name
  environment  = var.environment
  tags         = local.tags

  alert_email        = var.alert_email
  monthly_budget_usd = var.monthly_budget_usd
}
