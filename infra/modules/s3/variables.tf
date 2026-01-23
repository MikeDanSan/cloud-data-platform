variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "tags" {
  type = map(string)
}

variable "raw_expire_days" {
  type    = number
  default = 14
}

variable "processed_expire_days" {
  type    = number
  default = 30
}

variable "noncurrent_expire_days" {
  type    = number
  default = 7
}
