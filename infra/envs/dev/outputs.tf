output "tags" {
  value = local.tags
}

output "raw_bucket_name" {
  value = module.s3.raw_bucket_name
}

output "processed_bucket_name" {
  value = module.s3.processed_bucket_name
}

output "jobs_table_name" {
  value = module.dynamodb.jobs_table_name
}

output "backend_task_role_arn" {
  value = module.iam.task_role_arn
}

output "backend_execution_role_arn" {
  value = module.iam.execution_role_arn
}


output "ecr_repository_url" {
  value = module.ecr.repository_url
}

output "alb_dns_name" {
  value = module.ecs_backend.alb_dns_name
}

output "github_deploy_role_arn" {
  value = module.github_oidc.role_arn
}

output "waf_web_acl_name" {
  value = module.waf.web_acl_name
}

output "budget_name" {
  value = module.budget.budget_name
}
