@file:Suppress(
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
    "Detekt:VariableNaming",
)

package de.peekandpoke.kraft.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.attributesMapOf
import kotlinx.html.body
import kotlinx.html.stream.createHTML

@Suppress("unused", "PropertyName")
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

    @SemanticUiCssMarker
    fun with(cls: String): SemanticFlag = this + cls

    @SemanticUiConditionalMarker
    fun given(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag = when (condition) {
        false -> this
        else -> this.action()
    }

    @SemanticUiConditionalMarker
    fun givenNot(condition: Boolean, action: SemanticFlag.() -> SemanticFlag): SemanticFlag = given(!condition, action)

    @SemanticUiConditionalMarker val then: SemanticFlag get() = this

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Flags ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticFlagMarker inline val afghanistan: SemanticFlag
        get() = this + "afghanistan"

    @SemanticFlagMarker inline val aland_islands: SemanticFlag
        get() = this + "aland islands"

    @SemanticFlagMarker inline val albania: SemanticFlag
        get() = this + "albania"

    @SemanticFlagMarker inline val algeria: SemanticFlag
        get() = this + "algeria"

    @SemanticFlagMarker inline val american_samoa: SemanticFlag
        get() = this + "american samoa"

    @SemanticFlagMarker inline val andorra: SemanticFlag
        get() = this + "andorra"

    @SemanticFlagMarker inline val angola: SemanticFlag
        get() = this + "angola"

    @SemanticFlagMarker inline val anguilla: SemanticFlag
        get() = this + "anguilla"

    @SemanticFlagMarker inline val antigua: SemanticFlag
        get() = this + "antigua"

    @SemanticFlagMarker inline val argentina: SemanticFlag
        get() = this + "argentina"

    @SemanticFlagMarker inline val armenia: SemanticFlag
        get() = this + "armenia"

    @SemanticFlagMarker inline val aruba: SemanticFlag
        get() = this + "aruba"

    @SemanticFlagMarker inline val australia: SemanticFlag
        get() = this + "australia"

    @SemanticFlagMarker inline val austria: SemanticFlag
        get() = this + "austria"

    @SemanticFlagMarker inline val azerbaijan: SemanticFlag
        get() = this + "azerbaijan"

    @SemanticFlagMarker inline val bahamas: SemanticFlag
        get() = this + "bahamas"

    @SemanticFlagMarker inline val bahrain: SemanticFlag
        get() = this + "bahrain"

    @SemanticFlagMarker inline val bangladesh: SemanticFlag
        get() = this + "bangladesh"

    @SemanticFlagMarker inline val barbados: SemanticFlag
        get() = this + "barbados"

    @SemanticFlagMarker inline val belarus: SemanticFlag
        get() = this + "belarus"

    @SemanticFlagMarker inline val belgium: SemanticFlag
        get() = this + "belgium"

    @SemanticFlagMarker inline val belize: SemanticFlag
        get() = this + "belize"

    @SemanticFlagMarker inline val benin: SemanticFlag
        get() = this + "benin"

    @SemanticFlagMarker inline val bermuda: SemanticFlag
        get() = this + "bermuda"

    @SemanticFlagMarker inline val bhutan: SemanticFlag
        get() = this + "bhutan"

    @SemanticFlagMarker inline val bolivia: SemanticFlag
        get() = this + "bolivia"

    @SemanticFlagMarker inline val bosnia: SemanticFlag
        get() = this + "bosnia"

    @SemanticFlagMarker inline val botswana: SemanticFlag
        get() = this + "botswana"

    @SemanticFlagMarker inline val bouvet_island: SemanticFlag
        get() = this + "bouvet island"

    @SemanticFlagMarker inline val brazil: SemanticFlag
        get() = this + "brazil"

    @SemanticFlagMarker inline val british_virgin_islands: SemanticFlag
        get() = this + "british virgin islands"

    @SemanticFlagMarker inline val brunei: SemanticFlag
        get() = this + "brunei"

    @SemanticFlagMarker inline val bulgaria: SemanticFlag
        get() = this + "bulgaria"

    @SemanticFlagMarker inline val burkina_faso: SemanticFlag
        get() = this + "burkina faso"

    @SemanticFlagMarker inline val burma: SemanticFlag
        get() = this + "burma"

    @SemanticFlagMarker inline val burundi: SemanticFlag
        get() = this + "burundi"

    @SemanticFlagMarker inline val caicos_islands: SemanticFlag
        get() = this + "caicos islands"

    @SemanticFlagMarker inline val cambodia: SemanticFlag
        get() = this + "cambodia"

    @SemanticFlagMarker inline val cameroon: SemanticFlag
        get() = this + "cameroon"

    @SemanticFlagMarker inline val canada: SemanticFlag
        get() = this + "canada"

    @SemanticFlagMarker inline val cape_verde: SemanticFlag
        get() = this + "cape verde"

    @SemanticFlagMarker inline val cayman_islands: SemanticFlag
        get() = this + "cayman islands"

    @SemanticFlagMarker inline val central_african_republic: SemanticFlag
        get() = this + "central african republic"

    @SemanticFlagMarker inline val chad: SemanticFlag
        get() = this + "chad"

    @SemanticFlagMarker inline val chile: SemanticFlag
        get() = this + "chile"

    @SemanticFlagMarker inline val china: SemanticFlag
        get() = this + "china"

    @SemanticFlagMarker inline val christmas_island: SemanticFlag
        get() = this + "christmas island"

    @SemanticFlagMarker inline val cocos_islands: SemanticFlag
        get() = this + "cocos islands"

    @SemanticFlagMarker inline val colombia: SemanticFlag
        get() = this + "colombia"

    @SemanticFlagMarker inline val comoros: SemanticFlag
        get() = this + "comoros"

    @SemanticFlagMarker inline val congo: SemanticFlag
        get() = this + "congo"

    @SemanticFlagMarker inline val congo_brazzaville: SemanticFlag
        get() = this + "congo brazzaville"

    @SemanticFlagMarker inline val cook_islands: SemanticFlag
        get() = this + "cook islands"

    @SemanticFlagMarker inline val costa_rica: SemanticFlag
        get() = this + "costa rica"

    @SemanticFlagMarker inline val cote_divoire: SemanticFlag
        get() = this + "cote divoire"

    @SemanticFlagMarker inline val croatia: SemanticFlag
        get() = this + "croatia"

    @SemanticFlagMarker inline val cuba: SemanticFlag
        get() = this + "cuba"

    @SemanticFlagMarker inline val cyprus: SemanticFlag
        get() = this + "cyprus"

    @SemanticFlagMarker inline val czech_republic: SemanticFlag
        get() = this + "czech republic"

    @SemanticFlagMarker inline val denmark: SemanticFlag
        get() = this + "denmark"

    @SemanticFlagMarker inline val djibouti: SemanticFlag
        get() = this + "djibouti"

    @SemanticFlagMarker inline val dominica: SemanticFlag
        get() = this + "dominica"

    @SemanticFlagMarker inline val dominican_republic: SemanticFlag
        get() = this + "dominican republic"

    @SemanticFlagMarker inline val ecuador: SemanticFlag
        get() = this + "ecuador"

    @SemanticFlagMarker inline val egypt: SemanticFlag
        get() = this + "egypt"

    @SemanticFlagMarker inline val el_salvador: SemanticFlag
        get() = this + "el salvador"

    @SemanticFlagMarker inline val england: SemanticFlag
        get() = this + "england"

    @SemanticFlagMarker inline val equatorial_guinea: SemanticFlag
        get() = this + "equatorial guinea"

    @SemanticFlagMarker inline val eritrea: SemanticFlag
        get() = this + "eritrea"

    @SemanticFlagMarker inline val estonia: SemanticFlag
        get() = this + "estonia"

    @SemanticFlagMarker inline val ethiopia: SemanticFlag
        get() = this + "ethiopia"

    @SemanticFlagMarker inline val european_union: SemanticFlag
        get() = this + "european union"

    @SemanticFlagMarker inline val falkland_islands: SemanticFlag
        get() = this + "falkland islands"

    @SemanticFlagMarker inline val faroe_islands: SemanticFlag
        get() = this + "faroe islands"

    @SemanticFlagMarker inline val fiji: SemanticFlag
        get() = this + "fiji"

    @SemanticFlagMarker inline val finland: SemanticFlag
        get() = this + "finland"

    @SemanticFlagMarker inline val france: SemanticFlag
        get() = this + "france"

    @SemanticFlagMarker inline val french_guiana: SemanticFlag
        get() = this + "french guiana"

    @SemanticFlagMarker inline val french_polynesia: SemanticFlag
        get() = this + "french polynesia"

    @SemanticFlagMarker inline val french_territories: SemanticFlag
        get() = this + "french territories"

    @SemanticFlagMarker inline val gabon: SemanticFlag
        get() = this + "gabon"

    @SemanticFlagMarker inline val gambia: SemanticFlag
        get() = this + "gambia"

    @SemanticFlagMarker inline val georgia: SemanticFlag
        get() = this + "georgia"

    @SemanticFlagMarker inline val germany: SemanticFlag
        get() = this + "germany"

    @SemanticFlagMarker inline val ghana: SemanticFlag
        get() = this + "ghana"

    @SemanticFlagMarker inline val gibraltar: SemanticFlag
        get() = this + "gibraltar"

    @SemanticFlagMarker inline val greece: SemanticFlag
        get() = this + "greece"

    @SemanticFlagMarker inline val greenland: SemanticFlag
        get() = this + "greenland"

    @SemanticFlagMarker inline val grenada: SemanticFlag
        get() = this + "grenada"

    @SemanticFlagMarker inline val guadeloupe: SemanticFlag
        get() = this + "guadeloupe"

    @SemanticFlagMarker inline val guam: SemanticFlag
        get() = this + "guam"

    @SemanticFlagMarker inline val guatemala: SemanticFlag
        get() = this + "guatemala"

    @SemanticFlagMarker inline val guinea: SemanticFlag
        get() = this + "guinea"

    @SemanticFlagMarker inline val guineabissau: SemanticFlag
        get() = this + "guinea-bissau"

    @SemanticFlagMarker inline val guyana: SemanticFlag
        get() = this + "guyana"

    @SemanticFlagMarker inline val haiti: SemanticFlag
        get() = this + "haiti"

    @SemanticFlagMarker inline val heard_island: SemanticFlag
        get() = this + "heard island"

    @SemanticFlagMarker inline val honduras: SemanticFlag
        get() = this + "honduras"

    @SemanticFlagMarker inline val hong_kong: SemanticFlag
        get() = this + "hong kong"

    @SemanticFlagMarker inline val hungary: SemanticFlag
        get() = this + "hungary"

    @SemanticFlagMarker inline val iceland: SemanticFlag
        get() = this + "iceland"

    @SemanticFlagMarker inline val india: SemanticFlag
        get() = this + "india"

    @SemanticFlagMarker inline val indian_ocean_territory: SemanticFlag
        get() = this + "indian ocean territory"

    @SemanticFlagMarker inline val indonesia: SemanticFlag
        get() = this + "indonesia"

    @SemanticFlagMarker inline val iran: SemanticFlag
        get() = this + "iran"

    @SemanticFlagMarker inline val iraq: SemanticFlag
        get() = this + "iraq"

    @SemanticFlagMarker inline val ireland: SemanticFlag
        get() = this + "ireland"

    @SemanticFlagMarker inline val israel: SemanticFlag
        get() = this + "israel"

    @SemanticFlagMarker inline val italy: SemanticFlag
        get() = this + "italy"

    @SemanticFlagMarker inline val jamaica: SemanticFlag
        get() = this + "jamaica"

    @SemanticFlagMarker inline val jan_mayen: SemanticFlag
        get() = this + "jan mayen"

    @SemanticFlagMarker inline val japan: SemanticFlag
        get() = this + "japan"

    @SemanticFlagMarker inline val jordan: SemanticFlag
        get() = this + "jordan"

    @SemanticFlagMarker inline val kazakhstan: SemanticFlag
        get() = this + "kazakhstan"

    @SemanticFlagMarker inline val kenya: SemanticFlag
        get() = this + "kenya"

    @SemanticFlagMarker inline val kiribati: SemanticFlag
        get() = this + "kiribati"

    @SemanticFlagMarker inline val kuwait: SemanticFlag
        get() = this + "kuwait"

    @SemanticFlagMarker inline val kyrgyzstan: SemanticFlag
        get() = this + "kyrgyzstan"

    @SemanticFlagMarker inline val laos: SemanticFlag
        get() = this + "laos"

    @SemanticFlagMarker inline val latvia: SemanticFlag
        get() = this + "latvia"

    @SemanticFlagMarker inline val lebanon: SemanticFlag
        get() = this + "lebanon"

    @SemanticFlagMarker inline val lesotho: SemanticFlag
        get() = this + "lesotho"

    @SemanticFlagMarker inline val liberia: SemanticFlag
        get() = this + "liberia"

    @SemanticFlagMarker inline val libya: SemanticFlag
        get() = this + "libya"

    @SemanticFlagMarker inline val liechtenstein: SemanticFlag
        get() = this + "liechtenstein"

    @SemanticFlagMarker inline val lithuania: SemanticFlag
        get() = this + "lithuania"

    @SemanticFlagMarker inline val luxembourg: SemanticFlag
        get() = this + "luxembourg"

    @SemanticFlagMarker inline val macau: SemanticFlag
        get() = this + "macau"

    @SemanticFlagMarker inline val macedonia: SemanticFlag
        get() = this + "macedonia"

    @SemanticFlagMarker inline val madagascar: SemanticFlag
        get() = this + "madagascar"

    @SemanticFlagMarker inline val malawi: SemanticFlag
        get() = this + "malawi"

    @SemanticFlagMarker inline val malaysia: SemanticFlag
        get() = this + "malaysia"

    @SemanticFlagMarker inline val maldives: SemanticFlag
        get() = this + "maldives"

    @SemanticFlagMarker inline val mali: SemanticFlag
        get() = this + "mali"

    @SemanticFlagMarker inline val malta: SemanticFlag
        get() = this + "malta"

    @SemanticFlagMarker inline val marshall_islands: SemanticFlag
        get() = this + "marshall islands"

    @SemanticFlagMarker inline val martinique: SemanticFlag
        get() = this + "martinique"

    @SemanticFlagMarker inline val mauritania: SemanticFlag
        get() = this + "mauritania"

    @SemanticFlagMarker inline val mauritius: SemanticFlag
        get() = this + "mauritius"

    @SemanticFlagMarker inline val mayotte: SemanticFlag
        get() = this + "mayotte"

    @SemanticFlagMarker inline val mexico: SemanticFlag
        get() = this + "mexico"

    @SemanticFlagMarker inline val micronesia: SemanticFlag
        get() = this + "micronesia"

    @SemanticFlagMarker inline val moldova: SemanticFlag
        get() = this + "moldova"

    @SemanticFlagMarker inline val monaco: SemanticFlag
        get() = this + "monaco"

    @SemanticFlagMarker inline val mongolia: SemanticFlag
        get() = this + "mongolia"

    @SemanticFlagMarker inline val montenegro: SemanticFlag
        get() = this + "montenegro"

    @SemanticFlagMarker inline val montserrat: SemanticFlag
        get() = this + "montserrat"

    @SemanticFlagMarker inline val morocco: SemanticFlag
        get() = this + "morocco"

    @SemanticFlagMarker inline val mozambique: SemanticFlag
        get() = this + "mozambique"

    @SemanticFlagMarker inline val namibia: SemanticFlag
        get() = this + "namibia"

    @SemanticFlagMarker inline val nauru: SemanticFlag
        get() = this + "nauru"

    @SemanticFlagMarker inline val nepal: SemanticFlag
        get() = this + "nepal"

    @SemanticFlagMarker inline val netherlands: SemanticFlag
        get() = this + "netherlands"

    @SemanticFlagMarker inline val netherlands_antilles: SemanticFlag
        get() = this + "netherlands antilles"

    @SemanticFlagMarker inline val new_caledonia: SemanticFlag
        get() = this + "new caledonia"

    @SemanticFlagMarker inline val new_guinea: SemanticFlag
        get() = this + "new guinea"

    @SemanticFlagMarker inline val new_zealand: SemanticFlag
        get() = this + "new zealand"

    @SemanticFlagMarker inline val nicaragua: SemanticFlag
        get() = this + "nicaragua"

    @SemanticFlagMarker inline val niger: SemanticFlag
        get() = this + "niger"

    @SemanticFlagMarker inline val nigeria: SemanticFlag
        get() = this + "nigeria"

    @SemanticFlagMarker inline val niue: SemanticFlag
        get() = this + "niue"

    @SemanticFlagMarker inline val norfolk_island: SemanticFlag
        get() = this + "norfolk island"

    @SemanticFlagMarker inline val north_korea: SemanticFlag
        get() = this + "north korea"

    @SemanticFlagMarker inline val northern_mariana_islands: SemanticFlag
        get() = this + "northern mariana islands"

    @SemanticFlagMarker inline val norway: SemanticFlag
        get() = this + "norway"

    @SemanticFlagMarker inline val oman: SemanticFlag
        get() = this + "oman"

    @SemanticFlagMarker inline val pakistan: SemanticFlag
        get() = this + "pakistan"

    @SemanticFlagMarker inline val palau: SemanticFlag
        get() = this + "palau"

    @SemanticFlagMarker inline val palestine: SemanticFlag
        get() = this + "palestine"

    @SemanticFlagMarker inline val panama: SemanticFlag
        get() = this + "panama"

    @SemanticFlagMarker inline val paraguay: SemanticFlag
        get() = this + "paraguay"

    @SemanticFlagMarker inline val peru: SemanticFlag
        get() = this + "peru"

    @SemanticFlagMarker inline val philippines: SemanticFlag
        get() = this + "philippines"

    @SemanticFlagMarker inline val pitcairn_islands: SemanticFlag
        get() = this + "pitcairn islands"

    @SemanticFlagMarker inline val poland: SemanticFlag
        get() = this + "poland"

    @SemanticFlagMarker inline val portugal: SemanticFlag
        get() = this + "portugal"

    @SemanticFlagMarker inline val puerto_rico: SemanticFlag
        get() = this + "puerto rico"

    @SemanticFlagMarker inline val qatar: SemanticFlag
        get() = this + "qatar"

    @SemanticFlagMarker inline val reunion: SemanticFlag
        get() = this + "reunion"

    @SemanticFlagMarker inline val romania: SemanticFlag
        get() = this + "romania"

    @SemanticFlagMarker inline val russia: SemanticFlag
        get() = this + "russia"

    @SemanticFlagMarker inline val rwanda: SemanticFlag
        get() = this + "rwanda"

    @SemanticFlagMarker inline val saint_helena: SemanticFlag
        get() = this + "saint helena"

    @SemanticFlagMarker inline val saint_kitts_and_nevis: SemanticFlag
        get() = this + "saint kitts and nevis"

    @SemanticFlagMarker inline val saint_lucia: SemanticFlag
        get() = this + "saint lucia"

    @SemanticFlagMarker inline val saint_pierre: SemanticFlag
        get() = this + "saint pierre"

    @SemanticFlagMarker inline val saint_vincent: SemanticFlag
        get() = this + "saint vincent"

    @SemanticFlagMarker inline val samoa: SemanticFlag
        get() = this + "samoa"

    @SemanticFlagMarker inline val san_marino: SemanticFlag
        get() = this + "san marino"

    @SemanticFlagMarker inline val sandwich_islands: SemanticFlag
        get() = this + "sandwich islands"

    @SemanticFlagMarker inline val sao_tome: SemanticFlag
        get() = this + "sao tome"

    @SemanticFlagMarker inline val saudi_arabia: SemanticFlag
        get() = this + "saudi arabia"

    @SemanticFlagMarker inline val scotland: SemanticFlag
        get() = this + "scotland"

    @SemanticFlagMarker inline val senegal: SemanticFlag
        get() = this + "senegal"

    @SemanticFlagMarker inline val serbia: SemanticFlag
        get() = this + "serbia"

    @SemanticFlagMarker inline val seychelles: SemanticFlag
        get() = this + "seychelles"

    @SemanticFlagMarker inline val sierra_leone: SemanticFlag
        get() = this + "sierra leone"

    @SemanticFlagMarker inline val singapore: SemanticFlag
        get() = this + "singapore"

    @SemanticFlagMarker inline val slovakia: SemanticFlag
        get() = this + "slovakia"

    @SemanticFlagMarker inline val slovenia: SemanticFlag
        get() = this + "slovenia"

    @SemanticFlagMarker inline val solomon_islands: SemanticFlag
        get() = this + "solomon islands"

    @SemanticFlagMarker inline val somalia: SemanticFlag
        get() = this + "somalia"

    @SemanticFlagMarker inline val south_africa: SemanticFlag
        get() = this + "south africa"

    @SemanticFlagMarker inline val south_korea: SemanticFlag
        get() = this + "south korea"

    @SemanticFlagMarker inline val spain: SemanticFlag
        get() = this + "spain"

    @SemanticFlagMarker inline val sri_lanka: SemanticFlag
        get() = this + "sri lanka"

    @SemanticFlagMarker inline val sudan: SemanticFlag
        get() = this + "sudan"

    @SemanticFlagMarker inline val suriname: SemanticFlag
        get() = this + "suriname"

    @SemanticFlagMarker inline val swaziland: SemanticFlag
        get() = this + "swaziland"

    @SemanticFlagMarker inline val sweden: SemanticFlag
        get() = this + "sweden"

    @SemanticFlagMarker inline val switzerland: SemanticFlag
        get() = this + "switzerland"

    @SemanticFlagMarker inline val syria: SemanticFlag
        get() = this + "syria"

    @SemanticFlagMarker inline val taiwan: SemanticFlag
        get() = this + "taiwan"

    @SemanticFlagMarker inline val tajikistan: SemanticFlag
        get() = this + "tajikistan"

    @SemanticFlagMarker inline val tanzania: SemanticFlag
        get() = this + "tanzania"

    @SemanticFlagMarker inline val thailand: SemanticFlag
        get() = this + "thailand"

    @SemanticFlagMarker inline val timorleste: SemanticFlag
        get() = this + "timorleste"

    @SemanticFlagMarker inline val togo: SemanticFlag
        get() = this + "togo"

    @SemanticFlagMarker inline val tokelau: SemanticFlag
        get() = this + "tokelau"

    @SemanticFlagMarker inline val tonga: SemanticFlag
        get() = this + "tonga"

    @SemanticFlagMarker inline val trinidad: SemanticFlag
        get() = this + "trinidad"

    @SemanticFlagMarker inline val tunisia: SemanticFlag
        get() = this + "tunisia"

    @SemanticFlagMarker inline val turkey: SemanticFlag
        get() = this + "turkey"

    @SemanticFlagMarker inline val turkmenistan: SemanticFlag
        get() = this + "turkmenistan"

    @SemanticFlagMarker inline val tuvalu: SemanticFlag
        get() = this + "tuvalu"

    @SemanticFlagMarker inline val uae: SemanticFlag
        get() = this + "uae"

    @SemanticFlagMarker inline val uganda: SemanticFlag
        get() = this + "uganda"

    @SemanticFlagMarker inline val ukraine: SemanticFlag
        get() = this + "ukraine"

    @SemanticFlagMarker inline val united_kingdom: SemanticFlag
        get() = this + "united kingdom"

    @SemanticFlagMarker inline val united_states: SemanticFlag
        get() = this + "united states"

    @SemanticFlagMarker inline val uruguay: SemanticFlag
        get() = this + "uruguay"

    @SemanticFlagMarker inline val us_minor_islands: SemanticFlag
        get() = this + "us minor islands"

    @SemanticFlagMarker inline val us_virgin_islands: SemanticFlag
        get() = this + "us virgin islands"

    @SemanticFlagMarker inline val uzbekistan: SemanticFlag
        get() = this + "uzbekistan"

    @SemanticFlagMarker inline val vanuatu: SemanticFlag
        get() = this + "vanuatu"

    @SemanticFlagMarker inline val vatican_city: SemanticFlag
        get() = this + "vatican city"

    @SemanticFlagMarker inline val venezuela: SemanticFlag
        get() = this + "venezuela"

    @SemanticFlagMarker inline val vietnam: SemanticFlag
        get() = this + "vietnam"

    @SemanticFlagMarker inline val wales: SemanticFlag
        get() = this + "wales"

    @SemanticFlagMarker inline val wallis_and_futuna: SemanticFlag
        get() = this + "wallis and futuna"

    @SemanticFlagMarker inline val western_sahara: SemanticFlag
        get() = this + "western sahara"

    @SemanticFlagMarker inline val yemen: SemanticFlag
        get() = this + "yemen"

    @SemanticFlagMarker inline val zambia: SemanticFlag
        get() = this + "zambia"

    @SemanticFlagMarker inline val zimbabwe: SemanticFlag
        get() = this + "zimbabwe"
}
