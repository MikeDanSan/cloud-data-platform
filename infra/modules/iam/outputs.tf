output "backend_role_name" {
  value = aws_iam_role.backend.name
}

output "backend_role_arn" {
  value = aws_iam_role.backend.arn
}

output "backend_policy_arn" {
  value = aws_iam_policy.backend.arn
}
