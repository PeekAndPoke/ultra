# General

- ( ) write docs, have a look at these tools
    - ( ) https://squidfunk.github.io/mkdocs-material/

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

- ( ) Reset Password Flow
    - (x) create token, store in AuthRecords (with expiration time)
    - (x) send Email
  - (x) Frontend to set new password
  - (x) Api to reset the password
  - (x) Method to reset the password, revoke the token
  - ( ) Send email to user when password is reset

- ( ) Activate account
    - ( ) Send account activation email / validate email address email
    - ( ) Api to activate account
    - ( ) Frontend to activate account

- ( ) Keep track of devices
    - ( ) Store "device id" in local storage ... or just some random string
    - ( ) Send "device id" along with sign in request / sign up request
    - ( ) Send email to user when a new device is used

- ( ) Logout from certain devices ??? how ???

- ( ) Default Email templates for AuthRealm.Messaging
    - ( ) sendPasswordChangedEmail
    - ( ) sendPasswordResetEmail
    - ( ) sendAccountVerificationEmail
    - ( ) sendWelcomeEmail
  - ( ) sign in from new device

- ( ) Frontend
    - ( ) Automatically refresh the JWT token
    - ( ) On window focus, check if the JWT token is still valid, otherwise log out


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

