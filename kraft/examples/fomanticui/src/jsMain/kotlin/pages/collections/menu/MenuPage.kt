@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.collections.menu

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.MenuPage() = comp {
    MenuPage(it)
}

class MenuPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Menu")

        ui.basic.segment {
            ui.dividing.header H1 { +"Menu" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/menu.html")

            ui.dividing.header H2 { +"Types" }

            renderMenu()
            renderSecondaryMenu()
            renderPointingMenu()
            renderTabularMenu()
            renderTextMenu()
            renderVerticalMenu()
            renderPaginationMenu()

            ui.dividing.header H2 { +"Content" }

            renderHeaderContent()
            renderInputContent()
            renderButtonContent()
            renderLinkItem()
            renderSubMenu()

            ui.dividing.header H2 { +"States" }

            renderActiveState()
            renderDisabledState()

            ui.dividing.header H2 { +"Variations" }

            renderStackable()
            renderInverted()
            renderColored()
            renderIcons()
            renderLabeledIcon()
            renderFluid()
            renderCompact()
            renderEvenlyDivided()
            renderAttached()
            renderSize()
            renderFitted()
            renderBorderless()
        }
    }

    // Types ////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun FlowContent.renderMenu() = example {
        ui.header H3 { +"Menu" }

        p { +"A menu." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderMenu,
        ) {
            // <CodeBlock renderMenu>
            ui.three.item.menu {
                noui.active.item A { +"Editorials" }
                noui.item A { +"Reviews" }
                noui.item A { +"Upcoming Events" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSecondaryMenu() = example {
        ui.header H3 { +"Secondary Menu" }

        p { +"A menu can adjust its appearance to de-emphasize its contents." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderSecondaryMenu,
        ) {
            // <CodeBlock renderSecondaryMenu>
            ui.secondary.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
                noui.right.menu {
                    noui.item {
                        ui.input {
                            input(type = InputType.text) {
                                placeholder = "Search..."
                            }
                        }
                    }
                    noui.item A { +"Logout" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPointingMenu() = example {
        ui.header H3 { +"Pointing" }

        p { +"A menu can point to show its relationship to nearby content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderPointingMenu,
        ) {
            // <CodeBlock renderPointingMenu>
            ui.pointing.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
                noui.right.item {
                    ui.input {
                        input(type = InputType.text) {
                            placeholder = "Search..."
                        }
                    }
                }
            }
            ui.segment {
                p { +"Content for the Home tab." }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTabularMenu() = example {
        ui.header H3 { +"Tabular" }

        p { +"A menu can be formatted to show tabs of information." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderTabularMenu,
        ) {
            // <CodeBlock renderTabularMenu>
            ui.tabular.menu {
                noui.active.item A { +"Bio" }
                noui.item A { +"Photos" }
            }
            ui.bottom.attached.segment {
                p { +"Bio content appears here." }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextMenu() = example {
        ui.header H3 { +"Text" }

        p { +"A menu can be formatted for text content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderTextMenu,
        ) {
            // <CodeBlock renderTextMenu>
            ui.text.menu {
                noui.header.item { +"Sort By" }
                noui.active.item A { +"Closest" }
                noui.item A { +"Most Comments" }
                noui.item A { +"Most Popular" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVerticalMenu() = example {
        ui.header H3 { +"Vertical Menu" }

        p { +"A vertical menu displays elements vertically." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderVerticalMenu,
        ) {
            // <CodeBlock renderVerticalMenu>
            ui.vertical.menu {
                noui.active.item A {
                    +"Inbox"
                    ui.teal.left.pointing.label { +"1" }
                }
                noui.item A {
                    +"Spam"
                    ui.label { +"51" }
                }
                noui.item A {
                    +"Updates"
                    ui.label { +"1" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPaginationMenu() = example {
        ui.header H3 { +"Pagination" }

        p { +"A pagination menu is specially formatted to present links to pages of content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderPaginationMenu,
        ) {
            // <CodeBlock renderPaginationMenu>
            ui.pagination.menu {
                noui.active.item A { +"1" }
                noui.disabled.item { +"..." }
                noui.item A { +"10" }
                noui.item A { +"11" }
                noui.item A { +"12" }
            }
            // </CodeBlock>
        }
    }

    // Content //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun FlowContent.renderHeaderContent() = example {
        ui.header H3 { +"Header" }

        p { +"A menu item may include a header or may itself be a header." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderHeaderContent,
        ) {
            // <CodeBlock renderHeaderContent>
            ui.vertical.menu {
                noui.item {
                    noui.header { +"Products" }
                    noui.menu {
                        noui.item A { +"Enterprise" }
                        noui.item A { +"Consumer" }
                    }
                }
                noui.item {
                    noui.header { +"CMS Solutions" }
                    noui.menu {
                        noui.item A { +"Rails" }
                        noui.item A { +"Python" }
                        noui.item A { +"PHP" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInputContent() = example {
        ui.header H3 { +"Input" }

        p { +"A menu item can contain an input inside of it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderInputContent,
        ) {
            // <CodeBlock renderInputContent>
            ui.menu {
                noui.item {
                    ui.input {
                        input(type = InputType.text) {
                            placeholder = "Search..."
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderButtonContent() = example {
        ui.header H3 { +"Button" }

        p { +"A menu item can contain a button inside of it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderButtonContent,
        ) {
            // <CodeBlock renderButtonContent>
            ui.menu {
                noui.item {
                    ui.primary.button { +"Sign up" }
                }
                noui.item {
                    ui.button { +"Log in" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLinkItem() = example {
        ui.header H3 { +"Link Item" }

        p { +"A menu may contain a link item, or an item formatted as if it is a link." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderLinkItem,
        ) {
            // <CodeBlock renderLinkItem>
            ui.vertical.menu {
                noui.item A { +"Visit Google" }
                noui.item A { +"Visit Reddit" }
                noui.link.item { +"Link via class" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSubMenu() = example {
        ui.header H3 { +"Sub Menu" }

        p { +"A menu item can contain another menu nested inside that acts as a grouped sub-menu." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderSubMenu,
        ) {
            // <CodeBlock renderSubMenu>
            ui.vertical.menu {
                noui.item {
                    ui.input {
                        input(type = InputType.text) {
                            placeholder = "Search..."
                        }
                    }
                }
                noui.item {
                    +"Home"
                    noui.menu {
                        noui.active.item A { +"Search" }
                        noui.item A { +"Add" }
                        noui.item A { +"Remove" }
                    }
                }
                noui.item A { +"Browse" }
                noui.item A { +"Messages" }
            }
            // </CodeBlock>
        }
    }

    // States ///////////////////////////////////////////////////////////////////////////////////////////////////

    private fun FlowContent.renderActiveState() = example {
        ui.header H3 { +"Active" }

        p { +"A menu item can be active." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderActiveState,
        ) {
            // <CodeBlock renderActiveState>
            ui.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledState() = example {
        ui.header H3 { +"Disabled" }

        p { +"A menu item can be disabled." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderDisabledState,
        ) {
            // <CodeBlock renderDisabledState>
            ui.menu {
                noui.item A { +"Home" }
                noui.disabled.item { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    // Variations ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun FlowContent.renderStackable() = example {
        ui.header H3 { +"Stackable" }

        p { +"A menu can stack at mobile resolutions." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderStackable,
        ) {
            // <CodeBlock renderStackable>
            ui.stackable.menu {
                noui.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"A menu may have its colors inverted to show greater contrast." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColored() = example {
        ui.header H3 { +"Colored" }

        p { +"Additional colors can be specified." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderColored,
        ) {
            // <CodeBlock renderColored>
            ui.inverted.red.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }

            ui.inverted.blue.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }

            ui.inverted.green.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }

            ui.inverted.purple.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIcons() = example {
        ui.header H3 { +"Icons" }

        p { +"A menu may have just icons." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderIcons,
        ) {
            // <CodeBlock renderIcons>
            ui.icon.menu {
                noui.active.item A {
                    icon.gamepad()
                }
                noui.item A {
                    icon.camera()
                }
                noui.item A {
                    icon.mail()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLabeledIcon() = example {
        ui.header H3 { +"Labeled Icon" }

        p { +"A menu may have labeled icons." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderLabeledIcon,
        ) {
            // <CodeBlock renderLabeledIcon>
            ui.labeled.icon.menu {
                noui.active.item A {
                    icon.gamepad()
                    +"Games"
                }
                noui.item A {
                    icon.camera()
                    +"Photos"
                }
                noui.item A {
                    icon.mail()
                    +"Mail"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluid() = example {
        ui.header H3 { +"Fluid" }

        p { +"A vertical menu may take the size of its container." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderFluid,
        ) {
            // <CodeBlock renderFluid>
            ui.fluid.vertical.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCompact() = example {
        ui.header H3 { +"Compact" }

        p { +"A menu can take up only the space necessary to fit its content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderCompact,
        ) {
            // <CodeBlock renderCompact>
            ui.compact.menu {
                noui.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderEvenlyDivided() = example {
        ui.header H3 { +"Evenly Divided" }

        p { +"A menu may divide its items evenly." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderEvenlyDivided,
        ) {
            // <CodeBlock renderEvenlyDivided>
            ui.three.item.menu {
                noui.active.item A { +"Editorials" }
                noui.item A { +"Reviews" }
                noui.item A { +"Upcoming Events" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttached() = example {
        ui.header H3 { +"Attached" }

        p { +"A menu may be attached to other content segments." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderAttached,
        ) {
            // <CodeBlock renderAttached>
            ui.top.attached.menu {
                noui.active.item A { +"Tab 1" }
                noui.item A { +"Tab 2" }
            }
            ui.attached.segment {
                p { +"Content of Tab 1." }
            }
            ui.bottom.attached.menu {
                noui.item A { +"Tab 3" }
                noui.item A { +"Tab 4" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A menu can vary in size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.mini.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.tiny.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.small.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.large.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.big.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.huge.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            ui.massive.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFitted() = example {
        ui.header H3 { +"Fitted" }

        p { +"A menu item can remove element padding, vertically or horizontally." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderFitted,
        ) {
            // <CodeBlock renderFitted>
            ui.menu {
                noui.fitted.item { +"No padding whatsoever" }
                noui.horizontally.fitted.item { +"No horizontal padding" }
                noui.vertically.fitted.item { +"No vertical padding" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBorderless() = example {
        ui.header H3 { +"Borderless" }

        p { +"A menu item or menu can have no borders." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_menu_MenuPage_kt_renderBorderless,
        ) {
            // <CodeBlock renderBorderless>
            ui.borderless.menu {
                noui.active.item A { +"Home" }
                noui.item A { +"Messages" }
                noui.item A { +"Friends" }
            }
            // </CodeBlock>
        }
    }
}
