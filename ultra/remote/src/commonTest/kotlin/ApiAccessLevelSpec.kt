package io.peekandpoke.ultra.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ApiAccessLevelSpec : StringSpec({

    "isGranted/isPartial/isDenied" {
        ApiAccessLevel.Granted.isGranted() shouldBe true
        ApiAccessLevel.Granted.isPartial() shouldBe false
        ApiAccessLevel.Granted.isDenied() shouldBe false

        ApiAccessLevel.Partial.isGranted() shouldBe false
        ApiAccessLevel.Partial.isPartial() shouldBe true
        ApiAccessLevel.Partial.isDenied() shouldBe false

        ApiAccessLevel.Denied.isGranted() shouldBe false
        ApiAccessLevel.Denied.isPartial() shouldBe false
        ApiAccessLevel.Denied.isDenied() shouldBe true
    }

    "and returns the most restrictive level" {
        (ApiAccessLevel.Granted and ApiAccessLevel.Granted) shouldBe ApiAccessLevel.Granted
        (ApiAccessLevel.Granted and ApiAccessLevel.Partial) shouldBe ApiAccessLevel.Partial
        (ApiAccessLevel.Granted and ApiAccessLevel.Denied) shouldBe ApiAccessLevel.Denied
        (ApiAccessLevel.Partial and ApiAccessLevel.Denied) shouldBe ApiAccessLevel.Denied
        (ApiAccessLevel.Denied and ApiAccessLevel.Denied) shouldBe ApiAccessLevel.Denied
    }

    "or returns the least restrictive level" {
        (ApiAccessLevel.Denied or ApiAccessLevel.Denied) shouldBe ApiAccessLevel.Denied
        (ApiAccessLevel.Denied or ApiAccessLevel.Partial) shouldBe ApiAccessLevel.Partial
        (ApiAccessLevel.Denied or ApiAccessLevel.Granted) shouldBe ApiAccessLevel.Granted
        (ApiAccessLevel.Partial or ApiAccessLevel.Granted) shouldBe ApiAccessLevel.Granted
        (ApiAccessLevel.Granted or ApiAccessLevel.Granted) shouldBe ApiAccessLevel.Granted
    }
})
