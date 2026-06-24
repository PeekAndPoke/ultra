@file:Suppress(
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
    "Detekt:VariableNaming",
)

package io.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.attributesMapOf
import kotlinx.html.body
import kotlinx.html.stream.createHTML

@Suppress("unused", "PropertyName")
@SemanticFlagMarker
class SemanticFlag(private val parent: FlowContent) {

    companion object {
        val all = listOf(
            "afghanistan",
            "aland islands",
            "albania",
            "algeria",
            "american samoa",
            "andorra",
            "angola",
            "anguilla",
            "antigua",
            "argentina",
            "armenia",
            "aruba",
            "australia",
            "austria",
            "azerbaijan",
            "bahamas",
            "bahrain",
            "bangladesh",
            "barbados",
            "belarus",
            "belgium",
            "belize",
            "benin",
            "bermuda",
            "bhutan",
            "bolivia",
            "bosnia",
            "botswana",
            "bouvet island",
            "brazil",
            "british virgin islands",
            "brunei",
            "bulgaria",
            "burkina faso",
            "burma",
            "burundi",
            "caicos islands",
            "cambodia",
            "cameroon",
            "canada",
            "cape verde",
            "cayman islands",
            "central african republic",
            "chad",
            "chile",
            "china",
            "christmas island",
            "cocos islands",
            "colombia",
            "comoros",
            "congo brazzaville",
            "congo",
            "cook islands",
            "costa rica",
            "cote divoire",
            "croatia",
            "cuba",
            "cyprus",
            "czech republic",
            "denmark",
            "djibouti",
            "dominica",
            "dominican republic",
            "ecuador",
            "egypt",
            "el salvador",
            "england",
            "equatorial guinea",
            "eritrea",
            "estonia",
            "ethiopia",
            "european union",
            "falkland islands",
            "faroe islands",
            "fiji",
            "finland",
            "france",
            "french guiana",
            "french polynesia",
            "french territories",
            "gabon",
            "gambia",
            "georgia",
            "germany",
            "ghana",
            "gibraltar",
            "greece",
            "greenland",
            "grenada",
            "guadeloupe",
            "guam",
            "guatemala",
            "guinea",
            "guinea-bissau",
            "guyana",
            "haiti",
            "heard island",
            "honduras",
            "hong kong",
            "hungary",
            "iceland",
            "india",
            "indian ocean territory",
            "indonesia",
            "iran",
            "iraq",
            "ireland",
            "israel",
            "italy",
            "jamaica",
            "jan mayen",
            "japan",
            "jordan",
            "kazakhstan",
            "kenya",
            "kiribati",
            "kuwait",
            "kyrgyzstan",
            "laos",
            "latvia",
            "lebanon",
            "lesotho",
            "liberia",
            "libya",
            "liechtenstein",
            "lithuania",
            "luxembourg",
            "macau",
            "macedonia",
            "madagascar",
            "malawi",
            "malaysia",
            "maldives",
            "mali",
            "malta",
            "marshall islands",
            "martinique",
            "mauritania",
            "mauritius",
            "mayotte",
            "mexico",
            "micronesia",
            "moldova",
            "monaco",
            "mongolia",
            "montenegro",
            "montserrat",
            "morocco",
            "mozambique",
            "namibia",
            "nauru",
            "nepal",
            "netherlands antilles",
            "netherlands",
            "new caledonia",
            "new guinea",
            "new zealand",
            "nicaragua",
            "niger",
            "nigeria",
            "niue",
            "norfolk island",
            "north korea",
            "northern mariana islands",
            "norway",
            "oman",
            "pakistan",
            "palau",
            "palestine",
            "panama",
            "paraguay",
            "peru",
            "philippines",
            "pitcairn islands",
            "poland",
            "portugal",
            "puerto rico",
            "qatar",
            "reunion",
            "romania",
            "russia",
            "rwanda",
            "saint helena",
            "saint kitts and nevis",
            "saint lucia",
            "saint pierre",
            "saint vincent",
            "samoa",
            "san marino",
            "sandwich islands",
            "sao tome",
            "saudi arabia",
            "scotland",
            "senegal",
            "serbia",
            "seychelles",
            "sierra leone",
            "singapore",
            "slovakia",
            "slovenia",
            "solomon islands",
            "somalia",
            "south africa",
            "south korea",
            "spain",
            "sri lanka",
            "sudan",
            "suriname",
            "swaziland",
            "sweden",
            "switzerland",
            "syria",
            "taiwan",
            "tajikistan",
            "tanzania",
            "thailand",
            "timorleste",
            "togo",
            "tokelau",
            "tonga",
            "trinidad",
            "tunisia",
            "turkey",
            "turkmenistan",
            "tuvalu",
            "uae",
            "uganda",
            "ukraine",
            "united kingdom",
            "united states",
            "uruguay",
            "us minor islands",
            "us virgin islands",
            "uzbekistan",
            "vanuatu",
            "vatican city",
            "venezuela",
            "vietnam",
            "wales",
            "wallis and futuna",
            "western sahara",
            "yemen",
            "zambia",
            "zimbabwe",
        )

        fun cssClassOf(block: SemanticFlag.() -> SemanticFlag): String {
            return cssClassOfAsList(block).joinToString(" ")
        }

        fun cssClassOfAsList(block: SemanticFlag.() -> SemanticFlag): List<String> {
            lateinit var classes: List<String>

            createHTML().body {
                classes = flag.block().cssClasses.filter { it.isNotBlank() }
            }

            return classes
        }
    }

