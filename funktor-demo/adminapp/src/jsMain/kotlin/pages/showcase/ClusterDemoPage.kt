package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.DepotRepoInfo
import io.peekandpoke.funktor.demo.common.showcase.JobInfo
import io.peekandpoke.funktor.demo.common.showcase.LockAcquireRequest
import io.peekandpoke.funktor.demo.common.showcase.LockInfo
import io.peekandpoke.funktor.demo.common.showcase.QueueJobRequest
import io.peekandpoke.funktor.demo.common.showcase.StorageEntry
import io.peekandpoke.funktor.demo.common.showcase.StorageSaveRequest
import io.peekandpoke.funktor.demo.common.showcase.WorkerInfo
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.w3c.dom.HTMLInputElement

@Suppress("FunctionName")
fun Tag.ClusterDemoPage() = comp {
    ClusterDemoPage(it)
}

class ClusterDemoPage(ctx: NoProps) : PureComponent(ctx) {

    // Background Jobs
    private var queuedJobs: List<JobInfo> by value(emptyList())
    private var archivedJobs: List<JobInfo> by value(emptyList())
    private var jobText: String by value("Demo task")

    // Depot
    private var depotRepos: List<DepotRepoInfo> by value(emptyList())

    // Storage
    private var storageEntries: List<StorageEntry> by value(emptyList())
    private var storageCategory: String by value("demo")
    private var storageKey: String by value("item-1")
    private var storageValue: String by value("hello world")

    // Locks
    private var activeLocks: List<LockInfo> by value(emptyList())

    // Workers
    private var workers: List<WorkerInfo> by value(emptyList())

    init {
        refreshAll()
    }

    private fun refreshAll() {
        launch { queuedJobs = Apis.showcase.getQueuedJobs().first().data ?: emptyList() }
        launch { archivedJobs = Apis.showcase.getArchivedJobs().first().data ?: emptyList() }
        launch { depotRepos = Apis.showcase.getDepotRepos().first().data ?: emptyList() }
        launch { storageEntries = Apis.showcase.getStorageEntries().first().data ?: emptyList() }
        launch { activeLocks = Apis.showcase.getActiveLocks().first().data ?: emptyList() }
        launch { workers = Apis.showcase.getWorkers().first().data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"Cluster Features" }
            ui.dividing.header H3 { +"Background jobs, file depot, storage, locks, and workers" }
        }

