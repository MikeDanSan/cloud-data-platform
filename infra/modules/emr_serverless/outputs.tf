output "application_id" {
  value = aws_emrserverless_application.spark.id
}

output "application_arn" {
  value = aws_emrserverless_application.spark.arn
}

output "job_role_arn" {
  value = aws_iam_role.emr_job_role.arn
}

output "job_role_name" {
  value = aws_iam_role.emr_job_role.name
}