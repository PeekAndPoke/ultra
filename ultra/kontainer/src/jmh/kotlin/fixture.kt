package io.peekandpoke.ultra.kontainer.jmh

import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.kontainer

// Generated once by a script (ULTRA-00351), shaped like a funktor app. Keep the shape below when editing it:
// the benchmark numbers in the ticket were measured on it.
//
// 149 static singletons (S000..S139 by class and factory, Handler1..5, SystemClock, registries, LazyUser),
// 60 dynamic singletons (UserProvider and RequestInfo overridable, D02..D59), 40 semi dynamic singletons
// through chains (SD00..SD39), 5 prototypes (P0..P4), one interface with 5 implementations (Handler).

class S000
class S001
class S002
class S003
class S004
class S005
class S006
class S007
class S008
class S009
class S010
class S011
class S012
class S013
class S014
class S015
class S016
class S017
class S018
class S019
class S020
class S021
class S022
class S023
class S024
class S025
class S026
class S027
class S028
class S029
class S030
class S031
class S032
class S033
class S034
class S035
class S036
class S037
class S038
class S039
class S040
class S041
class S042
class S043
class S044
class S045
class S046
class S047
class S048
class S049
class S050(val p0: S000, val p1: S013, val p2: S026)
class S051(val p0: S000)
class S052(val p0: S000, val p1: S013)
class S053(val p0: S000, val p1: S013, val p2: S026)
class S054(val p0: S000)
class S055(val p0: S000, val p1: S013)
class S056(val p0: S000, val p1: S013, val p2: S026)
class S057(val p0: S000)
class S058(val p0: S000, val p1: S013)
class S059(val p0: S000, val p1: S013, val p2: S026)
class S060(val p0: S000)
class S061(val p0: S000, val p1: S013)
class S062(val p0: S000, val p1: S013, val p2: S026)
class S063(val p0: S000)
class S064(val p0: S000, val p1: S013)
class S065(val p0: S000, val p1: S013, val p2: S026)
class S066(val p0: S000)
class S067(val p0: S000, val p1: S013)
class S068(val p0: S000, val p1: S013, val p2: S026)
class S069(val p0: S000)
class S070(val p0: S000, val p1: S013)
class S071(val p0: S000, val p1: S013, val p2: S026)
class S072(val p0: S000)
class S073(val p0: S000, val p1: S013)
class S074(val p0: S000, val p1: S013, val p2: S026)
class S075(val p0: S000)
class S076(val p0: S000, val p1: S013)
class S077(val p0: S000, val p1: S013, val p2: S026)
class S078(val p0: S000)
class S079(val p0: S000, val p1: S013)
class S080(val p0: S000, val p1: S013, val p2: S026)
class S081(val p0: S000)
class S082(val p0: S000, val p1: S013)
class S083(val p0: S000, val p1: S013, val p2: S026)
class S084(val p0: S000)
class S085(val p0: S000, val p1: S013)
class S086(val p0: S000, val p1: S013, val p2: S026)
class S087(val p0: S000)
class S088(val p0: S000, val p1: S013)
class S089(val p0: S000, val p1: S013, val p2: S026)
class S090(val p0: S000)
class S091(val p0: S000, val p1: S013)
class S092(val p0: S000, val p1: S013, val p2: S026)
class S093(val p0: S000)
class S094(val p0: S000, val p1: S013)
class S095(val p0: S000, val p1: S013, val p2: S026)
class S096(val p0: S000)
class S097(val p0: S000, val p1: S013)
class S098(val p0: S000, val p1: S013, val p2: S026)
class S099(val p0: S000)
class S100(val p0: S000, val p1: S013)
class S101(val p0: S000, val p1: S013, val p2: S026)
class S102(val p0: S000)
class S103(val p0: S000, val p1: S013)
class S104(val p0: S000, val p1: S013, val p2: S026)
class S105(val p0: S000)
class S106(val p0: S000, val p1: S013)
class S107(val p0: S000, val p1: S013, val p2: S026)
class S108(val p0: S000)
class S109(val p0: S000, val p1: S013)
class S110(val p0: S000, val p1: S013, val p2: S026)
class S111(val p0: S000)
class S112(val p0: S000, val p1: S013)
class S113(val p0: S000, val p1: S013, val p2: S026)
class S114(val p0: S000)
class S115(val p0: S000, val p1: S013)
class S116(val p0: S000, val p1: S013, val p2: S026)
class S117(val p0: S000)
class S118(val p0: S000, val p1: S013)
class S119(val p0: S000, val p1: S013, val p2: S026)
class S120(val p0: S010, val p1: S100)
class S121(val p0: S013, val p1: S101)
class S122(val p0: S016, val p1: S102)
class S123(val p0: S019, val p1: S103)
class S124(val p0: S022, val p1: S104)
class S125(val p0: S025, val p1: S105)
class S126(val p0: S028, val p1: S106)
class S127(val p0: S031, val p1: S107)
class S128(val p0: S034, val p1: S108)
class S129(val p0: S037, val p1: S109)
class S130(val p0: S040, val p1: S110)
class S131(val p0: S043, val p1: S111)
class S132(val p0: S046, val p1: S112)
class S133(val p0: S049, val p1: S113)
class S134(val p0: S002, val p1: S114)
class S135(val p0: S005, val p1: S115)
class S136(val p0: S008, val p1: S116)
class S137(val p0: S011, val p1: S117)
class S138(val p0: S014, val p1: S118)
class S139(val p0: S017, val p1: S119)

