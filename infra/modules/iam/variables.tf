variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "tags" {
  type = map(string)
}

variable "raw_bucket_arn" {
  type = string
}

variable "processed_bucket_arn" {
  type = string
}

variable "jobs_table_arn" {
  type = string
}
