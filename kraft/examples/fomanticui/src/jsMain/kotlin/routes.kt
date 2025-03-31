package de.peekandpoke.kraft.examples.fomanticui

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.breadcrumb.BreadcrumbPage
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.form.FormPage
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.grid.GridPage
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.menu.MenuPage
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.message.MessagePage
import de.peekandpoke.kraft.examples.fomanticui.pages.collections.table.TablePage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.button.ButtonPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.container.ContainerPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.divider.DividerPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.emoji.EmojiPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.flag.FlagPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.header.HeaderPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.icon.IconPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.image.ImagePage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.input.InputPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.label.LabelPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.list.ListPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.loader.LoaderPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.placeholder.PlaceholderPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.rail.RailPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.reveal.RevealPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.segment.SegmentPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.step.StepPage
import de.peekandpoke.kraft.examples.fomanticui.pages.elements.text.TextPage
import de.peekandpoke.kraft.examples.fomanticui.pages.home.HomePage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.dnd.DragAndDropPage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.listfield.ListFieldPage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo.FormDemosPage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.KraftComponentBasicsPage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.KraftComponentStatePage
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.KraftGettingStartedPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.advertisement.AdvertisementPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.card.CardPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.comment.CommentPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.feed.FeedPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.item.ItemPage
import de.peekandpoke.kraft.examples.fomanticui.pages.views.statistic.StatisticPage

class Routes {
    val home = Static("")
    val homeSlash = Static("/")

    val howtoKraftGettingStarted = Static("/howto/kraft/getting-started")
    val howtoKraftComponentBasics = Static("/howto/kraft/components/basics")
    val howtoKraftComponentState = Static("/howto/kraft/components/state")

    val howtoFormsDemo = Static("/howto/forms/demo")

    val howtoBlocksDragAndDrop = Static("/howto/blocks/drag-and-drop")
    val howtoBlocksListField = Static("/howto/blocks/list-field")

    val elementsButton = Static("/elements/button")
    val elementsContainer = Static("/elements/container")
    val elementsDivider = Static("/elements/divider")
    val elementsEmoji = Static("/elements/emoji")
    val elementsFlag = Static("/elements/flag")
    val elementsHeader = Static("/elements/header")
    val elementsIcon = Static("/elements/icon")
    val elementsImage = Static("/elements/image")
    val elementsInput = Static("/elements/input")
    val elementsLabel = Static("/elements/label")
    val elementsList = Static("/elements/list")
    val elementsLoader = Static("/elements/loader")
    val elementsPlaceholder = Static("/elements/placeholder")
    val elementsRail = Static("/elements/rail")
    val elementsReveal = Static("/elements/reveal")
    val elementsSegment = Static("/elements/segment")
    val elementsStep = Static("/elements/step")
    val elementsText = Static("/elements/text")

    val collectionsBreadcrumb = Static("/collections/breadcrumb")
    val collectionsForm = Static("/collections/form")
    val collectionsGrid = Static("/collections/grid")
    val collectionsMenu = Static("/collections/menu")
    val collectionsMessage = Static("/collections/message")
    val collectionsTable = Static("/collections/table")

    val viewsAdvertisement = Static("/views/advertisement")
    val viewsCard = Static("/views/card")
    val viewsComment = Static("/views/comment")
    val viewsFeed = Static("/views/feed")
    val viewsItem = Static("/views/item")
    val viewsStatistic = Static("/views/statistic")
}

fun RouterBuilder.mount(routes: Routes) {
    mount(routes.home) { HomePage() }
    mount(routes.homeSlash) { HomePage() }

    mount(routes.howtoKraftGettingStarted) { KraftGettingStartedPage() }
    mount(routes.howtoKraftComponentBasics) { KraftComponentBasicsPage() }
    mount(routes.howtoKraftComponentState) { KraftComponentStatePage() }

    mount(routes.howtoFormsDemo) { FormDemosPage() }

    mount(routes.howtoBlocksDragAndDrop) { DragAndDropPage() }
    mount(routes.howtoBlocksListField) { ListFieldPage() }

    mount(routes.elementsButton) { ButtonPage() }
    mount(routes.elementsContainer) { ContainerPage() }
    mount(routes.elementsDivider) { DividerPage() }
    mount(routes.elementsEmoji) { EmojiPage() }
    mount(routes.elementsFlag) { FlagPage() }
    mount(routes.elementsHeader) { HeaderPage() }
    mount(routes.elementsIcon) { IconPage() }
    mount(routes.elementsImage) { ImagePage() }
    mount(routes.elementsInput) { InputPage() }
    mount(routes.elementsLabel) { LabelPage() }
    mount(routes.elementsList) { ListPage() }
    mount(routes.elementsLoader) { LoaderPage() }
    mount(routes.elementsPlaceholder) { PlaceholderPage() }
    mount(routes.elementsRail) { RailPage() }
    mount(routes.elementsReveal) { RevealPage() }
    mount(routes.elementsSegment) { SegmentPage() }
    mount(routes.elementsStep) { StepPage() }
    mount(routes.elementsText) { TextPage() }

    mount(routes.collectionsBreadcrumb) { BreadcrumbPage() }
    mount(routes.collectionsForm) { FormPage() }
    mount(routes.collectionsGrid) { GridPage() }
    mount(routes.collectionsMenu) { MenuPage() }
    mount(routes.collectionsMessage) { MessagePage() }
    mount(routes.collectionsTable) { TablePage() }

    mount(routes.viewsAdvertisement) { AdvertisementPage() }
    mount(routes.viewsCard) { CardPage() }
    mount(routes.viewsComment) { CommentPage() }
    mount(routes.viewsFeed) { FeedPage() }
    mount(routes.viewsItem) { ItemPage() }
    mount(routes.viewsStatistic) { StatisticPage() }
}
