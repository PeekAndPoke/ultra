In this file i will collect observations i saw during reviewing the code.

# Missing imports for referenced types in docs

Problem:

For example in AuthError.kt the docs reference [AuthSignInResponse.ActivationRequired] but AuthSignInResponse was not
imported. This leads to an IDE warning

Solution:

Always import the referenced types. If the referenced type cannot be imported, since it lives in module not available to
the code, DO NOT write the type into brackets or leave the reference out completely.

# AuthRealm KnownRole and getKnownRoles

Problem:

What are these needed for? Ok i see they are used for the ApiAccessDescriptor.collectKnownRoles () ... and here we
duplicate the code of the known roles again.

Suggestion:

We should move the KnownRole class somewhere more central. Where? The getKnownRoles () should return an empty list by
default. ApiAccessDescriptor.collectKnownRoles () should always append SuperUser and Anonymous to the list it gets.

KnownRole should have two static (companion properties):
.superUser = KnownRole ("SuperUser", UserPermissions (isSuperUser = true))
.anonymous = KnownRole ("Anonymous", UserPermissions ())
...which we will reuse.

Ok and each realm has to override the getKnownRoles, correct? This means whenever a roles is added, someone needs to
remember to adjust this function. Would be better to have some data structure that defines all roles for a realm, and is
passed into the realm... not sure exactly, we need to discuss.

# AuthRecordStorage comments and test coverage

Problem:

AuthRecordStorage seems to not have a test for hasExpired (). Also not clear if the other functions are well tested.

Solution:

Test all the methods, for each db backend.

# Missing comments

Problem:

The following files are missing comments and description of purpose:

- AuthSystemAppHooks.kt

Solution:

Add proper comments, precise and concise.

# OrgPolicy and SignupOrgBehavior placement

Problem:

OrgPolicy and SignupOrgBehavior currently lives in the auth module, while they are related to SaaS.

Proposal:

Check if they can easily be moved to the saas module without the need to introduce more plumbing.

# SessionJwtClaims

Questions:

This seems to be stubs for future features, like revoking tokens, correct? But there also is a SessionStore in place
already.

Do we have or plan to have a list of devices a user is logged into? How do we identify the device and send this info to
the backend? How do we persist the session id on the frontend, so we do not create an ever growing list of tokens,
whenever the user logs in. Do we plan to give the user or admins or superusers the chance to revoke individual tokens or
all tokens of a user or an entire org? Do we auto-prune expired tokens from the SessionStorage?




