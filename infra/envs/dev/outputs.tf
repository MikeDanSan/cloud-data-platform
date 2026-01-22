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
