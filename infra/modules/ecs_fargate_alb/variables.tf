variable "project_name" { type = string }
variable "environment" { type = string }
variable "tags" { type = map(string) }

variable "aws_region" { type = string }

variable "ecr_repository_url" {
  type = string
}

variable "domain_name" {
  description = "Full domain name for the API (e.g. api.cloudpipes.net)"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID for the domain"
  type        = string
}

variable "task_role_arn" {
  type = string
}

variable "execution_role_arn" {
  type = string
}

variable "raw_bucket_name" {
  type = string
}

variable "processed_bucket_name" {
  type = string
}

variable "jobs_table_name" {
  type = string
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "cpu" {
  type    = number
  default = 256
}

variable "memory" {
  type    = number
  default = 512
}
