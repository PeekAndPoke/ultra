@file:Suppress("DuplicatedCode")

package de.peekandpoke.kraft.examples.fomanticui.pages.views.comment

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.CommentPage() = comp {
    CommentPage(it)
}

class CommentPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Comment")

        ui.basic.segment {
            ui.dividing.header H1 { +"Comment" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/comment.html")

            ui.dividing.header H2 { +"Types" }

            renderComments()

            ui.dividing.header H2 { +"Content" }

            renderAvatar()
            renderMetadata()
            renderActions()
            renderReplyForm()

            ui.dividing.header H2 { +"States" }

            renderCollapsed()

            ui.dividing.header H2 { +"Variations" }

            renderThreaded()
            renderMinimal()
            renderSize()
            renderInverted()
        }
    }

    private fun FlowContent.renderComments() = example {
        ui.header { +"Comments" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderComments,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderComments>
                ui.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"How artistic!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                        noui.comments {
                            ui.comment {
                                noui.avatar A {
                                    href = "https://example.com"
                                    img(src = "images/avatar2/large/jenny.jpg")
                                }
                                noui.content {
                                    noui.author A {
                                        href = "https://example.com"
                                        +"Jenny"
                                    }
                                    noui.metadata {
                                        +"Today at 5:43PM"
                                    }
                                    noui.text {
                                        +"Very much!"
                                    }
                                    noui.actions {
                                        a(href = "https://example.com") { +"Reply" }
                                    }
                                }
                            }
                        }
                    }

                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Elliot"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"This has been very useful for my research. Thanks as well!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderAvatar() = example {
        ui.header { +"Avatar" }

        p { +"A comment can contain an image or avatar" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderAvatar,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderAvatar>
                ui.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderMetadata() = example {
        ui.header { +"Metadata" }

        p { +"A comment can contain metadata about the comment, an arbitrary amount of metadata may be defined." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderMetadata,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderMetadata>
                ui.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                div { +"2 days ago" }
                                div { icon.star(); +"5 Faves" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderActions() = example {
        ui.header { +"Actions" }

        p { +"A comment can contain an list of actions a user may perform related to this comment." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderActions,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderActions>
                ui.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.text {
                                +"This will be great for business reports. I will definitely download this."
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                                a(href = "https://example.com") { +"Save" }
                                a(href = "https://example.com") { +"Hide" }
                                a(href = "https://example.com") { icon.expand(); +"Full-screen" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderReplyForm() = example {
        ui.header { +"Reply Form" }

        p { +"A comment can contain a form to reply to a comment. This may have arbitrary content." }

        ui.info.message {
            +"If a comment form is located inside a "
            ui.label { +"comment" }
            +" it will be formatted as an nested reply form. If the comment form is included after all comments, "
            +"it will be formatted as a normal reply form."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderReplyForm,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderReplyForm>
                ui.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.text {
                                +"This will be great for business reports. I will definitely download this."
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                            ui.reply.form {
                                ui.field {
                                    textArea {}
                                }
                                ui.primary.labeled.icon.button {
                                    icon.edit()
                                    +"Reply"
                                }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderCollapsed() = example {
        ui.header { +"Collapsed" }

        readTheFomanticUiDocs("https://fomantic-ui.com/views/comment.html#collapsed")
    }

    private fun FlowContent.renderThreaded() = example {
        ui.header { +"Threaded" }

        p { +"A comment list can be threaded to showing the relationship between conversations" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderThreaded,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderThreaded>
                ui.threaded.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"How artistic!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                        noui.comments {
                            ui.comment {
                                noui.avatar A {
                                    href = "https://example.com"
                                    img(src = "images/avatar2/large/jenny.jpg")
                                }
                                noui.content {
                                    noui.author A {
                                        href = "https://example.com"
                                        +"Jenny"
                                    }
                                    noui.metadata {
                                        +"Today at 5:43PM"
                                    }
                                    noui.text {
                                        +"Very much!"
                                    }
                                    noui.actions {
                                        a(href = "https://example.com") { +"Reply" }
                                    }
                                }
                            }
                        }
                    }

                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Elliot"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"This has been very useful for my research. Thanks as well!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderMinimal() = example {
        ui.header { +"Minimal" }

        p { +"Comments can hide extra information unless a user shows intent to interact with a comment." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderMinimal,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderMinimal>
                ui.minimal.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"How artistic!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                        noui.comments {
                            ui.comment {
                                noui.avatar A {
                                    href = "https://example.com"
                                    img(src = "images/avatar2/large/jenny.jpg")
                                }
                                noui.content {
                                    noui.author A {
                                        href = "https://example.com"
                                        +"Jenny"
                                    }
                                    noui.metadata {
                                        +"Today at 5:43PM"
                                    }
                                    noui.text {
                                        +"Very much!"
                                    }
                                    noui.actions {
                                        a(href = "https://example.com") { +"Reply" }
                                    }
                                }
                            }
                        }
                    }

                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Elliot"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"This has been very useful for my research. Thanks as well!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header { +"Size" }

        p { +"Comments can have different sizes." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderSize,
        ) {
            ui.basic.padded.segment {
                // <CodeBlock renderSize>
                ui.huge.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"How artistic!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                        noui.comments {
                            ui.comment {
                                noui.avatar A {
                                    href = "https://example.com"
                                    img(src = "images/avatar2/large/jenny.jpg")
                                }
                                noui.content {
                                    noui.author A {
                                        href = "https://example.com"
                                        +"Jenny"
                                    }
                                    noui.metadata {
                                        +"Today at 5:43PM"
                                    }
                                    noui.text {
                                        +"Very much!"
                                    }
                                    noui.actions {
                                        a(href = "https://example.com") { +"Reply" }
                                    }
                                }
                            }
                        }
                    }

                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Elliot"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"This has been very useful for my research. Thanks as well!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header { +"Inverted" }

        p { +"Comments' colors can be inverted" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_views_comment_CommentPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.padded.segment {
                ui.inverted.comments {
                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/matthew.png")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Matthew"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"How artistic!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                        noui.comments {
                            ui.comment {
                                noui.avatar A {
                                    href = "https://example.com"
                                    img(src = "images/avatar2/large/jenny.jpg")
                                }
                                noui.content {
                                    noui.author A {
                                        href = "https://example.com"
                                        +"Jenny"
                                    }
                                    noui.metadata {
                                        +"Today at 5:43PM"
                                    }
                                    noui.text {
                                        +"Very much!"
                                    }
                                    noui.actions {
                                        a(href = "https://example.com") { +"Reply" }
                                    }
                                }
                            }
                        }
                    }

                    ui.comment {
                        noui.avatar A {
                            href = "https://example.com"
                            img(src = "images/avatar2/large/elliot.jpg")
                        }
                        noui.content {
                            noui.author A {
                                href = "https://example.com"
                                +"Elliot"
                            }
                            noui.metadata {
                                +"Today at 5:42PM"
                            }
                            noui.text {
                                +"This has been very useful for my research. Thanks as well!"
                            }
                            noui.actions {
                                a(href = "https://example.com") { +"Reply" }
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
