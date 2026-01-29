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

output "emr_application_id" {
  value = module.emr_serverless.application_id
}

output "emr_job_role_arn" {
  value = module.emr_serverless.job_role_arn
}

output "ecs_log_group_name" {
  value = module.cloudwatch_alarms.ecs_log_group_name
}

output "emr_log_group_name" {
  value = module.cloudwatch_alarms.emr_log_group_name
}

output "alarm_names" {
  value = module.cloudwatch_alarms.alarm_names
}