output "ecs_log_group_name" {
  value = aws_cloudwatch_log_group.ecs_logs.name
}

output "emr_log_group_name" {
  value = aws_cloudwatch_log_group.emr_logs.name
}

output "alarm_names" {
  value = [
    aws_cloudwatch_metric_alarm.api_5xx_errors.alarm_name,
    aws_cloudwatch_metric_alarm.dynamodb_throttle.alarm_name
  ]
}