package io.peekandpoke.funktor.saas.api

import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.domain.Slugs
import io.peekandpoke.funktor.saas.domain.asApiModel
import io.peekandpoke.funktor.saas.funktorSaas
import io.peekandpoke.funktor.saas.model.BranchModel
import io.peekandpoke.funktor.saas.model.OrgModel
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.Stored

/** Super-user CRUD endpoints for organisations. */
class OrgsApi : ApiRoutes("orgs", authFloor = { isSuperUser() }) {

    // The organisation is resolved by the entity-binding param converter: `{id}` loads the
    // `Stored<Organisation>` before the handler runs, 404-ing a missing id at the binding.
    //
    // NOTE: this requires a saas storage backend (`funktorSaas { useKarango()/useMonko() }`). Without
    // one there is no Organisation repository, so `Stored<Organisation>` cannot convert and this route
    // 500s (a degenerate misconfiguration — writes already fail loudly). A self-validating module
    // builder will turn this into an actionable boot failure — see
    // `.claude/future-plans/funktor-module-config-builder.md`.
    data class OrgParam(val id: Stored<Organisation>)

    val list = OrgsApiClient.List.mount {
        docs {
            name = "List organisations"
        }.codeGen {
            funcName = "list"
        }.handle {
            ApiResponse.ok(funktorSaas.findAll().map { it.asApiModel() })
        }
    }

    val get = OrgsApiClient.Get.mount(OrgParam::class) {
        docs {
            name = "Get organisation"
        }.codeGen {
            funcName = "get"
        }.handle { params ->
            ApiResponse.ok(params.id.asApiModel())
        }
    }

    val create = OrgsApiClient.Create.mount {
        docs {
            name = "Create organisation"
        }.codeGen {
            funcName = "create"
        }.handle { body ->
            val slug = Slugs.normalize(body.slug)
            val slugError = Slugs.validationError(slug)
            val branchError = validateBranches(body.branches)

            when {
                slugError != null ->
                    ApiResponse.badRequest<OrgModel>().withError("slug $slugError")

                branchError != null ->
                    ApiResponse.badRequest<OrgModel>().withError(branchError)

                funktorSaas.findBySlug(slug) != null ->
                    ApiResponse.conflict<OrgModel>().withError("an organisation with slug '$slug' already exists")

                else -> {
                    val created = funktorSaas.create(
                        Organisation(
                            slug = slug,
                            name = body.name.trim(),
                            status = body.status,
                            branches = body.branches.map { it.toDomain() },
                        )
                    )

                    ApiResponse.ok(created.asApiModel())
                }
            }
        }
    }

    val update = OrgsApiClient.Update.mount(OrgParam::class) {
        docs {
            name = "Update organisation"
        }.codeGen {
            funcName = "update"
        }.handle { params, body ->
            val branchError = validateBranches(body.branches)

            when {
                branchError != null ->
                    ApiResponse.badRequest<OrgModel>().withError(branchError)

                else -> {
                    val updated = funktorSaas.save(
                        params.id.modify { org ->
                            org.copy(
                                name = body.name.trim(),
                                status = body.status,
                                branches = body.branches.map { it.toDomain() },
                            )
                        }
                    )

                    ApiResponse.ok(updated.asApiModel())
                }
            }
        }
    }

    /** Returns an error message if the branch list is invalid, or `null` when it is valid. */
    private fun validateBranches(branches: List<BranchModel>): String? {
        if (branches.any { it.id.isBlank() }) return "branch id must not be blank"

        for (branch in branches) {
            val slugError = Slugs.validationError(Slugs.normalize(branch.slug))
            if (slugError != null) return "branch slug '${branch.slug}' $slugError"
        }

        val ids = branches.map { it.id }
        if (ids.size != ids.toSet().size) return "branch ids must be unique within an organisation"

        return null
    }

    private fun BranchModel.toDomain() = Organisation.Branch(
        id = id,
        slug = Slugs.normalize(slug),
        name = name.trim(),
        status = status,
    )
}
