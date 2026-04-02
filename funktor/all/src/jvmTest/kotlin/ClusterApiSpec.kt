package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.cluster.FunktorClusterApiFeature
import io.peekandpoke.funktor.cluster.backgroundjobs.api.BackgroundJobsApi
import io.peekandpoke.funktor.cluster.depot.api.DepotApi
import io.peekandpoke.funktor.cluster.workers.api.WorkersApi
import io.peekandpoke.funktor.rest.QueryParams

class ClusterApiSpec : FunktorApiSpec() {

    private val api by service(FunktorClusterApiFeature::class)

    init {
        // GlobalLocksApi

        api.globalLocks.listServerBeacons { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.globalLocks.listGlobalLocks { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        // BackgroundJobsApi

        api.backgroundJobs.listQueued { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(BackgroundJobsApi.PagingParam()) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(BackgroundJobsApi.PagingParam()) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.backgroundJobs.getQueued { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(BackgroundJobsApi.JobIdParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(BackgroundJobsApi.JobIdParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        api.backgroundJobs.listArchived { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(BackgroundJobsApi.PagingParam()) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(BackgroundJobsApi.PagingParam()) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.backgroundJobs.getArchived { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(BackgroundJobsApi.JobIdParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(BackgroundJobsApi.JobIdParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        // VaultApi

        api.vault.listRepositories { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        // WorkersApi

        api.workers.list { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.workers.get { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(WorkersApi.WorkerParam(worker = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(WorkersApi.WorkerParam(worker = "non-existent")) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        // DepotApi

        api.depot.listRepositories { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.depot.browse { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(DepotApi.BrowseParam(repo = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent repo must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(DepotApi.BrowseParam(repo = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        // RandomDataStorageApi

        api.storageRandomData.list { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(QueryParams.List()) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(QueryParams.List()) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.storageRandomData.get { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(QueryParams.GetById(id = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(QueryParams.GetById(id = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }

        // RandomCacheStorageApi

        api.storageRandomCache.list { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(QueryParams.List()) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(QueryParams.List()) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.storageRandomCache.get { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(QueryParams.GetById(id = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(QueryParams.GetById(id = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }
    }
}
