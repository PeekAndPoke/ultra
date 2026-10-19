package io.peekandpoke.funktor.auth

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.value
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

abstract class AuthRecordStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorAuthBuilder.configureAuth()

    suspend fun createKontainer(): Kontainer {
        return createAuthTestContainer(
            configureKontainer = { configureKontainer() },
            configureAuth = { configureAuth() },
        )
    }

    init {
        "Creating a passwords and reloading the latest one must work" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            authRecords
                .create(AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner1"), token = "pw1"))

            // ensure they get a different timestamp
            delay(1.milliseconds)

            val owner1pw2 = authRecords
                .create(AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner1"), token = "pw2"))

            delay(2.milliseconds)

            authRecords
                .create(AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner2"), token = "pw1"))

            // ensure they get a different timestamp
            delay(1.milliseconds)

            val owner2pw2 = authRecords
                .create(AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner2"), token = "pw2"))

            withClue("Loading latest password of owner1") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm"), UserId("owner1"))

                loaded.shouldNotBeNull()
                loaded._id shouldBe owner1pw2._id
                loaded.resolve().realm shouldBe owner1pw2.resolve().realm
                loaded.resolve().ownerId shouldBe owner1pw2.resolve().ownerId
                loaded.resolve().token shouldBe owner1pw2.resolve().token
            }

            withClue("Loading latest password of owner2") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm"), UserId("owner2"))

                loaded.shouldNotBeNull()
                loaded._id shouldBe owner2pw2._id
                loaded.resolve().realm shouldBe owner2pw2.resolve().realm
                loaded.resolve().ownerId shouldBe owner2pw2.resolve().ownerId
                loaded.resolve().token shouldBe owner2pw2.resolve().token
            }

            withClue("Loading from a realm that does not have password stored") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("UNKNOWN"), UserId("owner1"))

                loaded.shouldBeNull()
            }

            withClue("Loading for an owner that does not have password stored") {
                val loaded = authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm"), UserId("UNKNOWN"))

                loaded.shouldBeNull()
            }
        }

        "Creating and loading a password reset token must work" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            val token1 = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner1"),
                    token = "token1",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            val token2 = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner2"),
                    token = "token2",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            withClue("Loading password reset token of owner1") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = RealmId("realm"), token = "token1")

                loaded.shouldNotBeNull()
                loaded._id shouldBe token1._id
                loaded.resolve().realm shouldBe token1.resolve().realm
                loaded.resolve().ownerId shouldBe token1.resolve().ownerId
                loaded.resolve().token shouldBe token1.resolve().token
                loaded.resolve().expiresAt shouldBe token1.resolve().expiresAt
            }

            withClue("Loading password reset token of owner2") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = RealmId("realm"), token = "token2")

                loaded.shouldNotBeNull()
                loaded._id shouldBe token2._id
                loaded.resolve().realm shouldBe token2.resolve().realm
                loaded.resolve().ownerId shouldBe token2.resolve().ownerId
                loaded.resolve().token shouldBe token2.resolve().token
                loaded.resolve().expiresAt shouldBe token2.resolve().expiresAt
            }
        }

        "Creating a password reset token that has already expired" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner1"),
                    token = "token1",
                    expiresAt = kronos.instantNow().minus(1.hours).toEpochSeconds(),
                )
            )

            val loaded = authRecords
                .findLatestRecordBy(AuthRecord.PasswordRecoveryToken, RealmId("realm"), UserId("owner1"))

            loaded.shouldBeNull()
        }

        "findByToken returns null for an unknown token" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            // Populate the store so the backend isn't just returning null from an empty table.
            authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner1"),
                    token = "known-token",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            val loaded = authRecords
                .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = RealmId("realm"), token = "unknown-token")

            loaded.shouldBeNull()
        }

        "findByToken scopes lookups by realm" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()
            val kronos = kontainer.get(Kronos::class)

            authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm-a"),
                    ownerId = UserId("owner1"),
                    token = "shared-token",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            withClue("Same token in a different realm should not match") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = RealmId("realm-b"), token = "shared-token")
                loaded.shouldBeNull()
            }

            withClue("Same token in the right realm should match") {
                val loaded = authRecords
                    .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = RealmId("realm-a"), token = "shared-token")
                loaded.shouldNotBeNull()
            }
        }

        "A pending-activation marker must round-trip with no token and no expiry" {
            // The shape is unique among auth records: BOTH `token` and `expiresAt` are null. And the
            // null expiry is load-bearing rather than incidental — `findLatestRecordBy` drops expired
            // records, so a marker that read back as expired would silently activate the account.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            authRecords.create(
                AuthRecord.PendingActivation(realm = RealmId("realm"), ownerId = UserId("owner1"))
            )

            val loaded = authRecords
                .findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm"), UserId("owner1"))

            loaded.shouldNotBeNull()
            loaded.resolve().ownerId shouldBe UserId("owner1")
            loaded.resolve().token.shouldBeNull()
            loaded.resolve().expiresAt.shouldBeNull()
        }

        "removeAllByOwner must remove a user's records, scoped to realm and owner" {
            // This is what "activate the account" actually does. Untested until activation needed it,
            // and a backend where it silently removed nothing would leave every activated account
            // still locked out — with the API answering success = true.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            authRecords.create(AuthRecord.PendingActivation(RealmId("realm-a"), UserId("owner1")))
            authRecords.create(AuthRecord.PendingActivation(RealmId("realm-a"), UserId("owner2")))
            authRecords.create(AuthRecord.PendingActivation(RealmId("realm-b"), UserId("owner1")))
            authRecords.create(
                AuthRecord.Password(realm = RealmId("realm-a"), ownerId = UserId("owner1"), token = "pw")
            )

            authRecords.removeAllByOwner(AuthRecord.PendingActivation, RealmId("realm-a"), UserId("owner1"))

            withClue("the target marker is gone") {
                authRecords
                    .findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm-a"), UserId("owner1"))
                    .shouldBeNull()
            }

            withClue("another owner in the same realm keeps theirs") {
                authRecords
                    .findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm-a"), UserId("owner2"))
                    .shouldNotBeNull()
            }

            withClue("the same owner in another realm keeps theirs") {
                authRecords
                    .findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm-b"), UserId("owner1"))
                    .shouldNotBeNull()
            }

            withClue("another record TYPE of the same owner is untouched") {
                authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm-a"), UserId("owner1"))
                    .shouldNotBeNull()
            }
        }

        "hasNotExpired must treat a NULL expiresAt as never expiring" {
            // THE property that makes a PendingActivation marker permanent. A marker has no expiry, and
            // every lookup filters on `hasNotExpired()` — so if null were ever read as "expired", the
            // marker would vanish from every query and an unactivated account would silently become
            // activated. Nothing else in the suite pins this.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            val marker = authRecords.create(
                AuthRecord.PendingActivation(realm = RealmId("realm"), ownerId = UserId("owner1"))
            )

            with(authRecords) {
                withClue("a record with no expiry has not expired") {
                    marker.hasNotExpired() shouldBe true
                    marker.hasExpired() shouldBe false
                }
            }
        }

        "hasExpired must be decided by the clock, on both sides of the boundary" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            val future = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner1"),
                    token = "future",
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            val past = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner2"),
                    token = "past",
                    expiresAt = kronos.instantNow().minus(1.hours).toEpochSeconds(),
                )
            )

            // Exactly `now` in whole seconds. `hasNotExpired` compares `expiresAt > secondsNow()`, so
            // the boundary second counts as EXPIRED — asserted rather than left to chance, because a
            // flipped comparison would keep a just-expired token usable for one more second.
            val boundary = authRecords.create(
                AuthRecord.PasswordRecoveryToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner3"),
                    token = "boundary",
                    expiresAt = kronos.secondsNow(),
                )
            )

            with(authRecords) {
                withClue("expiry in the future") {
                    future.hasNotExpired() shouldBe true
                    future.hasExpired() shouldBe false
                }

                withClue("expiry in the past") {
                    past.hasNotExpired() shouldBe false
                    past.hasExpired() shouldBe true
                }

                withClue("expiry exactly now counts as expired") {
                    boundary.hasExpired() shouldBe true
                }
            }
        }

        "findAllByOwner must return every live record, scoped to type, realm and owner, and drop expired ones" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            suspend fun token(realm: String, owner: String, token: String, expiresIn: Long) =
                authRecords.create(
                    AuthRecord.PasswordRecoveryToken(
                        realm = RealmId(realm),
                        ownerId = UserId(owner),
                        token = token,
                        expiresAt = kronos.instantNow().plus(expiresIn.hours).toEpochSeconds(),
                    )
                )

            token(realm = "realm-a", owner = "owner1", token = "a1", expiresIn = 1)
            token(realm = "realm-a", owner = "owner1", token = "a2", expiresIn = 1)
            token(realm = "realm-a", owner = "owner1", token = "expired", expiresIn = -1)
            token(realm = "realm-a", owner = "owner2", token = "other-owner", expiresIn = 1)
            token(realm = "realm-b", owner = "owner1", token = "other-realm", expiresIn = 1)

            authRecords.create(
                AuthRecord.PendingActivation(realm = RealmId("realm-a"), ownerId = UserId("owner1"))
            )

            val found = authRecords.findAllByOwner(
                AuthRecord.PasswordRecoveryToken, RealmId("realm-a"), UserId("owner1"),
            )

            withClue("only the live tokens of that type, realm and owner") {
                found.map { it.resolve().token }.sorted() shouldBe listOf("a1", "a2")
            }
        }

        "removeById must remove exactly one record" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            val keep = authRecords.create(
                AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner1"), token = "keep")
            )
            val drop = authRecords.create(
                AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner2"), token = "drop")
            )

            authRecords.removeById(drop._id)

            withClue("the removed record is gone") {
                authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm"), UserId("owner2"))
                    .shouldBeNull()
            }

            withClue("the other record is untouched") {
                authRecords
                    .findLatestRecordBy(AuthRecord.Password, RealmId("realm"), UserId("owner1"))
                    .shouldNotBeNull()
                    .resolve().token shouldBe keep.resolve().token
            }
        }

        "An activation-resend token must round-trip through findByToken, scoped by realm and TYPE" {
            // Written in production by every refused sign-in on a pending account, and read back by
            // `AuthRealm.resendActivation` — a backend that mangled it would make resend silently dead.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            val expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds()

            val created = authRecords.create(
                AuthRecord.ActivationResendToken(
                    realm = RealmId("realm"), ownerId = UserId("owner1"), token = "resend-1", expiresAt = expiresAt,
                )
            )
            // The SAME secret under another record type — a lookup must never cross types.
            authRecords.create(
                AuthRecord.EmailVerificationToken(
                    realm = RealmId("realm"), ownerId = UserId("owner2"), token = "verify-1", expiresAt = expiresAt,
                )
            )

            withClue("round trip") {
                val loaded = authRecords
                    .findByToken(AuthRecord.ActivationResendToken, RealmId("realm"), "resend-1")
                    .shouldNotBeNull()

                loaded._id shouldBe created._id
                loaded.value.ownerId shouldBe UserId("owner1")
                loaded.value.token shouldBe "resend-1"
                loaded.value.expiresAt shouldBe expiresAt
            }

            withClue("another realm does not match") {
                authRecords.findByToken(AuthRecord.ActivationResendToken, RealmId("other"), "resend-1").shouldBeNull()
            }

            withClue("a verification token is not a resend token, and the other way round") {
                authRecords.findByToken(AuthRecord.ActivationResendToken, RealmId("realm"), "verify-1").shouldBeNull()
                authRecords.findByToken(AuthRecord.EmailVerificationToken, RealmId("realm"), "resend-1").shouldBeNull()
            }
        }

        "An EXPIRED activation-resend token must not be found" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            authRecords.create(
                AuthRecord.ActivationResendToken(
                    realm = RealmId("realm"),
                    ownerId = UserId("owner1"),
                    token = "expired",
                    expiresAt = kronos.instantNow().minus(1.hours).toEpochSeconds(),
                )
            )

            authRecords.findByToken(AuthRecord.ActivationResendToken, RealmId("realm"), "expired").shouldBeNull()
        }

        "removeById must report 1 for the call that removed a record and 0 for every later one" {
            // Single-use under concurrency rests on THIS count: of two requests that both found a token,
            // only the one whose delete reports 1 may act on it.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            val record = authRecords.create(
                AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner1"), token = "pw")
            )

            authRecords.removeById(record._id).count shouldBe 1L
            authRecords.removeById(record._id).count shouldBe 0L
        }

        "removeAllByOwner(exceptId) must keep exactly the excepted record" {
            // What token rotation relies on: the newest link is created first, then every OTHER one
            // goes. A backend that ignored `exceptId` would delete the link it is about to mail.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            suspend fun token(owner: String, token: String) = authRecords.create(
                AuthRecord.EmailVerificationToken(
                    realm = RealmId("realm"),
                    ownerId = UserId(owner),
                    token = token,
                    expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds(),
                )
            )

            token("owner1", "old-1")
            token("owner1", "old-2")
            val keep = token("owner1", "newest")
            token("owner2", "someone-else")

            val removed = authRecords.removeAllByOwner(
                AuthRecord.EmailVerificationToken, RealmId("realm"), UserId("owner1"), exceptId = keep._id,
            )

            removed.count shouldBe 2L

            authRecords.findAllByOwner(AuthRecord.EmailVerificationToken, RealmId("realm"), UserId("owner1"))
                .map { it._id } shouldBe listOf(keep._id)

            authRecords.findByToken(AuthRecord.EmailVerificationToken, RealmId("realm"), "someone-else")
                .shouldNotBeNull()
        }

        "A pending-activation marker's resend schedule must survive save and reload" {
            // The growing resend delay lives on the marker, so a backend that dropped these fields on
            // save would reset every account to the first, shortest delay — an unbounded faucet again.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            val created = authRecords.create(
                AuthRecord.PendingActivation(realm = RealmId("realm"), ownerId = UserId("owner1"))
            )

            created.value.resends shouldBe 0
            created.value.lastSentAt.shouldBeNull()

            val sentAt = MpInstant.parse("2026-10-19T12:34:56Z")

            authRecords.save(created.modify { it.copy(resends = 3, lastSentAt = sentAt) })

            val loaded = authRecords
                .findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm"), UserId("owner1"))
                .shouldNotBeNull()

            loaded._id shouldBe created._id
            loaded.value.resends shouldBe 3
            loaded.value.lastSentAt shouldBe sentAt

            withClue("save REPLACES — it must not add a second marker") {
                authRecords.findAllByOwner(AuthRecord.PendingActivation, RealmId("realm"), UserId("owner1"))
                    .size shouldBe 1
            }
        }

        "removeAllOfOwner must remove every record TYPE of one owner in one realm, except the one kept" {
            // What retiring an account does first: credentials and tokens go, the marker stays until last.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            val expiresAt = kronos.instantNow().plus(1.hours).toEpochSeconds()
            val realm = RealmId("realm-a")
            val owner = UserId("owner1")

            authRecords.create(AuthRecord.Password(realm = realm, ownerId = owner, token = "pw"))
            authRecords.create(AuthRecord.EmailVerificationToken(realm, owner, expiresAt, token = "v"))
            authRecords.create(AuthRecord.ActivationResendToken(realm, owner, expiresAt, token = "r"))
            authRecords.create(AuthRecord.PasswordRecoveryToken(realm, owner, expiresAt, token = "p"))
            authRecords.create(AuthRecord.PendingActivation(realm = realm, ownerId = owner))
            // Untouchable neighbours:
            authRecords.create(AuthRecord.Password(realm = realm, ownerId = UserId("owner2"), token = "pw2"))
            authRecords.create(AuthRecord.Password(realm = RealmId("realm-b"), ownerId = owner, token = "pw3"))

            authRecords.removeAllOfOwner(realm, owner, except = AuthRecord.PendingActivation).count shouldBe 4L

            authRecords.findLatestRecordBy(AuthRecord.Password, realm, owner).shouldBeNull()
            authRecords.findLatestRecordBy(AuthRecord.EmailVerificationToken, realm, owner).shouldBeNull()
            authRecords.findLatestRecordBy(AuthRecord.ActivationResendToken, realm, owner).shouldBeNull()
            authRecords.findLatestRecordBy(AuthRecord.PasswordRecoveryToken, realm, owner).shouldBeNull()

            withClue("the excepted type stays") {
                authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realm, owner).shouldNotBeNull()
            }
            withClue("another owner in the same realm is untouched") {
                authRecords.findLatestRecordBy(AuthRecord.Password, realm, UserId("owner2")).shouldNotBeNull()
            }
            withClue("the same owner in another realm is untouched") {
                authRecords.findLatestRecordBy(AuthRecord.Password, RealmId("realm-b"), owner).shouldNotBeNull()
            }

            withClue("without an exception, the rest goes too") {
                authRecords.removeAllOfOwner(realm, owner).count shouldBe 1L
                authRecords.findLatestRecordBy(AuthRecord.PendingActivation, realm, owner).shouldBeNull()
            }
        }

        "findCreatedBefore must return only that type in that realm created before the cutoff, oldest first" {
            // What the expiry sweep reads. A backend that ignored the cutoff would retire FRESH sign-ups.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            val first = authRecords.create(AuthRecord.PendingActivation(RealmId("realm"), UserId("owner1")))
            delay(5.milliseconds)
            val second = authRecords.create(AuthRecord.PendingActivation(RealmId("realm"), UserId("owner2")))
            authRecords.create(AuthRecord.PendingActivation(RealmId("other-realm"), UserId("owner3")))
            authRecords.create(AuthRecord.Password(realm = RealmId("realm"), ownerId = UserId("owner4"), token = "pw"))
            delay(5.milliseconds)

            val cutoff = kronos.instantNow()
            delay(5.milliseconds)

            authRecords.create(AuthRecord.PendingActivation(RealmId("realm"), UserId("fresh")))

            authRecords.findCreatedBefore(AuthRecord.PendingActivation, RealmId("realm"), cutoff, limit = 10)
                .map { it._id } shouldBe listOf(first._id, second._id)

            withClue("the limit takes the OLDEST") {
                authRecords.findCreatedBefore(AuthRecord.PendingActivation, RealmId("realm"), cutoff, limit = 1)
                    .map { it._id } shouldBe listOf(first._id)
            }

            withClue("`after` pages on: strictly newer than the given instant") {
                authRecords.findCreatedBefore(
                    AuthRecord.PendingActivation, RealmId("realm"), cutoff, after = first.value.createdAt, limit = 10,
                ).map { it._id } shouldBe listOf(second._id)
            }
        }

        "replaceIfExists must replace a live record, and must NEVER bring a deleted one back" {
            // The resend schedule's write. A save (upsert) here re-inserted a marker that a concurrent
            // activation had just removed — the active account read pending again and the sweep retired it.
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            authRecords.adapter.removeAll()

            val marker = authRecords.create(AuthRecord.PendingActivation(RealmId("realm"), UserId("owner1")))

            withClue("a live record is replaced in place") {
                val replaced = authRecords.replaceIfExists(marker.modify { it.copy(resends = 2) }).shouldNotBeNull()
                replaced._id shouldBe marker._id

                val loaded = authRecords.findLatestRecordBy(AuthRecord.PendingActivation, RealmId("realm"), UserId("owner1"))
                    .shouldNotBeNull()
                loaded._id shouldBe marker._id
                loaded.value.resends shouldBe 2
                loaded.value.createdAt.toEpochMillis() shouldBe marker.value.createdAt.toEpochMillis()
            }

            authRecords.removeById(marker._id)

            withClue("a deleted record stays deleted") {
                authRecords.replaceIfExists(marker.modify { it.copy(resends = 3) }).shouldBeNull()
                authRecords.findAllByOwner(AuthRecord.PendingActivation, RealmId("realm"), UserId("owner1")) shouldBe
                        emptyList()
            }
        }

        "A sign-up cancel token must round-trip through findByToken, scoped by type, and expire with the account" {
            val kontainer = createKontainer()
            val authRecords = kontainer.funktorAuth.deps.storage.authRecords
            val kronos = kontainer.get(Kronos::class)
            authRecords.adapter.removeAll()

            val expiresAt = kronos.instantNow().plus(150.hours).toEpochSeconds()

            authRecords.create(
                AuthRecord.SignUpCancelToken(RealmId("realm"), UserId("owner1"), expiresAt = expiresAt, token = "cancel-1")
            )
            authRecords.create(
                AuthRecord.SignUpCancelToken(
                    RealmId("realm"), UserId("owner2"),
                    expiresAt = kronos.instantNow().minus(1.hours).toEpochSeconds(), token = "cancel-old",
                )
            )

            val loaded = authRecords.findByToken(AuthRecord.SignUpCancelToken, RealmId("realm"), "cancel-1")
                .shouldNotBeNull()
            loaded.value.ownerId shouldBe UserId("owner1")
            loaded.value.expiresAt shouldBe expiresAt

            authRecords.findLatestRecordBy(AuthRecord.SignUpCancelToken, RealmId("realm"), UserId("owner1"))
                .shouldNotBeNull()
            authRecords.findByToken(AuthRecord.EmailVerificationToken, RealmId("realm"), "cancel-1").shouldBeNull()
            authRecords.findByToken(AuthRecord.SignUpCancelToken, RealmId("realm"), "cancel-old").shouldBeNull()
        }
    }
}
