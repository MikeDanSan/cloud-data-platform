locals {
  name_prefix = "${var.project_name}-${var.environment}"
  waf_name    = "${local.name_prefix}-waf"
  mode_count  = upper(var.waf_mode) == "COUNT"
}

resource "aws_wafv2_web_acl" "this" {
  name  = local.waf_name
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${local.waf_name}-metrics"
    sampled_requests_enabled   = true
  }

  # AWS Managed Common Rule Set
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 10

    override_action {
      dynamic "count" {
        for_each = local.mode_count ? [1] : []
        content {}
      }
      dynamic "none" {
        for_each = local.mode_count ? [] : [1]
        content {}
      }
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "common-rules"
      sampled_requests_enabled   = true
    }
  }

  # AWS Managed Known Bad Inputs
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 20

    override_action {
      dynamic "count" {
        for_each = local.mode_count ? [1] : []
        content {}
      }
      dynamic "none" {
        for_each = local.mode_count ? [] : [1]
        content {}
      }
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "known-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  # AWS Managed Amazon IP Reputation
  rule {
    name     = "AWSManagedRulesAmazonIpReputationList"
    priority = 30

    override_action {
      dynamic "count" {
        for_each = local.mode_count ? [1] : []
        content {}
      }
      dynamic "none" {
        for_each = local.mode_count ? [] : [1]
        content {}
      }
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "ip-reputation"
      sampled_requests_enabled   = true
    }
  }

  # Rate limit (per IP, 5-minute window)
  rule {
    name     = "RateLimitPerIP"
    priority = 40

    action {
      dynamic "count" {
        for_each = local.mode_count ? [1] : []
        content {}
      }
      dynamic "block" {
        for_each = local.mode_count ? [] : [1]
        content {}
      }
    }

    statement {
      rate_based_statement {
        limit              = var.rate_limit
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "rate-limit"
      sampled_requests_enabled   = true
    }
  }

  tags = var.tags
}

resource "aws_wafv2_web_acl_association" "alb" {
  resource_arn = var.alb_arn
  web_acl_arn  = aws_wafv2_web_acl.this.arn
}