        renderBackgroundJobs()
        renderDepot()
        renderStorage()
        renderLocks()
        renderWorkers()
    }

    private fun FlowContent.renderBackgroundJobs() {
        ui.segment {
            ui.header H2 { icon.tasks(); noui.content { +"Background Jobs" } }

            ui.form {
                noui.field {
                    ui.labeled.input {
                        ui.label { +"Job text" }
                        input(type = InputType.text) {
                            value = jobText
                            onChange { jobText = (it.target as HTMLInputElement).value }
                        }
                    }
                }
            }

            ui.buttons {
                ui.green.button {
                    onClick {
                        launch {
                            Apis.showcase.queueJob(QueueJobRequest(text = jobText)).first()
                            launch { queuedJobs = Apis.showcase.getQueuedJobs().first().data ?: emptyList() }
                        }
                    }
                    icon.play(); +"Queue Job"
                }
                ui.red.button {
                    onClick {
                        launch {
                            Apis.showcase.queueJob(QueueJobRequest(text = jobText, shouldFail = true)).first()
                            launch { queuedJobs = Apis.showcase.getQueuedJobs().first().data ?: emptyList() }
                        }
                    }
                    icon.bug(); +"Queue Failing Job"
                }
                ui.button {
                    onClick {
                        launch {
                            queuedJobs = Apis.showcase.getQueuedJobs().first().data ?: emptyList(); archivedJobs =
                            Apis.showcase.getArchivedJobs().first().data ?: emptyList()
                        }
                    }
                    icon.redo(); +"Refresh"
                }
            }

            if (queuedJobs.isNotEmpty()) {
                ui.header H4 { +"Queued (${queuedJobs.size})" }
                renderJobTable(queuedJobs)
            }

            if (archivedJobs.isNotEmpty()) {
                ui.header H4 { +"Archived (${archivedJobs.size})" }
                renderJobTable(archivedJobs)
            }
        }
    }

    private fun FlowContent.renderJobTable(jobs: List<JobInfo>) {
        ui.small.striped.table Table {
            thead { tr { th { +"ID" }; th { +"Type" }; th { +"Status" }; th { +"Created" } } }
            tbody {
                jobs.forEach { job ->
                    tr {
                        td { +job.id.take(12) }
                        td { +job.jobType }
                        td {
                            when (job.status) {
                                "queued" -> {
                                    ui.blue.label { +job.status }
                                }

                                "succeeded" -> {
                                    ui.green.label { +job.status }
                                }

                                else -> {
                                    ui.red.label { +job.status }
                                }
                            }
                        }
                        td { +job.createdAt }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderDepot() {
        ui.segment {
            ui.header H2 { icon.folder_open(); noui.content { +"File Depot" } }

            if (depotRepos.isEmpty()) {
                ui.message { +"No depot repositories configured" }
            } else {
                ui.striped.table Table {
                    thead { tr { th { +"Name" }; th { +"Type" }; th { +"Location" } } }
                    tbody {
                        depotRepos.forEach { repo ->
                            tr {
                                td { +repo.name }
                                td { ui.label { +repo.type } }
                                td { +repo.location }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderStorage() {
        ui.segment {
            ui.header H2 { icon.database(); noui.content { +"Random Data Storage" } }

            ui.form {
                noui.three.fields {
                    noui.field {
                        ui.labeled.input {
                            ui.label { +"Category" }
                            input(type = InputType.text) {
                                value = storageCategory
                                onChange { storageCategory = (it.target as HTMLInputElement).value }
                            }
                        }
                    }
                    noui.field {
                        ui.labeled.input {
                            ui.label { +"Key" }
                            input(type = InputType.text) {
                                value = storageKey
                                onChange { storageKey = (it.target as HTMLInputElement).value }
                            }
                        }
                    }
                    noui.field {
                        ui.labeled.input {
                            ui.label { +"Value" }
                            input(type = InputType.text) {
                                value = storageValue
                                onChange { storageValue = (it.target as HTMLInputElement).value }
                            }
                        }
                    }
                }
            }

            ui.buttons {
                ui.green.button {
                    onClick {
                        launch {
                            Apis.showcase.saveStorageEntry(
                                StorageSaveRequest(
                                    storageCategory,
                                    storageKey,
                                    storageValue
                                )
                            ).first()
                            storageEntries = Apis.showcase.getStorageEntries().first().data ?: emptyList()
                        }
                    }
                    icon.save(); +"Save"
                }
                ui.button {
                    onClick {
                        launch {
                            storageEntries = Apis.showcase.getStorageEntries().first().data ?: emptyList()
                        }
                    }
                    icon.redo(); +"Refresh"
                }
            }

            if (storageEntries.isNotEmpty()) {
                ui.small.striped.table Table {
                    thead { tr { th { +"ID" }; th { +"Category" }; th { +"Data ID" }; th { +"Updated" } } }
                    tbody {
                        storageEntries.forEach { entry ->
                            tr {
                                td { +entry.id.take(12) }
                                td { +entry.category }
                                td { +entry.dataId }
                                td { +entry.updatedAt }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderLocks() {
        ui.segment {
            ui.header H2 { icon.lock(); noui.content { +"Distributed Locks" } }

            ui.buttons {
                ui.orange.button {
                    onClick {
                        launch {
                            Apis.showcase.acquireLock(LockAcquireRequest(key = "demo-lock", holdForMs = 3000)).first()
                            activeLocks = Apis.showcase.getActiveLocks().first().data ?: emptyList()
                        }
                    }
                    icon.lock(); +"Acquire Lock (3s)"
                }
                ui.button {
                    onClick { launch { activeLocks = Apis.showcase.getActiveLocks().first().data ?: emptyList() } }
                    icon.redo(); +"Refresh"
                }
            }

            if (activeLocks.isNotEmpty()) {
                ui.small.striped.table Table {
                    thead { tr { th { +"Key" }; th { +"Server" }; th { +"Created" } } }
                    tbody {
                        activeLocks.forEach { lock ->
                            tr {
                                td { +lock.key }
                                td { +lock.serverId }
                                td { +lock.createdAt }
                            }
                        }
                    }
                }
            } else {
                ui.message { +"No active locks" }
            }
        }
    }

    private fun FlowContent.renderWorkers() {
        ui.segment {
            ui.header H2 { icon.cog(); noui.content { +"Workers" } }

            ui.button {
                onClick { launch { workers = Apis.showcase.getWorkers().first().data ?: emptyList() } }
                icon.redo(); +"Refresh"
            }

            if (workers.isNotEmpty()) {
                ui.striped.table Table {
                    thead { tr { th { +"Worker ID" }; th { +"Last Run" }; th { +"Result" }; th { +"Total Runs" } } }
                    tbody {
                        workers.forEach { worker ->
                            tr {
                                td { +worker.id }
                                td { +(worker.lastRunAt ?: "never") }
                                td {
                                    when {
                                        worker.lastRunResult == null -> {
                                            +"n/a"
                                        }

                                        worker.lastRunResult!!.startsWith("success") -> {
                                            icon.green.check_circle(); +worker.lastRunResult!!
                                        }

                                        else -> {
                                            icon.red.times_circle(); +worker.lastRunResult!!
                                        }
                                    }
                                }
                                td { +"${worker.totalRuns}" }
                            }
                        }
                    }
                }
            } else {
                ui.message { +"No workers registered" }
            }
        }
    }
}
