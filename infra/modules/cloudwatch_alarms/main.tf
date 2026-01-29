locals {
  alarm_actions = var.sns_topic_arn != null ? [var.sns_topic_arn] : []
}

# Alarm: API 5xx errors
resource "aws_cloudwatch_metric_alarm" "api_5xx_errors" {
  alarm_name          = "${var.project_name}-${var.environment}-api-5xx-errors"
  alarm_description   = "Alert when API returns 5xx errors"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.target_group_arn_suffix
  }

  alarm_actions = local.alarm_actions
  tags          = var.tags
}

# Alarm: DynamoDB throttling
resource "aws_cloudwatch_metric_alarm" "dynamodb_throttle" {
  alarm_name          = "${var.project_name}-${var.environment}-dynamodb-throttle"
  alarm_description   = "Alert when DynamoDB is throttled"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  metric_name         = "UserErrors"
  namespace           = "AWS/DynamoDB"
  period              = 60
  statistic           = "Sum"
  threshold           = 1
  treat_missing_data  = "notBreaching"

  dimensions = {
    TableName = var.dynamodb_table_name
  }

  alarm_actions = local.alarm_actions
  tags          = var.tags
}

# CloudWatch Log Group for ECS
resource "aws_cloudwatch_log_group" "ecs_logs" {
  name              = "/ecs/${var.project_name}-${var.environment}-backend-service"
  retention_in_days = 7

  tags = var.tags
}

# CloudWatch Log Group for EMR
resource "aws_cloudwatch_log_group" "emr_logs" {
  name              = "/aws/emr-serverless/jobs"
  retention_in_days = 7

  tags = var.tags
}