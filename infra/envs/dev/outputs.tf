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

output "backend_role_arn" {
  value = module.iam.backend_role_arn
}