interface Handler
class Handler1(val s: S010) : Handler
class Handler2(val s: S020) : Handler
class Handler3(val s: S030) : Handler
class Handler4(val s: S040) : Handler
class Handler5(val s: S050) : Handler

interface Clock
class SystemClock : Clock
class HandlerRegistry(val handlers: Lookup<Handler>)
class HandlerList(val handlers: List<Handler>)
class LazyUser(val s: Lazy<S100>, val c: Lazy<Clock>)

class UserProvider(val user: String)
class RequestInfo(val path: String)
class D02
class D03
class D04
class D05
class D06
class D07
class D08
class D09
class D10
class D11
class D12
class D13
class D14
class D15
class D16
class D17
class D18
class D19
class D20
class D21
class D22
class D23
class D24
class D25
class D26
class D27
class D28
class D29
class D30(val p0: S010, val p1: D04, val p2: UserProvider)
class D31(val p0: S015)
class D32(val p0: S020, val p1: D06)
class D33(val p0: S025)
class D34(val p0: S030, val p1: D08)
class D35(val p0: S035, val p1: UserProvider)
class D36(val p0: S040, val p1: D10)
class D37(val p0: S045)
class D38(val p0: S050, val p1: D12)
class D39(val p0: S055)
class D40(val p0: S060, val p1: D14, val p2: UserProvider)
class D41(val p0: S065)
class D42(val p0: S070, val p1: D16)
class D43(val p0: S075)
class D44(val p0: S080, val p1: D18)
class D45(val p0: S085, val p1: UserProvider)
class D46(val p0: S090, val p1: D20)
class D47(val p0: S095)
class D48(val p0: S100, val p1: D22)
class D49(val p0: S105)
class D50(val p0: S110, val p1: D24, val p2: UserProvider)
class D51(val p0: S115)
class D52(val p0: S120, val p1: D26)
class D53(val p0: S125)
class D54(val p0: S130, val p1: D28)
class D55(val p0: S135, val p1: UserProvider)
class D56(val p0: S000, val p1: D02)
class D57(val p0: S005)
class D58(val p0: S010, val p1: D04)
class D59(val p0: S015)

class SD00(val p0: D02, val p1: S000)
class SD01(val p0: D07, val p1: S011)
class SD02(val p0: D12, val p1: S022)
class SD03(val p0: D17, val p1: S033)
class SD04(val p0: D22, val p1: S044)
class SD05(val p0: D27, val p1: S055)
class SD06(val p0: UserProvider, val p1: S066)
class SD07(val p0: UserProvider, val p1: S077)
class SD08(val p0: UserProvider, val p1: S088)
class SD09(val p0: UserProvider, val p1: S099)
class SD10(val p0: SD00, val p1: S030)
class SD11(val p0: SD01, val p1: S033)
class SD12(val p0: SD02, val p1: S036, val p2: SD05)
class SD13(val p0: SD03, val p1: S039)
class SD14(val p0: SD04, val p1: S042)
class SD15(val p0: SD05, val p1: S045)
class SD16(val p0: SD06, val p1: S048, val p2: SD09)
class SD17(val p0: SD07, val p1: S051)
class SD18(val p0: SD08, val p1: S054)
class SD19(val p0: SD09, val p1: S057)
class SD20(val p0: SD10, val p1: S060, val p2: SD13)
class SD21(val p0: SD11, val p1: S063)
class SD22(val p0: SD12, val p1: S066)
class SD23(val p0: SD13, val p1: S069)
class SD24(val p0: SD14, val p1: S072, val p2: SD17)
class SD25(val p0: SD15, val p1: S075)
class SD26(val p0: SD16, val p1: S078)
class SD27(val p0: SD17, val p1: S081)
class SD28(val p0: SD18, val p1: S084, val p2: SD21)
class SD29(val p0: SD19, val p1: S087)
class SD30(val p0: SD20, val p1: S090)
class SD31(val p0: SD21, val p1: S093)
class SD32(val p0: SD22, val p1: S096, val p2: SD25)
class SD33(val p0: SD23, val p1: S099)
class SD34(val p0: SD24, val p1: S102)
class SD35(val p0: SD25, val p1: S105)
class SD36(val p0: SD26, val p1: S108, val p2: SD29)
class SD37(val p0: SD27, val p1: S111)
class SD38(val p0: SD28, val p1: S114)
class SD39(val p0: SD29, val p1: S117)

