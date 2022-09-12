package de.peekandpoke.ultra.common.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldStartWith

class FileBase64Spec : StringSpec({

    val png =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="
    val html =
        "data:text/html;charset=utf-8,<!DOCTYPE%20html><html%20lang%3D\"en\"><head><title>Embedded%20Window<%2Ftitle><%2Fhead><body><h1>42<%2Fh1><%2Fbody><%2Fhtml>"
    val svg =
        "data:image/svg+xml;base64,PD94bWwgdmVyzeiBNMyw2djJoMThWNkgzeiIvPjwvZz4KPC9zdmc+Cgo="
    val zip =
        "data:application/x-zip-compressed;base64,UEsDBBQAAAAAANdY/VBQMVWPFgAAABYAAAALAAAAZm9vICgxKS50eHRXZWxjb21lIHRvIHRoZSBqdW5nbGUhUEsDBBQAAAAAAGtY/VBQMVWPFgAAABYAAAAHAAAAZm9vLnR4dFdlbGNvbWUgdG8gdGhlIGp1bmdsZSFQSwECFAAUAAAAAADXWP1QUDFVjxYAAAAWAAAACwAAAAAAAAABACAAAAAAAAAAZm9vICgxKS50eHRQSwECFAAUAAAAAABrWP1QUDFVjxYAAAAWAAAABwAAAAAAAAABACAAAAA/AAAAZm9vLnR4dFBLBQYAAAAAAgACAG4AAAB6AAAAAAA="
    val note =
        "data:,A%20brief%20note"

    "fromDataUrl - working for empty data url" {

        val subject = FileBase64.fromDataUrl("data:,")

        subject.shouldNotBeNull()

        subject.mimeType.shouldBeNull()
        subject.data.shouldBeEmpty()
    }

    "fromDataUrl - working for 'png'" {

        val subject = FileBase64.fromDataUrl(png)

        subject.shouldNotBeNull()

        subject.mimeType shouldBe "image/png"
        subject.data shouldStartWith "iVB"
        subject.data shouldHaveLength 116
    }

    "fromDataUrl - working for 'html'" {

        val subject = FileBase64.fromDataUrl(html)

        subject.shouldNotBeNull()

        subject.mimeType shouldBe "text/html"
        subject.data shouldStartWith "<!DOCTYPE"
        subject.data shouldHaveLength 123
    }

    "fromDataUrl - working for 'svg'" {

        val subject = FileBase64.fromDataUrl(svg)

        subject.shouldNotBeNull()

        subject.mimeType shouldBe "image/svg+xml"
        subject.data shouldStartWith "PD94"
        subject.data shouldHaveLength 57
    }

    "fromDataUrl - working for 'zip'" {

        val subject = FileBase64.fromDataUrl(zip)

        subject.shouldNotBeNull()

        subject.mimeType shouldBe "application/x-zip-compressed"
        subject.data shouldStartWith "UEsDBB"
        subject.data shouldHaveLength 340
    }

    "fromDataUrl - working for 'note'" {

        val subject = FileBase64.fromDataUrl(note)

        subject.shouldNotBeNull()

        subject.mimeType shouldBe null
        subject.data shouldStartWith "A%20brief"
        subject.data shouldHaveLength 16
    }

    "fromDataUrl - failing for non-data-urls" {

        FileBase64.fromDataUrl("").shouldBeNull()
        FileBase64.fromDataUrl("data").shouldBeNull()
        FileBase64.fromDataUrl("data:").shouldBeNull()

        FileBase64.fromDataUrl(" $png").shouldBeNull()
    }
})
