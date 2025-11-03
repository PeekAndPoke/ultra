# General

- ( ) Compile testing
    - ( ) write compile tests for Monko
    - ( ) write compile tests for Mutator

- ( ) Email templates modeling
- ( ) Email templates editor

## Ultra::Common

- ( ) write tests for FastCache maxMemorySize behavior

# Funktor

## Rest

- ( ) make slumberer cache size configurable

## Vault

- ( ) make Cursor truely async

### Karango

- ( ) Check and implement todos

### Monko

- ( ) Implement SoftDelete repo addon (see karango for reference)
- ( ) Implement all Hooks in MonkoRepository
    - ( ) onBeforeSave
        - -> in insert many
        - -> in update many
    - ( ) onAfterSave
        - -> in insert many
        - -> in update many
    - ( ) onAfterDelete

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

- ( ) Store attachments in blob-storage or different table
    - ( ) compress with password protected zip -> https://github.com/srikanth-lingala/zip4j
    - ( ) wire up with cluster::depot

- ( ) Add more email providers
    - ( ) sendgrid

## Slumber

- ( ) Add caching for slumber() keeping the last x models in memory

## Rest

- ( ) Refactor + Complete or remove the Dart-Codegen
    - ( )move the codegen into its own package

