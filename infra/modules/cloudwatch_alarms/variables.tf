variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "sns_topic_arn" {
  type        = string
  default     = null
  description = "SNS topic ARN for alarm notifications (optional)"
}

variable "alb_arn_suffix" {
  type        = string
  description = "ALB ARN suffix for metrics"
}

variable "target_group_arn_suffix" {
  type        = string
  description = "Target group ARN suffix for metrics"
}

variable "dynamodb_table_name" {
  type = string
}

variable "tags" {
  type = map(string)
}