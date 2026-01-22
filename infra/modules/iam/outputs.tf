output "task_role_name" {
  value = aws_iam_role.task.name
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "execution_role_name" {
  value = aws_iam_role.execution.name
}

output "execution_role_arn" {
  value = aws_iam_role.execution.arn
}

output "task_policy_arn" {
  value = aws_iam_policy.task.arn
}
