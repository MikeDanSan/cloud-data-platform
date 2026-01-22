variable "project_name" { type = string }
variable "environment" { type = string }
variable "tags" { type = map(string) }

variable "github_repo" {
  description = "GitHub repo in OWNER/REPO format"
  type        = string
}

variable "github_ref" {
  description = "Git reference to allow (e.g., refs/heads/main)"
  type        = string
  default     = "refs/heads/main"
}

variable "aws_account_id" { type = string }
variable "aws_region" { type = string }

variable "ecr_repository_name" {
  description = "ECR repository name (not URL)"
  type        = string
}

variable "ecs_cluster_name" { type = string }
variable "ecs_service_name" { type = string }

variable "ecs_task_role_arn" { type = string }
variable "ecs_execution_role_arn" { type = string }
