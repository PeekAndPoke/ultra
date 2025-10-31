# Funktor

## Vault

- ( ) Generalize Karango::Addons so it can be used by Monko as well
    - ( ) SoftDelete

## Auth

- ( ) Automatically refresh the JWT token
- ( ) On window focus, check if the JWT token is still valid, otherwise log out
- ( ) Reset Password Flow
    - ( ) create token, store in AuthRecords (with expiration time)
    - ( ) send Email
    - ( ) Frontend to set new password
    - ( ) Api to reset the password
    - ( ) Method to reset the password, revoke the token

- ( ) FunktorRestBuilder
    - ( ) check that the JWT signing key is "good" enough
    - ( ) cli command to generate a JWT signing key

## Messaging

- ( ) Add more email providers
    - ( ) sendgrid

## General

- ( ) remove common Cache interface in favor of cache4k https://github.com/ReactiveCircus/cache4k

- ( ) Compile testing
    - ( ) write compile tests for Monko
    - ( ) write compile tests for Mutator

- ( ) Email templates modeling
- ( ) Email templates editor

## Rest

1. Refactor + Complete or remove the Dart-Codegen

- move the codegen into its own package

## Karango

- ( ) Check and implement todos