    private val cssClasses = mutableListOf<String>()

    private fun attrs() = attributesMapOf("class", cssClasses.joinToString(" ") + " flag")

    operator fun plus(cls: String): SemanticFlag = apply { cssClasses.add(cls) }

    operator fun plus(classes: Array<out String>): SemanticFlag = apply { cssClasses.addAll(classes) }

    fun render(block: I.() -> Unit = {}): Unit = I(attrs(), parent.consumer).visitNoInline(block)

    operator fun invoke(): Unit = render()

    operator fun invoke(block: I.() -> Unit): Unit = render(block)

    fun with(cls: String): SemanticFlag = this + cls

    fun given(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag = when (condition) {
        false -> this
        else -> this.action()
    }

    fun givenNot(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag = given(!condition, action)

    val then: SemanticFlag get() = this

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Flags ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    inline val afghanistan: SemanticFlag
        get() = this + "afghanistan"

    inline val aland_islands: SemanticFlag
        get() = this + "aland islands"

    inline val albania: SemanticFlag
        get() = this + "albania"

    inline val algeria: SemanticFlag
        get() = this + "algeria"

    inline val american_samoa: SemanticFlag
        get() = this + "american samoa"

    inline val andorra: SemanticFlag
        get() = this + "andorra"

    inline val angola: SemanticFlag
        get() = this + "angola"

    inline val anguilla: SemanticFlag
        get() = this + "anguilla"

    inline val antigua: SemanticFlag
        get() = this + "antigua"

    inline val argentina: SemanticFlag
        get() = this + "argentina"

    inline val armenia: SemanticFlag
        get() = this + "armenia"

    inline val aruba: SemanticFlag
        get() = this + "aruba"

    inline val australia: SemanticFlag
        get() = this + "australia"

    inline val austria: SemanticFlag
        get() = this + "austria"

    inline val azerbaijan: SemanticFlag
        get() = this + "azerbaijan"

    inline val bahamas: SemanticFlag
        get() = this + "bahamas"

    inline val bahrain: SemanticFlag
        get() = this + "bahrain"

    inline val bangladesh: SemanticFlag
        get() = this + "bangladesh"

    inline val barbados: SemanticFlag
        get() = this + "barbados"

    inline val belarus: SemanticFlag
        get() = this + "belarus"

    inline val belgium: SemanticFlag
        get() = this + "belgium"

    inline val belize: SemanticFlag
        get() = this + "belize"

    inline val benin: SemanticFlag
        get() = this + "benin"

    inline val bermuda: SemanticFlag
        get() = this + "bermuda"

    inline val bhutan: SemanticFlag
        get() = this + "bhutan"

    inline val bolivia: SemanticFlag
        get() = this + "bolivia"

    inline val bosnia: SemanticFlag
        get() = this + "bosnia"

    inline val botswana: SemanticFlag
        get() = this + "botswana"

    inline val bouvet_island: SemanticFlag
        get() = this + "bouvet island"

    inline val brazil: SemanticFlag
        get() = this + "brazil"

    inline val british_virgin_islands: SemanticFlag
        get() = this + "british virgin islands"

    inline val brunei: SemanticFlag
        get() = this + "brunei"

    inline val bulgaria: SemanticFlag
        get() = this + "bulgaria"

    inline val burkina_faso: SemanticFlag
        get() = this + "burkina faso"

    inline val burma: SemanticFlag
        get() = this + "burma"

    inline val burundi: SemanticFlag
        get() = this + "burundi"

    inline val caicos_islands: SemanticFlag
        get() = this + "caicos islands"

    inline val cambodia: SemanticFlag
        get() = this + "cambodia"

    inline val cameroon: SemanticFlag
        get() = this + "cameroon"

    inline val canada: SemanticFlag
        get() = this + "canada"

    inline val cape_verde: SemanticFlag
        get() = this + "cape verde"

    inline val cayman_islands: SemanticFlag
        get() = this + "cayman islands"

    inline val central_african_republic: SemanticFlag
        get() = this + "central african republic"

    inline val chad: SemanticFlag
        get() = this + "chad"

    inline val chile: SemanticFlag
        get() = this + "chile"

    inline val china: SemanticFlag
        get() = this + "china"

    inline val christmas_island: SemanticFlag
        get() = this + "christmas island"

    inline val cocos_islands: SemanticFlag
        get() = this + "cocos islands"

    inline val colombia: SemanticFlag
        get() = this + "colombia"

    inline val comoros: SemanticFlag
        get() = this + "comoros"

    inline val congo: SemanticFlag
        get() = this + "congo"

    inline val congo_brazzaville: SemanticFlag
        get() = this + "congo brazzaville"

    inline val cook_islands: SemanticFlag
        get() = this + "cook islands"

    inline val costa_rica: SemanticFlag
        get() = this + "costa rica"

    inline val cote_divoire: SemanticFlag
        get() = this + "cote divoire"

    inline val croatia: SemanticFlag
        get() = this + "croatia"

    inline val cuba: SemanticFlag
        get() = this + "cuba"

    inline val cyprus: SemanticFlag
        get() = this + "cyprus"

    inline val czech_republic: SemanticFlag
        get() = this + "czech republic"

    inline val denmark: SemanticFlag
        get() = this + "denmark"

    inline val djibouti: SemanticFlag
        get() = this + "djibouti"

    inline val dominica: SemanticFlag
        get() = this + "dominica"

    inline val dominican_republic: SemanticFlag
        get() = this + "dominican republic"

    inline val ecuador: SemanticFlag
        get() = this + "ecuador"

    inline val egypt: SemanticFlag
        get() = this + "egypt"

    inline val el_salvador: SemanticFlag
        get() = this + "el salvador"

    inline val england: SemanticFlag
        get() = this + "england"

    inline val equatorial_guinea: SemanticFlag
        get() = this + "equatorial guinea"

    inline val eritrea: SemanticFlag
        get() = this + "eritrea"

    inline val estonia: SemanticFlag
        get() = this + "estonia"

    inline val ethiopia: SemanticFlag
        get() = this + "ethiopia"

    inline val european_union: SemanticFlag
        get() = this + "european union"

    inline val falkland_islands: SemanticFlag
        get() = this + "falkland islands"

    inline val faroe_islands: SemanticFlag
        get() = this + "faroe islands"

    inline val fiji: SemanticFlag
        get() = this + "fiji"

    inline val finland: SemanticFlag
        get() = this + "finland"

    inline val france: SemanticFlag
        get() = this + "france"

    inline val french_guiana: SemanticFlag
        get() = this + "french guiana"

    inline val french_polynesia: SemanticFlag
        get() = this + "french polynesia"

    inline val french_territories: SemanticFlag
        get() = this + "french territories"

    inline val gabon: SemanticFlag
        get() = this + "gabon"

    inline val gambia: SemanticFlag
        get() = this + "gambia"

    inline val georgia: SemanticFlag
        get() = this + "georgia"

    inline val germany: SemanticFlag
        get() = this + "germany"

    inline val ghana: SemanticFlag
        get() = this + "ghana"

    inline val gibraltar: SemanticFlag
        get() = this + "gibraltar"

    inline val greece: SemanticFlag
        get() = this + "greece"

    inline val greenland: SemanticFlag
        get() = this + "greenland"

    inline val grenada: SemanticFlag
        get() = this + "grenada"

    inline val guadeloupe: SemanticFlag
        get() = this + "guadeloupe"

    inline val guam: SemanticFlag
        get() = this + "guam"

    inline val guatemala: SemanticFlag
        get() = this + "guatemala"

    inline val guinea: SemanticFlag
        get() = this + "guinea"

    inline val guineabissau: SemanticFlag
        get() = this + "guinea-bissau"

    inline val guyana: SemanticFlag
        get() = this + "guyana"

    inline val haiti: SemanticFlag
        get() = this + "haiti"

    inline val heard_island: SemanticFlag
        get() = this + "heard island"

    inline val honduras: SemanticFlag
        get() = this + "honduras"

    inline val hong_kong: SemanticFlag
        get() = this + "hong kong"

    inline val hungary: SemanticFlag
        get() = this + "hungary"

    inline val iceland: SemanticFlag
        get() = this + "iceland"

    inline val india: SemanticFlag
        get() = this + "india"

    inline val indian_ocean_territory: SemanticFlag
        get() = this + "indian ocean territory"

    inline val indonesia: SemanticFlag
        get() = this + "indonesia"

    inline val iran: SemanticFlag
        get() = this + "iran"

    inline val iraq: SemanticFlag
        get() = this + "iraq"

    inline val ireland: SemanticFlag
        get() = this + "ireland"

    inline val israel: SemanticFlag
        get() = this + "israel"

    inline val italy: SemanticFlag
        get() = this + "italy"

    inline val jamaica: SemanticFlag
        get() = this + "jamaica"

    inline val jan_mayen: SemanticFlag
        get() = this + "jan mayen"

    inline val japan: SemanticFlag
        get() = this + "japan"

    inline val jordan: SemanticFlag
        get() = this + "jordan"

    inline val kazakhstan: SemanticFlag
        get() = this + "kazakhstan"

    inline val kenya: SemanticFlag
        get() = this + "kenya"

    inline val kiribati: SemanticFlag
        get() = this + "kiribati"

    inline val kuwait: SemanticFlag
        get() = this + "kuwait"

    inline val kyrgyzstan: SemanticFlag
        get() = this + "kyrgyzstan"

    inline val laos: SemanticFlag
        get() = this + "laos"

    inline val latvia: SemanticFlag
        get() = this + "latvia"

    inline val lebanon: SemanticFlag
        get() = this + "lebanon"

    inline val lesotho: SemanticFlag
        get() = this + "lesotho"

    inline val liberia: SemanticFlag
        get() = this + "liberia"

    inline val libya: SemanticFlag
        get() = this + "libya"

    inline val liechtenstein: SemanticFlag
        get() = this + "liechtenstein"

    inline val lithuania: SemanticFlag
        get() = this + "lithuania"

    inline val luxembourg: SemanticFlag
        get() = this + "luxembourg"

    inline val macau: SemanticFlag
        get() = this + "macau"

    inline val macedonia: SemanticFlag
        get() = this + "macedonia"

    inline val madagascar: SemanticFlag
        get() = this + "madagascar"

    inline val malawi: SemanticFlag
        get() = this + "malawi"

    inline val malaysia: SemanticFlag
        get() = this + "malaysia"

    inline val maldives: SemanticFlag
        get() = this + "maldives"

    inline val mali: SemanticFlag
        get() = this + "mali"

    inline val malta: SemanticFlag
        get() = this + "malta"

    inline val marshall_islands: SemanticFlag
        get() = this + "marshall islands"

    inline val martinique: SemanticFlag
        get() = this + "martinique"

    inline val mauritania: SemanticFlag
        get() = this + "mauritania"

    inline val mauritius: SemanticFlag
        get() = this + "mauritius"

    inline val mayotte: SemanticFlag
        get() = this + "mayotte"

    inline val mexico: SemanticFlag
        get() = this + "mexico"

    inline val micronesia: SemanticFlag
        get() = this + "micronesia"

    inline val moldova: SemanticFlag
        get() = this + "moldova"

    inline val monaco: SemanticFlag
        get() = this + "monaco"

    inline val mongolia: SemanticFlag
        get() = this + "mongolia"

    inline val montenegro: SemanticFlag
        get() = this + "montenegro"

    inline val montserrat: SemanticFlag
        get() = this + "montserrat"

    inline val morocco: SemanticFlag
        get() = this + "morocco"

    inline val mozambique: SemanticFlag
        get() = this + "mozambique"

    inline val namibia: SemanticFlag
        get() = this + "namibia"

    inline val nauru: SemanticFlag
        get() = this + "nauru"

    inline val nepal: SemanticFlag
        get() = this + "nepal"

    inline val netherlands: SemanticFlag
        get() = this + "netherlands"

    inline val netherlands_antilles: SemanticFlag
        get() = this + "netherlands antilles"

    inline val new_caledonia: SemanticFlag
        get() = this + "new caledonia"

    inline val new_guinea: SemanticFlag
        get() = this + "new guinea"

    inline val new_zealand: SemanticFlag
        get() = this + "new zealand"

    inline val nicaragua: SemanticFlag
        get() = this + "nicaragua"

    inline val niger: SemanticFlag
        get() = this + "niger"

    inline val nigeria: SemanticFlag
        get() = this + "nigeria"

    inline val niue: SemanticFlag
        get() = this + "niue"

    inline val norfolk_island: SemanticFlag
        get() = this + "norfolk island"

    inline val north_korea: SemanticFlag
        get() = this + "north korea"

    inline val northern_mariana_islands: SemanticFlag
        get() = this + "northern mariana islands"

    inline val norway: SemanticFlag
        get() = this + "norway"

    inline val oman: SemanticFlag
        get() = this + "oman"

    inline val pakistan: SemanticFlag
        get() = this + "pakistan"

    inline val palau: SemanticFlag
        get() = this + "palau"

    inline val palestine: SemanticFlag
        get() = this + "palestine"

    inline val panama: SemanticFlag
        get() = this + "panama"

    inline val paraguay: SemanticFlag
        get() = this + "paraguay"

    inline val peru: SemanticFlag
        get() = this + "peru"

    inline val philippines: SemanticFlag
        get() = this + "philippines"

    inline val pitcairn_islands: SemanticFlag
        get() = this + "pitcairn islands"

    inline val poland: SemanticFlag
        get() = this + "poland"

    inline val portugal: SemanticFlag
        get() = this + "portugal"

    inline val puerto_rico: SemanticFlag
        get() = this + "puerto rico"

    inline val qatar: SemanticFlag
        get() = this + "qatar"

    inline val reunion: SemanticFlag
        get() = this + "reunion"

    inline val romania: SemanticFlag
        get() = this + "romania"

    inline val russia: SemanticFlag
        get() = this + "russia"

    inline val rwanda: SemanticFlag
        get() = this + "rwanda"

    inline val saint_helena: SemanticFlag
        get() = this + "saint helena"

    inline val saint_kitts_and_nevis: SemanticFlag
        get() = this + "saint kitts and nevis"

    inline val saint_lucia: SemanticFlag
        get() = this + "saint lucia"

    inline val saint_pierre: SemanticFlag
        get() = this + "saint pierre"

    inline val saint_vincent: SemanticFlag
        get() = this + "saint vincent"

    inline val samoa: SemanticFlag
        get() = this + "samoa"

    inline val san_marino: SemanticFlag
        get() = this + "san marino"

    inline val sandwich_islands: SemanticFlag
        get() = this + "sandwich islands"

    inline val sao_tome: SemanticFlag
        get() = this + "sao tome"

    inline val saudi_arabia: SemanticFlag
        get() = this + "saudi arabia"

    inline val scotland: SemanticFlag
        get() = this + "scotland"

    inline val senegal: SemanticFlag
        get() = this + "senegal"

    inline val serbia: SemanticFlag
        get() = this + "serbia"

    inline val seychelles: SemanticFlag
        get() = this + "seychelles"

    inline val sierra_leone: SemanticFlag
        get() = this + "sierra leone"

    inline val singapore: SemanticFlag
        get() = this + "singapore"

    inline val slovakia: SemanticFlag
        get() = this + "slovakia"

    inline val slovenia: SemanticFlag
        get() = this + "slovenia"

    inline val solomon_islands: SemanticFlag
        get() = this + "solomon islands"

    inline val somalia: SemanticFlag
        get() = this + "somalia"

    inline val south_africa: SemanticFlag
        get() = this + "south africa"

    inline val south_korea: SemanticFlag
        get() = this + "south korea"

    inline val spain: SemanticFlag
        get() = this + "spain"

    inline val sri_lanka: SemanticFlag
        get() = this + "sri lanka"

    inline val sudan: SemanticFlag
        get() = this + "sudan"

    inline val suriname: SemanticFlag
        get() = this + "suriname"

    inline val swaziland: SemanticFlag
        get() = this + "swaziland"

    inline val sweden: SemanticFlag
        get() = this + "sweden"

    inline val switzerland: SemanticFlag
        get() = this + "switzerland"

    inline val syria: SemanticFlag
        get() = this + "syria"

    inline val taiwan: SemanticFlag
        get() = this + "taiwan"

    inline val tajikistan: SemanticFlag
        get() = this + "tajikistan"

    inline val tanzania: SemanticFlag
        get() = this + "tanzania"

    inline val thailand: SemanticFlag
        get() = this + "thailand"

    inline val timorleste: SemanticFlag
        get() = this + "timorleste"

    inline val togo: SemanticFlag
        get() = this + "togo"

    inline val tokelau: SemanticFlag
        get() = this + "tokelau"

    inline val tonga: SemanticFlag
        get() = this + "tonga"

    inline val trinidad: SemanticFlag
        get() = this + "trinidad"

    inline val tunisia: SemanticFlag
        get() = this + "tunisia"

    inline val turkey: SemanticFlag
        get() = this + "turkey"

    inline val turkmenistan: SemanticFlag
        get() = this + "turkmenistan"

    inline val tuvalu: SemanticFlag
        get() = this + "tuvalu"

    inline val uae: SemanticFlag
        get() = this + "uae"

    inline val uganda: SemanticFlag
        get() = this + "uganda"

    inline val ukraine: SemanticFlag
        get() = this + "ukraine"

    inline val united_kingdom: SemanticFlag
        get() = this + "united kingdom"

    inline val united_states: SemanticFlag
        get() = this + "united states"

    inline val uruguay: SemanticFlag
        get() = this + "uruguay"

    inline val us_minor_islands: SemanticFlag
        get() = this + "us minor islands"

    inline val us_virgin_islands: SemanticFlag
        get() = this + "us virgin islands"

    inline val uzbekistan: SemanticFlag
        get() = this + "uzbekistan"

    inline val vanuatu: SemanticFlag
        get() = this + "vanuatu"

    inline val vatican_city: SemanticFlag
        get() = this + "vatican city"

    inline val venezuela: SemanticFlag
        get() = this + "venezuela"

    inline val vietnam: SemanticFlag
        get() = this + "vietnam"

    inline val wales: SemanticFlag
        get() = this + "wales"

    inline val wallis_and_futuna: SemanticFlag
        get() = this + "wallis and futuna"

    inline val western_sahara: SemanticFlag
        get() = this + "western sahara"

    inline val yemen: SemanticFlag
        get() = this + "yemen"

    inline val zambia: SemanticFlag
        get() = this + "zambia"

    inline val zimbabwe: SemanticFlag
        get() = this + "zimbabwe"
}
