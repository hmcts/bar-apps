variable "product" {}

variable "location" {
  default = "UK South"
}

variable "component" {}

variable "env" {}

variable "subscription" {}

variable "ilbIp"{}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description  = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "database_name" {
  default = "bar"
}

variable "postgresql_user" {
  default = "bar"
}

variable "postgresql_version" {
  default = "11"
}

variable "common_tags" {
  type = map(string)
}

variable "postgresql_version" {
  default = "11"
}


