package datetime

import de.peekandpoke.common.datetime.PortableDate
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class PortableDateSpec : StringSpec({

    "Comparison" {

        assertSoftly {
            (PortableDate(1000) < PortableDate(1001)) shouldBe true
            (PortableDate(1000) == PortableDate(1001)) shouldBe false
            (PortableDate(1000) > PortableDate(1001)) shouldBe false

            (PortableDate(1001) < PortableDate(1000)) shouldBe false
            (PortableDate(1001) == PortableDate(1000)) shouldBe false
            (PortableDate(1001) > PortableDate(1000)) shouldBe true

            (PortableDate(1000) < PortableDate(1000)) shouldBe false
            (PortableDate(1000) == PortableDate(1000)) shouldBe true
            (PortableDate(1000) > PortableDate(1000)) shouldBe false
        }
    }
})
