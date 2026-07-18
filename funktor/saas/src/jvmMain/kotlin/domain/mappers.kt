package io.peekandpoke.funktor.saas.domain

import io.peekandpoke.funktor.saas.model.BranchModel
import io.peekandpoke.funktor.saas.model.OrgModel
import io.peekandpoke.ultra.vault.Storable

/** Maps a stored [Organisation] to its frontend [OrgModel]. The model id is the vault `_key`. */
suspend fun Storable<Organisation>.asApiModel(): OrgModel {
    val org = resolve()

    return OrgModel(
        id = _key,
        slug = org.slug,
        name = org.name,
        status = org.status,
        branches = org.branches.map { it.asApiModel() },
    )
}

/** Maps an embedded [Organisation.Branch] to its frontend [BranchModel]. */
fun Organisation.Branch.asApiModel(): BranchModel = BranchModel(
    id = id,
    slug = slug,
    name = name,
    status = status,
)