class P0
class P1(val a: S001, val b: S060, val c: S120)
class P2(val a: S002, val b: S070)
class P3(val n: Int)
class P4(val a: S003)

/** The benchmark blueprint: about 250 definitions plus the 3 built-ins. */
fun funktorShapedBlueprint(): KontainerBlueprint = kontainer {
    singleton(S000::class)
    singleton(S001::class)
    singleton(S002::class)
    singleton(S003::class)
    singleton(S004::class)
    singleton(S005::class)
    singleton(S006::class)
    singleton(S007::class)
    singleton(S008::class)
    singleton(S009::class)
    singleton(S010::class)
    singleton(S011::class)
    singleton(S012::class)
    singleton(S013::class)
    singleton(S014::class)
    singleton(S015::class)
    singleton(S016::class)
    singleton(S017::class)
    singleton(S018::class)
    singleton(S019::class)
    singleton(S020::class)
    singleton(S021::class)
    singleton(S022::class)
    singleton(S023::class)
    singleton(S024::class)
    singleton(S025::class)
    singleton(S026::class)
    singleton(S027::class)
    singleton(S028::class)
    singleton(S029::class)
    singleton(S030::class)
    singleton(S031::class)
    singleton(S032::class)
    singleton(S033::class)
    singleton(S034::class)
    singleton(S035::class)
    singleton(S036::class)
    singleton(S037::class)
    singleton(S038::class)
    singleton(S039::class)
    singleton(S040::class)
    singleton(S041::class)
    singleton(S042::class)
    singleton(S043::class)
    singleton(S044::class)
    singleton(S045::class)
    singleton(S046::class)
    singleton(S047::class)
    singleton(S048::class)
    singleton(S049::class)
    singleton(S050::class)
    singleton(S051::class)
    singleton(S052::class)
    singleton(S053::class)
    singleton(S054::class)
    singleton(S055::class)
    singleton(S056::class)
    singleton(S057::class)
    singleton(S058::class)
    singleton(S059::class)
    singleton(S060::class)
    singleton(S061::class)
    singleton(S062::class)
    singleton(S063::class)
    singleton(S064::class)
    singleton(S065::class)
    singleton(S066::class)
    singleton(S067::class)
    singleton(S068::class)
    singleton(S069::class)
    singleton(S070::class)
    singleton(S071::class)
    singleton(S072::class)
    singleton(S073::class)
    singleton(S074::class)
    singleton(S075::class)
    singleton(S076::class)
    singleton(S077::class)
    singleton(S078::class)
    singleton(S079::class)
    singleton(S080::class)
    singleton(S081::class)
    singleton(S082::class)
    singleton(S083::class)
    singleton(S084::class)
    singleton(S085::class)
    singleton(S086::class)
    singleton(S087::class)
    singleton(S088::class)
    singleton(S089::class)
    singleton(S090::class)
    singleton(S091::class)
    singleton(S092::class)
    singleton(S093::class)
    singleton(S094::class)
    singleton(S095::class)
    singleton(S096::class)
    singleton(S097::class)
    singleton(S098::class)
    singleton(S099::class)
    singleton(S100::class)
    singleton(S101::class)
    singleton(S102::class)
    singleton(S103::class)
    singleton(S104::class)
    singleton(S105::class)
    singleton(S106::class)
    singleton(S107::class)
    singleton(S108::class)
    singleton(S109::class)
    singleton(S110::class)
    singleton(S111::class)
    singleton(S112::class)
    singleton(S113::class)
    singleton(S114::class)
    singleton(S115::class)
    singleton(S116::class)
    singleton(S117::class)
    singleton(S118::class)
    singleton(S119::class)
    singleton(S120::class) { a: S010, b: S100 -> S120(a, b) }
    singleton(S121::class) { a: S013, b: S101 -> S121(a, b) }
    singleton(S122::class) { a: S016, b: S102 -> S122(a, b) }
    singleton(S123::class) { a: S019, b: S103 -> S123(a, b) }
    singleton(S124::class) { a: S022, b: S104 -> S124(a, b) }
    singleton(S125::class) { a: S025, b: S105 -> S125(a, b) }
    singleton(S126::class) { a: S028, b: S106 -> S126(a, b) }
    singleton(S127::class) { a: S031, b: S107 -> S127(a, b) }
    singleton(S128::class) { a: S034, b: S108 -> S128(a, b) }
    singleton(S129::class) { a: S037, b: S109 -> S129(a, b) }
    singleton(S130::class) { a: S040, b: S110 -> S130(a, b) }
    singleton(S131::class) { a: S043, b: S111 -> S131(a, b) }
    singleton(S132::class) { a: S046, b: S112 -> S132(a, b) }
    singleton(S133::class) { a: S049, b: S113 -> S133(a, b) }
    singleton(S134::class) { a: S002, b: S114 -> S134(a, b) }
    singleton(S135::class) { a: S005, b: S115 -> S135(a, b) }
    singleton(S136::class) { a: S008, b: S116 -> S136(a, b) }
    singleton(S137::class) { a: S011, b: S117 -> S137(a, b) }
    singleton(S138::class) { a: S014, b: S118 -> S138(a, b) }
    singleton(S139::class) { a: S017, b: S119 -> S139(a, b) }
    singleton(Handler1::class)
    singleton(Handler2::class)
    singleton(Handler3::class)
    singleton(Handler4::class)
    singleton(Handler5::class)
    singleton(SystemClock::class)
    singleton(HandlerRegistry::class)
    singleton(HandlerList::class)
    singleton(LazyUser::class)

    dynamic(UserProvider::class) { UserProvider("anonymous") }
    dynamic(RequestInfo::class) { RequestInfo("/") }
    dynamic(D02::class)
    dynamic(D03::class)
    dynamic(D04::class)
    dynamic(D05::class)
    dynamic(D06::class)
    dynamic(D07::class)
    dynamic(D08::class)
    dynamic(D09::class)
    dynamic(D10::class)
    dynamic(D11::class)
    dynamic(D12::class)
    dynamic(D13::class)
    dynamic(D14::class)
    dynamic(D15::class)
    dynamic(D16::class)
    dynamic(D17::class)
    dynamic(D18::class)
    dynamic(D19::class)
    dynamic(D20::class)
    dynamic(D21::class)
    dynamic(D22::class)
    dynamic(D23::class)
    dynamic(D24::class)
    dynamic(D25::class)
    dynamic(D26::class)
    dynamic(D27::class)
    dynamic(D28::class)
    dynamic(D29::class)
    dynamic(D30::class)
    dynamic(D31::class)
    dynamic(D32::class)
    dynamic(D33::class)
    dynamic(D34::class)
    dynamic(D35::class)
    dynamic(D36::class)
    dynamic(D37::class)
    dynamic(D38::class)
    dynamic(D39::class)
    dynamic(D40::class)
    dynamic(D41::class)
    dynamic(D42::class)
    dynamic(D43::class)
    dynamic(D44::class)
    dynamic(D45::class)
    dynamic(D46::class)
    dynamic(D47::class)
    dynamic(D48::class)
    dynamic(D49::class)
    dynamic(D50::class)
    dynamic(D51::class)
    dynamic(D52::class)
    dynamic(D53::class)
    dynamic(D54::class)
    dynamic(D55::class)
    dynamic(D56::class)
    dynamic(D57::class)
    dynamic(D58::class)
    dynamic(D59::class)

    singleton(SD00::class)
    singleton(SD01::class)
    singleton(SD02::class)
    singleton(SD03::class)
    singleton(SD04::class)
    singleton(SD05::class)
    singleton(SD06::class)
    singleton(SD07::class)
    singleton(SD08::class)
    singleton(SD09::class)
    singleton(SD10::class)
    singleton(SD11::class)
    singleton(SD12::class)
    singleton(SD13::class)
    singleton(SD14::class)
    singleton(SD15::class)
    singleton(SD16::class)
    singleton(SD17::class)
    singleton(SD18::class)
    singleton(SD19::class)
    singleton(SD20::class)
    singleton(SD21::class)
    singleton(SD22::class)
    singleton(SD23::class)
    singleton(SD24::class)
    singleton(SD25::class)
    singleton(SD26::class)
    singleton(SD27::class)
    singleton(SD28::class)
    singleton(SD29::class)
    singleton(SD30::class)
    singleton(SD31::class)
    singleton(SD32::class)
    singleton(SD33::class)
    singleton(SD34::class)
    singleton(SD35::class)
    singleton(SD36::class)
    singleton(SD37::class)
    singleton(SD38::class)
    singleton(SD39::class)

    prototype(P0::class)
    prototype(P1::class)
    prototype(P2::class) { a: S002, b: S070 -> P2(a, b) }
    prototype(P3::class) { P3(42) }
    prototype(P4::class)
}
