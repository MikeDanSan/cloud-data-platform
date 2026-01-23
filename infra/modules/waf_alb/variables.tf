variable "project_name" { type = string }
variable "environment" { type = string }
variable "tags" { type = map(string) }

variable "alb_arn" {
  description = "ARN of the ALB to protect"
  type        = string
}

# Start in COUNT mode for safety
variable "waf_mode" {
  description = "WAF mode: COUNT or BLOCK"
  type        = string
  default     = "COUNT"
}

# Requests per 5 minutes per IP before rate rule triggers
variable "rate_limit" {
  type    = number
  default = 2000
}
