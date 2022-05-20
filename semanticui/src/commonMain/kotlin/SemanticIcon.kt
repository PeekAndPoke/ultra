@file:Suppress("Detekt:LargeClass", "Detekt:VariableNaming")

package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.attributesMapOf
import kotlinx.html.body
import kotlinx.html.stream.createHTML
import kotlin.js.JsName

@Suppress("PropertyName", "FunctionName", "unused")
class SemanticIcon(private val parent: FlowContent) {

    companion object {
        val all = listOf(
            "500px",
            "accessible",
            "accusoft",
            "acquisitions incorporated",
            "ad",
            "address book outline",
            "address book",
            "address card outline",
            "address card",
            "adjust",
            "adn",
            "adobe",
            "adversal",
            "affiliatetheme",
            "air freshener",
            "airbnb",
            "algolia",
            "align center",
            "align justify",
            "align left",
            "align right",
            "alipay",
            "allergies",
            "alternate github",
            "amazon pay",
            "amazon",
            "ambulance",
            "american sign language interpreting",
            "amilia",
            "anchor",
            "android",
            "angellist",
            "angle double down",
            "angle double left",
            "angle double right",
            "angle double up",
            "angle down",
            "angle left",
            "angle right",
            "angle up",
            "angry outline",
            "angry",
            "angrycreative",
            "angular",
            "ankh",
            "app store ios",
            "app store",
            "apper",
            "apple pay",
            "apple",
            "archive",
            "archway",
            "arrow alternate circle down outline",
            "arrow alternate circle down",
            "arrow alternate circle left outline",
            "arrow alternate circle left",
            "arrow alternate circle right outline",
            "arrow alternate circle right",
            "arrow alternate circle up outline",
            "arrow alternate circle up",
            "arrow circle down",
            "arrow circle left",
            "arrow circle right",
            "arrow circle up",
            "arrow down",
            "arrow left",
            "arrow right",
            "arrow up",
            "arrows alternate horizontal",
            "arrows alternate vertical",
            "arrows alternate",
            "artstation",
            "assistive listening systems",
            "asterisk loading",
            "asterisk",
            "asymmetrik",
            "at",
            "atlas",
            "atlassian",
            "atom",
            "audible",
            "audio description",
            "autoprefixer",
            "avianex",
            "aviato",
            "award",
            "aws",
            "baby carriage",
            "baby",
            "backward",
            "bacon",
            "bahai",
            "balance scale left",
            "balance scale right",
            "balance scale",
            "ban",
            "band aid",
            "bandcamp",
            "barcode",
            "bars",
            "baseball ball",
            "basketball ball",
            "bath",
            "battery empty",
            "battery full",
            "battery half",
            "battery quarter",
            "battery three quarters",
            "battle net",
            "bed",
            "beer",
            "behance square",
            "behance",
            "bell outline",
            "bell slash outline",
            "bell slash",
            "bell",
            "bezier curve",
            "bible",
            "bicycle",
            "big circle outline",
            "big home",
            "big red dont",
            "biking",
            "bimobject",
            "binoculars",
            "biohazard",
            "birthday cake",
            "bitbucket",
            "bitcoin",
            "bity",
            "black tie",
            "black user",
            "black users",
            "blackberry",
            "blender",
            "blind",
            "blog",
            "blogger b",
            "blogger",
            "blue users",
            "bluetooth b",
            "bluetooth",
            "bold",
            "bolt",
            "bomb",
            "bone",
            "bong",
            "book dead",
            "book medical",
            "book open",
            "book reader",
            "book",
            "bookmark outline",
            "bookmark",
            "bootstrap",
            "border all",
            "border none",
            "border style",
            "bordered colored blue users",
            "bordered colored green users",
            "bordered colored red users",
            "bordered inverted black users",
            "bordered inverted teal users",
            "bordered teal users",
            "bordered users",
            "bottom left corner add",
            "bottom right corner add",
            "bowling ball",
            "box open",
            "box tissue",
            "box",
            "boxes",
            "braille",
            "brain",
            "bread slice",
            "briefcase medical",
            "briefcase",
            "broadcast tower",
            "broom",
            "brown users",
            "brush",
            "btc",
            "buffer",
            "bug",
            "building outline",
            "building",
            "bullhorn",
            "bullseye",
            "burn",
            "buromobelexperte",
            "bus alternate",
            "bus",
            "business time",
            "buy n large",
            "buysellads",
            "calculator",
            "calendar alternate outline",
            "calendar alternate",
            "calendar check outline",
            "calendar check",
            "calendar day",
            "calendar minus outline",
            "calendar minus",
            "calendar outline",
            "calendar plus outline",
            "calendar plus",
            "calendar times outline",
            "calendar times",
            "calendar week",
            "calendar",
            "camera retro",
            "camera",
            "campground",
            "canadian maple leaf",
            "candy cane",
            "cannabis",
            "capsules",
            "car alternate",
            "car battery",
            "car crash",
            "car side",
            "car",
            "caravan",
            "caret down",
            "caret left",
            "caret right",
            "caret square down outline",
            "caret square down",
            "caret square left outline",
            "caret square left",
            "caret square right outline",
            "caret square right",
            "caret square up outline",
            "caret square up",
            "caret up",
            "carrot",
            "cart arrow down",
            "cart plus",
            "cash register",
            "cat",
            "cc amazon pay",
            "cc amex",
            "cc apple pay",
            "cc diners club",
            "cc discover",
            "cc jcb",
            "cc mastercard",
            "cc paypal",
            "cc stripe",
            "cc visa",
            "centercode",
            "centos",
            "certificate",
            "chair",
            "chalkboard teacher",
            "chalkboard",
            "charging station",
            "chart area",
            "chart bar outline",
            "chart bar",
            "chart line",
            "chart pie",
            "check circle outline",
            "check circle",
            "check double",
            "check square outline",
            "check square",
            "check",
            "cheese",
            "chess bishop",
            "chess board",
            "chess king",
            "chess knight",
            "chess pawn",
            "chess queen",
            "chess rook",
            "chess",
            "chevron circle down",
            "chevron circle left",
            "chevron circle right",
            "chevron circle up",
            "chevron down",
            "chevron left",
            "chevron right",
            "chevron up",
            "child",
            "chrome",
            "chromecast",
            "church",
            "circle notch",
            "circle outline",
            "circle",
            "circular colored blue users",
            "circular colored green users",
            "circular colored red users",
            "circular inverted teal users",
            "circular inverted users",
            "circular teal users",
            "circular users",
            "city",
            "clinic medical",
            "clipboard check",
            "clipboard list",
            "clipboard outline",
            "clipboard",
            "clock outline",
            "clock",
            "clockwise rotated cloud",
            "clone outline",
            "clone",
            "close link",
            "close",
            "closed captioning outline",
            "closed captioning",
            "cloud download alternate",
            "cloud meatball",
            "cloud moon rain",
            "cloud moon",
            "cloud rain",
            "cloud showers heavy",
            "cloud sun rain",
            "cloud sun",
            "cloud upload alternate",
            "cloud",
            "cloudscale",
            "cloudsmith",
            "cloudversify",
            "cocktail",
            "code branch",
            "code",
            "codepen",
            "codiepie",
            "coffee",
            "cog",
            "cogs",
            "coins",
            "columns",
            "comment alternate outline",
            "comment alternate",
            "comment dollar",
            "comment dots outline",
            "comment dots",
            "comment medical",
            "comment outline",
            "comment slash",
            "comment",
            "comments dollar",
            "comments outline",
            "comments",
            "compact disc",
            "compass outline",
            "compass",
            "compress alternate",
            "compress arrows alternate",
            "compress",
            "concierge bell",
            "confluence",
            "connectdevelop",
            "contao",
            "content",
            "cookie bite",
            "cookie",
            "copy link",
            "copy outline",
            "copy",
            "copyright outline",
            "copyright",
            "corner add",
            "cotton bureau",
            "couch",
            "counterclockwise rotated cloud",
            "cpanel",
            "creative commons by",
            "creative commons nc eu",
            "creative commons nc jp",
            "creative commons nc",
            "creative commons nd",
            "creative commons pd alternate",
            "creative commons pd",
            "creative commons remix",
            "creative commons sa",
            "creative commons sampling plus",
            "creative commons sampling",
            "creative commons share",
            "creative commons zero",
            "creative commons",
            "credit card outline",
            "credit card",
            "critical role",
            "crop alternate",
            "crop",
            "cross",
            "crosshairs",
            "crow",
            "crutch",
            "css3 alternate",
            "css3",
            "cube",
            "cubes",
            "cut",
            "cuttlefish",
            "d and d beyond",
            "d and d",
            "dailymotion",
            "dashcube",
            "database",
            "deaf",
            "delicious",
            "democrat",
            "deploydog",
            "deskpro",
            "desktop",
            "dev",
            "deviantart",
            "dharmachakra",
            "dhl",
            "diagnoses",
            "diaspora",
            "dice d20",
            "dice d6",
            "dice five",
            "dice four",
            "dice one",
            "dice six",
            "dice three",
            "dice two",
            "dice",
            "digg",
            "digital ocean",
            "digital tachograph",
            "directions",
            "disabled users",
            "discord",
            "discourse",
            "disease",
            "divide",
            "dizzy outline",
            "dizzy",
            "dna",
            "dochub",
            "docker",
            "dog",
            "dollar sign",
            "dolly flatbed",
            "dolly",
            "donate",
            "door closed",
            "door open",
            "dot circle outline",
            "dot circle",
            "dove",
            "download",
            "draft2digital",
            "drafting compass",
            "dragon",
            "draw polygon",
            "dribbble square",
            "dribbble",
            "dropbox",
            "dropdown",
            "drum steelpan",
            "drum",
            "drumstick bite",
            "drupal",
            "dumbbell",
            "dumpster",
            "dungeon",
            "dyalog",
            "earlybirds",
            "ebay",
            "edge",
            "edit outline",
            "edit",
            "egg",
            "eject",
            "elementor",
            "ellipsis horizontal",
            "ellipsis vertical",
            "ello",
            "ember",
            "empire",
            "envelope open outline",
            "envelope open text",
            "envelope open",
            "envelope outline",
            "envelope square",
            "envelope",
            "envira",
            "equals",
            "eraser",
            "erlang",
            "ethereum",
            "ethernet",
            "etsy",
            "euro sign",
            "evernote",
            "exchange alternate",
            "exclamation circle",
            "exclamation triangle",
            "exclamation",
            "expand alternate",
            "expand arrows alternate",
            "expand",
            "expeditedssl",
            "external alternate",
            "external link square alternate",
            "eye dropper",
            "eye outline",
            "eye slash outline",
            "eye slash",
            "eye",
            "facebook f",
            "facebook messenger",
            "facebook square",
            "facebook",
            "fan",
            "fantasy flight games",
            "fast backward",
            "fast forward",
            "faucet",
            "fax",
            "feather alternate",
            "feather",
            "fedex",
            "fedora",
            "female",
            "fighter jet",
            "figma",
            "file alternate outline",
            "file alternate",
            "file archive outline",
            "file archive",
            "file audio outline",
            "file audio",
            "file code outline",
            "file code",
            "file contract",
            "file download",
            "file excel outline",
            "file excel",
            "file export",
            "file image outline",
            "file image",
            "file import",
            "file invoice dollar",
            "file invoice",
            "file medical alternate",
            "file medical",
            "file outline",
            "file pdf outline",
            "file pdf",
            "file powerpoint outline",
            "file powerpoint",
            "file prescription",
            "file signature",
            "file upload",
            "file video outline",
            "file video",
            "file word outline",
            "file word",
            "file",
            "fill drip",
            "fill",
            "film",
            "filter",
            "fingerprint",
            "fire alternate",
            "fire extinguisher",
            "fire",
            "firefox browser",
            "firefox",
            "first aid",
            "first order alternate",
            "first order",
            "firstdraft",
            "fish",
            "fist raised",
            "fitted help",
            "fitted small linkify",
            "flag checkered",
            "flag outline",
            "flag usa",
            "flag",
            "flask",
            "flickr",
            "flipboard",
            "flushed outline",
            "flushed",
            "fly",
            "folder minus",
            "folder open outline",
            "folder open",
            "folder outline",
            "folder plus",
            "folder",
            "font awesome alternate",
            "font awesome flag",
            "font awesome",
            "font",
            "fonticons fi",
            "fonticons",
            "football ball",
            "fort awesome alternate",
            "fort awesome",
            "forumbee",
            "forward",
            "foursquare",
            "free code camp",
            "freebsd",
            "frog",
            "frown open outline",
            "frown open",
            "frown outline",
            "frown",
            "fruit-apple",
            "fulcrum",
            "funnel dollar",
            "futbol outline",
            "futbol",
            "galactic republic",
            "galactic senate",
            "gamepad",
            "gas pump",
            "gavel",
            "gem outline",
            "gem",
            "genderless",
            "get pocket",
            "gg circle",
            "gg",
            "ghost",
            "gift",
            "gifts",
            "git alternate",
            "git square",
            "git",
            "github alternate",
            "github square",
            "github",
            "gitkraken",
            "gitlab",
            "gitter",
            "glass cheers",
            "glass martini alternate",
            "glass martini",
            "glass whiskey",
            "glasses",
            "glide g",
            "glide",
            "globe africa",
            "globe americas",
            "globe asia",
            "globe europe",
            "globe",
            "gofore",
            "golf ball",
            "goodreads g",
            "goodreads",
            "google drive",
            "google play",
            "google plus g",
            "google plus square",
            "google plus",
            "google wallet",
            "google",
            "gopuram",
            "graduation cap",
            "gratipay",
            "grav",
            "greater than equal",
            "greater than",
            "green users",
            "grey users",
            "grimace outline",
            "grimace",
            "grin alternate outline",
            "grin alternate",
            "grin beam outline",
            "grin beam sweat outline",
            "grin beam sweat",
            "grin beam",
            "grin hearts outline",
            "grin hearts",
            "grin outline",
            "grin squint outline",
            "grin squint tears outline",
            "grin squint tears",
            "grin squint",
            "grin stars outline",
            "grin stars",
            "grin tears outline",
            "grin tears",
            "grin tongue outline",
            "grin tongue squint outline",
            "grin tongue squint",
            "grin tongue wink outline",
            "grin tongue wink",
            "grin tongue",
            "grin wink outline",
            "grin wink",
            "grin",
            "grip horizontal",
            "grip lines vertical",
            "grip lines",
            "grip vertical",
            "gripfire",
            "grunt",
            "guitar",
            "gulp",
            "h square",
            "hacker news square",
            "hacker news",
            "hackerrank",
            "hamburger",
            "hammer",
            "hamsa",
            "hand holding heart",
            "hand holding medical",
            "hand holding usd",
            "hand holding water",
            "hand holding",
            "hand lizard outline",
            "hand lizard",
            "hand middle finger",
            "hand paper outline",
            "hand paper",
            "hand peace outline",
            "hand peace",
            "hand point down outline",
            "hand point down",
            "hand point left outline",
            "hand point left",
            "hand point right outline",
            "hand point right",
            "hand point up outline",
            "hand point up",
            "hand pointer outline",
            "hand pointer",
            "hand rock outline",
            "hand rock",
            "hand scissors outline",
            "hand scissors",
            "hand sparkles",
            "hand spock outline",
            "hand spock",
            "hands helping",
            "hands wash",
            "hands",
            "handshake alternate slash",
            "handshake outline",
            "handshake slash",
            "handshake",
            "hanukiah",
            "hard hat",
            "hashtag",
            "hat cowboy side",
            "hat cowboy",
            "hat wizard",
            "hdd outline",
            "hdd",
            "head side cough slash",
            "head side cough",
            "head side mask",
            "head side virus",
            "heading",
            "headphones alternate",
            "headphones",
            "headset",
            "heart broken",
            "heart outline",
            "heart",
            "heartbeat",
            "helicopter",
            "help link",
            "highlighter",
            "hiking",
            "hippo",
            "hips",
            "hire a helper",
            "history",
            "hockey puck",
            "holly berry",
            "home",
            "hooli",
            "horizontally flipped cloud",
            "hornbill",
            "horse head",
            "horse",
            "hospital alternate",
            "hospital outline",
            "hospital symbol",
            "hospital user",
            "hospital",
            "hot tub",
            "hotdog",
            "hotel",
            "hotjar",
            "hourglass end",
            "hourglass half",
            "hourglass outline",
            "hourglass start",
            "hourglass",
            "house damage",
            "house user",
            "houzz",
            "hryvnia",
            "html5",
            "hubspot",
            "huge home",
            "i cursor",
            "ice cream",
            "icicles",
            "icons",
            "id badge outline",
            "id badge",
            "id card alternate",
            "id card outline",
            "id card",
            "ideal",
            "igloo",
            "image outline",
            "image",
            "images outline",
            "images",
            "imdb",
            "inbox",
            "indent",
            "industry",
            "infinity",
            "info circle",
            "info",
            "instagram square",
            "instagram",
            "intercom",
            "internet explorer",
            "inverted blue users",
            "inverted brown users",
            "inverted corner add",
            "inverted green users",
            "inverted grey users",
            "inverted olive users",
            "inverted orange users",
            "inverted pink users",
            "inverted primary users",
            "inverted purple users",
            "inverted red users",
            "inverted secondary users",
            "inverted teal users",
            "inverted users",
            "inverted violet users",
            "inverted yellow users",
            "invision",
            "ioxhost",
            "italic",
            "itch io",
            "itunes note",
            "itunes",
            "java",
            "jedi order",
            "jedi",
            "jenkins",
            "jira",
            "joget",
            "joint",
            "joomla",
            "journal whills",
            "js square",
            "js",
            "jsfiddle",
            "kaaba",
            "kaggle",
            "key",
            "keybase",
            "keyboard outline",
            "keyboard",
            "keycdn",
            "khanda",
            "kickstarter k",
            "kickstarter",
            "kiss beam outline",
            "kiss beam",
            "kiss outline",
            "kiss wink heart outline",
            "kiss wink heart",
            "kiss",
            "kiwi bird",
            "korvue",
            "landmark",
            "language",
            "laptop code",
            "laptop house",
            "laptop medical",
            "laptop",
            "laravel",
            "large home",
            "lastfm square",
            "lastfm",
            "laugh beam outline",
            "laugh beam",
            "laugh outline",
            "laugh squint outline",
            "laugh squint",
            "laugh wink outline",
            "laugh wink",
            "laugh",
            "layer group",
            "leaf",
            "leanpub",
            "lemon outline",
            "lemon",
            "less than equal",
            "less than",
            "lesscss",
            "level down alternate",
            "level up alternate",
            "life ring outline",
            "life ring",
            "lightbulb outline",
            "lightbulb",
            "linechat",
            "linkedin in",
            "linkedin",
            "linkify",
            "linode",
            "linux",
            "lira sign",
            "list alternate outline",
            "list alternate",
            "list ol",
            "list ul",
            "list",
            "location arrow",
            "lock open",
            "lock",
            "long arrow alternate down",
            "long arrow alternate left",
            "long arrow alternate right",
            "long arrow alternate up",
            "low vision",
            "luggage cart",
            "lungs virus",
            "lungs",
            "lyft",
            "magento",
            "magic",
            "magnet",
            "mail bulk",
            "mail",
            "mailchimp",
            "male",
            "mandalorian",
            "map marked alternate",
            "map marked",
            "map marker alternate",
            "map marker",
            "map outline",
            "map pin",
            "map signs",
            "map",
            "markdown",
            "marker",
            "mars double",
            "mars stroke horizontal",
            "mars stroke vertical",
            "mars stroke",
            "mars",
            "mask",
            "massive home",
            "mastodon",
            "maxcdn",
            "mdb",
            "medal",
            "medapps",
            "medium m",
            "medium",
            "medkit",
            "medrt",
            "meetup",
            "megaport",
            "meh blank outline",
            "meh blank",
            "meh outline",
            "meh rolling eyes outline",
            "meh rolling eyes",
            "meh",
            "memory",
            "mendeley",
            "menorah",
            "mercury",
            "meteor",
            "microblog",
            "microchip",
            "microphone alternate slash",
            "microphone alternate",
            "microphone slash",
            "microphone",
            "microscope",
            "microsoft",
            "mini home",
            "minus circle",
            "minus square outline",
            "minus square",
            "minus",
            "mitten",
            "mix",
            "mixcloud",
            "mixer",
            "mizuni",
            "mobile alternate",
            "mobile",
            "modx",
            "monero",
            "money bill alternate outline",
            "money bill alternate",
            "money bill wave alternate",
            "money bill wave",
            "money bill",
            "money check alternate",
            "money check",
            "monument",
            "moon outline",
            "moon",
            "mortar pestle",
            "mosque",
            "motorcycle",
            "mountain",
            "mouse pointer",
            "mouse",
            "mug hot",
            "music",
            "napster",
            "neos",
            "neuter",
            "newspaper outline",
            "newspaper",
            "nimblr",
            "node js",
            "node",
            "not equal",
            "notched circle loading",
            "notes medical",
            "npm",
            "ns8",
            "nutritionix",
            "object group outline",
            "object group",
            "object ungroup outline",
            "object ungroup",
            "odnoklassniki square",
            "odnoklassniki",
            "oil can",
            "old republic",
            "olive users",
            "om",
            "opencart",
            "openid",
            "opera",
            "optin monster",
            "orange users",
            "orcid",
            "osi",
            "otter",
            "outdent",
            "page4",
            "pagelines",
            "pager",
            "paint brush",
            "paint roller",
            "palette",
            "palfed",
            "pallet",
            "paper plane outline",
            "paper plane",
            "paperclip",
            "parachute box",
            "paragraph",
            "parking",
            "passport",
            "pastafarianism",
            "paste",
            "patreon",
            "pause circle outline",
            "pause circle",
            "pause",
            "paw",
            "paypal",
            "peace",
            "pen alternate",
            "pen fancy",
            "pen nib",
            "pen square",
            "pen",
            "pencil alternate",
            "pencil ruler",
            "penny arcade",
            "people arrows",
            "people carry",
            "pepper hot",
            "percent",
            "percentage",
            "periscope",
            "person booth",
            "phabricator",
            "phoenix framework",
            "phoenix squadron",
            "phone alternate",
            "phone slash",
            "phone square alternate",
            "phone square",
            "phone volume",
            "phone",
            "photo video",
            "php",
            "pied piper alternate",
            "pied piper hat",
            "pied piper pp",
            "pied piper square",
            "pied piper",
            "piggy bank",
            "pills",
            "pink users",
            "pinterest p",
            "pinterest square",
            "pinterest",
            "pizza slice",
            "place of worship",
            "plane arrival",
            "plane departure",
            "plane",
            "play circle outline",
            "play circle",
            "play",
            "playstation",
            "plug",
            "plus circle",
            "plus square outline",
            "plus square",
            "plus",
            "podcast",
            "poll horizontal",
            "poll",
            "poo storm",
            "poo",
            "poop",
            "portrait",
            "pound sign",
            "power off",
            "pray",
            "praying hands",
            "prescription bottle alternate",
            "prescription bottle",
            "prescription",
            "primary users",
            "print",
            "procedures",
            "product hunt",
            "project diagram",
            "pump medical",
            "pump soap",
            "purple users",
            "pushed",
            "puzzle piece",
            "puzzle",
            "python",
            "qq",
            "qrcode",
            "question circle outline",
            "question circle",
            "question",
            "quidditch",
            "quinscape",
            "quora",
            "quote left",
            "quote right",
            "quran",
            "r project",
            "radiation alternate",
            "radiation",
            "rainbow",
            "random",
            "raspberry pi",
            "ravelry",
            "react",
            "reacteurope",
            "readme",
            "rebel",
            "receipt",
            "record vinyl",
            "recycle",
            "red users",
            "reddit alien",
            "reddit square",
            "reddit",
            "redhat",
            "redo alternate",
            "redo",
            "redriver",
            "redyeti",
            "registered outline",
            "registered",
            "remove format",
            "renren",
            "reply all",
            "reply",
            "replyd",
            "republican",
            "researchgate",
            "resolving",
            "restroom",
            "retweet",
            "rev",
            "ribbon",
            "ring",
            "road",
            "robot",
            "rocket",
            "rocketchat",
            "rockrms",
            "route",
            "rss square",
            "rss",
            "ruble sign",
            "ruler combined",
            "ruler horizontal",
            "ruler vertical",
            "ruler",
            "running",
            "rupee sign",
            "sad cry outline",
            "sad cry",
            "sad tear outline",
            "sad tear",
            "safari",
            "salesforce",
            "sass",
            "satellite dish",
            "satellite",
            "save outline",
            "save",
            "schlix",
            "school",
            "screwdriver",
            "scribd",
            "scroll",
            "sd card",
            "search dollar",
            "search location",
            "search minus",
            "search plus",
            "search",
            "searchengin",
            "secondary users",
            "seedling",
            "sellcast",
            "sellsy",
            "server",
            "servicestack",
            "shapes",
            "share alternate square",
            "share alternate",
            "share square outline",
            "share square",
            "share",
            "shekel sign",
            "shield alternate",
            "shield virus",
            "ship",
            "shipping fast",
            "shirtsinbulk",
            "shoe prints",
            "shopify",
            "shopping bag",
            "shopping basket",
            "shopping cart",
            "shopware",
            "shower",
            "shuttle van",
            "sign in alternate",
            "sign language",
            "sign out alternate",
            "sign",
            "signal",
            "sim card",
            "simplybuilt",
            "sistrix",
            "sitemap",
            "sith",
            "skating",
            "sketch",
            "skiing nordic",
            "skiing",
            "skull crossbones",
            "skyatlas",
            "skype",
            "slack hash",
            "slack",
            "slash",
            "sleigh",
            "sliders horizontal",
            "slideshare",
            "small home",
            "smile beam outline",
            "smile beam",
            "smile outline",
            "smile wink outline",
            "smile wink",
            "smile",
            "smog",
            "smoking ban",
            "smoking",
            "sms",
            "snapchat ghost",
            "snapchat square",
            "snapchat",
            "snowboarding",
            "snowflake outline",
            "snowflake",
            "snowman",
            "snowplow",
            "soap",
            "socks",
            "solar panel",
            "sort alphabet down alternate",
            "sort alphabet down",
            "sort alphabet up alternate",
            "sort alphabet up",
            "sort amount down alternate",
            "sort amount down",
            "sort amount up alternate",
            "sort amount up",
            "sort down",
            "sort numeric down alternate",
            "sort numeric down",
            "sort numeric up alternate",
            "sort numeric up",
            "sort up",
            "sort",
            "soundcloud",
            "sourcetree",
            "spa",
            "space shuttle",
            "speakap",
            "speaker deck",
            "spell check",
            "spider",
            "spinner loading",
            "spinner",
            "splotch",
            "spotify",
            "spray can",
            "square full",
            "square outline",
            "square root alternate",
            "square",
            "squarespace",
            "stack exchange",
            "stack overflow",
            "stackpath",
            "stamp",
            "star and crescent",
            "star half alternate",
            "star half outline",
            "star half",
            "star of david",
            "star of life",
            "star outline",
            "star",
            "staylinked",
            "steam square",
            "steam symbol",
            "steam",
            "step backward",
            "step forward",
            "stethoscope",
            "sticker mule",
            "sticky note outline",
            "sticky note",
            "stop circle outline",
            "stop circle",
            "stop",
            "stopwatch",
            "store alternate slash",
            "store alternate",
            "store slash",
            "store",
            "strava",
            "stream",
            "street view",
            "strikethrough",
            "stripe s",
            "stripe",
            "stroopwafel",
            "studiovinari",
            "stumbleupon circle",
            "stumbleupon",
            "subscript",
            "subway",
            "suitcase rolling",
            "suitcase",
            "sun outline",
            "sun",
            "superpowers",
            "superscript",
            "supple",
            "surprise outline",
            "surprise",
            "suse",
            "swatchbook",
            "swift",
            "swimmer",
            "swimming pool",
            "symfony",
            "synagogue",
            "sync alternate",
            "sync",
            "syringe",
            "table tennis",
            "table",
            "tablet alternate",
            "tablet",
            "tablets",
            "tachometer alternate",
            "tag",
            "tags",
            "tape",
            "tasks",
            "taxi",
            "teal users",
            "teamspeak",
            "teeth open",
            "teeth",
            "telegram plane",
            "telegram",
            "temperature high",
            "temperature low",
            "tencent weibo",
            "tenge",
            "terminal",
            "text height",
            "text width",
            "th large",
            "th list",
            "th",
            "theater masks",
            "themeco",
            "themeisle",
            "thermometer empty",
            "thermometer full",
            "thermometer half",
            "thermometer quarter",
            "thermometer three quarters",
            "thermometer",
            "think peaks",
            "thumbs down outline",
            "thumbs down",
            "thumbs up outline",
            "thumbs up",
            "thumbtack",
            "ticket alternate",
            "times circle outline",
            "times circle",
            "times",
            "tint slash",
            "tint",
            "tiny home",
            "tired outline",
            "tired",
            "toggle off",
            "toggle on",
            "toilet paper slash",
            "toilet paper",
            "toilet",
            "toolbox",
            "tools",
            "tooth",
            "top left corner add",
            "top right corner add",
            "torah",
            "torii gate",
            "tractor",
            "trade federation",
            "trademark",
            "traffic light",
            "trailer",
            "train",
            "tram",
            "transgender alternate",
            "transgender",
            "trash alternate outline",
            "trash alternate",
            "trash restore alternate",
            "trash restore",
            "trash",
            "tree",
            "trello",
            "tripadvisor",
            "trophy",
            "truck monster",
            "truck moving",
            "truck packing",
            "truck pickup",
            "truck",
            "tshirt",
            "tty",
            "tumblr square",
            "tumblr",
            "tv",
            "twitch",
            "twitter square",
            "twitter",
            "typo3",
            "uber",
            "ubuntu",
            "uikit",
            "umbraco",
            "umbrella beach",
            "umbrella",
            "underline",
            "undo alternate",
            "undo",
            "uniregistry",
            "unity",
            "universal access",
            "university",
            "unlink",
            "unlock alternate",
            "unlock",
            "untappd",
            "upload",
            "ups",
            "usb",
            "user alternate slash",
            "user alternate",
            "user astronaut",
            "user check",
            "user circle outline",
            "user circle",
            "user clock",
            "user cog",
            "user edit",
            "user friends",
            "user graduate",
            "user injured",
            "user lock",
            "user md",
            "user minus",
            "user ninja",
            "user nurse",
            "user outline",
            "user plus",
            "user secret",
            "user shield",
            "user slash",
            "user tag",
            "user tie",
            "user times",
            "user",
            "users cog",
            "users",
            "usps",
            "ussunnah",
            "utensil spoon",
            "utensils",
            "vaadin",
            "vector square",
            "venus double",
            "venus mars",
            "venus",
            "vertically flipped cloud",
            "viacoin",
            "viadeo square",
            "viadeo",
            "vial",
            "vials",
            "viber",
            "video slash",
            "video",
            "vihara",
            "vimeo square",
            "vimeo v",
            "vimeo",
            "vine",
            "violet users",
            "virus slash",
            "virus",
            "viruses",
            "vk",
            "vnv",
            "voicemail",
            "volleyball ball",
            "volume down",
            "volume mute",
            "volume off",
            "volume up",
            "vote yea",
            "vuejs",
            "walking",
            "wallet",
            "warehouse",
            "water",
            "wave square",
            "waze",
            "weebly",
            "weibo",
            "weight",
            "weixin",
            "whatsapp square",
            "whatsapp",
            "wheelchair",
            "whmcs",
            "wifi",
            "wikipedia w",
            "wind",
            "window close outline",
            "window close",
            "window maximize outline",
            "window maximize",
            "window minimize outline",
            "window minimize",
            "window restore outline",
            "window restore",
            "windows",
            "wine bottle",
            "wine glass alternate",
            "wine glass",
            "wix",
            "wizards of the coast",
            "wolf pack battalion",
            "won sign",
            "wordpress simple",
            "wordpress",
            "world",
            "wpbeginner",
            "wpexplorer",
            "wpforms",
            "wpressr",
            "wrench",
            "x ray",
            "xbox",
            "xing square",
            "xing",
            "y combinator",
            "yahoo",
            "yammer",
            "yandex international",
            "yandex",
            "yarn",
            "yellow users",
            "yelp",
            "yen sign",
            "yin yang",
            "yoast",
            "youtube square",
            "youtube",
            "zhihu"
        )
            // custom things
            .plus("dont")

        fun cssClassOf(block: SemanticIcon.() -> SemanticIcon): String {
            lateinit var classes: String

            createHTML().body {
                classes = icon.block().cssClasses.joinToString(" ")
            }

            return classes
        }
    }

    private val cssClasses = mutableListOf<String>()

    private fun attrs() = attributesMapOf("class", cssClasses.joinToString(" ") + " icon")

    @JsName("p") operator fun plus(cls: String): SemanticIcon = apply { cssClasses.add(cls) }

    @JsName("p2") operator fun plus(classes: Array<out String>): SemanticIcon = apply { cssClasses.addAll(classes) }

    @JsName("r") fun render(block: I.() -> Unit = {}): Unit = I(attrs(), parent.consumer).visitNoInline(block)

    @JsName("i") operator fun invoke(): Unit = render()

    @JsName("i2") operator fun invoke(block: I.() -> Unit): Unit = render(block)

    @SemanticUiCssMarker @JsName("w") fun with(cls: String): SemanticIcon = this + cls

    @SemanticUiCssMarker @JsName("w2") fun custom(cls: String): Unit = (this + cls).render()

    // conditional classes

    @SemanticUiConditionalMarker @JsName("g") fun given(
        condition: Boolean,
        action: SemanticIcon.() -> SemanticIcon,
    ): SemanticIcon = when (condition) {
        false -> this
        else -> this.action()
    }

    @SemanticUiConditionalMarker @JsName("gn") fun givenNot(
        condition: Boolean,
        action: SemanticIcon.() -> SemanticIcon,
    ): SemanticIcon = given(!condition, action)

    @SemanticUiConditionalMarker inline val then: SemanticIcon get() = this

    // coloring ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker @JsName("c") fun color(color: SemanticColor): SemanticIcon = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    @SemanticUiCssMarker val red: SemanticIcon @JsName(Fn.f9001) get() = this + "red"

    @SemanticUiCssMarker val orange: SemanticIcon @JsName(Fn.f9002) get() = this + "orange"

    @SemanticUiCssMarker val yellow: SemanticIcon @JsName(Fn.f9003) get() = this + "yellow"

    @SemanticUiCssMarker val olive: SemanticIcon @JsName(Fn.f9004) get() = this + "olive"

    @SemanticUiCssMarker val green: SemanticIcon @JsName(Fn.f9005) get() = this + "green"

    @SemanticUiCssMarker val teal: SemanticIcon @JsName(Fn.f9006) get() = this + "teal"

    @SemanticUiCssMarker val blue: SemanticIcon @JsName(Fn.f9007) get() = this + "blue"

    @SemanticUiCssMarker val violet: SemanticIcon @JsName(Fn.f9008) get() = this + "violet"

    @SemanticUiCssMarker val purple: SemanticIcon @JsName(Fn.f9009) get() = this + "purple"

    @SemanticUiCssMarker val pink: SemanticIcon @JsName(Fn.f9010) get() = this + "pink"

    @SemanticUiCssMarker val brown: SemanticIcon @JsName(Fn.f9011) get() = this + "brown"

    @SemanticUiCssMarker val grey: SemanticIcon @JsName(Fn.f9012) get() = this + "grey"

    @SemanticUiCssMarker val black: SemanticIcon @JsName(Fn.f9013) get() = this + "black"

    @SemanticUiCssMarker val white: SemanticIcon @JsName(Fn.f9014) get() = this + "white"

    // Behaviour ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker val bordered: SemanticIcon @JsName(Fn.f9015) get() = this + "bordered"

    @SemanticUiCssMarker val circular: SemanticIcon @JsName(Fn.f9016) get() = this + "circular"

    @SemanticUiCssMarker val clockwise: SemanticIcon @JsName(Fn.f9017) get() = this + "clockwise"

    @SemanticUiCssMarker val counterclockwise: SemanticIcon @JsName(Fn.f9018) get() = this + "counterclockwise"

    @SemanticUiCssMarker val colored: SemanticIcon @JsName(Fn.f9019) get() = this + "colored"

    @SemanticUiCssMarker val corner: SemanticIcon @JsName(Fn.f9020) get() = this + "corner"

    @SemanticUiCssMarker val disabled: SemanticIcon @JsName(Fn.f9021) get() = this + "disabled"

    @SemanticUiCssMarker val fitted: SemanticIcon @JsName(Fn.f9022) get() = this + "fitted"

    @SemanticUiCssMarker val flipped: SemanticIcon @JsName(Fn.f9023) get() = this + "flipped"

    @SemanticUiCssMarker val horizontally: SemanticIcon @JsName(Fn.f9024) get() = this + "horizontally"

    @SemanticUiCssMarker val link: SemanticIcon @JsName(Fn.f9025) get() = this + "link"

    @SemanticUiCssMarker val loading: SemanticIcon @JsName(Fn.f9026) get() = this + "loading"

    @SemanticUiCssMarker val inverted: SemanticIcon @JsName(Fn.f9027) get() = this + "inverted"

    @SemanticUiCssMarker val rotated: SemanticIcon @JsName(Fn.f9028) get() = this + "rotated"

    @SemanticUiCssMarker val vertically: SemanticIcon @JsName(Fn.f9029) get() = this + "vertically"

    // Position & Alignment ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker val aligned: SemanticIcon @JsName(Fn.f9030) get() = this + "aligned"

    @SemanticUiCssMarker val bottom: SemanticIcon @JsName(Fn.f9031) get() = this + "bottom"

    @SemanticUiCssMarker val floated: SemanticIcon @JsName(Fn.f9032) get() = this + "floated"

    @SemanticUiCssMarker val left: SemanticIcon @JsName(Fn.f9033) get() = this + "right"

    @SemanticUiCssMarker val middle: SemanticIcon @JsName(Fn.f9034) get() = this + "middle"

    @SemanticUiCssMarker val right: SemanticIcon @JsName(Fn.f9035) get() = this + "right"

    @SemanticUiCssMarker val top: SemanticIcon @JsName(Fn.f9036) get() = this + "top"

    // Size ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker val mini: SemanticIcon @JsName(Fn.f9037) get() = this + "mini"

    @SemanticUiCssMarker val tiny: SemanticIcon @JsName(Fn.f9038) get() = this + "tiny"

    @SemanticUiCssMarker val small: SemanticIcon @JsName(Fn.f9039) get() = this + "small"

    @SemanticUiCssMarker val medium: SemanticIcon @JsName(Fn.f9040) get() = this

    @SemanticUiCssMarker val large: SemanticIcon @JsName(Fn.f9041) get() = this + "large"

    @SemanticUiCssMarker val big: SemanticIcon @JsName(Fn.f9042) get() = this + "big"

    @SemanticUiCssMarker val huge: SemanticIcon @JsName(Fn.f9043) get() = this + "huge"

    @SemanticUiCssMarker val massive: SemanticIcon @JsName(Fn.f9044) get() = this + "massive"

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Custom //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticIconMarker inline val dont: SemanticIcon @JsName(Fn.f9045) get() = this + "dont"

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Icons ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticIconMarker val _500px: SemanticIcon
        @JsName("_a") get() = this + "500px"
    @SemanticIconMarker val accessible: SemanticIcon
        @JsName("_b") get() = this + "accessible"
    @SemanticIconMarker val accusoft: SemanticIcon
        @JsName("_c") get() = this + "accusoft"
    @SemanticIconMarker val acquisitions_incorporated: SemanticIcon
        @JsName("_d") get() = this + "acquisitions incorporated"
    @SemanticIconMarker val ad: SemanticIcon
        @JsName("_e") get() = this + "ad"
    @SemanticIconMarker val address_book: SemanticIcon
        @JsName("_f") get() = this + "address book"
    @SemanticIconMarker val address_book_outline: SemanticIcon
        @JsName("_g") get() = this + "address book outline"
    @SemanticIconMarker val address_card: SemanticIcon
        @JsName("_h") get() = this + "address card"
    @SemanticIconMarker val address_card_outline: SemanticIcon
        @JsName("_i") get() = this + "address card outline"
    @SemanticIconMarker val adjust: SemanticIcon
        @JsName("_j") get() = this + "adjust"
    @SemanticIconMarker val adn: SemanticIcon
        @JsName("_k") get() = this + "adn"
    @SemanticIconMarker val adobe: SemanticIcon
        @JsName("_l") get() = this + "adobe"
    @SemanticIconMarker val adversal: SemanticIcon
        @JsName("_m") get() = this + "adversal"
    @SemanticIconMarker val affiliatetheme: SemanticIcon
        @JsName("_n") get() = this + "affiliatetheme"
    @SemanticIconMarker val air_freshener: SemanticIcon
        @JsName("_o") get() = this + "air freshener"
    @SemanticIconMarker val airbnb: SemanticIcon
        @JsName("_p") get() = this + "airbnb"
    @SemanticIconMarker val algolia: SemanticIcon
        @JsName("_q") get() = this + "algolia"
    @SemanticIconMarker val align_center: SemanticIcon
        @JsName("_r") get() = this + "align center"
    @SemanticIconMarker val align_justify: SemanticIcon
        @JsName("_s") get() = this + "align justify"
    @SemanticIconMarker val align_left: SemanticIcon
        @JsName("_t") get() = this + "align left"
    @SemanticIconMarker val align_right: SemanticIcon
        @JsName("_u") get() = this + "align right"
    @SemanticIconMarker val alipay: SemanticIcon
        @JsName("_v") get() = this + "alipay"
    @SemanticIconMarker val allergies: SemanticIcon
        @JsName("_w") get() = this + "allergies"
    @SemanticIconMarker val alternate_github: SemanticIcon
        @JsName("_x") get() = this + "alternate github"
    @SemanticIconMarker val amazon: SemanticIcon
        @JsName("_y") get() = this + "amazon"
    @SemanticIconMarker val amazon_pay: SemanticIcon
        @JsName("_z") get() = this + "amazon pay"
    @SemanticIconMarker val ambulance: SemanticIcon
        @JsName("_A") get() = this + "ambulance"
    @SemanticIconMarker val american_sign_language_interpreting: SemanticIcon
        @JsName("_B") get() = this + "american sign language interpreting"
    @SemanticIconMarker val amilia: SemanticIcon
        @JsName("_C") get() = this + "amilia"
    @SemanticIconMarker val anchor: SemanticIcon
        @JsName("_D") get() = this + "anchor"
    @SemanticIconMarker val android: SemanticIcon
        @JsName("_E") get() = this + "android"
    @SemanticIconMarker val angellist: SemanticIcon
        @JsName("_F") get() = this + "angellist"
    @SemanticIconMarker val angle_double_down: SemanticIcon
        @JsName("_G") get() = this + "angle double down"
    @SemanticIconMarker val angle_double_left: SemanticIcon
        @JsName("_H") get() = this + "angle double left"
    @SemanticIconMarker val angle_double_right: SemanticIcon
        @JsName("_I") get() = this + "angle double right"
    @SemanticIconMarker val angle_double_up: SemanticIcon
        @JsName("_J") get() = this + "angle double up"
    @SemanticIconMarker val angle_down: SemanticIcon
        @JsName("_K") get() = this + "angle down"
    @SemanticIconMarker val angle_left: SemanticIcon
        @JsName("_L") get() = this + "angle left"
    @SemanticIconMarker val angle_right: SemanticIcon
        @JsName("_M") get() = this + "angle right"
    @SemanticIconMarker val angle_up: SemanticIcon
        @JsName("_N") get() = this + "angle up"
    @SemanticIconMarker val angry: SemanticIcon
        @JsName("_O") get() = this + "angry"
    @SemanticIconMarker val angry_outline: SemanticIcon
        @JsName("_P") get() = this + "angry outline"
    @SemanticIconMarker val angrycreative: SemanticIcon
        @JsName("_Q") get() = this + "angrycreative"
    @SemanticIconMarker val angular: SemanticIcon
        @JsName("_R") get() = this + "angular"
    @SemanticIconMarker val ankh: SemanticIcon
        @JsName("_S") get() = this + "ankh"
    @SemanticIconMarker val app_store: SemanticIcon
        @JsName("_T") get() = this + "app store"
    @SemanticIconMarker val app_store_ios: SemanticIcon
        @JsName("_U") get() = this + "app store ios"
    @SemanticIconMarker val apper: SemanticIcon
        @JsName("_V") get() = this + "apper"
    @SemanticIconMarker val apple: SemanticIcon
        @JsName("_W") get() = this + "apple"
    @SemanticIconMarker val apple_pay: SemanticIcon
        @JsName("_X") get() = this + "apple pay"
    @SemanticIconMarker val archive: SemanticIcon
        @JsName("_Y") get() = this + "archive"
    @SemanticIconMarker val archway: SemanticIcon
        @JsName("_Z") get() = this + "archway"
    @SemanticIconMarker val arrow_alternate_circle_down: SemanticIcon
        @JsName("_ab") get() = this + "arrow alternate circle down"
    @SemanticIconMarker val arrow_alternate_circle_down_outline: SemanticIcon
        @JsName("_bb") get() = this + "arrow alternate circle down outline"
    @SemanticIconMarker val arrow_alternate_circle_left: SemanticIcon
        @JsName("_cb") get() = this + "arrow alternate circle left"
    @SemanticIconMarker val arrow_alternate_circle_left_outline: SemanticIcon
        @JsName("_db") get() = this + "arrow alternate circle left outline"
    @SemanticIconMarker val arrow_alternate_circle_right: SemanticIcon
        @JsName("_eb") get() = this + "arrow alternate circle right"
    @SemanticIconMarker val arrow_alternate_circle_right_outline: SemanticIcon
        @JsName("_fb") get() = this + "arrow alternate circle right outline"
    @SemanticIconMarker val arrow_alternate_circle_up: SemanticIcon
        @JsName("_gb") get() = this + "arrow alternate circle up"
    @SemanticIconMarker val arrow_alternate_circle_up_outline: SemanticIcon
        @JsName("_hb") get() = this + "arrow alternate circle up outline"
    @SemanticIconMarker val arrow_circle_down: SemanticIcon
        @JsName("_ib") get() = this + "arrow circle down"
    @SemanticIconMarker val arrow_circle_left: SemanticIcon
        @JsName("_jb") get() = this + "arrow circle left"
    @SemanticIconMarker val arrow_circle_right: SemanticIcon
        @JsName("_kb") get() = this + "arrow circle right"
    @SemanticIconMarker val arrow_circle_up: SemanticIcon
        @JsName("_lb") get() = this + "arrow circle up"
    @SemanticIconMarker val arrow_down: SemanticIcon
        @JsName("_mb") get() = this + "arrow down"
    @SemanticIconMarker val arrow_left: SemanticIcon
        @JsName("_nb") get() = this + "arrow left"
    @SemanticIconMarker val arrow_right: SemanticIcon
        @JsName("_ob") get() = this + "arrow right"
    @SemanticIconMarker val arrow_up: SemanticIcon
        @JsName("_pb") get() = this + "arrow up"
    @SemanticIconMarker val arrows_alternate: SemanticIcon
        @JsName("_qb") get() = this + "arrows alternate"
    @SemanticIconMarker val arrows_alternate_horizontal: SemanticIcon
        @JsName("_rb") get() = this + "arrows alternate horizontal"
    @SemanticIconMarker val arrows_alternate_vertical: SemanticIcon
        @JsName("_sb") get() = this + "arrows alternate vertical"
    @SemanticIconMarker val artstation: SemanticIcon
        @JsName("_tb") get() = this + "artstation"
    @SemanticIconMarker val assistive_listening_systems: SemanticIcon
        @JsName("_ub") get() = this + "assistive listening systems"
    @SemanticIconMarker val asterisk: SemanticIcon
        @JsName("_vb") get() = this + "asterisk"
    @SemanticIconMarker val asterisk_loading: SemanticIcon
        @JsName("_wb") get() = this + "asterisk loading"
    @SemanticIconMarker val asymmetrik: SemanticIcon
        @JsName("_xb") get() = this + "asymmetrik"
    @SemanticIconMarker val at: SemanticIcon
        @JsName("_yb") get() = this + "at"
    @SemanticIconMarker val atlas: SemanticIcon
        @JsName("_zb") get() = this + "atlas"
    @SemanticIconMarker val atlassian: SemanticIcon
        @JsName("_Ab") get() = this + "atlassian"
    @SemanticIconMarker val atom: SemanticIcon
        @JsName("_Bb") get() = this + "atom"
    @SemanticIconMarker val audible: SemanticIcon
        @JsName("_Cb") get() = this + "audible"
    @SemanticIconMarker val audio_description: SemanticIcon
        @JsName("_Db") get() = this + "audio description"
    @SemanticIconMarker val autoprefixer: SemanticIcon
        @JsName("_Eb") get() = this + "autoprefixer"
    @SemanticIconMarker val avianex: SemanticIcon
        @JsName("_Fb") get() = this + "avianex"
    @SemanticIconMarker val aviato: SemanticIcon
        @JsName("_Gb") get() = this + "aviato"
    @SemanticIconMarker val award: SemanticIcon
        @JsName("_Hb") get() = this + "award"
    @SemanticIconMarker val aws: SemanticIcon
        @JsName("_Ib") get() = this + "aws"
    @SemanticIconMarker val baby: SemanticIcon
        @JsName("_Jb") get() = this + "baby"
    @SemanticIconMarker val baby_carriage: SemanticIcon
        @JsName("_Kb") get() = this + "baby carriage"
    @SemanticIconMarker val backward: SemanticIcon
        @JsName("_Lb") get() = this + "backward"
    @SemanticIconMarker val bacon: SemanticIcon
        @JsName("_Mb") get() = this + "bacon"
    @SemanticIconMarker val bahai: SemanticIcon
        @JsName("_Nb") get() = this + "bahai"
    @SemanticIconMarker val balance_scale: SemanticIcon
        @JsName("_Ob") get() = this + "balance scale"
    @SemanticIconMarker val balance_scale_left: SemanticIcon
        @JsName("_Pb") get() = this + "balance scale left"
    @SemanticIconMarker val balance_scale_right: SemanticIcon
        @JsName("_Qb") get() = this + "balance scale right"
    @SemanticIconMarker val ban: SemanticIcon
        @JsName("_Rb") get() = this + "ban"
    @SemanticIconMarker val band_aid: SemanticIcon
        @JsName("_Sb") get() = this + "band aid"
    @SemanticIconMarker val bandcamp: SemanticIcon
        @JsName("_Tb") get() = this + "bandcamp"
    @SemanticIconMarker val barcode: SemanticIcon
        @JsName("_Ub") get() = this + "barcode"
    @SemanticIconMarker val bars: SemanticIcon
        @JsName("_Vb") get() = this + "bars"
    @SemanticIconMarker val baseball_ball: SemanticIcon
        @JsName("_Wb") get() = this + "baseball ball"
    @SemanticIconMarker val basketball_ball: SemanticIcon
        @JsName("_Xb") get() = this + "basketball ball"
    @SemanticIconMarker val bath: SemanticIcon
        @JsName("_Yb") get() = this + "bath"
    @SemanticIconMarker val battery_empty: SemanticIcon
        @JsName("_Zb") get() = this + "battery empty"
    @SemanticIconMarker val battery_full: SemanticIcon
        @JsName("_ac") get() = this + "battery full"
    @SemanticIconMarker val battery_half: SemanticIcon
        @JsName("_bc") get() = this + "battery half"
    @SemanticIconMarker val battery_quarter: SemanticIcon
        @JsName("_cc") get() = this + "battery quarter"
    @SemanticIconMarker val battery_three_quarters: SemanticIcon
        @JsName("_dc") get() = this + "battery three quarters"
    @SemanticIconMarker val battle_net: SemanticIcon
        @JsName("_ec") get() = this + "battle net"
    @SemanticIconMarker val bed: SemanticIcon
        @JsName("_fc") get() = this + "bed"
    @SemanticIconMarker val beer: SemanticIcon
        @JsName("_gc") get() = this + "beer"
    @SemanticIconMarker val behance: SemanticIcon
        @JsName("_hc") get() = this + "behance"
    @SemanticIconMarker val behance_square: SemanticIcon
        @JsName("_ic") get() = this + "behance square"
    @SemanticIconMarker val bell: SemanticIcon
        @JsName("_jc") get() = this + "bell"
    @SemanticIconMarker val bell_outline: SemanticIcon
        @JsName("_kc") get() = this + "bell outline"
    @SemanticIconMarker val bell_slash: SemanticIcon
        @JsName("_lc") get() = this + "bell slash"
    @SemanticIconMarker val bell_slash_outline: SemanticIcon
        @JsName("_mc") get() = this + "bell slash outline"
    @SemanticIconMarker val bezier_curve: SemanticIcon
        @JsName("_nc") get() = this + "bezier curve"
    @SemanticIconMarker val bible: SemanticIcon
        @JsName("_oc") get() = this + "bible"
    @SemanticIconMarker val bicycle: SemanticIcon
        @JsName("_pc") get() = this + "bicycle"
    @SemanticIconMarker val big_circle_outline: SemanticIcon
        @JsName("_qc") get() = this + "big circle outline"
    @SemanticIconMarker val big_home: SemanticIcon
        @JsName("_rc") get() = this + "big home"
    @SemanticIconMarker val big_red_dont: SemanticIcon
        @JsName("_sc") get() = this + "big red dont"
    @SemanticIconMarker val biking: SemanticIcon
        @JsName("_tc") get() = this + "biking"
    @SemanticIconMarker val bimobject: SemanticIcon
        @JsName("_uc") get() = this + "bimobject"
    @SemanticIconMarker val binoculars: SemanticIcon
        @JsName("_vc") get() = this + "binoculars"
    @SemanticIconMarker val biohazard: SemanticIcon
        @JsName("_wc") get() = this + "biohazard"
    @SemanticIconMarker val birthday_cake: SemanticIcon
        @JsName("_xc") get() = this + "birthday cake"
    @SemanticIconMarker val bitbucket: SemanticIcon
        @JsName("_yc") get() = this + "bitbucket"
    @SemanticIconMarker val bitcoin: SemanticIcon
        @JsName("_zc") get() = this + "bitcoin"
    @SemanticIconMarker val bity: SemanticIcon
        @JsName("_Ac") get() = this + "bity"
    @SemanticIconMarker val black_tie: SemanticIcon
        @JsName("_Bc") get() = this + "black tie"
    @SemanticIconMarker val black_user: SemanticIcon
        @JsName("_Cc") get() = this + "black user"
    @SemanticIconMarker val black_users: SemanticIcon
        @JsName("_Dc") get() = this + "black users"
    @SemanticIconMarker val blackberry: SemanticIcon
        @JsName("_Ec") get() = this + "blackberry"
    @SemanticIconMarker val blender: SemanticIcon
        @JsName("_Fc") get() = this + "blender"
    @SemanticIconMarker val blind: SemanticIcon
        @JsName("_Gc") get() = this + "blind"
    @SemanticIconMarker val blog: SemanticIcon
        @JsName("_Hc") get() = this + "blog"
    @SemanticIconMarker val blogger: SemanticIcon
        @JsName("_Ic") get() = this + "blogger"
    @SemanticIconMarker val blogger_b: SemanticIcon
        @JsName("_Jc") get() = this + "blogger b"
    @SemanticIconMarker val blue_users: SemanticIcon
        @JsName("_Kc") get() = this + "blue users"
    @SemanticIconMarker val bluetooth: SemanticIcon
        @JsName("_Lc") get() = this + "bluetooth"
    @SemanticIconMarker val bluetooth_b: SemanticIcon
        @JsName("_Mc") get() = this + "bluetooth b"
    @SemanticIconMarker val bold: SemanticIcon
        @JsName("_Nc") get() = this + "bold"
    @SemanticIconMarker val bolt: SemanticIcon
        @JsName("_Oc") get() = this + "bolt"
    @SemanticIconMarker val bomb: SemanticIcon
        @JsName("_Pc") get() = this + "bomb"
    @SemanticIconMarker val bone: SemanticIcon
        @JsName("_Qc") get() = this + "bone"
    @SemanticIconMarker val bong: SemanticIcon
        @JsName("_Rc") get() = this + "bong"
    @SemanticIconMarker val book: SemanticIcon
        @JsName("_Sc") get() = this + "book"
    @SemanticIconMarker val book_dead: SemanticIcon
        @JsName("_Tc") get() = this + "book dead"
    @SemanticIconMarker val book_medical: SemanticIcon
        @JsName("_Uc") get() = this + "book medical"
    @SemanticIconMarker val book_open: SemanticIcon
        @JsName("_Vc") get() = this + "book open"
    @SemanticIconMarker val book_reader: SemanticIcon
        @JsName("_Wc") get() = this + "book reader"
    @SemanticIconMarker val bookmark: SemanticIcon
        @JsName("_Xc") get() = this + "bookmark"
    @SemanticIconMarker val bookmark_outline: SemanticIcon
        @JsName("_Yc") get() = this + "bookmark outline"
    @SemanticIconMarker val bootstrap: SemanticIcon
        @JsName("_Zc") get() = this + "bootstrap"
    @SemanticIconMarker val border_all: SemanticIcon
        @JsName("_ad") get() = this + "border all"
    @SemanticIconMarker val border_none: SemanticIcon
        @JsName("_bd") get() = this + "border none"
    @SemanticIconMarker val border_style: SemanticIcon
        @JsName("_cd") get() = this + "border style"
    @SemanticIconMarker val bordered_colored_blue_users: SemanticIcon
        @JsName("_dd") get() = this + "bordered colored blue users"
    @SemanticIconMarker val bordered_colored_green_users: SemanticIcon
        @JsName("_ed") get() = this + "bordered colored green users"
    @SemanticIconMarker val bordered_colored_red_users: SemanticIcon
        @JsName("_fd") get() = this + "bordered colored red users"
    @SemanticIconMarker val bordered_inverted_black_users: SemanticIcon
        @JsName("_gd") get() = this + "bordered inverted black users"
    @SemanticIconMarker val bordered_inverted_teal_users: SemanticIcon
        @JsName("_hd") get() = this + "bordered inverted teal users"
    @SemanticIconMarker val bordered_teal_users: SemanticIcon
        @JsName("_id") get() = this + "bordered teal users"
    @SemanticIconMarker val bordered_users: SemanticIcon
        @JsName("_jd") get() = this + "bordered users"
    @SemanticIconMarker val bottom_left_corner_add: SemanticIcon
        @JsName("_kd") get() = this + "bottom left corner add"
    @SemanticIconMarker val bottom_right_corner_add: SemanticIcon
        @JsName("_ld") get() = this + "bottom right corner add"
    @SemanticIconMarker val bowling_ball: SemanticIcon
        @JsName("_md") get() = this + "bowling ball"
    @SemanticIconMarker val box: SemanticIcon
        @JsName("_nd") get() = this + "box"
    @SemanticIconMarker val box_open: SemanticIcon
        @JsName("_od") get() = this + "box open"
    @SemanticIconMarker val box_tissue: SemanticIcon
        @JsName("_pd") get() = this + "box tissue"
    @SemanticIconMarker val boxes: SemanticIcon
        @JsName("_qd") get() = this + "boxes"
    @SemanticIconMarker val braille: SemanticIcon
        @JsName("_rd") get() = this + "braille"
    @SemanticIconMarker val brain: SemanticIcon
        @JsName("_sd") get() = this + "brain"
    @SemanticIconMarker val bread_slice: SemanticIcon
        @JsName("_td") get() = this + "bread slice"
    @SemanticIconMarker val briefcase: SemanticIcon
        @JsName("_ud") get() = this + "briefcase"
    @SemanticIconMarker val briefcase_medical: SemanticIcon
        @JsName("_vd") get() = this + "briefcase medical"
    @SemanticIconMarker val broadcast_tower: SemanticIcon
        @JsName("_wd") get() = this + "broadcast tower"
    @SemanticIconMarker val broom: SemanticIcon
        @JsName("_xd") get() = this + "broom"
    @SemanticIconMarker val brown_users: SemanticIcon
        @JsName("_yd") get() = this + "brown users"
    @SemanticIconMarker val brush: SemanticIcon
        @JsName("_zd") get() = this + "brush"
    @SemanticIconMarker val btc: SemanticIcon
        @JsName("_Ad") get() = this + "btc"
    @SemanticIconMarker val buffer: SemanticIcon
        @JsName("_Bd") get() = this + "buffer"
    @SemanticIconMarker val bug: SemanticIcon
        @JsName("_Cd") get() = this + "bug"
    @SemanticIconMarker val building: SemanticIcon
        @JsName("_Dd") get() = this + "building"
    @SemanticIconMarker val building_outline: SemanticIcon
        @JsName("_Ed") get() = this + "building outline"
    @SemanticIconMarker val bullhorn: SemanticIcon
        @JsName("_Fd") get() = this + "bullhorn"
    @SemanticIconMarker val bullseye: SemanticIcon
        @JsName("_Gd") get() = this + "bullseye"
    @SemanticIconMarker val burn: SemanticIcon
        @JsName("_Hd") get() = this + "burn"
    @SemanticIconMarker val buromobelexperte: SemanticIcon
        @JsName("_Id") get() = this + "buromobelexperte"
    @SemanticIconMarker val bus: SemanticIcon
        @JsName("_Jd") get() = this + "bus"
    @SemanticIconMarker val bus_alternate: SemanticIcon
        @JsName("_Kd") get() = this + "bus alternate"
    @SemanticIconMarker val business_time: SemanticIcon
        @JsName("_Ld") get() = this + "business time"
    @SemanticIconMarker val buy_n_large: SemanticIcon
        @JsName("_Md") get() = this + "buy n large"
    @SemanticIconMarker val buysellads: SemanticIcon
        @JsName("_Nd") get() = this + "buysellads"
    @SemanticIconMarker val calculator: SemanticIcon
        @JsName("_Od") get() = this + "calculator"
    @SemanticIconMarker val calendar: SemanticIcon
        @JsName("_Pd") get() = this + "calendar"
    @SemanticIconMarker val calendar_alternate: SemanticIcon
        @JsName("_Qd") get() = this + "calendar alternate"
    @SemanticIconMarker val calendar_alternate_outline: SemanticIcon
        @JsName("_Rd") get() = this + "calendar alternate outline"
    @SemanticIconMarker val calendar_check: SemanticIcon
        @JsName("_Sd") get() = this + "calendar check"
    @SemanticIconMarker val calendar_check_outline: SemanticIcon
        @JsName("_Td") get() = this + "calendar check outline"
    @SemanticIconMarker val calendar_day: SemanticIcon
        @JsName("_Ud") get() = this + "calendar day"
    @SemanticIconMarker val calendar_minus: SemanticIcon
        @JsName("_Vd") get() = this + "calendar minus"
    @SemanticIconMarker val calendar_minus_outline: SemanticIcon
        @JsName("_Wd") get() = this + "calendar minus outline"
    @SemanticIconMarker val calendar_outline: SemanticIcon
        @JsName("_Xd") get() = this + "calendar outline"
    @SemanticIconMarker val calendar_plus: SemanticIcon
        @JsName("_Yd") get() = this + "calendar plus"
    @SemanticIconMarker val calendar_plus_outline: SemanticIcon
        @JsName("_Zd") get() = this + "calendar plus outline"
    @SemanticIconMarker val calendar_times: SemanticIcon
        @JsName("_ae") get() = this + "calendar times"
    @SemanticIconMarker val calendar_times_outline: SemanticIcon
        @JsName("_be") get() = this + "calendar times outline"
    @SemanticIconMarker val calendar_week: SemanticIcon
        @JsName("_ce") get() = this + "calendar week"
    @SemanticIconMarker val camera: SemanticIcon
        @JsName("_de") get() = this + "camera"
    @SemanticIconMarker val camera_retro: SemanticIcon
        @JsName("_ee") get() = this + "camera retro"
    @SemanticIconMarker val campground: SemanticIcon
        @JsName("_fe") get() = this + "campground"
    @SemanticIconMarker val canadian_maple_leaf: SemanticIcon
        @JsName("_ge") get() = this + "canadian maple leaf"
    @SemanticIconMarker val candy_cane: SemanticIcon
        @JsName("_he") get() = this + "candy cane"
    @SemanticIconMarker val cannabis: SemanticIcon
        @JsName("_ie") get() = this + "cannabis"
    @SemanticIconMarker val capsules: SemanticIcon
        @JsName("_je") get() = this + "capsules"
    @SemanticIconMarker val car: SemanticIcon
        @JsName("_ke") get() = this + "car"
    @SemanticIconMarker val car_alternate: SemanticIcon
        @JsName("_le") get() = this + "car alternate"
    @SemanticIconMarker val car_battery: SemanticIcon
        @JsName("_me") get() = this + "car battery"
    @SemanticIconMarker val car_crash: SemanticIcon
        @JsName("_ne") get() = this + "car crash"
    @SemanticIconMarker val car_side: SemanticIcon
        @JsName("_oe") get() = this + "car side"
    @SemanticIconMarker val caravan: SemanticIcon
        @JsName("_pe") get() = this + "caravan"
    @SemanticIconMarker val caret_down: SemanticIcon
        @JsName("_qe") get() = this + "caret down"
    @SemanticIconMarker val caret_left: SemanticIcon
        @JsName("_re") get() = this + "caret left"
    @SemanticIconMarker val caret_right: SemanticIcon
        @JsName("_se") get() = this + "caret right"
    @SemanticIconMarker val caret_square_down: SemanticIcon
        @JsName("_te") get() = this + "caret square down"
    @SemanticIconMarker val caret_square_down_outline: SemanticIcon
        @JsName("_ue") get() = this + "caret square down outline"
    @SemanticIconMarker val caret_square_left: SemanticIcon
        @JsName("_ve") get() = this + "caret square left"
    @SemanticIconMarker val caret_square_left_outline: SemanticIcon
        @JsName("_we") get() = this + "caret square left outline"
    @SemanticIconMarker val caret_square_right: SemanticIcon
        @JsName("_xe") get() = this + "caret square right"
    @SemanticIconMarker val caret_square_right_outline: SemanticIcon
        @JsName("_ye") get() = this + "caret square right outline"
    @SemanticIconMarker val caret_square_up: SemanticIcon
        @JsName("_ze") get() = this + "caret square up"
    @SemanticIconMarker val caret_square_up_outline: SemanticIcon
        @JsName("_Ae") get() = this + "caret square up outline"
    @SemanticIconMarker val caret_up: SemanticIcon
        @JsName("_Be") get() = this + "caret up"
    @SemanticIconMarker val carrot: SemanticIcon
        @JsName("_Ce") get() = this + "carrot"
    @SemanticIconMarker val cart_arrow_down: SemanticIcon
        @JsName("_De") get() = this + "cart arrow down"
    @SemanticIconMarker val cart_plus: SemanticIcon
        @JsName("_Ee") get() = this + "cart plus"
    @SemanticIconMarker val cash_register: SemanticIcon
        @JsName("_Fe") get() = this + "cash register"
    @SemanticIconMarker val cat: SemanticIcon
        @JsName("_Ge") get() = this + "cat"
    @SemanticIconMarker val cc_amazon_pay: SemanticIcon
        @JsName("_He") get() = this + "cc amazon pay"
    @SemanticIconMarker val cc_amex: SemanticIcon
        @JsName("_Ie") get() = this + "cc amex"
    @SemanticIconMarker val cc_apple_pay: SemanticIcon
        @JsName("_Je") get() = this + "cc apple pay"
    @SemanticIconMarker val cc_diners_club: SemanticIcon
        @JsName("_Ke") get() = this + "cc diners club"
    @SemanticIconMarker val cc_discover: SemanticIcon
        @JsName("_Le") get() = this + "cc discover"
    @SemanticIconMarker val cc_jcb: SemanticIcon
        @JsName("_Me") get() = this + "cc jcb"
    @SemanticIconMarker val cc_mastercard: SemanticIcon
        @JsName("_Ne") get() = this + "cc mastercard"
    @SemanticIconMarker val cc_paypal: SemanticIcon
        @JsName("_Oe") get() = this + "cc paypal"
    @SemanticIconMarker val cc_stripe: SemanticIcon
        @JsName("_Pe") get() = this + "cc stripe"
    @SemanticIconMarker val cc_visa: SemanticIcon
        @JsName("_Qe") get() = this + "cc visa"
    @SemanticIconMarker val centercode: SemanticIcon
        @JsName("_Re") get() = this + "centercode"
    @SemanticIconMarker val centos: SemanticIcon
        @JsName("_Se") get() = this + "centos"
    @SemanticIconMarker val certificate: SemanticIcon
        @JsName("_Te") get() = this + "certificate"
    @SemanticIconMarker val chair: SemanticIcon
        @JsName("_Ue") get() = this + "chair"
    @SemanticIconMarker val chalkboard: SemanticIcon
        @JsName("_Ve") get() = this + "chalkboard"
    @SemanticIconMarker val chalkboard_teacher: SemanticIcon
        @JsName("_We") get() = this + "chalkboard teacher"
    @SemanticIconMarker val charging_station: SemanticIcon
        @JsName("_Xe") get() = this + "charging station"
    @SemanticIconMarker val chart_area: SemanticIcon
        @JsName("_Ye") get() = this + "chart area"
    @SemanticIconMarker val chart_bar: SemanticIcon
        @JsName("_Ze") get() = this + "chart bar"
    @SemanticIconMarker val chart_bar_outline: SemanticIcon
        @JsName("_af") get() = this + "chart bar outline"
    @SemanticIconMarker val chart_line: SemanticIcon
        @JsName("_bf") get() = this + "chart line"
    @SemanticIconMarker val chart_pie: SemanticIcon
        @JsName("_cf") get() = this + "chart pie"
    @SemanticIconMarker val check: SemanticIcon
        @JsName("_df") get() = this + "check"
    @SemanticIconMarker val check_circle: SemanticIcon
        @JsName("_ef") get() = this + "check circle"
    @SemanticIconMarker val check_circle_outline: SemanticIcon
        @JsName("_ff") get() = this + "check circle outline"
    @SemanticIconMarker val check_double: SemanticIcon
        @JsName("_gf") get() = this + "check double"
    @SemanticIconMarker val check_square: SemanticIcon
        @JsName("_hf") get() = this + "check square"
    @SemanticIconMarker val check_square_outline: SemanticIcon
        @JsName("_if") get() = this + "check square outline"
    @SemanticIconMarker val cheese: SemanticIcon
        @JsName("_jf") get() = this + "cheese"
    @SemanticIconMarker val chess: SemanticIcon
        @JsName("_kf") get() = this + "chess"
    @SemanticIconMarker val chess_bishop: SemanticIcon
        @JsName("_lf") get() = this + "chess bishop"
    @SemanticIconMarker val chess_board: SemanticIcon
        @JsName("_mf") get() = this + "chess board"
    @SemanticIconMarker val chess_king: SemanticIcon
        @JsName("_nf") get() = this + "chess king"
    @SemanticIconMarker val chess_knight: SemanticIcon
        @JsName("_of") get() = this + "chess knight"
    @SemanticIconMarker val chess_pawn: SemanticIcon
        @JsName("_pf") get() = this + "chess pawn"
    @SemanticIconMarker val chess_queen: SemanticIcon
        @JsName("_qf") get() = this + "chess queen"
    @SemanticIconMarker val chess_rook: SemanticIcon
        @JsName("_rf") get() = this + "chess rook"
    @SemanticIconMarker val chevron_circle_down: SemanticIcon
        @JsName("_sf") get() = this + "chevron circle down"
    @SemanticIconMarker val chevron_circle_left: SemanticIcon
        @JsName("_tf") get() = this + "chevron circle left"
    @SemanticIconMarker val chevron_circle_right: SemanticIcon
        @JsName("_uf") get() = this + "chevron circle right"
    @SemanticIconMarker val chevron_circle_up: SemanticIcon
        @JsName("_vf") get() = this + "chevron circle up"
    @SemanticIconMarker val chevron_down: SemanticIcon
        @JsName("_wf") get() = this + "chevron down"
    @SemanticIconMarker val chevron_left: SemanticIcon
        @JsName("_xf") get() = this + "chevron left"
    @SemanticIconMarker val chevron_right: SemanticIcon
        @JsName("_yf") get() = this + "chevron right"
    @SemanticIconMarker val chevron_up: SemanticIcon
        @JsName("_zf") get() = this + "chevron up"
    @SemanticIconMarker val child: SemanticIcon
        @JsName("_Af") get() = this + "child"
    @SemanticIconMarker val chrome: SemanticIcon
        @JsName("_Bf") get() = this + "chrome"
    @SemanticIconMarker val chromecast: SemanticIcon
        @JsName("_Cf") get() = this + "chromecast"
    @SemanticIconMarker val church: SemanticIcon
        @JsName("_Df") get() = this + "church"
    @SemanticIconMarker val circle: SemanticIcon
        @JsName("_Ef") get() = this + "circle"
    @SemanticIconMarker val circle_notch: SemanticIcon
        @JsName("_Ff") get() = this + "circle notch"
    @SemanticIconMarker val circle_outline: SemanticIcon
        @JsName("_Gf") get() = this + "circle outline"
    @SemanticIconMarker val circular_colored_blue_users: SemanticIcon
        @JsName("_Hf") get() = this + "circular colored blue users"
    @SemanticIconMarker val circular_colored_green_users: SemanticIcon
        @JsName("_If") get() = this + "circular colored green users"
    @SemanticIconMarker val circular_colored_red_users: SemanticIcon
        @JsName("_Jf") get() = this + "circular colored red users"
    @SemanticIconMarker val circular_inverted_teal_users: SemanticIcon
        @JsName("_Kf") get() = this + "circular inverted teal users"
    @SemanticIconMarker val circular_inverted_users: SemanticIcon
        @JsName("_Lf") get() = this + "circular inverted users"
    @SemanticIconMarker val circular_teal_users: SemanticIcon
        @JsName("_Mf") get() = this + "circular teal users"
    @SemanticIconMarker val circular_users: SemanticIcon
        @JsName("_Nf") get() = this + "circular users"
    @SemanticIconMarker val city: SemanticIcon
        @JsName("_Of") get() = this + "city"
    @SemanticIconMarker val clinic_medical: SemanticIcon
        @JsName("_Pf") get() = this + "clinic medical"
    @SemanticIconMarker val clipboard: SemanticIcon
        @JsName("_Qf") get() = this + "clipboard"
    @SemanticIconMarker val clipboard_check: SemanticIcon
        @JsName("_Rf") get() = this + "clipboard check"
    @SemanticIconMarker val clipboard_list: SemanticIcon
        @JsName("_Sf") get() = this + "clipboard list"
    @SemanticIconMarker val clipboard_outline: SemanticIcon
        @JsName("_Tf") get() = this + "clipboard outline"
    @SemanticIconMarker val clock: SemanticIcon
        @JsName("_Uf") get() = this + "clock"
    @SemanticIconMarker val clock_outline: SemanticIcon
        @JsName("_Vf") get() = this + "clock outline"
    @SemanticIconMarker val clockwise_rotated_cloud: SemanticIcon
        @JsName("_Wf") get() = this + "clockwise rotated cloud"
    @SemanticIconMarker val clone: SemanticIcon
        @JsName("_Xf") get() = this + "clone"
    @SemanticIconMarker val clone_outline: SemanticIcon
        @JsName("_Yf") get() = this + "clone outline"
    @SemanticIconMarker val close: SemanticIcon
        @JsName("_Zf") get() = this + "close"
    @SemanticIconMarker val close_link: SemanticIcon
        @JsName("_ag") get() = this + "close link"
    @SemanticIconMarker val closed_captioning: SemanticIcon
        @JsName("_bg") get() = this + "closed captioning"
    @SemanticIconMarker val closed_captioning_outline: SemanticIcon
        @JsName("_cg") get() = this + "closed captioning outline"
    @SemanticIconMarker val cloud: SemanticIcon
        @JsName("_dg") get() = this + "cloud"
    @SemanticIconMarker val cloud_download_alternate: SemanticIcon
        @JsName("_eg") get() = this + "cloud download alternate"
    @SemanticIconMarker val cloud_meatball: SemanticIcon
        @JsName("_fg") get() = this + "cloud meatball"
    @SemanticIconMarker val cloud_moon: SemanticIcon
        @JsName("_gg") get() = this + "cloud moon"
    @SemanticIconMarker val cloud_moon_rain: SemanticIcon
        @JsName("_hg") get() = this + "cloud moon rain"
    @SemanticIconMarker val cloud_rain: SemanticIcon
        @JsName("_ig") get() = this + "cloud rain"
    @SemanticIconMarker val cloud_showers_heavy: SemanticIcon
        @JsName("_jg") get() = this + "cloud showers heavy"
    @SemanticIconMarker val cloud_sun: SemanticIcon
        @JsName("_kg") get() = this + "cloud sun"
    @SemanticIconMarker val cloud_sun_rain: SemanticIcon
        @JsName("_lg") get() = this + "cloud sun rain"
    @SemanticIconMarker val cloud_upload_alternate: SemanticIcon
        @JsName("_mg") get() = this + "cloud upload alternate"
    @SemanticIconMarker val cloudscale: SemanticIcon
        @JsName("_ng") get() = this + "cloudscale"
    @SemanticIconMarker val cloudsmith: SemanticIcon
        @JsName("_og") get() = this + "cloudsmith"
    @SemanticIconMarker val cloudversify: SemanticIcon
        @JsName("_pg") get() = this + "cloudversify"
    @SemanticIconMarker val cocktail: SemanticIcon
        @JsName("_qg") get() = this + "cocktail"
    @SemanticIconMarker val code: SemanticIcon
        @JsName("_rg") get() = this + "code"
    @SemanticIconMarker val code_branch: SemanticIcon
        @JsName("_sg") get() = this + "code branch"
    @SemanticIconMarker val codepen: SemanticIcon
        @JsName("_tg") get() = this + "codepen"
    @SemanticIconMarker val codiepie: SemanticIcon
        @JsName("_ug") get() = this + "codiepie"
    @SemanticIconMarker val coffee: SemanticIcon
        @JsName("_vg") get() = this + "coffee"
    @SemanticIconMarker val cog: SemanticIcon
        @JsName("_wg") get() = this + "cog"
    @SemanticIconMarker val cogs: SemanticIcon
        @JsName("_xg") get() = this + "cogs"
    @SemanticIconMarker val coins: SemanticIcon
        @JsName("_yg") get() = this + "coins"
    @SemanticIconMarker val columns: SemanticIcon
        @JsName("_zg") get() = this + "columns"
    @SemanticIconMarker val comment: SemanticIcon
        @JsName("_Ag") get() = this + "comment"
    @SemanticIconMarker val comment_alternate: SemanticIcon
        @JsName("_Bg") get() = this + "comment alternate"
    @SemanticIconMarker val comment_alternate_outline: SemanticIcon
        @JsName("_Cg") get() = this + "comment alternate outline"
    @SemanticIconMarker val comment_dollar: SemanticIcon
        @JsName("_Dg") get() = this + "comment dollar"
    @SemanticIconMarker val comment_dots: SemanticIcon
        @JsName("_Eg") get() = this + "comment dots"
    @SemanticIconMarker val comment_dots_outline: SemanticIcon
        @JsName("_Fg") get() = this + "comment dots outline"
    @SemanticIconMarker val comment_medical: SemanticIcon
        @JsName("_Gg") get() = this + "comment medical"
    @SemanticIconMarker val comment_outline: SemanticIcon
        @JsName("_Hg") get() = this + "comment outline"
    @SemanticIconMarker val comment_slash: SemanticIcon
        @JsName("_Ig") get() = this + "comment slash"
    @SemanticIconMarker val comments: SemanticIcon
        @JsName("_Jg") get() = this + "comments"
    @SemanticIconMarker val comments_dollar: SemanticIcon
        @JsName("_Kg") get() = this + "comments dollar"
    @SemanticIconMarker val comments_outline: SemanticIcon
        @JsName("_Lg") get() = this + "comments outline"
    @SemanticIconMarker val compact_disc: SemanticIcon
        @JsName("_Mg") get() = this + "compact disc"
    @SemanticIconMarker val compass: SemanticIcon
        @JsName("_Ng") get() = this + "compass"
    @SemanticIconMarker val compass_outline: SemanticIcon
        @JsName("_Og") get() = this + "compass outline"
    @SemanticIconMarker val compress: SemanticIcon
        @JsName("_Pg") get() = this + "compress"
    @SemanticIconMarker val compress_alternate: SemanticIcon
        @JsName("_Qg") get() = this + "compress alternate"
    @SemanticIconMarker val compress_arrows_alternate: SemanticIcon
        @JsName("_Rg") get() = this + "compress arrows alternate"
    @SemanticIconMarker val concierge_bell: SemanticIcon
        @JsName("_Sg") get() = this + "concierge bell"
    @SemanticIconMarker val confluence: SemanticIcon
        @JsName("_Tg") get() = this + "confluence"
    @SemanticIconMarker val connectdevelop: SemanticIcon
        @JsName("_Ug") get() = this + "connectdevelop"
    @SemanticIconMarker val contao: SemanticIcon
        @JsName("_Vg") get() = this + "contao"
    @SemanticIconMarker val content: SemanticIcon
        @JsName("_Wg") get() = this + "content"
    @SemanticIconMarker val cookie: SemanticIcon
        @JsName("_Xg") get() = this + "cookie"
    @SemanticIconMarker val cookie_bite: SemanticIcon
        @JsName("_Yg") get() = this + "cookie bite"
    @SemanticIconMarker val copy: SemanticIcon
        @JsName("_Zg") get() = this + "copy"
    @SemanticIconMarker val copy_outline: SemanticIcon
        @JsName("_ah") get() = this + "copy outline"
    @SemanticIconMarker val copyright: SemanticIcon
        @JsName("_bh") get() = this + "copyright"
    @SemanticIconMarker val copyright_outline: SemanticIcon
        @JsName("_ch") get() = this + "copyright outline"
    @SemanticIconMarker val corner_add: SemanticIcon
        @JsName("_dh") get() = this + "corner add"
    @SemanticIconMarker val cotton_bureau: SemanticIcon
        @JsName("_eh") get() = this + "cotton bureau"
    @SemanticIconMarker val couch: SemanticIcon
        @JsName("_fh") get() = this + "couch"
    @SemanticIconMarker val counterclockwise_rotated_cloud: SemanticIcon
        @JsName("_gh") get() = this + "counterclockwise rotated cloud"
    @SemanticIconMarker val cpanel: SemanticIcon
        @JsName("_hh") get() = this + "cpanel"
    @SemanticIconMarker val creative_commons: SemanticIcon
        @JsName("_ih") get() = this + "creative commons"
    @SemanticIconMarker val creative_commons_by: SemanticIcon
        @JsName("_jh") get() = this + "creative commons by"
    @SemanticIconMarker val creative_commons_nc: SemanticIcon
        @JsName("_kh") get() = this + "creative commons nc"
    @SemanticIconMarker val creative_commons_nc_eu: SemanticIcon
        @JsName("_lh") get() = this + "creative commons nc eu"
    @SemanticIconMarker val creative_commons_nc_jp: SemanticIcon
        @JsName("_mh") get() = this + "creative commons nc jp"
    @SemanticIconMarker val creative_commons_nd: SemanticIcon
        @JsName("_nh") get() = this + "creative commons nd"
    @SemanticIconMarker val creative_commons_pd: SemanticIcon
        @JsName("_oh") get() = this + "creative commons pd"
    @SemanticIconMarker val creative_commons_pd_alternate: SemanticIcon
        @JsName("_ph") get() = this + "creative commons pd alternate"
    @SemanticIconMarker val creative_commons_remix: SemanticIcon
        @JsName("_qh") get() = this + "creative commons remix"
    @SemanticIconMarker val creative_commons_sa: SemanticIcon
        @JsName("_rh") get() = this + "creative commons sa"
    @SemanticIconMarker val creative_commons_sampling: SemanticIcon
        @JsName("_sh") get() = this + "creative commons sampling"
    @SemanticIconMarker val creative_commons_sampling_plus: SemanticIcon
        @JsName("_th") get() = this + "creative commons sampling plus"
    @SemanticIconMarker val creative_commons_share: SemanticIcon
        @JsName("_uh") get() = this + "creative commons share"
    @SemanticIconMarker val creative_commons_zero: SemanticIcon
        @JsName("_vh") get() = this + "creative commons zero"
    @SemanticIconMarker val credit_card: SemanticIcon
        @JsName("_wh") get() = this + "credit card"
    @SemanticIconMarker val credit_card_outline: SemanticIcon
        @JsName("_xh") get() = this + "credit card outline"
    @SemanticIconMarker val critical_role: SemanticIcon
        @JsName("_yh") get() = this + "critical role"
    @SemanticIconMarker val crop: SemanticIcon
        @JsName("_zh") get() = this + "crop"
    @SemanticIconMarker val crop_alternate: SemanticIcon
        @JsName("_Ah") get() = this + "crop alternate"
    @SemanticIconMarker val cross: SemanticIcon
        @JsName("_Bh") get() = this + "cross"
    @SemanticIconMarker val crosshairs: SemanticIcon
        @JsName("_Ch") get() = this + "crosshairs"
    @SemanticIconMarker val crow: SemanticIcon
        @JsName("_Dh") get() = this + "crow"
    @SemanticIconMarker val crutch: SemanticIcon
        @JsName("_Eh") get() = this + "crutch"
    @SemanticIconMarker val css3: SemanticIcon
        @JsName("_Fh") get() = this + "css3"
    @SemanticIconMarker val css3_alternate: SemanticIcon
        @JsName("_Gh") get() = this + "css3 alternate"
    @SemanticIconMarker val cube: SemanticIcon
        @JsName("_Hh") get() = this + "cube"
    @SemanticIconMarker val cubes: SemanticIcon
        @JsName("_Ih") get() = this + "cubes"
    @SemanticIconMarker val cut: SemanticIcon
        @JsName("_Jh") get() = this + "cut"
    @SemanticIconMarker val cuttlefish: SemanticIcon
        @JsName("_Kh") get() = this + "cuttlefish"
    @SemanticIconMarker val d_and_d: SemanticIcon
        @JsName("_Lh") get() = this + "d and d"
    @SemanticIconMarker val d_and_d_beyond: SemanticIcon
        @JsName("_Mh") get() = this + "d and d beyond"
    @SemanticIconMarker val dailymotion: SemanticIcon
        @JsName("_Nh") get() = this + "dailymotion"
    @SemanticIconMarker val dashcube: SemanticIcon
        @JsName("_Oh") get() = this + "dashcube"
    @SemanticIconMarker val database: SemanticIcon
        @JsName("_Ph") get() = this + "database"
    @SemanticIconMarker val deaf: SemanticIcon
        @JsName("_Qh") get() = this + "deaf"
    @SemanticIconMarker val delicious: SemanticIcon
        @JsName("_Rh") get() = this + "delicious"
    @SemanticIconMarker val democrat: SemanticIcon
        @JsName("_Sh") get() = this + "democrat"
    @SemanticIconMarker val deploydog: SemanticIcon
        @JsName("_Th") get() = this + "deploydog"
    @SemanticIconMarker val deskpro: SemanticIcon
        @JsName("_Uh") get() = this + "deskpro"
    @SemanticIconMarker val desktop: SemanticIcon
        @JsName("_Vh") get() = this + "desktop"
    @SemanticIconMarker val dev: SemanticIcon
        @JsName("_Wh") get() = this + "dev"
    @SemanticIconMarker val deviantart: SemanticIcon
        @JsName("_Xh") get() = this + "deviantart"
    @SemanticIconMarker val dharmachakra: SemanticIcon
        @JsName("_Yh") get() = this + "dharmachakra"
    @SemanticIconMarker val dhl: SemanticIcon
        @JsName("_Zh") get() = this + "dhl"
    @SemanticIconMarker val diagnoses: SemanticIcon
        @JsName("_ai") get() = this + "diagnoses"
    @SemanticIconMarker val diaspora: SemanticIcon
        @JsName("_bi") get() = this + "diaspora"
    @SemanticIconMarker val dice: SemanticIcon
        @JsName("_ci") get() = this + "dice"
    @SemanticIconMarker val dice_d20: SemanticIcon
        @JsName("_di") get() = this + "dice d20"
    @SemanticIconMarker val dice_d6: SemanticIcon
        @JsName("_ei") get() = this + "dice d6"
    @SemanticIconMarker val dice_five: SemanticIcon
        @JsName("_fi") get() = this + "dice five"
    @SemanticIconMarker val dice_four: SemanticIcon
        @JsName("_gi") get() = this + "dice four"
    @SemanticIconMarker val dice_one: SemanticIcon
        @JsName("_hi") get() = this + "dice one"
    @SemanticIconMarker val dice_six: SemanticIcon
        @JsName("_ii") get() = this + "dice six"
    @SemanticIconMarker val dice_three: SemanticIcon
        @JsName("_ji") get() = this + "dice three"
    @SemanticIconMarker val dice_two: SemanticIcon
        @JsName("_ki") get() = this + "dice two"
    @SemanticIconMarker val digg: SemanticIcon
        @JsName("_li") get() = this + "digg"
    @SemanticIconMarker val digital_ocean: SemanticIcon
        @JsName("_mi") get() = this + "digital ocean"
    @SemanticIconMarker val digital_tachograph: SemanticIcon
        @JsName("_ni") get() = this + "digital tachograph"
    @SemanticIconMarker val directions: SemanticIcon
        @JsName("_oi") get() = this + "directions"
    @SemanticIconMarker val disabled_users: SemanticIcon
        @JsName("_pi") get() = this + "disabled users"
    @SemanticIconMarker val discord: SemanticIcon
        @JsName("_qi") get() = this + "discord"
    @SemanticIconMarker val discourse: SemanticIcon
        @JsName("_ri") get() = this + "discourse"
    @SemanticIconMarker val disease: SemanticIcon
        @JsName("_si") get() = this + "disease"
    @SemanticIconMarker val divide: SemanticIcon
        @JsName("_ti") get() = this + "divide"
    @SemanticIconMarker val dizzy: SemanticIcon
        @JsName("_ui") get() = this + "dizzy"
    @SemanticIconMarker val dizzy_outline: SemanticIcon
        @JsName("_vi") get() = this + "dizzy outline"
    @SemanticIconMarker val dna: SemanticIcon
        @JsName("_wi") get() = this + "dna"
    @SemanticIconMarker val dochub: SemanticIcon
        @JsName("_xi") get() = this + "dochub"
    @SemanticIconMarker val docker: SemanticIcon
        @JsName("_yi") get() = this + "docker"
    @SemanticIconMarker val dog: SemanticIcon
        @JsName("_zi") get() = this + "dog"
    @SemanticIconMarker val dollar_sign: SemanticIcon
        @JsName("_Ai") get() = this + "dollar sign"
    @SemanticIconMarker val dolly: SemanticIcon
        @JsName("_Bi") get() = this + "dolly"
    @SemanticIconMarker val dolly_flatbed: SemanticIcon
        @JsName("_Ci") get() = this + "dolly flatbed"
    @SemanticIconMarker val donate: SemanticIcon
        @JsName("_Di") get() = this + "donate"
    @SemanticIconMarker val door_closed: SemanticIcon
        @JsName("_Ei") get() = this + "door closed"
    @SemanticIconMarker val door_open: SemanticIcon
        @JsName("_Fi") get() = this + "door open"
    @SemanticIconMarker val dot_circle: SemanticIcon
        @JsName("_Gi") get() = this + "dot circle"
    @SemanticIconMarker val dot_circle_outline: SemanticIcon
        @JsName("_Hi") get() = this + "dot circle outline"
    @SemanticIconMarker val dove: SemanticIcon
        @JsName("_Ii") get() = this + "dove"
    @SemanticIconMarker val download: SemanticIcon
        @JsName("_Ji") get() = this + "download"
    @SemanticIconMarker val draft2digital: SemanticIcon
        @JsName("_Ki") get() = this + "draft2digital"
    @SemanticIconMarker val drafting_compass: SemanticIcon
        @JsName("_Li") get() = this + "drafting compass"
    @SemanticIconMarker val dragon: SemanticIcon
        @JsName("_Mi") get() = this + "dragon"
    @SemanticIconMarker val draw_polygon: SemanticIcon
        @JsName("_Ni") get() = this + "draw polygon"
    @SemanticIconMarker val dribbble: SemanticIcon
        @JsName("_Oi") get() = this + "dribbble"
    @SemanticIconMarker val dribbble_square: SemanticIcon
        @JsName("_Pi") get() = this + "dribbble square"
    @SemanticIconMarker val dropbox: SemanticIcon
        @JsName("_Qi") get() = this + "dropbox"
    @SemanticIconMarker val dropdown: SemanticIcon
        @JsName("_Ri") get() = this + "dropdown"
    @SemanticIconMarker val drum: SemanticIcon
        @JsName("_Si") get() = this + "drum"
    @SemanticIconMarker val drum_steelpan: SemanticIcon
        @JsName("_Ti") get() = this + "drum steelpan"
    @SemanticIconMarker val drumstick_bite: SemanticIcon
        @JsName("_Ui") get() = this + "drumstick bite"
    @SemanticIconMarker val drupal: SemanticIcon
        @JsName("_Vi") get() = this + "drupal"
    @SemanticIconMarker val dumbbell: SemanticIcon
        @JsName("_Wi") get() = this + "dumbbell"
    @SemanticIconMarker val dumpster: SemanticIcon
        @JsName("_Xi") get() = this + "dumpster"
    @SemanticIconMarker val dungeon: SemanticIcon
        @JsName("_Yi") get() = this + "dungeon"
    @SemanticIconMarker val dyalog: SemanticIcon
        @JsName("_Zi") get() = this + "dyalog"
    @SemanticIconMarker val earlybirds: SemanticIcon
        @JsName("_aj") get() = this + "earlybirds"
    @SemanticIconMarker val ebay: SemanticIcon
        @JsName("_bj") get() = this + "ebay"
    @SemanticIconMarker val edge: SemanticIcon
        @JsName("_cj") get() = this + "edge"
    @SemanticIconMarker val edit: SemanticIcon
        @JsName("_dj") get() = this + "edit"
    @SemanticIconMarker val edit_outline: SemanticIcon
        @JsName("_ej") get() = this + "edit outline"
    @SemanticIconMarker val egg: SemanticIcon
        @JsName("_fj") get() = this + "egg"
    @SemanticIconMarker val eject: SemanticIcon
        @JsName("_gj") get() = this + "eject"
    @SemanticIconMarker val elementor: SemanticIcon
        @JsName("_hj") get() = this + "elementor"
    @SemanticIconMarker val ellipsis_horizontal: SemanticIcon
        @JsName("_ij") get() = this + "ellipsis horizontal"
    @SemanticIconMarker val ellipsis_vertical: SemanticIcon
        @JsName("_jj") get() = this + "ellipsis vertical"
    @SemanticIconMarker val ello: SemanticIcon
        @JsName("_kj") get() = this + "ello"
    @SemanticIconMarker val ember: SemanticIcon
        @JsName("_lj") get() = this + "ember"
    @SemanticIconMarker val empire: SemanticIcon
        @JsName("_mj") get() = this + "empire"
    @SemanticIconMarker val envelope: SemanticIcon
        @JsName("_nj") get() = this + "envelope"
    @SemanticIconMarker val envelope_open: SemanticIcon
        @JsName("_oj") get() = this + "envelope open"
    @SemanticIconMarker val envelope_open_outline: SemanticIcon
        @JsName("_pj") get() = this + "envelope open outline"
    @SemanticIconMarker val envelope_open_text: SemanticIcon
        @JsName("_qj") get() = this + "envelope open text"
    @SemanticIconMarker val envelope_outline: SemanticIcon
        @JsName("_rj") get() = this + "envelope outline"
    @SemanticIconMarker val envelope_square: SemanticIcon
        @JsName("_sj") get() = this + "envelope square"
    @SemanticIconMarker val envira: SemanticIcon
        @JsName("_tj") get() = this + "envira"
    @SemanticIconMarker val equals_: SemanticIcon
        @JsName("_uj") get() = this + "equals"
    @SemanticIconMarker val eraser: SemanticIcon
        @JsName("_vj") get() = this + "eraser"
    @SemanticIconMarker val erlang: SemanticIcon
        @JsName("_wj") get() = this + "erlang"
    @SemanticIconMarker val ethereum: SemanticIcon
        @JsName("_xj") get() = this + "ethereum"
    @SemanticIconMarker val ethernet: SemanticIcon
        @JsName("_yj") get() = this + "ethernet"
    @SemanticIconMarker val etsy: SemanticIcon
        @JsName("_zj") get() = this + "etsy"
    @SemanticIconMarker val euro_sign: SemanticIcon
        @JsName("_Aj") get() = this + "euro sign"
    @SemanticIconMarker val evernote: SemanticIcon
        @JsName("_Bj") get() = this + "evernote"
    @SemanticIconMarker val exchange_alternate: SemanticIcon
        @JsName("_Cj") get() = this + "exchange alternate"
    @SemanticIconMarker val exclamation: SemanticIcon
        @JsName("_Dj") get() = this + "exclamation"
    @SemanticIconMarker val exclamation_circle: SemanticIcon
        @JsName("_Ej") get() = this + "exclamation circle"
    @SemanticIconMarker val exclamation_triangle: SemanticIcon
        @JsName("_Fj") get() = this + "exclamation triangle"
    @SemanticIconMarker val expand: SemanticIcon
        @JsName("_Gj") get() = this + "expand"
    @SemanticIconMarker val expand_alternate: SemanticIcon
        @JsName("_Hj") get() = this + "expand alternate"
    @SemanticIconMarker val expand_arrows_alternate: SemanticIcon
        @JsName("_Ij") get() = this + "expand arrows alternate"
    @SemanticIconMarker val expeditedssl: SemanticIcon
        @JsName("_Jj") get() = this + "expeditedssl"
    @SemanticIconMarker val external_alternate: SemanticIcon
        @JsName("_Kj") get() = this + "external alternate"
    @SemanticIconMarker val external_link_square_alternate: SemanticIcon
        @JsName("_Lj") get() = this + "external link square alternate"
    @SemanticIconMarker val eye: SemanticIcon
        @JsName("_Mj") get() = this + "eye"
    @SemanticIconMarker val eye_dropper: SemanticIcon
        @JsName("_Nj") get() = this + "eye dropper"
    @SemanticIconMarker val eye_outline: SemanticIcon
        @JsName("_Oj") get() = this + "eye outline"
    @SemanticIconMarker val eye_slash: SemanticIcon
        @JsName("_Pj") get() = this + "eye slash"
    @SemanticIconMarker val eye_slash_outline: SemanticIcon
        @JsName("_Qj") get() = this + "eye slash outline"
    @SemanticIconMarker val facebook: SemanticIcon
        @JsName("_Rj") get() = this + "facebook"
    @SemanticIconMarker val facebook_f: SemanticIcon
        @JsName("_Sj") get() = this + "facebook f"
    @SemanticIconMarker val facebook_messenger: SemanticIcon
        @JsName("_Tj") get() = this + "facebook messenger"
    @SemanticIconMarker val facebook_square: SemanticIcon
        @JsName("_Uj") get() = this + "facebook square"
    @SemanticIconMarker val fan: SemanticIcon
        @JsName("_Vj") get() = this + "fan"
    @SemanticIconMarker val fantasy_flight_games: SemanticIcon
        @JsName("_Wj") get() = this + "fantasy flight games"
    @SemanticIconMarker val fast_backward: SemanticIcon
        @JsName("_Xj") get() = this + "fast backward"
    @SemanticIconMarker val fast_forward: SemanticIcon
        @JsName("_Yj") get() = this + "fast forward"
    @SemanticIconMarker val faucet: SemanticIcon
        @JsName("_Zj") get() = this + "faucet"
    @SemanticIconMarker val fax: SemanticIcon
        @JsName("_ak") get() = this + "fax"
    @SemanticIconMarker val feather: SemanticIcon
        @JsName("_bk") get() = this + "feather"
    @SemanticIconMarker val feather_alternate: SemanticIcon
        @JsName("_ck") get() = this + "feather alternate"
    @SemanticIconMarker val fedex: SemanticIcon
        @JsName("_dk") get() = this + "fedex"
    @SemanticIconMarker val fedora: SemanticIcon
        @JsName("_ek") get() = this + "fedora"
    @SemanticIconMarker val female: SemanticIcon
        @JsName("_fk") get() = this + "female"
    @SemanticIconMarker val fighter_jet: SemanticIcon
        @JsName("_gk") get() = this + "fighter jet"
    @SemanticIconMarker val figma: SemanticIcon
        @JsName("_hk") get() = this + "figma"
    @SemanticIconMarker val file: SemanticIcon
        @JsName("_ik") get() = this + "file"
    @SemanticIconMarker val file_alternate: SemanticIcon
        @JsName("_jk") get() = this + "file alternate"
    @SemanticIconMarker val file_alternate_outline: SemanticIcon
        @JsName("_kk") get() = this + "file alternate outline"
    @SemanticIconMarker val file_archive: SemanticIcon
        @JsName("_lk") get() = this + "file archive"
    @SemanticIconMarker val file_archive_outline: SemanticIcon
        @JsName("_mk") get() = this + "file archive outline"
    @SemanticIconMarker val file_audio: SemanticIcon
        @JsName("_nk") get() = this + "file audio"
    @SemanticIconMarker val file_audio_outline: SemanticIcon
        @JsName("_ok") get() = this + "file audio outline"
    @SemanticIconMarker val file_code: SemanticIcon
        @JsName("_pk") get() = this + "file code"
    @SemanticIconMarker val file_code_outline: SemanticIcon
        @JsName("_qk") get() = this + "file code outline"
    @SemanticIconMarker val file_contract: SemanticIcon
        @JsName("_rk") get() = this + "file contract"
    @SemanticIconMarker val file_download: SemanticIcon
        @JsName("_sk") get() = this + "file download"
    @SemanticIconMarker val file_excel: SemanticIcon
        @JsName("_tk") get() = this + "file excel"
    @SemanticIconMarker val file_excel_outline: SemanticIcon
        @JsName("_uk") get() = this + "file excel outline"
    @SemanticIconMarker val file_export: SemanticIcon
        @JsName("_vk") get() = this + "file export"
    @SemanticIconMarker val file_image: SemanticIcon
        @JsName("_wk") get() = this + "file image"
    @SemanticIconMarker val file_image_outline: SemanticIcon
        @JsName("_xk") get() = this + "file image outline"
    @SemanticIconMarker val file_import: SemanticIcon
        @JsName("_yk") get() = this + "file import"
    @SemanticIconMarker val file_invoice: SemanticIcon
        @JsName("_zk") get() = this + "file invoice"
    @SemanticIconMarker val file_invoice_dollar: SemanticIcon
        @JsName("_Ak") get() = this + "file invoice dollar"
    @SemanticIconMarker val file_medical: SemanticIcon
        @JsName("_Bk") get() = this + "file medical"
    @SemanticIconMarker val file_medical_alternate: SemanticIcon
        @JsName("_Ck") get() = this + "file medical alternate"
    @SemanticIconMarker val file_outline: SemanticIcon
        @JsName("_Dk") get() = this + "file outline"
    @SemanticIconMarker val file_pdf: SemanticIcon
        @JsName("_Ek") get() = this + "file pdf"
    @SemanticIconMarker val file_pdf_outline: SemanticIcon
        @JsName("_Fk") get() = this + "file pdf outline"
    @SemanticIconMarker val file_powerpoint: SemanticIcon
        @JsName("_Gk") get() = this + "file powerpoint"
    @SemanticIconMarker val file_powerpoint_outline: SemanticIcon
        @JsName("_Hk") get() = this + "file powerpoint outline"
    @SemanticIconMarker val file_prescription: SemanticIcon
        @JsName("_Ik") get() = this + "file prescription"
    @SemanticIconMarker val file_signature: SemanticIcon
        @JsName("_Jk") get() = this + "file signature"
    @SemanticIconMarker val file_upload: SemanticIcon
        @JsName("_Kk") get() = this + "file upload"
    @SemanticIconMarker val file_video: SemanticIcon
        @JsName("_Lk") get() = this + "file video"
    @SemanticIconMarker val file_video_outline: SemanticIcon
        @JsName("_Mk") get() = this + "file video outline"
    @SemanticIconMarker val file_word: SemanticIcon
        @JsName("_Nk") get() = this + "file word"
    @SemanticIconMarker val file_word_outline: SemanticIcon
        @JsName("_Ok") get() = this + "file word outline"
    @SemanticIconMarker val fill: SemanticIcon
        @JsName("_Pk") get() = this + "fill"
    @SemanticIconMarker val fill_drip: SemanticIcon
        @JsName("_Qk") get() = this + "fill drip"
    @SemanticIconMarker val film: SemanticIcon
        @JsName("_Rk") get() = this + "film"
    @SemanticIconMarker val filter: SemanticIcon
        @JsName("_Sk") get() = this + "filter"
    @SemanticIconMarker val fingerprint: SemanticIcon
        @JsName("_Tk") get() = this + "fingerprint"
    @SemanticIconMarker val fire: SemanticIcon
        @JsName("_Uk") get() = this + "fire"
    @SemanticIconMarker val fire_alternate: SemanticIcon
        @JsName("_Vk") get() = this + "fire alternate"
    @SemanticIconMarker val fire_extinguisher: SemanticIcon
        @JsName("_Wk") get() = this + "fire extinguisher"
    @SemanticIconMarker val firefox: SemanticIcon
        @JsName("_Xk") get() = this + "firefox"
    @SemanticIconMarker val firefox_browser: SemanticIcon
        @JsName("_Yk") get() = this + "firefox browser"
    @SemanticIconMarker val first_aid: SemanticIcon
        @JsName("_Zk") get() = this + "first aid"
    @SemanticIconMarker val first_order: SemanticIcon
        @JsName("_al") get() = this + "first order"
    @SemanticIconMarker val first_order_alternate: SemanticIcon
        @JsName("_bl") get() = this + "first order alternate"
    @SemanticIconMarker val firstdraft: SemanticIcon
        @JsName("_cl") get() = this + "firstdraft"
    @SemanticIconMarker val fish: SemanticIcon
        @JsName("_dl") get() = this + "fish"
    @SemanticIconMarker val fist_raised: SemanticIcon
        @JsName("_el") get() = this + "fist raised"
    @SemanticIconMarker val fitted_help: SemanticIcon
        @JsName("_fl") get() = this + "fitted help"
    @SemanticIconMarker val fitted_small_linkify: SemanticIcon
        @JsName("_gl") get() = this + "fitted small linkify"
    @SemanticIconMarker val flag: SemanticIcon
        @JsName("_hl") get() = this + "flag"
    @SemanticIconMarker val flag_checkered: SemanticIcon
        @JsName("_il") get() = this + "flag checkered"
    @SemanticIconMarker val flag_outline: SemanticIcon
        @JsName("_jl") get() = this + "flag outline"
    @SemanticIconMarker val flag_usa: SemanticIcon
        @JsName("_kl") get() = this + "flag usa"
    @SemanticIconMarker val flask: SemanticIcon
        @JsName("_ll") get() = this + "flask"
    @SemanticIconMarker val flickr: SemanticIcon
        @JsName("_ml") get() = this + "flickr"
    @SemanticIconMarker val flipboard: SemanticIcon
        @JsName("_nl") get() = this + "flipboard"
    @SemanticIconMarker val flushed: SemanticIcon
        @JsName("_ol") get() = this + "flushed"
    @SemanticIconMarker val flushed_outline: SemanticIcon
        @JsName("_pl") get() = this + "flushed outline"
    @SemanticIconMarker val fly: SemanticIcon
        @JsName("_ql") get() = this + "fly"
    @SemanticIconMarker val folder: SemanticIcon
        @JsName("_rl") get() = this + "folder"
    @SemanticIconMarker val folder_minus: SemanticIcon
        @JsName("_sl") get() = this + "folder minus"
    @SemanticIconMarker val folder_open: SemanticIcon
        @JsName("_tl") get() = this + "folder open"
    @SemanticIconMarker val folder_open_outline: SemanticIcon
        @JsName("_ul") get() = this + "folder open outline"
    @SemanticIconMarker val folder_outline: SemanticIcon
        @JsName("_vl") get() = this + "folder outline"
    @SemanticIconMarker val folder_plus: SemanticIcon
        @JsName("_wl") get() = this + "folder plus"
    @SemanticIconMarker val font: SemanticIcon
        @JsName("_xl") get() = this + "font"
    @SemanticIconMarker val font_awesome: SemanticIcon
        @JsName("_yl") get() = this + "font awesome"
    @SemanticIconMarker val font_awesome_alternate: SemanticIcon
        @JsName("_zl") get() = this + "font awesome alternate"
    @SemanticIconMarker val font_awesome_flag: SemanticIcon
        @JsName("_Al") get() = this + "font awesome flag"
    @SemanticIconMarker val fonticons: SemanticIcon
        @JsName("_Bl") get() = this + "fonticons"
    @SemanticIconMarker val fonticons_fi: SemanticIcon
        @JsName("_Cl") get() = this + "fonticons fi"
    @SemanticIconMarker val football_ball: SemanticIcon
        @JsName("_Dl") get() = this + "football ball"
    @SemanticIconMarker val fort_awesome: SemanticIcon
        @JsName("_El") get() = this + "fort awesome"
    @SemanticIconMarker val fort_awesome_alternate: SemanticIcon
        @JsName("_Fl") get() = this + "fort awesome alternate"
    @SemanticIconMarker val forumbee: SemanticIcon
        @JsName("_Gl") get() = this + "forumbee"
    @SemanticIconMarker val forward: SemanticIcon
        @JsName("_Hl") get() = this + "forward"
    @SemanticIconMarker val foursquare: SemanticIcon
        @JsName("_Il") get() = this + "foursquare"
    @SemanticIconMarker val free_code_camp: SemanticIcon
        @JsName("_Jl") get() = this + "free code camp"
    @SemanticIconMarker val freebsd: SemanticIcon
        @JsName("_Kl") get() = this + "freebsd"
    @SemanticIconMarker val frog: SemanticIcon
        @JsName("_Ll") get() = this + "frog"
    @SemanticIconMarker val frown: SemanticIcon
        @JsName("_Ml") get() = this + "frown"
    @SemanticIconMarker val frown_open: SemanticIcon
        @JsName("_Nl") get() = this + "frown open"
    @SemanticIconMarker val frown_open_outline: SemanticIcon
        @JsName("_Ol") get() = this + "frown open outline"
    @SemanticIconMarker val frown_outline: SemanticIcon
        @JsName("_Pl") get() = this + "frown outline"
    @SemanticIconMarker val fruitapple: SemanticIcon
        @JsName("_Ql") get() = this + "fruit-apple"
    @SemanticIconMarker val fulcrum: SemanticIcon
        @JsName("_Rl") get() = this + "fulcrum"
    @SemanticIconMarker val funnel_dollar: SemanticIcon
        @JsName("_Sl") get() = this + "funnel dollar"
    @SemanticIconMarker val futbol: SemanticIcon
        @JsName("_Tl") get() = this + "futbol"
    @SemanticIconMarker val futbol_outline: SemanticIcon
        @JsName("_Ul") get() = this + "futbol outline"
    @SemanticIconMarker val galactic_republic: SemanticIcon
        @JsName("_Vl") get() = this + "galactic republic"
    @SemanticIconMarker val galactic_senate: SemanticIcon
        @JsName("_Wl") get() = this + "galactic senate"
    @SemanticIconMarker val gamepad: SemanticIcon
        @JsName("_Xl") get() = this + "gamepad"
    @SemanticIconMarker val gas_pump: SemanticIcon
        @JsName("_Yl") get() = this + "gas pump"
    @SemanticIconMarker val gavel: SemanticIcon
        @JsName("_Zl") get() = this + "gavel"
    @SemanticIconMarker val gem: SemanticIcon
        @JsName("_am") get() = this + "gem"
    @SemanticIconMarker val gem_outline: SemanticIcon
        @JsName("_bm") get() = this + "gem outline"
    @SemanticIconMarker val genderless: SemanticIcon
        @JsName("_cm") get() = this + "genderless"
    @SemanticIconMarker val get_pocket: SemanticIcon
        @JsName("_dm") get() = this + "get pocket"
    @SemanticIconMarker val gg: SemanticIcon
        @JsName("_em") get() = this + "gg"
    @SemanticIconMarker val gg_circle: SemanticIcon
        @JsName("_fm") get() = this + "gg circle"
    @SemanticIconMarker val ghost: SemanticIcon
        @JsName("_gm") get() = this + "ghost"
    @SemanticIconMarker val gift: SemanticIcon
        @JsName("_hm") get() = this + "gift"
    @SemanticIconMarker val gifts: SemanticIcon
        @JsName("_im") get() = this + "gifts"
    @SemanticIconMarker val git: SemanticIcon
        @JsName("_jm") get() = this + "git"
    @SemanticIconMarker val git_alternate: SemanticIcon
        @JsName("_km") get() = this + "git alternate"
    @SemanticIconMarker val git_square: SemanticIcon
        @JsName("_lm") get() = this + "git square"
    @SemanticIconMarker val github: SemanticIcon
        @JsName("_mm") get() = this + "github"
    @SemanticIconMarker val github_alternate: SemanticIcon
        @JsName("_nm") get() = this + "github alternate"
    @SemanticIconMarker val github_square: SemanticIcon
        @JsName("_om") get() = this + "github square"
    @SemanticIconMarker val gitkraken: SemanticIcon
        @JsName("_pm") get() = this + "gitkraken"
    @SemanticIconMarker val gitlab: SemanticIcon
        @JsName("_qm") get() = this + "gitlab"
    @SemanticIconMarker val gitter: SemanticIcon
        @JsName("_rm") get() = this + "gitter"
    @SemanticIconMarker val glass_cheers: SemanticIcon
        @JsName("_sm") get() = this + "glass cheers"
    @SemanticIconMarker val glass_martini: SemanticIcon
        @JsName("_tm") get() = this + "glass martini"
    @SemanticIconMarker val glass_martini_alternate: SemanticIcon
        @JsName("_um") get() = this + "glass martini alternate"
    @SemanticIconMarker val glass_whiskey: SemanticIcon
        @JsName("_vm") get() = this + "glass whiskey"
    @SemanticIconMarker val glasses: SemanticIcon
        @JsName("_wm") get() = this + "glasses"
    @SemanticIconMarker val glide: SemanticIcon
        @JsName("_xm") get() = this + "glide"
    @SemanticIconMarker val glide_g: SemanticIcon
        @JsName("_ym") get() = this + "glide g"
    @SemanticIconMarker val globe: SemanticIcon
        @JsName("_zm") get() = this + "globe"
    @SemanticIconMarker val globe_africa: SemanticIcon
        @JsName("_Am") get() = this + "globe africa"
    @SemanticIconMarker val globe_americas: SemanticIcon
        @JsName("_Bm") get() = this + "globe americas"
    @SemanticIconMarker val globe_asia: SemanticIcon
        @JsName("_Cm") get() = this + "globe asia"
    @SemanticIconMarker val globe_europe: SemanticIcon
        @JsName("_Dm") get() = this + "globe europe"
    @SemanticIconMarker val gofore: SemanticIcon
        @JsName("_Em") get() = this + "gofore"
    @SemanticIconMarker val golf_ball: SemanticIcon
        @JsName("_Fm") get() = this + "golf ball"
    @SemanticIconMarker val goodreads: SemanticIcon
        @JsName("_Gm") get() = this + "goodreads"
    @SemanticIconMarker val goodreads_g: SemanticIcon
        @JsName("_Hm") get() = this + "goodreads g"
    @SemanticIconMarker val google: SemanticIcon
        @JsName("_Im") get() = this + "google"
    @SemanticIconMarker val google_drive: SemanticIcon
        @JsName("_Jm") get() = this + "google drive"
    @SemanticIconMarker val google_play: SemanticIcon
        @JsName("_Km") get() = this + "google play"
    @SemanticIconMarker val google_plus: SemanticIcon
        @JsName("_Lm") get() = this + "google plus"
    @SemanticIconMarker val google_plus_g: SemanticIcon
        @JsName("_Mm") get() = this + "google plus g"
    @SemanticIconMarker val google_plus_square: SemanticIcon
        @JsName("_Nm") get() = this + "google plus square"
    @SemanticIconMarker val google_wallet: SemanticIcon
        @JsName("_Om") get() = this + "google wallet"
    @SemanticIconMarker val gopuram: SemanticIcon
        @JsName("_Pm") get() = this + "gopuram"
    @SemanticIconMarker val graduation_cap: SemanticIcon
        @JsName("_Qm") get() = this + "graduation cap"
    @SemanticIconMarker val gratipay: SemanticIcon
        @JsName("_Rm") get() = this + "gratipay"
    @SemanticIconMarker val grav: SemanticIcon
        @JsName("_Sm") get() = this + "grav"
    @SemanticIconMarker val greater_than: SemanticIcon
        @JsName("_Tm") get() = this + "greater than"
    @SemanticIconMarker val greater_than_equal: SemanticIcon
        @JsName("_Um") get() = this + "greater than equal"
    @SemanticIconMarker val green_users: SemanticIcon
        @JsName("_Vm") get() = this + "green users"
    @SemanticIconMarker val grey_users: SemanticIcon
        @JsName("_Wm") get() = this + "grey users"
    @SemanticIconMarker val grimace: SemanticIcon
        @JsName("_Xm") get() = this + "grimace"
    @SemanticIconMarker val grimace_outline: SemanticIcon
        @JsName("_Ym") get() = this + "grimace outline"
    @SemanticIconMarker val grin: SemanticIcon
        @JsName("_Zm") get() = this + "grin"
    @SemanticIconMarker val grin_alternate: SemanticIcon
        @JsName("_an") get() = this + "grin alternate"
    @SemanticIconMarker val grin_alternate_outline: SemanticIcon
        @JsName("_bn") get() = this + "grin alternate outline"
    @SemanticIconMarker val grin_beam: SemanticIcon
        @JsName("_cn") get() = this + "grin beam"
    @SemanticIconMarker val grin_beam_outline: SemanticIcon
        @JsName("_dn") get() = this + "grin beam outline"
    @SemanticIconMarker val grin_beam_sweat: SemanticIcon
        @JsName("_en") get() = this + "grin beam sweat"
    @SemanticIconMarker val grin_beam_sweat_outline: SemanticIcon
        @JsName("_fn") get() = this + "grin beam sweat outline"
    @SemanticIconMarker val grin_hearts: SemanticIcon
        @JsName("_gn") get() = this + "grin hearts"
    @SemanticIconMarker val grin_hearts_outline: SemanticIcon
        @JsName("_hn") get() = this + "grin hearts outline"
    @SemanticIconMarker val grin_outline: SemanticIcon
        @JsName("_in") get() = this + "grin outline"
    @SemanticIconMarker val grin_squint: SemanticIcon
        @JsName("_jn") get() = this + "grin squint"
    @SemanticIconMarker val grin_squint_outline: SemanticIcon
        @JsName("_kn") get() = this + "grin squint outline"
    @SemanticIconMarker val grin_squint_tears: SemanticIcon
        @JsName("_ln") get() = this + "grin squint tears"
    @SemanticIconMarker val grin_squint_tears_outline: SemanticIcon
        @JsName("_mn") get() = this + "grin squint tears outline"
    @SemanticIconMarker val grin_stars: SemanticIcon
        @JsName("_nn") get() = this + "grin stars"
    @SemanticIconMarker val grin_stars_outline: SemanticIcon
        @JsName("_on") get() = this + "grin stars outline"
    @SemanticIconMarker val grin_tears: SemanticIcon
        @JsName("_pn") get() = this + "grin tears"
    @SemanticIconMarker val grin_tears_outline: SemanticIcon
        @JsName("_qn") get() = this + "grin tears outline"
    @SemanticIconMarker val grin_tongue: SemanticIcon
        @JsName("_rn") get() = this + "grin tongue"
    @SemanticIconMarker val grin_tongue_outline: SemanticIcon
        @JsName("_sn") get() = this + "grin tongue outline"
    @SemanticIconMarker val grin_tongue_squint: SemanticIcon
        @JsName("_tn") get() = this + "grin tongue squint"
    @SemanticIconMarker val grin_tongue_squint_outline: SemanticIcon
        @JsName("_un") get() = this + "grin tongue squint outline"
    @SemanticIconMarker val grin_tongue_wink: SemanticIcon
        @JsName("_vn") get() = this + "grin tongue wink"
    @SemanticIconMarker val grin_tongue_wink_outline: SemanticIcon
        @JsName("_wn") get() = this + "grin tongue wink outline"
    @SemanticIconMarker val grin_wink: SemanticIcon
        @JsName("_xn") get() = this + "grin wink"
    @SemanticIconMarker val grin_wink_outline: SemanticIcon
        @JsName("_yn") get() = this + "grin wink outline"
    @SemanticIconMarker val grip_horizontal: SemanticIcon
        @JsName("_zn") get() = this + "grip horizontal"
    @SemanticIconMarker val grip_lines: SemanticIcon
        @JsName("_An") get() = this + "grip lines"
    @SemanticIconMarker val grip_lines_vertical: SemanticIcon
        @JsName("_Bn") get() = this + "grip lines vertical"
    @SemanticIconMarker val grip_vertical: SemanticIcon
        @JsName("_Cn") get() = this + "grip vertical"
    @SemanticIconMarker val gripfire: SemanticIcon
        @JsName("_Dn") get() = this + "gripfire"
    @SemanticIconMarker val grunt: SemanticIcon
        @JsName("_En") get() = this + "grunt"
    @SemanticIconMarker val guitar: SemanticIcon
        @JsName("_Fn") get() = this + "guitar"
    @SemanticIconMarker val gulp: SemanticIcon
        @JsName("_Gn") get() = this + "gulp"
    @SemanticIconMarker val h_square: SemanticIcon
        @JsName("_Hn") get() = this + "h square"
    @SemanticIconMarker val hacker_news: SemanticIcon
        @JsName("_In") get() = this + "hacker news"
    @SemanticIconMarker val hacker_news_square: SemanticIcon
        @JsName("_Jn") get() = this + "hacker news square"
    @SemanticIconMarker val hackerrank: SemanticIcon
        @JsName("_Kn") get() = this + "hackerrank"
    @SemanticIconMarker val hamburger: SemanticIcon
        @JsName("_Ln") get() = this + "hamburger"
    @SemanticIconMarker val hammer: SemanticIcon
        @JsName("_Mn") get() = this + "hammer"
    @SemanticIconMarker val hamsa: SemanticIcon
        @JsName("_Nn") get() = this + "hamsa"
    @SemanticIconMarker val hand_holding: SemanticIcon
        @JsName("_On") get() = this + "hand holding"
    @SemanticIconMarker val hand_holding_heart: SemanticIcon
        @JsName("_Pn") get() = this + "hand holding heart"
    @SemanticIconMarker val hand_holding_medical: SemanticIcon
        @JsName("_Qn") get() = this + "hand holding medical"
    @SemanticIconMarker val hand_holding_usd: SemanticIcon
        @JsName("_Rn") get() = this + "hand holding usd"
    @SemanticIconMarker val hand_holding_water: SemanticIcon
        @JsName("_Sn") get() = this + "hand holding water"
    @SemanticIconMarker val hand_lizard: SemanticIcon
        @JsName("_Tn") get() = this + "hand lizard"
    @SemanticIconMarker val hand_lizard_outline: SemanticIcon
        @JsName("_Un") get() = this + "hand lizard outline"
    @SemanticIconMarker val hand_middle_finger: SemanticIcon
        @JsName("_Vn") get() = this + "hand middle finger"
    @SemanticIconMarker val hand_paper: SemanticIcon
        @JsName("_Wn") get() = this + "hand paper"
    @SemanticIconMarker val hand_paper_outline: SemanticIcon
        @JsName("_Xn") get() = this + "hand paper outline"
    @SemanticIconMarker val hand_peace: SemanticIcon
        @JsName("_Yn") get() = this + "hand peace"
    @SemanticIconMarker val hand_peace_outline: SemanticIcon
        @JsName("_Zn") get() = this + "hand peace outline"
    @SemanticIconMarker val hand_point_down: SemanticIcon
        @JsName("_ao") get() = this + "hand point down"
    @SemanticIconMarker val hand_point_down_outline: SemanticIcon
        @JsName("_bo") get() = this + "hand point down outline"
    @SemanticIconMarker val hand_point_left: SemanticIcon
        @JsName("_co") get() = this + "hand point left"
    @SemanticIconMarker val hand_point_left_outline: SemanticIcon
        @JsName("_do") get() = this + "hand point left outline"
    @SemanticIconMarker val hand_point_right: SemanticIcon
        @JsName("_eo") get() = this + "hand point right"
    @SemanticIconMarker val hand_point_right_outline: SemanticIcon
        @JsName("_fo") get() = this + "hand point right outline"
    @SemanticIconMarker val hand_point_up: SemanticIcon
        @JsName("_go") get() = this + "hand point up"
    @SemanticIconMarker val hand_point_up_outline: SemanticIcon
        @JsName("_ho") get() = this + "hand point up outline"
    @SemanticIconMarker val hand_pointer: SemanticIcon
        @JsName("_io") get() = this + "hand pointer"
    @SemanticIconMarker val hand_pointer_outline: SemanticIcon
        @JsName("_jo") get() = this + "hand pointer outline"
    @SemanticIconMarker val hand_rock: SemanticIcon
        @JsName("_ko") get() = this + "hand rock"
    @SemanticIconMarker val hand_rock_outline: SemanticIcon
        @JsName("_lo") get() = this + "hand rock outline"
    @SemanticIconMarker val hand_scissors: SemanticIcon
        @JsName("_mo") get() = this + "hand scissors"
    @SemanticIconMarker val hand_scissors_outline: SemanticIcon
        @JsName("_no") get() = this + "hand scissors outline"
    @SemanticIconMarker val hand_sparkles: SemanticIcon
        @JsName("_oo") get() = this + "hand sparkles"
    @SemanticIconMarker val hand_spock: SemanticIcon
        @JsName("_po") get() = this + "hand spock"
    @SemanticIconMarker val hand_spock_outline: SemanticIcon
        @JsName("_qo") get() = this + "hand spock outline"
    @SemanticIconMarker val hands: SemanticIcon
        @JsName("_ro") get() = this + "hands"
    @SemanticIconMarker val hands_helping: SemanticIcon
        @JsName("_so") get() = this + "hands helping"
    @SemanticIconMarker val hands_wash: SemanticIcon
        @JsName("_to") get() = this + "hands wash"
    @SemanticIconMarker val handshake: SemanticIcon
        @JsName("_uo") get() = this + "handshake"
    @SemanticIconMarker val handshake_alternate_slash: SemanticIcon
        @JsName("_vo") get() = this + "handshake alternate slash"
    @SemanticIconMarker val handshake_outline: SemanticIcon
        @JsName("_wo") get() = this + "handshake outline"
    @SemanticIconMarker val handshake_slash: SemanticIcon
        @JsName("_xo") get() = this + "handshake slash"
    @SemanticIconMarker val hanukiah: SemanticIcon
        @JsName("_yo") get() = this + "hanukiah"
    @SemanticIconMarker val hard_hat: SemanticIcon
        @JsName("_zo") get() = this + "hard hat"
    @SemanticIconMarker val hashtag: SemanticIcon
        @JsName("_Ao") get() = this + "hashtag"
    @SemanticIconMarker val hat_cowboy: SemanticIcon
        @JsName("_Bo") get() = this + "hat cowboy"
    @SemanticIconMarker val hat_cowboy_side: SemanticIcon
        @JsName("_Co") get() = this + "hat cowboy side"
    @SemanticIconMarker val hat_wizard: SemanticIcon
        @JsName("_Do") get() = this + "hat wizard"
    @SemanticIconMarker val hdd: SemanticIcon
        @JsName("_Eo") get() = this + "hdd"
    @SemanticIconMarker val hdd_outline: SemanticIcon
        @JsName("_Fo") get() = this + "hdd outline"
    @SemanticIconMarker val head_side_cough: SemanticIcon
        @JsName("_Go") get() = this + "head side cough"
    @SemanticIconMarker val head_side_cough_slash: SemanticIcon
        @JsName("_Ho") get() = this + "head side cough slash"
    @SemanticIconMarker val head_side_mask: SemanticIcon
        @JsName("_Io") get() = this + "head side mask"
    @SemanticIconMarker val head_side_virus: SemanticIcon
        @JsName("_Jo") get() = this + "head side virus"
    @SemanticIconMarker val heading: SemanticIcon
        @JsName("_Ko") get() = this + "heading"
    @SemanticIconMarker val headphones: SemanticIcon
        @JsName("_Lo") get() = this + "headphones"
    @SemanticIconMarker val headphones_alternate: SemanticIcon
        @JsName("_Mo") get() = this + "headphones alternate"
    @SemanticIconMarker val headset: SemanticIcon
        @JsName("_No") get() = this + "headset"
    @SemanticIconMarker val heart: SemanticIcon
        @JsName("_Oo") get() = this + "heart"
    @SemanticIconMarker val heart_broken: SemanticIcon
        @JsName("_Po") get() = this + "heart broken"
    @SemanticIconMarker val heart_outline: SemanticIcon
        @JsName("_Qo") get() = this + "heart outline"
    @SemanticIconMarker val heartbeat: SemanticIcon
        @JsName("_Ro") get() = this + "heartbeat"
    @SemanticIconMarker val helicopter: SemanticIcon
        @JsName("_So") get() = this + "helicopter"
    @SemanticIconMarker val help_link: SemanticIcon
        @JsName("_To") get() = this + "help link"
    @SemanticIconMarker val highlighter: SemanticIcon
        @JsName("_Uo") get() = this + "highlighter"
    @SemanticIconMarker val hiking: SemanticIcon
        @JsName("_Vo") get() = this + "hiking"
    @SemanticIconMarker val hippo: SemanticIcon
        @JsName("_Wo") get() = this + "hippo"
    @SemanticIconMarker val hips: SemanticIcon
        @JsName("_Xo") get() = this + "hips"
    @SemanticIconMarker val hire_a_helper: SemanticIcon
        @JsName("_Yo") get() = this + "hire a helper"
    @SemanticIconMarker val history: SemanticIcon
        @JsName("_Zo") get() = this + "history"
    @SemanticIconMarker val hockey_puck: SemanticIcon
        @JsName("_ap") get() = this + "hockey puck"
    @SemanticIconMarker val holly_berry: SemanticIcon
        @JsName("_bp") get() = this + "holly berry"
    @SemanticIconMarker val home: SemanticIcon
        @JsName("_cp") get() = this + "home"
    @SemanticIconMarker val hooli: SemanticIcon
        @JsName("_dp") get() = this + "hooli"
    @SemanticIconMarker val horizontally_flipped_cloud: SemanticIcon
        @JsName("_ep") get() = this + "horizontally flipped cloud"
    @SemanticIconMarker val hornbill: SemanticIcon
        @JsName("_fp") get() = this + "hornbill"
    @SemanticIconMarker val horse: SemanticIcon
        @JsName("_gp") get() = this + "horse"
    @SemanticIconMarker val horse_head: SemanticIcon
        @JsName("_hp") get() = this + "horse head"
    @SemanticIconMarker val hospital: SemanticIcon
        @JsName("_ip") get() = this + "hospital"
    @SemanticIconMarker val hospital_alternate: SemanticIcon
        @JsName("_jp") get() = this + "hospital alternate"
    @SemanticIconMarker val hospital_outline: SemanticIcon
        @JsName("_kp") get() = this + "hospital outline"
    @SemanticIconMarker val hospital_symbol: SemanticIcon
        @JsName("_lp") get() = this + "hospital symbol"
    @SemanticIconMarker val hospital_user: SemanticIcon
        @JsName("_mp") get() = this + "hospital user"
    @SemanticIconMarker val hot_tub: SemanticIcon
        @JsName("_np") get() = this + "hot tub"
    @SemanticIconMarker val hotdog: SemanticIcon
        @JsName("_op") get() = this + "hotdog"
    @SemanticIconMarker val hotel: SemanticIcon
        @JsName("_pp") get() = this + "hotel"
    @SemanticIconMarker val hotjar: SemanticIcon
        @JsName("_qp") get() = this + "hotjar"
    @SemanticIconMarker val hourglass: SemanticIcon
        @JsName("_rp") get() = this + "hourglass"
    @SemanticIconMarker val hourglass_end: SemanticIcon
        @JsName("_sp") get() = this + "hourglass end"
    @SemanticIconMarker val hourglass_half: SemanticIcon
        @JsName("_tp") get() = this + "hourglass half"
    @SemanticIconMarker val hourglass_outline: SemanticIcon
        @JsName("_up") get() = this + "hourglass outline"
    @SemanticIconMarker val hourglass_start: SemanticIcon
        @JsName("_vp") get() = this + "hourglass start"
    @SemanticIconMarker val house_damage: SemanticIcon
        @JsName("_wp") get() = this + "house damage"
    @SemanticIconMarker val house_user: SemanticIcon
        @JsName("_xp") get() = this + "house user"
    @SemanticIconMarker val houzz: SemanticIcon
        @JsName("_yp") get() = this + "houzz"
    @SemanticIconMarker val hryvnia: SemanticIcon
        @JsName("_zp") get() = this + "hryvnia"
    @SemanticIconMarker val html5: SemanticIcon
        @JsName("_Ap") get() = this + "html5"
    @SemanticIconMarker val hubspot: SemanticIcon
        @JsName("_Bp") get() = this + "hubspot"
    @SemanticIconMarker val huge_home: SemanticIcon
        @JsName("_Cp") get() = this + "huge home"
    @SemanticIconMarker val i_cursor: SemanticIcon
        @JsName("_Dp") get() = this + "i cursor"
    @SemanticIconMarker val ice_cream: SemanticIcon
        @JsName("_Ep") get() = this + "ice cream"
    @SemanticIconMarker val icicles: SemanticIcon
        @JsName("_Fp") get() = this + "icicles"
    @SemanticIconMarker val icons: SemanticIcon
        @JsName("_Gp") get() = this + "icons"
    @SemanticIconMarker val id_badge: SemanticIcon
        @JsName("_Hp") get() = this + "id badge"
    @SemanticIconMarker val id_badge_outline: SemanticIcon
        @JsName("_Ip") get() = this + "id badge outline"
    @SemanticIconMarker val id_card: SemanticIcon
        @JsName("_Jp") get() = this + "id card"
    @SemanticIconMarker val id_card_alternate: SemanticIcon
        @JsName("_Kp") get() = this + "id card alternate"
    @SemanticIconMarker val id_card_outline: SemanticIcon
        @JsName("_Lp") get() = this + "id card outline"
    @SemanticIconMarker val ideal: SemanticIcon
        @JsName("_Mp") get() = this + "ideal"
    @SemanticIconMarker val igloo: SemanticIcon
        @JsName("_Np") get() = this + "igloo"
    @SemanticIconMarker val image: SemanticIcon
        @JsName("_Op") get() = this + "image"
    @SemanticIconMarker val image_outline: SemanticIcon
        @JsName("_Pp") get() = this + "image outline"
    @SemanticIconMarker val images: SemanticIcon
        @JsName("_Qp") get() = this + "images"
    @SemanticIconMarker val images_outline: SemanticIcon
        @JsName("_Rp") get() = this + "images outline"
    @SemanticIconMarker val imdb: SemanticIcon
        @JsName("_Sp") get() = this + "imdb"
    @SemanticIconMarker val inbox: SemanticIcon
        @JsName("_Tp") get() = this + "inbox"
    @SemanticIconMarker val indent: SemanticIcon
        @JsName("_Up") get() = this + "indent"
    @SemanticIconMarker val industry: SemanticIcon
        @JsName("_Vp") get() = this + "industry"
    @SemanticIconMarker val infinity: SemanticIcon
        @JsName("_Wp") get() = this + "infinity"
    @SemanticIconMarker val info: SemanticIcon
        @JsName("_Xp") get() = this + "info"
    @SemanticIconMarker val info_circle: SemanticIcon
        @JsName("_Yp") get() = this + "info circle"
    @SemanticIconMarker val instagram: SemanticIcon
        @JsName("_Zp") get() = this + "instagram"
    @SemanticIconMarker val instagram_square: SemanticIcon
        @JsName("_aq") get() = this + "instagram square"
    @SemanticIconMarker val intercom: SemanticIcon
        @JsName("_bq") get() = this + "intercom"
    @SemanticIconMarker val internet_explorer: SemanticIcon
        @JsName("_cq") get() = this + "internet explorer"
    @SemanticIconMarker val inverted_blue_users: SemanticIcon
        @JsName("_dq") get() = this + "inverted blue users"
    @SemanticIconMarker val inverted_brown_users: SemanticIcon
        @JsName("_eq") get() = this + "inverted brown users"
    @SemanticIconMarker val inverted_corner_add: SemanticIcon
        @JsName("_fq") get() = this + "inverted corner add"
    @SemanticIconMarker val inverted_green_users: SemanticIcon
        @JsName("_gq") get() = this + "inverted green users"
    @SemanticIconMarker val inverted_grey_users: SemanticIcon
        @JsName("_hq") get() = this + "inverted grey users"
    @SemanticIconMarker val inverted_olive_users: SemanticIcon
        @JsName("_iq") get() = this + "inverted olive users"
    @SemanticIconMarker val inverted_orange_users: SemanticIcon
        @JsName("_jq") get() = this + "inverted orange users"
    @SemanticIconMarker val inverted_pink_users: SemanticIcon
        @JsName("_kq") get() = this + "inverted pink users"
    @SemanticIconMarker val inverted_primary_users: SemanticIcon
        @JsName("_lq") get() = this + "inverted primary users"
    @SemanticIconMarker val inverted_purple_users: SemanticIcon
        @JsName("_mq") get() = this + "inverted purple users"
    @SemanticIconMarker val inverted_red_users: SemanticIcon
        @JsName("_nq") get() = this + "inverted red users"
    @SemanticIconMarker val inverted_secondary_users: SemanticIcon
        @JsName("_oq") get() = this + "inverted secondary users"
    @SemanticIconMarker val inverted_teal_users: SemanticIcon
        @JsName("_pq") get() = this + "inverted teal users"
    @SemanticIconMarker val inverted_users: SemanticIcon
        @JsName("_qq") get() = this + "inverted users"
    @SemanticIconMarker val inverted_violet_users: SemanticIcon
        @JsName("_rq") get() = this + "inverted violet users"
    @SemanticIconMarker val inverted_yellow_users: SemanticIcon
        @JsName("_sq") get() = this + "inverted yellow users"
    @SemanticIconMarker val invision: SemanticIcon
        @JsName("_tq") get() = this + "invision"
    @SemanticIconMarker val ioxhost: SemanticIcon
        @JsName("_uq") get() = this + "ioxhost"
    @SemanticIconMarker val italic: SemanticIcon
        @JsName("_vq") get() = this + "italic"
    @SemanticIconMarker val itch_io: SemanticIcon
        @JsName("_wq") get() = this + "itch io"
    @SemanticIconMarker val itunes: SemanticIcon
        @JsName("_xq") get() = this + "itunes"
    @SemanticIconMarker val itunes_note: SemanticIcon
        @JsName("_yq") get() = this + "itunes note"
    @SemanticIconMarker val java: SemanticIcon
        @JsName("_zq") get() = this + "java"
    @SemanticIconMarker val jedi: SemanticIcon
        @JsName("_Aq") get() = this + "jedi"
    @SemanticIconMarker val jedi_order: SemanticIcon
        @JsName("_Bq") get() = this + "jedi order"
    @SemanticIconMarker val jenkins: SemanticIcon
        @JsName("_Cq") get() = this + "jenkins"
    @SemanticIconMarker val jira: SemanticIcon
        @JsName("_Dq") get() = this + "jira"
    @SemanticIconMarker val joget: SemanticIcon
        @JsName("_Eq") get() = this + "joget"
    @SemanticIconMarker val joint: SemanticIcon
        @JsName("_Fq") get() = this + "joint"
    @SemanticIconMarker val joomla: SemanticIcon
        @JsName("_Gq") get() = this + "joomla"
    @SemanticIconMarker val journal_whills: SemanticIcon
        @JsName("_Hq") get() = this + "journal whills"
    @SemanticIconMarker val js: SemanticIcon
        @JsName("_Iq") get() = this + "js"
    @SemanticIconMarker val js_square: SemanticIcon
        @JsName("_Jq") get() = this + "js square"
    @SemanticIconMarker val jsfiddle: SemanticIcon
        @JsName("_Kq") get() = this + "jsfiddle"
    @SemanticIconMarker val kaaba: SemanticIcon
        @JsName("_Lq") get() = this + "kaaba"
    @SemanticIconMarker val kaggle: SemanticIcon
        @JsName("_Mq") get() = this + "kaggle"
    @SemanticIconMarker val key: SemanticIcon
        @JsName("_Nq") get() = this + "key"
    @SemanticIconMarker val keybase: SemanticIcon
        @JsName("_Oq") get() = this + "keybase"
    @SemanticIconMarker val keyboard: SemanticIcon
        @JsName("_Pq") get() = this + "keyboard"
    @SemanticIconMarker val keyboard_outline: SemanticIcon
        @JsName("_Qq") get() = this + "keyboard outline"
    @SemanticIconMarker val keycdn: SemanticIcon
        @JsName("_Rq") get() = this + "keycdn"
    @SemanticIconMarker val khanda: SemanticIcon
        @JsName("_Sq") get() = this + "khanda"
    @SemanticIconMarker val kickstarter: SemanticIcon
        @JsName("_Tq") get() = this + "kickstarter"
    @SemanticIconMarker val kickstarter_k: SemanticIcon
        @JsName("_Uq") get() = this + "kickstarter k"
    @SemanticIconMarker val kiss: SemanticIcon
        @JsName("_Vq") get() = this + "kiss"
    @SemanticIconMarker val kiss_beam: SemanticIcon
        @JsName("_Wq") get() = this + "kiss beam"
    @SemanticIconMarker val kiss_beam_outline: SemanticIcon
        @JsName("_Xq") get() = this + "kiss beam outline"
    @SemanticIconMarker val kiss_outline: SemanticIcon
        @JsName("_Yq") get() = this + "kiss outline"
    @SemanticIconMarker val kiss_wink_heart: SemanticIcon
        @JsName("_Zq") get() = this + "kiss wink heart"
    @SemanticIconMarker val kiss_wink_heart_outline: SemanticIcon
        @JsName("_ar") get() = this + "kiss wink heart outline"
    @SemanticIconMarker val kiwi_bird: SemanticIcon
        @JsName("_br") get() = this + "kiwi bird"
    @SemanticIconMarker val korvue: SemanticIcon
        @JsName("_cr") get() = this + "korvue"
    @SemanticIconMarker val landmark: SemanticIcon
        @JsName("_dr") get() = this + "landmark"
    @SemanticIconMarker val language: SemanticIcon
        @JsName("_er") get() = this + "language"
    @SemanticIconMarker val laptop: SemanticIcon
        @JsName("_fr") get() = this + "laptop"
    @SemanticIconMarker val laptop_code: SemanticIcon
        @JsName("_gr") get() = this + "laptop code"
    @SemanticIconMarker val laptop_house: SemanticIcon
        @JsName("_hr") get() = this + "laptop house"
    @SemanticIconMarker val laptop_medical: SemanticIcon
        @JsName("_ir") get() = this + "laptop medical"
    @SemanticIconMarker val laravel: SemanticIcon
        @JsName("_jr") get() = this + "laravel"
    @SemanticIconMarker val large_home: SemanticIcon
        @JsName("_kr") get() = this + "large home"
    @SemanticIconMarker val lastfm: SemanticIcon
        @JsName("_lr") get() = this + "lastfm"
    @SemanticIconMarker val lastfm_square: SemanticIcon
        @JsName("_mr") get() = this + "lastfm square"
    @SemanticIconMarker val laugh: SemanticIcon
        @JsName("_nr") get() = this + "laugh"
    @SemanticIconMarker val laugh_beam: SemanticIcon
        @JsName("_or") get() = this + "laugh beam"
    @SemanticIconMarker val laugh_beam_outline: SemanticIcon
        @JsName("_pr") get() = this + "laugh beam outline"
    @SemanticIconMarker val laugh_outline: SemanticIcon
        @JsName("_qr") get() = this + "laugh outline"
    @SemanticIconMarker val laugh_squint: SemanticIcon
        @JsName("_rr") get() = this + "laugh squint"
    @SemanticIconMarker val laugh_squint_outline: SemanticIcon
        @JsName("_sr") get() = this + "laugh squint outline"
    @SemanticIconMarker val laugh_wink: SemanticIcon
        @JsName("_tr") get() = this + "laugh wink"
    @SemanticIconMarker val laugh_wink_outline: SemanticIcon
        @JsName("_ur") get() = this + "laugh wink outline"
    @SemanticIconMarker val layer_group: SemanticIcon
        @JsName("_vr") get() = this + "layer group"
    @SemanticIconMarker val leaf: SemanticIcon
        @JsName("_wr") get() = this + "leaf"
    @SemanticIconMarker val leanpub: SemanticIcon
        @JsName("_xr") get() = this + "leanpub"
    @SemanticIconMarker val lemon: SemanticIcon
        @JsName("_yr") get() = this + "lemon"
    @SemanticIconMarker val lemon_outline: SemanticIcon
        @JsName("_zr") get() = this + "lemon outline"
    @SemanticIconMarker val less_than: SemanticIcon
        @JsName("_Ar") get() = this + "less than"
    @SemanticIconMarker val less_than_equal: SemanticIcon
        @JsName("_Br") get() = this + "less than equal"
    @SemanticIconMarker val lesscss: SemanticIcon
        @JsName("_Cr") get() = this + "lesscss"
    @SemanticIconMarker val level_down_alternate: SemanticIcon
        @JsName("_Dr") get() = this + "level down alternate"
    @SemanticIconMarker val level_up_alternate: SemanticIcon
        @JsName("_Er") get() = this + "level up alternate"
    @SemanticIconMarker val life_ring: SemanticIcon
        @JsName("_Fr") get() = this + "life ring"
    @SemanticIconMarker val life_ring_outline: SemanticIcon
        @JsName("_Gr") get() = this + "life ring outline"
    @SemanticIconMarker val lightbulb: SemanticIcon
        @JsName("_Hr") get() = this + "lightbulb"
    @SemanticIconMarker val lightbulb_outline: SemanticIcon
        @JsName("_Ir") get() = this + "lightbulb outline"
    @SemanticIconMarker val linechat: SemanticIcon
        @JsName("_Jr") get() = this + "linechat"
    @SemanticIconMarker val linkedin: SemanticIcon
        @JsName("_Kr") get() = this + "linkedin"
    @SemanticIconMarker val linkedin_in: SemanticIcon
        @JsName("_Lr") get() = this + "linkedin in"
    @SemanticIconMarker val linkify: SemanticIcon
        @JsName("_Mr") get() = this + "linkify"
    @SemanticIconMarker val linode: SemanticIcon
        @JsName("_Nr") get() = this + "linode"
    @SemanticIconMarker val linux: SemanticIcon
        @JsName("_Or") get() = this + "linux"
    @SemanticIconMarker val lira_sign: SemanticIcon
        @JsName("_Pr") get() = this + "lira sign"
    @SemanticIconMarker val list: SemanticIcon
        @JsName("_Qr") get() = this + "list"
    @SemanticIconMarker val list_alternate: SemanticIcon
        @JsName("_Rr") get() = this + "list alternate"
    @SemanticIconMarker val list_alternate_outline: SemanticIcon
        @JsName("_Sr") get() = this + "list alternate outline"
    @SemanticIconMarker val list_ol: SemanticIcon
        @JsName("_Tr") get() = this + "list ol"
    @SemanticIconMarker val list_ul: SemanticIcon
        @JsName("_Ur") get() = this + "list ul"
    @SemanticIconMarker val location_arrow: SemanticIcon
        @JsName("_Vr") get() = this + "location arrow"
    @SemanticIconMarker val lock: SemanticIcon
        @JsName("_Wr") get() = this + "lock"
    @SemanticIconMarker val lock_open: SemanticIcon
        @JsName("_Xr") get() = this + "lock open"
    @SemanticIconMarker val long_arrow_alternate_down: SemanticIcon
        @JsName("_Yr") get() = this + "long arrow alternate down"
    @SemanticIconMarker val long_arrow_alternate_left: SemanticIcon
        @JsName("_Zr") get() = this + "long arrow alternate left"
    @SemanticIconMarker val long_arrow_alternate_right: SemanticIcon
        @JsName("_as") get() = this + "long arrow alternate right"
    @SemanticIconMarker val long_arrow_alternate_up: SemanticIcon
        @JsName("_bs") get() = this + "long arrow alternate up"
    @SemanticIconMarker val low_vision: SemanticIcon
        @JsName("_cs") get() = this + "low vision"
    @SemanticIconMarker val luggage_cart: SemanticIcon
        @JsName("_ds") get() = this + "luggage cart"
    @SemanticIconMarker val lungs: SemanticIcon
        @JsName("_es") get() = this + "lungs"
    @SemanticIconMarker val lungs_virus: SemanticIcon
        @JsName("_fs") get() = this + "lungs virus"
    @SemanticIconMarker val lyft: SemanticIcon
        @JsName("_gs") get() = this + "lyft"
    @SemanticIconMarker val magento: SemanticIcon
        @JsName("_hs") get() = this + "magento"
    @SemanticIconMarker val magic: SemanticIcon
        @JsName("_is") get() = this + "magic"
    @SemanticIconMarker val magnet: SemanticIcon
        @JsName("_js") get() = this + "magnet"
    @SemanticIconMarker val mail: SemanticIcon
        @JsName("_ks") get() = this + "mail"
    @SemanticIconMarker val mail_bulk: SemanticIcon
        @JsName("_ls") get() = this + "mail bulk"
    @SemanticIconMarker val mailchimp: SemanticIcon
        @JsName("_ms") get() = this + "mailchimp"
    @SemanticIconMarker val male: SemanticIcon
        @JsName("_ns") get() = this + "male"
    @SemanticIconMarker val mandalorian: SemanticIcon
        @JsName("_os") get() = this + "mandalorian"
    @SemanticIconMarker val map: SemanticIcon
        @JsName("_ps") get() = this + "map"
    @SemanticIconMarker val map_marked: SemanticIcon
        @JsName("_qs") get() = this + "map marked"
    @SemanticIconMarker val map_marked_alternate: SemanticIcon
        @JsName("_rs") get() = this + "map marked alternate"
    @SemanticIconMarker val map_marker: SemanticIcon
        @JsName("_ss") get() = this + "map marker"
    @SemanticIconMarker val map_marker_alternate: SemanticIcon
        @JsName("_ts") get() = this + "map marker alternate"
    @SemanticIconMarker val map_outline: SemanticIcon
        @JsName("_us") get() = this + "map outline"
    @SemanticIconMarker val map_pin: SemanticIcon
        @JsName("_vs") get() = this + "map pin"
    @SemanticIconMarker val map_signs: SemanticIcon
        @JsName("_ws") get() = this + "map signs"
    @SemanticIconMarker val markdown: SemanticIcon
        @JsName("_xs") get() = this + "markdown"
    @SemanticIconMarker val marker: SemanticIcon
        @JsName("_ys") get() = this + "marker"
    @SemanticIconMarker val mars: SemanticIcon
        @JsName("_zs") get() = this + "mars"
    @SemanticIconMarker val mars_double: SemanticIcon
        @JsName("_As") get() = this + "mars double"
    @SemanticIconMarker val mars_stroke: SemanticIcon
        @JsName("_Bs") get() = this + "mars stroke"
    @SemanticIconMarker val mars_stroke_horizontal: SemanticIcon
        @JsName("_Cs") get() = this + "mars stroke horizontal"
    @SemanticIconMarker val mars_stroke_vertical: SemanticIcon
        @JsName("_Ds") get() = this + "mars stroke vertical"
    @SemanticIconMarker val mask: SemanticIcon
        @JsName("_Es") get() = this + "mask"
    @SemanticIconMarker val massive_home: SemanticIcon
        @JsName("_Fs") get() = this + "massive home"
    @SemanticIconMarker val mastodon: SemanticIcon
        @JsName("_Gs") get() = this + "mastodon"
    @SemanticIconMarker val maxcdn: SemanticIcon
        @JsName("_Hs") get() = this + "maxcdn"
    @SemanticIconMarker val mdb: SemanticIcon
        @JsName("_Is") get() = this + "mdb"
    @SemanticIconMarker val medal: SemanticIcon
        @JsName("_Js") get() = this + "medal"
    @SemanticIconMarker val medapps: SemanticIcon
        @JsName("_Ks") get() = this + "medapps"
    @SemanticIconMarker val medium_: SemanticIcon
        @JsName("_Ls") get() = this + "medium"
    @SemanticIconMarker val medium_m: SemanticIcon
        @JsName("_Ms") get() = this + "medium m"
    @SemanticIconMarker val medkit: SemanticIcon
        @JsName("_Ns") get() = this + "medkit"
    @SemanticIconMarker val medrt: SemanticIcon
        @JsName("_Os") get() = this + "medrt"
    @SemanticIconMarker val meetup: SemanticIcon
        @JsName("_Ps") get() = this + "meetup"
    @SemanticIconMarker val megaport: SemanticIcon
        @JsName("_Qs") get() = this + "megaport"
    @SemanticIconMarker val meh: SemanticIcon
        @JsName("_Rs") get() = this + "meh"
    @SemanticIconMarker val meh_blank: SemanticIcon
        @JsName("_Ss") get() = this + "meh blank"
    @SemanticIconMarker val meh_blank_outline: SemanticIcon
        @JsName("_Ts") get() = this + "meh blank outline"
    @SemanticIconMarker val meh_outline: SemanticIcon
        @JsName("_Us") get() = this + "meh outline"
    @SemanticIconMarker val meh_rolling_eyes: SemanticIcon
        @JsName("_Vs") get() = this + "meh rolling eyes"
    @SemanticIconMarker val meh_rolling_eyes_outline: SemanticIcon
        @JsName("_Ws") get() = this + "meh rolling eyes outline"
    @SemanticIconMarker val memory: SemanticIcon
        @JsName("_Xs") get() = this + "memory"
    @SemanticIconMarker val mendeley: SemanticIcon
        @JsName("_Ys") get() = this + "mendeley"
    @SemanticIconMarker val menorah: SemanticIcon
        @JsName("_Zs") get() = this + "menorah"
    @SemanticIconMarker val mercury: SemanticIcon
        @JsName("_at") get() = this + "mercury"
    @SemanticIconMarker val meteor: SemanticIcon
        @JsName("_bt") get() = this + "meteor"
    @SemanticIconMarker val microblog: SemanticIcon
        @JsName("_ct") get() = this + "microblog"
    @SemanticIconMarker val microchip: SemanticIcon
        @JsName("_dt") get() = this + "microchip"
    @SemanticIconMarker val microphone: SemanticIcon
        @JsName("_et") get() = this + "microphone"
    @SemanticIconMarker val microphone_alternate: SemanticIcon
        @JsName("_ft") get() = this + "microphone alternate"
    @SemanticIconMarker val microphone_alternate_slash: SemanticIcon
        @JsName("_gt") get() = this + "microphone alternate slash"
    @SemanticIconMarker val microphone_slash: SemanticIcon
        @JsName("_ht") get() = this + "microphone slash"
    @SemanticIconMarker val microscope: SemanticIcon
        @JsName("_it") get() = this + "microscope"
    @SemanticIconMarker val microsoft: SemanticIcon
        @JsName("_jt") get() = this + "microsoft"
    @SemanticIconMarker val mini_home: SemanticIcon
        @JsName("_kt") get() = this + "mini home"
    @SemanticIconMarker val minus: SemanticIcon
        @JsName("_lt") get() = this + "minus"
    @SemanticIconMarker val minus_circle: SemanticIcon
        @JsName("_mt") get() = this + "minus circle"
    @SemanticIconMarker val minus_square: SemanticIcon
        @JsName("_nt") get() = this + "minus square"
    @SemanticIconMarker val minus_square_outline: SemanticIcon
        @JsName("_ot") get() = this + "minus square outline"
    @SemanticIconMarker val mitten: SemanticIcon
        @JsName("_pt") get() = this + "mitten"
    @SemanticIconMarker val mix: SemanticIcon
        @JsName("_qt") get() = this + "mix"
    @SemanticIconMarker val mixcloud: SemanticIcon
        @JsName("_rt") get() = this + "mixcloud"
    @SemanticIconMarker val mixer: SemanticIcon
        @JsName("_st") get() = this + "mixer"
    @SemanticIconMarker val mizuni: SemanticIcon
        @JsName("_tt") get() = this + "mizuni"
    @SemanticIconMarker val mobile: SemanticIcon
        @JsName("_ut") get() = this + "mobile"
    @SemanticIconMarker val mobile_alternate: SemanticIcon
        @JsName("_vt") get() = this + "mobile alternate"
    @SemanticIconMarker val modx: SemanticIcon
        @JsName("_wt") get() = this + "modx"
    @SemanticIconMarker val monero: SemanticIcon
        @JsName("_xt") get() = this + "monero"
    @SemanticIconMarker val money_bill: SemanticIcon
        @JsName("_yt") get() = this + "money bill"
    @SemanticIconMarker val money_bill_alternate: SemanticIcon
        @JsName("_zt") get() = this + "money bill alternate"
    @SemanticIconMarker val money_bill_alternate_outline: SemanticIcon
        @JsName("_At") get() = this + "money bill alternate outline"
    @SemanticIconMarker val money_bill_wave: SemanticIcon
        @JsName("_Bt") get() = this + "money bill wave"
    @SemanticIconMarker val money_bill_wave_alternate: SemanticIcon
        @JsName("_Ct") get() = this + "money bill wave alternate"
    @SemanticIconMarker val money_check: SemanticIcon
        @JsName("_Dt") get() = this + "money check"
    @SemanticIconMarker val money_check_alternate: SemanticIcon
        @JsName("_Et") get() = this + "money check alternate"
    @SemanticIconMarker val monument: SemanticIcon
        @JsName("_Ft") get() = this + "monument"
    @SemanticIconMarker val moon: SemanticIcon
        @JsName("_Gt") get() = this + "moon"
    @SemanticIconMarker val moon_outline: SemanticIcon
        @JsName("_Ht") get() = this + "moon outline"
    @SemanticIconMarker val mortar_pestle: SemanticIcon
        @JsName("_It") get() = this + "mortar pestle"
    @SemanticIconMarker val mosque: SemanticIcon
        @JsName("_Jt") get() = this + "mosque"
    @SemanticIconMarker val motorcycle: SemanticIcon
        @JsName("_Kt") get() = this + "motorcycle"
    @SemanticIconMarker val mountain: SemanticIcon
        @JsName("_Lt") get() = this + "mountain"
    @SemanticIconMarker val mouse: SemanticIcon
        @JsName("_Mt") get() = this + "mouse"
    @SemanticIconMarker val mouse_pointer: SemanticIcon
        @JsName("_Nt") get() = this + "mouse pointer"
    @SemanticIconMarker val mug_hot: SemanticIcon
        @JsName("_Ot") get() = this + "mug hot"
    @SemanticIconMarker val music: SemanticIcon
        @JsName("_Pt") get() = this + "music"
    @SemanticIconMarker val napster: SemanticIcon
        @JsName("_Qt") get() = this + "napster"
    @SemanticIconMarker val neos: SemanticIcon
        @JsName("_Rt") get() = this + "neos"
    @SemanticIconMarker val neuter: SemanticIcon
        @JsName("_St") get() = this + "neuter"
    @SemanticIconMarker val newspaper: SemanticIcon
        @JsName("_Tt") get() = this + "newspaper"
    @SemanticIconMarker val newspaper_outline: SemanticIcon
        @JsName("_Ut") get() = this + "newspaper outline"
    @SemanticIconMarker val nimblr: SemanticIcon
        @JsName("_Vt") get() = this + "nimblr"
    @SemanticIconMarker val node: SemanticIcon
        @JsName("_Wt") get() = this + "node"
    @SemanticIconMarker val node_js: SemanticIcon
        @JsName("_Xt") get() = this + "node js"
    @SemanticIconMarker val not_equal: SemanticIcon
        @JsName("_Yt") get() = this + "not equal"
    @SemanticIconMarker val notched_circle_loading: SemanticIcon
        @JsName("_Zt") get() = this + "notched circle loading"
    @SemanticIconMarker val notes_medical: SemanticIcon
        @JsName("_au") get() = this + "notes medical"
    @SemanticIconMarker val npm: SemanticIcon
        @JsName("_bu") get() = this + "npm"
    @SemanticIconMarker val ns8: SemanticIcon
        @JsName("_cu") get() = this + "ns8"
    @SemanticIconMarker val nutritionix: SemanticIcon
        @JsName("_du") get() = this + "nutritionix"
    @SemanticIconMarker val object_group: SemanticIcon
        @JsName("_eu") get() = this + "object group"
    @SemanticIconMarker val object_group_outline: SemanticIcon
        @JsName("_fu") get() = this + "object group outline"
    @SemanticIconMarker val object_ungroup: SemanticIcon
        @JsName("_gu") get() = this + "object ungroup"
    @SemanticIconMarker val object_ungroup_outline: SemanticIcon
        @JsName("_hu") get() = this + "object ungroup outline"
    @SemanticIconMarker val odnoklassniki: SemanticIcon
        @JsName("_iu") get() = this + "odnoklassniki"
    @SemanticIconMarker val odnoklassniki_square: SemanticIcon
        @JsName("_ju") get() = this + "odnoklassniki square"
    @SemanticIconMarker val oil_can: SemanticIcon
        @JsName("_ku") get() = this + "oil can"
    @SemanticIconMarker val old_republic: SemanticIcon
        @JsName("_lu") get() = this + "old republic"
    @SemanticIconMarker val olive_users: SemanticIcon
        @JsName("_mu") get() = this + "olive users"
    @SemanticIconMarker val om: SemanticIcon
        @JsName("_nu") get() = this + "om"
    @SemanticIconMarker val opencart: SemanticIcon
        @JsName("_ou") get() = this + "opencart"
    @SemanticIconMarker val openid: SemanticIcon
        @JsName("_pu") get() = this + "openid"
    @SemanticIconMarker val opera: SemanticIcon
        @JsName("_qu") get() = this + "opera"
    @SemanticIconMarker val optin_monster: SemanticIcon
        @JsName("_ru") get() = this + "optin monster"
    @SemanticIconMarker val orange_users: SemanticIcon
        @JsName("_su") get() = this + "orange users"
    @SemanticIconMarker val orcid: SemanticIcon
        @JsName("_tu") get() = this + "orcid"
    @SemanticIconMarker val osi: SemanticIcon
        @JsName("_uu") get() = this + "osi"
    @SemanticIconMarker val otter: SemanticIcon
        @JsName("_vu") get() = this + "otter"
    @SemanticIconMarker val outdent: SemanticIcon
        @JsName("_wu") get() = this + "outdent"
    @SemanticIconMarker val page4: SemanticIcon
        @JsName("_xu") get() = this + "page4"
    @SemanticIconMarker val pagelines: SemanticIcon
        @JsName("_yu") get() = this + "pagelines"
    @SemanticIconMarker val pager: SemanticIcon
        @JsName("_zu") get() = this + "pager"
    @SemanticIconMarker val paint_brush: SemanticIcon
        @JsName("_Au") get() = this + "paint brush"
    @SemanticIconMarker val paint_roller: SemanticIcon
        @JsName("_Bu") get() = this + "paint roller"
    @SemanticIconMarker val palette: SemanticIcon
        @JsName("_Cu") get() = this + "palette"
    @SemanticIconMarker val palfed: SemanticIcon
        @JsName("_Du") get() = this + "palfed"
    @SemanticIconMarker val pallet: SemanticIcon
        @JsName("_Eu") get() = this + "pallet"
    @SemanticIconMarker val paper_plane: SemanticIcon
        @JsName("_Fu") get() = this + "paper plane"
    @SemanticIconMarker val paper_plane_outline: SemanticIcon
        @JsName("_Gu") get() = this + "paper plane outline"
    @SemanticIconMarker val paperclip: SemanticIcon
        @JsName("_Hu") get() = this + "paperclip"
    @SemanticIconMarker val parachute_box: SemanticIcon
        @JsName("_Iu") get() = this + "parachute box"
    @SemanticIconMarker val paragraph: SemanticIcon
        @JsName("_Ju") get() = this + "paragraph"
    @SemanticIconMarker val parking: SemanticIcon
        @JsName("_Ku") get() = this + "parking"
    @SemanticIconMarker val passport: SemanticIcon
        @JsName("_Lu") get() = this + "passport"
    @SemanticIconMarker val pastafarianism: SemanticIcon
        @JsName("_Mu") get() = this + "pastafarianism"
    @SemanticIconMarker val paste: SemanticIcon
        @JsName("_Nu") get() = this + "paste"
    @SemanticIconMarker val patreon: SemanticIcon
        @JsName("_Ou") get() = this + "patreon"
    @SemanticIconMarker val pause: SemanticIcon
        @JsName("_Pu") get() = this + "pause"
    @SemanticIconMarker val pause_circle: SemanticIcon
        @JsName("_Qu") get() = this + "pause circle"
    @SemanticIconMarker val pause_circle_outline: SemanticIcon
        @JsName("_Ru") get() = this + "pause circle outline"
    @SemanticIconMarker val paw: SemanticIcon
        @JsName("_Su") get() = this + "paw"
    @SemanticIconMarker val paypal: SemanticIcon
        @JsName("_Tu") get() = this + "paypal"
    @SemanticIconMarker val peace: SemanticIcon
        @JsName("_Uu") get() = this + "peace"
    @SemanticIconMarker val pen: SemanticIcon
        @JsName("_Vu") get() = this + "pen"
    @SemanticIconMarker val pen_alternate: SemanticIcon
        @JsName("_Wu") get() = this + "pen alternate"
    @SemanticIconMarker val pen_fancy: SemanticIcon
        @JsName("_Xu") get() = this + "pen fancy"
    @SemanticIconMarker val pen_nib: SemanticIcon
        @JsName("_Yu") get() = this + "pen nib"
    @SemanticIconMarker val pen_square: SemanticIcon
        @JsName("_Zu") get() = this + "pen square"
    @SemanticIconMarker val pencil_alternate: SemanticIcon
        @JsName("_av") get() = this + "pencil alternate"
    @SemanticIconMarker val pencil_ruler: SemanticIcon
        @JsName("_bv") get() = this + "pencil ruler"
    @SemanticIconMarker val penny_arcade: SemanticIcon
        @JsName("_cv") get() = this + "penny arcade"
    @SemanticIconMarker val people_arrows: SemanticIcon
        @JsName("_dv") get() = this + "people arrows"
    @SemanticIconMarker val people_carry: SemanticIcon
        @JsName("_ev") get() = this + "people carry"
    @SemanticIconMarker val pepper_hot: SemanticIcon
        @JsName("_fv") get() = this + "pepper hot"
    @SemanticIconMarker val percent: SemanticIcon
        @JsName("_gv") get() = this + "percent"
    @SemanticIconMarker val percentage: SemanticIcon
        @JsName("_hv") get() = this + "percentage"
    @SemanticIconMarker val periscope: SemanticIcon
        @JsName("_iv") get() = this + "periscope"
    @SemanticIconMarker val person_booth: SemanticIcon
        @JsName("_jv") get() = this + "person booth"
    @SemanticIconMarker val phabricator: SemanticIcon
        @JsName("_kv") get() = this + "phabricator"
    @SemanticIconMarker val phoenix_framework: SemanticIcon
        @JsName("_lv") get() = this + "phoenix framework"
    @SemanticIconMarker val phoenix_squadron: SemanticIcon
        @JsName("_mv") get() = this + "phoenix squadron"
    @SemanticIconMarker val phone: SemanticIcon
        @JsName("_nv") get() = this + "phone"
    @SemanticIconMarker val phone_alternate: SemanticIcon
        @JsName("_ov") get() = this + "phone alternate"
    @SemanticIconMarker val phone_slash: SemanticIcon
        @JsName("_pv") get() = this + "phone slash"
    @SemanticIconMarker val phone_square: SemanticIcon
        @JsName("_qv") get() = this + "phone square"
    @SemanticIconMarker val phone_square_alternate: SemanticIcon
        @JsName("_rv") get() = this + "phone square alternate"
    @SemanticIconMarker val phone_volume: SemanticIcon
        @JsName("_sv") get() = this + "phone volume"
    @SemanticIconMarker val photo_video: SemanticIcon
        @JsName("_tv") get() = this + "photo video"
    @SemanticIconMarker val php: SemanticIcon
        @JsName("_uv") get() = this + "php"
    @SemanticIconMarker val pied_piper: SemanticIcon
        @JsName("_vv") get() = this + "pied piper"
    @SemanticIconMarker val pied_piper_alternate: SemanticIcon
        @JsName("_wv") get() = this + "pied piper alternate"
    @SemanticIconMarker val pied_piper_hat: SemanticIcon
        @JsName("_xv") get() = this + "pied piper hat"
    @SemanticIconMarker val pied_piper_pp: SemanticIcon
        @JsName("_yv") get() = this + "pied piper pp"
    @SemanticIconMarker val pied_piper_square: SemanticIcon
        @JsName("_zv") get() = this + "pied piper square"
    @SemanticIconMarker val piggy_bank: SemanticIcon
        @JsName("_Av") get() = this + "piggy bank"
    @SemanticIconMarker val pills: SemanticIcon
        @JsName("_Bv") get() = this + "pills"
    @SemanticIconMarker val pink_users: SemanticIcon
        @JsName("_Cv") get() = this + "pink users"
    @SemanticIconMarker val pinterest: SemanticIcon
        @JsName("_Dv") get() = this + "pinterest"
    @SemanticIconMarker val pinterest_p: SemanticIcon
        @JsName("_Ev") get() = this + "pinterest p"
    @SemanticIconMarker val pinterest_square: SemanticIcon
        @JsName("_Fv") get() = this + "pinterest square"
    @SemanticIconMarker val pizza_slice: SemanticIcon
        @JsName("_Gv") get() = this + "pizza slice"
    @SemanticIconMarker val place_of_worship: SemanticIcon
        @JsName("_Hv") get() = this + "place of worship"
    @SemanticIconMarker val plane: SemanticIcon
        @JsName("_Iv") get() = this + "plane"
    @SemanticIconMarker val plane_arrival: SemanticIcon
        @JsName("_Jv") get() = this + "plane arrival"
    @SemanticIconMarker val plane_departure: SemanticIcon
        @JsName("_Kv") get() = this + "plane departure"
    @SemanticIconMarker val play: SemanticIcon
        @JsName("_Lv") get() = this + "play"
    @SemanticIconMarker val play_circle: SemanticIcon
        @JsName("_Mv") get() = this + "play circle"
    @SemanticIconMarker val play_circle_outline: SemanticIcon
        @JsName("_Nv") get() = this + "play circle outline"
    @SemanticIconMarker val playstation: SemanticIcon
        @JsName("_Ov") get() = this + "playstation"
    @SemanticIconMarker val plug: SemanticIcon
        @JsName("_Pv") get() = this + "plug"
    @SemanticIconMarker val plus: SemanticIcon
        @JsName("_Qv") get() = this + "plus"
    @SemanticIconMarker val plus_circle: SemanticIcon
        @JsName("_Rv") get() = this + "plus circle"
    @SemanticIconMarker val plus_square: SemanticIcon
        @JsName("_Sv") get() = this + "plus square"
    @SemanticIconMarker val plus_square_outline: SemanticIcon
        @JsName("_Tv") get() = this + "plus square outline"
    @SemanticIconMarker val podcast: SemanticIcon
        @JsName("_Uv") get() = this + "podcast"
    @SemanticIconMarker val poll: SemanticIcon
        @JsName("_Vv") get() = this + "poll"
    @SemanticIconMarker val poll_horizontal: SemanticIcon
        @JsName("_Wv") get() = this + "poll horizontal"
    @SemanticIconMarker val poo: SemanticIcon
        @JsName("_Xv") get() = this + "poo"
    @SemanticIconMarker val poo_storm: SemanticIcon
        @JsName("_Yv") get() = this + "poo storm"
    @SemanticIconMarker val poop: SemanticIcon
        @JsName("_Zv") get() = this + "poop"
    @SemanticIconMarker val portrait: SemanticIcon
        @JsName("_aw") get() = this + "portrait"
    @SemanticIconMarker val pound_sign: SemanticIcon
        @JsName("_bw") get() = this + "pound sign"
    @SemanticIconMarker val power_off: SemanticIcon
        @JsName("_cw") get() = this + "power off"
    @SemanticIconMarker val pray: SemanticIcon
        @JsName("_dw") get() = this + "pray"
    @SemanticIconMarker val praying_hands: SemanticIcon
        @JsName("_ew") get() = this + "praying hands"
    @SemanticIconMarker val prescription: SemanticIcon
        @JsName("_fw") get() = this + "prescription"
    @SemanticIconMarker val prescription_bottle: SemanticIcon
        @JsName("_gw") get() = this + "prescription bottle"
    @SemanticIconMarker val prescription_bottle_alternate: SemanticIcon
        @JsName("_hw") get() = this + "prescription bottle alternate"
    @SemanticIconMarker val primary_users: SemanticIcon
        @JsName("_iw") get() = this + "primary users"
    @SemanticIconMarker val print: SemanticIcon
        @JsName("_jw") get() = this + "print"
    @SemanticIconMarker val procedures: SemanticIcon
        @JsName("_kw") get() = this + "procedures"
    @SemanticIconMarker val product_hunt: SemanticIcon
        @JsName("_lw") get() = this + "product hunt"
    @SemanticIconMarker val project_diagram: SemanticIcon
        @JsName("_mw") get() = this + "project diagram"
    @SemanticIconMarker val pump_medical: SemanticIcon
        @JsName("_nw") get() = this + "pump medical"
    @SemanticIconMarker val pump_soap: SemanticIcon
        @JsName("_ow") get() = this + "pump soap"
    @SemanticIconMarker val purple_users: SemanticIcon
        @JsName("_pw") get() = this + "purple users"
    @SemanticIconMarker val pushed: SemanticIcon
        @JsName("_qw") get() = this + "pushed"
    @SemanticIconMarker val puzzle: SemanticIcon
        @JsName("_rw") get() = this + "puzzle"
    @SemanticIconMarker val puzzle_piece: SemanticIcon
        @JsName("_sw") get() = this + "puzzle piece"
    @SemanticIconMarker val python: SemanticIcon
        @JsName("_tw") get() = this + "python"
    @SemanticIconMarker val qq: SemanticIcon
        @JsName("_uw") get() = this + "qq"
    @SemanticIconMarker val qrcode: SemanticIcon
        @JsName("_vw") get() = this + "qrcode"
    @SemanticIconMarker val question: SemanticIcon
        @JsName("_ww") get() = this + "question"
    @SemanticIconMarker val question_circle: SemanticIcon
        @JsName("_xw") get() = this + "question circle"
    @SemanticIconMarker val question_circle_outline: SemanticIcon
        @JsName("_yw") get() = this + "question circle outline"
    @SemanticIconMarker val quidditch: SemanticIcon
        @JsName("_zw") get() = this + "quidditch"
    @SemanticIconMarker val quinscape: SemanticIcon
        @JsName("_Aw") get() = this + "quinscape"
    @SemanticIconMarker val quora: SemanticIcon
        @JsName("_Bw") get() = this + "quora"
    @SemanticIconMarker val quote_left: SemanticIcon
        @JsName("_Cw") get() = this + "quote left"
    @SemanticIconMarker val quote_right: SemanticIcon
        @JsName("_Dw") get() = this + "quote right"
    @SemanticIconMarker val quran: SemanticIcon
        @JsName("_Ew") get() = this + "quran"
    @SemanticIconMarker val r_project: SemanticIcon
        @JsName("_Fw") get() = this + "r project"
    @SemanticIconMarker val radiation: SemanticIcon
        @JsName("_Gw") get() = this + "radiation"
    @SemanticIconMarker val radiation_alternate: SemanticIcon
        @JsName("_Hw") get() = this + "radiation alternate"
    @SemanticIconMarker val rainbow: SemanticIcon
        @JsName("_Iw") get() = this + "rainbow"
    @SemanticIconMarker val random: SemanticIcon
        @JsName("_Jw") get() = this + "random"
    @SemanticIconMarker val raspberry_pi: SemanticIcon
        @JsName("_Kw") get() = this + "raspberry pi"
    @SemanticIconMarker val ravelry: SemanticIcon
        @JsName("_Lw") get() = this + "ravelry"
    @SemanticIconMarker val react: SemanticIcon
        @JsName("_Mw") get() = this + "react"
    @SemanticIconMarker val reacteurope: SemanticIcon
        @JsName("_Nw") get() = this + "reacteurope"
    @SemanticIconMarker val readme: SemanticIcon
        @JsName("_Ow") get() = this + "readme"
    @SemanticIconMarker val rebel: SemanticIcon
        @JsName("_Pw") get() = this + "rebel"
    @SemanticIconMarker val receipt: SemanticIcon
        @JsName("_Qw") get() = this + "receipt"
    @SemanticIconMarker val record_vinyl: SemanticIcon
        @JsName("_Rw") get() = this + "record vinyl"
    @SemanticIconMarker val recycle: SemanticIcon
        @JsName("_Sw") get() = this + "recycle"
    @SemanticIconMarker val red_users: SemanticIcon
        @JsName("_Tw") get() = this + "red users"
    @SemanticIconMarker val reddit: SemanticIcon
        @JsName("_Uw") get() = this + "reddit"
    @SemanticIconMarker val reddit_alien: SemanticIcon
        @JsName("_Vw") get() = this + "reddit alien"
    @SemanticIconMarker val reddit_square: SemanticIcon
        @JsName("_Ww") get() = this + "reddit square"
    @SemanticIconMarker val redhat: SemanticIcon
        @JsName("_Xw") get() = this + "redhat"
    @SemanticIconMarker val redo: SemanticIcon
        @JsName("_Yw") get() = this + "redo"
    @SemanticIconMarker val redo_alternate: SemanticIcon
        @JsName("_Zw") get() = this + "redo alternate"
    @SemanticIconMarker val redriver: SemanticIcon
        @JsName("_ax") get() = this + "redriver"
    @SemanticIconMarker val redyeti: SemanticIcon
        @JsName("_bx") get() = this + "redyeti"
    @SemanticIconMarker val registered: SemanticIcon
        @JsName("_cx") get() = this + "registered"
    @SemanticIconMarker val registered_outline: SemanticIcon
        @JsName("_dx") get() = this + "registered outline"
    @SemanticIconMarker val remove_format: SemanticIcon
        @JsName("_ex") get() = this + "remove format"
    @SemanticIconMarker val renren: SemanticIcon
        @JsName("_fx") get() = this + "renren"
    @SemanticIconMarker val reply: SemanticIcon
        @JsName("_gx") get() = this + "reply"
    @SemanticIconMarker val reply_all: SemanticIcon
        @JsName("_hx") get() = this + "reply all"
    @SemanticIconMarker val replyd: SemanticIcon
        @JsName("_ix") get() = this + "replyd"
    @SemanticIconMarker val republican: SemanticIcon
        @JsName("_jx") get() = this + "republican"
    @SemanticIconMarker val researchgate: SemanticIcon
        @JsName("_kx") get() = this + "researchgate"
    @SemanticIconMarker val resolving: SemanticIcon
        @JsName("_lx") get() = this + "resolving"
    @SemanticIconMarker val restroom: SemanticIcon
        @JsName("_mx") get() = this + "restroom"
    @SemanticIconMarker val retweet: SemanticIcon
        @JsName("_nx") get() = this + "retweet"
    @SemanticIconMarker val rev: SemanticIcon
        @JsName("_ox") get() = this + "rev"
    @SemanticIconMarker val ribbon: SemanticIcon
        @JsName("_px") get() = this + "ribbon"
    @SemanticIconMarker val ring: SemanticIcon
        @JsName("_qx") get() = this + "ring"
    @SemanticIconMarker val road: SemanticIcon
        @JsName("_rx") get() = this + "road"
    @SemanticIconMarker val robot: SemanticIcon
        @JsName("_sx") get() = this + "robot"
    @SemanticIconMarker val rocket: SemanticIcon
        @JsName("_tx") get() = this + "rocket"
    @SemanticIconMarker val rocketchat: SemanticIcon
        @JsName("_ux") get() = this + "rocketchat"
    @SemanticIconMarker val rockrms: SemanticIcon
        @JsName("_vx") get() = this + "rockrms"
    @SemanticIconMarker val route: SemanticIcon
        @JsName("_wx") get() = this + "route"
    @SemanticIconMarker val rss: SemanticIcon
        @JsName("_xx") get() = this + "rss"
    @SemanticIconMarker val rss_square: SemanticIcon
        @JsName("_yx") get() = this + "rss square"
    @SemanticIconMarker val ruble_sign: SemanticIcon
        @JsName("_zx") get() = this + "ruble sign"
    @SemanticIconMarker val ruler: SemanticIcon
        @JsName("_Ax") get() = this + "ruler"
    @SemanticIconMarker val ruler_combined: SemanticIcon
        @JsName("_Bx") get() = this + "ruler combined"
    @SemanticIconMarker val ruler_horizontal: SemanticIcon
        @JsName("_Cx") get() = this + "ruler horizontal"
    @SemanticIconMarker val ruler_vertical: SemanticIcon
        @JsName("_Dx") get() = this + "ruler vertical"
    @SemanticIconMarker val running: SemanticIcon
        @JsName("_Ex") get() = this + "running"
    @SemanticIconMarker val rupee_sign: SemanticIcon
        @JsName("_Fx") get() = this + "rupee sign"
    @SemanticIconMarker val sad_cry: SemanticIcon
        @JsName("_Gx") get() = this + "sad cry"
    @SemanticIconMarker val sad_cry_outline: SemanticIcon
        @JsName("_Hx") get() = this + "sad cry outline"
    @SemanticIconMarker val sad_tear: SemanticIcon
        @JsName("_Ix") get() = this + "sad tear"
    @SemanticIconMarker val sad_tear_outline: SemanticIcon
        @JsName("_Jx") get() = this + "sad tear outline"
    @SemanticIconMarker val safari: SemanticIcon
        @JsName("_Kx") get() = this + "safari"
    @SemanticIconMarker val salesforce: SemanticIcon
        @JsName("_Lx") get() = this + "salesforce"
    @SemanticIconMarker val sass: SemanticIcon
        @JsName("_Mx") get() = this + "sass"
    @SemanticIconMarker val satellite: SemanticIcon
        @JsName("_Nx") get() = this + "satellite"
    @SemanticIconMarker val satellite_dish: SemanticIcon
        @JsName("_Ox") get() = this + "satellite dish"
    @SemanticIconMarker val save: SemanticIcon
        @JsName("_Px") get() = this + "save"
    @SemanticIconMarker val save_outline: SemanticIcon
        @JsName("_Qx") get() = this + "save outline"
    @SemanticIconMarker val schlix: SemanticIcon
        @JsName("_Rx") get() = this + "schlix"
    @SemanticIconMarker val school: SemanticIcon
        @JsName("_Sx") get() = this + "school"
    @SemanticIconMarker val screwdriver: SemanticIcon
        @JsName("_Tx") get() = this + "screwdriver"
    @SemanticIconMarker val scribd: SemanticIcon
        @JsName("_Ux") get() = this + "scribd"
    @SemanticIconMarker val scroll: SemanticIcon
        @JsName("_Vx") get() = this + "scroll"
    @SemanticIconMarker val sd_card: SemanticIcon
        @JsName("_Wx") get() = this + "sd card"
    @SemanticIconMarker val search: SemanticIcon
        @JsName("_Xx") get() = this + "search"
    @SemanticIconMarker val search_dollar: SemanticIcon
        @JsName("_Yx") get() = this + "search dollar"
    @SemanticIconMarker val search_location: SemanticIcon
        @JsName("_Zx") get() = this + "search location"
    @SemanticIconMarker val search_minus: SemanticIcon
        @JsName("_ay") get() = this + "search minus"
    @SemanticIconMarker val search_plus: SemanticIcon
        @JsName("_by") get() = this + "search plus"
    @SemanticIconMarker val searchengin: SemanticIcon
        @JsName("_cy") get() = this + "searchengin"
    @SemanticIconMarker val secondary_users: SemanticIcon
        @JsName("_dy") get() = this + "secondary users"
    @SemanticIconMarker val seedling: SemanticIcon
        @JsName("_ey") get() = this + "seedling"
    @SemanticIconMarker val sellcast: SemanticIcon
        @JsName("_fy") get() = this + "sellcast"
    @SemanticIconMarker val sellsy: SemanticIcon
        @JsName("_gy") get() = this + "sellsy"
    @SemanticIconMarker val server: SemanticIcon
        @JsName("_hy") get() = this + "server"
    @SemanticIconMarker val servicestack: SemanticIcon
        @JsName("_iy") get() = this + "servicestack"
    @SemanticIconMarker val shapes: SemanticIcon
        @JsName("_jy") get() = this + "shapes"
    @SemanticIconMarker val share: SemanticIcon
        @JsName("_ky") get() = this + "share"
    @SemanticIconMarker val share_alternate: SemanticIcon
        @JsName("_ly") get() = this + "share alternate"
    @SemanticIconMarker val share_alternate_square: SemanticIcon
        @JsName("_my") get() = this + "share alternate square"
    @SemanticIconMarker val share_square: SemanticIcon
        @JsName("_ny") get() = this + "share square"
    @SemanticIconMarker val share_square_outline: SemanticIcon
        @JsName("_oy") get() = this + "share square outline"
    @SemanticIconMarker val shekel_sign: SemanticIcon
        @JsName("_py") get() = this + "shekel sign"
    @SemanticIconMarker val shield_alternate: SemanticIcon
        @JsName("_qy") get() = this + "shield alternate"
    @SemanticIconMarker val shield_virus: SemanticIcon
        @JsName("_ry") get() = this + "shield virus"
    @SemanticIconMarker val ship: SemanticIcon
        @JsName("_sy") get() = this + "ship"
    @SemanticIconMarker val shipping_fast: SemanticIcon
        @JsName("_ty") get() = this + "shipping fast"
    @SemanticIconMarker val shirtsinbulk: SemanticIcon
        @JsName("_uy") get() = this + "shirtsinbulk"
    @SemanticIconMarker val shoe_prints: SemanticIcon
        @JsName("_vy") get() = this + "shoe prints"
    @SemanticIconMarker val shopify: SemanticIcon
        @JsName("_wy") get() = this + "shopify"
    @SemanticIconMarker val shopping_bag: SemanticIcon
        @JsName("_xy") get() = this + "shopping bag"
    @SemanticIconMarker val shopping_basket: SemanticIcon
        @JsName("_yy") get() = this + "shopping basket"
    @SemanticIconMarker val shopping_cart: SemanticIcon
        @JsName("_zy") get() = this + "shopping cart"
    @SemanticIconMarker val shopware: SemanticIcon
        @JsName("_Ay") get() = this + "shopware"
    @SemanticIconMarker val shower: SemanticIcon
        @JsName("_By") get() = this + "shower"
    @SemanticIconMarker val shuttle_van: SemanticIcon
        @JsName("_Cy") get() = this + "shuttle van"
    @SemanticIconMarker val sign: SemanticIcon
        @JsName("_Dy") get() = this + "sign"
    @SemanticIconMarker val sign_in_alternate: SemanticIcon
        @JsName("_Ey") get() = this + "sign in alternate"
    @SemanticIconMarker val sign_language: SemanticIcon
        @JsName("_Fy") get() = this + "sign language"
    @SemanticIconMarker val sign_out_alternate: SemanticIcon
        @JsName("_Gy") get() = this + "sign out alternate"
    @SemanticIconMarker val signal: SemanticIcon
        @JsName("_Hy") get() = this + "signal"
    @SemanticIconMarker val sim_card: SemanticIcon
        @JsName("_Iy") get() = this + "sim card"
    @SemanticIconMarker val simplybuilt: SemanticIcon
        @JsName("_Jy") get() = this + "simplybuilt"
    @SemanticIconMarker val sistrix: SemanticIcon
        @JsName("_Ky") get() = this + "sistrix"
    @SemanticIconMarker val sitemap: SemanticIcon
        @JsName("_Ly") get() = this + "sitemap"
    @SemanticIconMarker val sith: SemanticIcon
        @JsName("_My") get() = this + "sith"
    @SemanticIconMarker val skating: SemanticIcon
        @JsName("_Ny") get() = this + "skating"
    @SemanticIconMarker val sketch: SemanticIcon
        @JsName("_Oy") get() = this + "sketch"
    @SemanticIconMarker val skiing: SemanticIcon
        @JsName("_Py") get() = this + "skiing"
    @SemanticIconMarker val skiing_nordic: SemanticIcon
        @JsName("_Qy") get() = this + "skiing nordic"
    @SemanticIconMarker val skull_crossbones: SemanticIcon
        @JsName("_Ry") get() = this + "skull crossbones"
    @SemanticIconMarker val skyatlas: SemanticIcon
        @JsName("_Sy") get() = this + "skyatlas"
    @SemanticIconMarker val skype: SemanticIcon
        @JsName("_Ty") get() = this + "skype"
    @SemanticIconMarker val slack: SemanticIcon
        @JsName("_Uy") get() = this + "slack"
    @SemanticIconMarker val slack_hash: SemanticIcon
        @JsName("_Vy") get() = this + "slack hash"
    @SemanticIconMarker val slash: SemanticIcon
        @JsName("_Wy") get() = this + "slash"
    @SemanticIconMarker val sleigh: SemanticIcon
        @JsName("_Xy") get() = this + "sleigh"
    @SemanticIconMarker val sliders_horizontal: SemanticIcon
        @JsName("_Yy") get() = this + "sliders horizontal"
    @SemanticIconMarker val slideshare: SemanticIcon
        @JsName("_Zy") get() = this + "slideshare"
    @SemanticIconMarker val small_home: SemanticIcon
        @JsName("_az") get() = this + "small home"
    @SemanticIconMarker val smile: SemanticIcon
        @JsName("_bz") get() = this + "smile"
    @SemanticIconMarker val smile_beam: SemanticIcon
        @JsName("_cz") get() = this + "smile beam"
    @SemanticIconMarker val smile_beam_outline: SemanticIcon
        @JsName("_dz") get() = this + "smile beam outline"
    @SemanticIconMarker val smile_outline: SemanticIcon
        @JsName("_ez") get() = this + "smile outline"
    @SemanticIconMarker val smile_wink: SemanticIcon
        @JsName("_fz") get() = this + "smile wink"
    @SemanticIconMarker val smile_wink_outline: SemanticIcon
        @JsName("_gz") get() = this + "smile wink outline"
    @SemanticIconMarker val smog: SemanticIcon
        @JsName("_hz") get() = this + "smog"
    @SemanticIconMarker val smoking: SemanticIcon
        @JsName("_iz") get() = this + "smoking"
    @SemanticIconMarker val smoking_ban: SemanticIcon
        @JsName("_jz") get() = this + "smoking ban"
    @SemanticIconMarker val sms: SemanticIcon
        @JsName("_kz") get() = this + "sms"
    @SemanticIconMarker val snapchat: SemanticIcon
        @JsName("_lz") get() = this + "snapchat"
    @SemanticIconMarker val snapchat_ghost: SemanticIcon
        @JsName("_mz") get() = this + "snapchat ghost"
    @SemanticIconMarker val snapchat_square: SemanticIcon
        @JsName("_nz") get() = this + "snapchat square"
    @SemanticIconMarker val snowboarding: SemanticIcon
        @JsName("_oz") get() = this + "snowboarding"
    @SemanticIconMarker val snowflake: SemanticIcon
        @JsName("_pz") get() = this + "snowflake"
    @SemanticIconMarker val snowflake_outline: SemanticIcon
        @JsName("_qz") get() = this + "snowflake outline"
    @SemanticIconMarker val snowman: SemanticIcon
        @JsName("_rz") get() = this + "snowman"
    @SemanticIconMarker val snowplow: SemanticIcon
        @JsName("_sz") get() = this + "snowplow"
    @SemanticIconMarker val soap: SemanticIcon
        @JsName("_tz") get() = this + "soap"
    @SemanticIconMarker val socks: SemanticIcon
        @JsName("_uz") get() = this + "socks"
    @SemanticIconMarker val solar_panel: SemanticIcon
        @JsName("_vz") get() = this + "solar panel"
    @SemanticIconMarker val sort: SemanticIcon
        @JsName("_wz") get() = this + "sort"
    @SemanticIconMarker val sort_alphabet_down: SemanticIcon
        @JsName("_xz") get() = this + "sort alphabet down"
    @SemanticIconMarker val sort_alphabet_down_alternate: SemanticIcon
        @JsName("_yz") get() = this + "sort alphabet down alternate"
    @SemanticIconMarker val sort_alphabet_up: SemanticIcon
        @JsName("_zz") get() = this + "sort alphabet up"
    @SemanticIconMarker val sort_alphabet_up_alternate: SemanticIcon
        @JsName("_Az") get() = this + "sort alphabet up alternate"
    @SemanticIconMarker val sort_amount_down: SemanticIcon
        @JsName("_Bz") get() = this + "sort amount down"
    @SemanticIconMarker val sort_amount_down_alternate: SemanticIcon
        @JsName("_Cz") get() = this + "sort amount down alternate"
    @SemanticIconMarker val sort_amount_up: SemanticIcon
        @JsName("_Dz") get() = this + "sort amount up"
    @SemanticIconMarker val sort_amount_up_alternate: SemanticIcon
        @JsName("_Ez") get() = this + "sort amount up alternate"
    @SemanticIconMarker val sort_down: SemanticIcon
        @JsName("_Fz") get() = this + "sort down"
    @SemanticIconMarker val sort_numeric_down: SemanticIcon
        @JsName("_Gz") get() = this + "sort numeric down"
    @SemanticIconMarker val sort_numeric_down_alternate: SemanticIcon
        @JsName("_Hz") get() = this + "sort numeric down alternate"
    @SemanticIconMarker val sort_numeric_up: SemanticIcon
        @JsName("_Iz") get() = this + "sort numeric up"
    @SemanticIconMarker val sort_numeric_up_alternate: SemanticIcon
        @JsName("_Jz") get() = this + "sort numeric up alternate"
    @SemanticIconMarker val sort_up: SemanticIcon
        @JsName("_Kz") get() = this + "sort up"
    @SemanticIconMarker val soundcloud: SemanticIcon
        @JsName("_Lz") get() = this + "soundcloud"
    @SemanticIconMarker val sourcetree: SemanticIcon
        @JsName("_Mz") get() = this + "sourcetree"
    @SemanticIconMarker val spa: SemanticIcon
        @JsName("_Nz") get() = this + "spa"
    @SemanticIconMarker val space_shuttle: SemanticIcon
        @JsName("_Oz") get() = this + "space shuttle"
    @SemanticIconMarker val speakap: SemanticIcon
        @JsName("_Pz") get() = this + "speakap"
    @SemanticIconMarker val speaker_deck: SemanticIcon
        @JsName("_Qz") get() = this + "speaker deck"
    @SemanticIconMarker val spell_check: SemanticIcon
        @JsName("_Rz") get() = this + "spell check"
    @SemanticIconMarker val spider: SemanticIcon
        @JsName("_Sz") get() = this + "spider"
    @SemanticIconMarker val spinner: SemanticIcon
        @JsName("_Tz") get() = this + "spinner"
    @SemanticIconMarker val spinner_loading: SemanticIcon
        @JsName("_Uz") get() = this + "spinner loading"
    @SemanticIconMarker val splotch: SemanticIcon
        @JsName("_Vz") get() = this + "splotch"
    @SemanticIconMarker val spotify: SemanticIcon
        @JsName("_Wz") get() = this + "spotify"
    @SemanticIconMarker val spray_can: SemanticIcon
        @JsName("_Xz") get() = this + "spray can"
    @SemanticIconMarker val square: SemanticIcon
        @JsName("_Yz") get() = this + "square"
    @SemanticIconMarker val square_full: SemanticIcon
        @JsName("_Zz") get() = this + "square full"
    @SemanticIconMarker val square_outline: SemanticIcon
        @JsName("_aA") get() = this + "square outline"
    @SemanticIconMarker val square_root_alternate: SemanticIcon
        @JsName("_bA") get() = this + "square root alternate"
    @SemanticIconMarker val squarespace: SemanticIcon
        @JsName("_cA") get() = this + "squarespace"
    @SemanticIconMarker val stack_exchange: SemanticIcon
        @JsName("_dA") get() = this + "stack exchange"
    @SemanticIconMarker val stack_overflow: SemanticIcon
        @JsName("_eA") get() = this + "stack overflow"
    @SemanticIconMarker val stackpath: SemanticIcon
        @JsName("_fA") get() = this + "stackpath"
    @SemanticIconMarker val stamp: SemanticIcon
        @JsName("_gA") get() = this + "stamp"
    @SemanticIconMarker val star: SemanticIcon
        @JsName("_hA") get() = this + "star"
    @SemanticIconMarker val star_and_crescent: SemanticIcon
        @JsName("_iA") get() = this + "star and crescent"
    @SemanticIconMarker val star_half: SemanticIcon
        @JsName("_jA") get() = this + "star half"
    @SemanticIconMarker val star_half_alternate: SemanticIcon
        @JsName("_kA") get() = this + "star half alternate"
    @SemanticIconMarker val star_half_outline: SemanticIcon
        @JsName("_lA") get() = this + "star half outline"
    @SemanticIconMarker val star_of_david: SemanticIcon
        @JsName("_mA") get() = this + "star of david"
    @SemanticIconMarker val star_of_life: SemanticIcon
        @JsName("_nA") get() = this + "star of life"
    @SemanticIconMarker val star_outline: SemanticIcon
        @JsName("_oA") get() = this + "star outline"
    @SemanticIconMarker val staylinked: SemanticIcon
        @JsName("_pA") get() = this + "staylinked"
    @SemanticIconMarker val steam: SemanticIcon
        @JsName("_qA") get() = this + "steam"
    @SemanticIconMarker val steam_square: SemanticIcon
        @JsName("_rA") get() = this + "steam square"
    @SemanticIconMarker val steam_symbol: SemanticIcon
        @JsName("_sA") get() = this + "steam symbol"
    @SemanticIconMarker val step_backward: SemanticIcon
        @JsName("_tA") get() = this + "step backward"
    @SemanticIconMarker val step_forward: SemanticIcon
        @JsName("_uA") get() = this + "step forward"
    @SemanticIconMarker val stethoscope: SemanticIcon
        @JsName("_vA") get() = this + "stethoscope"
    @SemanticIconMarker val sticker_mule: SemanticIcon
        @JsName("_wA") get() = this + "sticker mule"
    @SemanticIconMarker val sticky_note: SemanticIcon
        @JsName("_xA") get() = this + "sticky note"
    @SemanticIconMarker val sticky_note_outline: SemanticIcon
        @JsName("_yA") get() = this + "sticky note outline"
    @SemanticIconMarker val stop: SemanticIcon
        @JsName("_zA") get() = this + "stop"
    @SemanticIconMarker val stop_circle: SemanticIcon
        @JsName("_AA") get() = this + "stop circle"
    @SemanticIconMarker val stop_circle_outline: SemanticIcon
        @JsName("_BA") get() = this + "stop circle outline"
    @SemanticIconMarker val stopwatch: SemanticIcon
        @JsName("_CA") get() = this + "stopwatch"
    @SemanticIconMarker val store: SemanticIcon
        @JsName("_DA") get() = this + "store"
    @SemanticIconMarker val store_alternate: SemanticIcon
        @JsName("_EA") get() = this + "store alternate"
    @SemanticIconMarker val store_alternate_slash: SemanticIcon
        @JsName("_FA") get() = this + "store alternate slash"
    @SemanticIconMarker val store_slash: SemanticIcon
        @JsName("_GA") get() = this + "store slash"
    @SemanticIconMarker val strava: SemanticIcon
        @JsName("_HA") get() = this + "strava"
    @SemanticIconMarker val stream: SemanticIcon
        @JsName("_IA") get() = this + "stream"
    @SemanticIconMarker val street_view: SemanticIcon
        @JsName("_JA") get() = this + "street view"
    @SemanticIconMarker val strikethrough: SemanticIcon
        @JsName("_KA") get() = this + "strikethrough"
    @SemanticIconMarker val stripe: SemanticIcon
        @JsName("_LA") get() = this + "stripe"
    @SemanticIconMarker val stripe_s: SemanticIcon
        @JsName("_MA") get() = this + "stripe s"
    @SemanticIconMarker val stroopwafel: SemanticIcon
        @JsName("_NA") get() = this + "stroopwafel"
    @SemanticIconMarker val studiovinari: SemanticIcon
        @JsName("_OA") get() = this + "studiovinari"
    @SemanticIconMarker val stumbleupon: SemanticIcon
        @JsName("_PA") get() = this + "stumbleupon"
    @SemanticIconMarker val stumbleupon_circle: SemanticIcon
        @JsName("_QA") get() = this + "stumbleupon circle"
    @SemanticIconMarker val subscript: SemanticIcon
        @JsName("_RA") get() = this + "subscript"
    @SemanticIconMarker val subway: SemanticIcon
        @JsName("_SA") get() = this + "subway"
    @SemanticIconMarker val suitcase: SemanticIcon
        @JsName("_TA") get() = this + "suitcase"
    @SemanticIconMarker val suitcase_rolling: SemanticIcon
        @JsName("_UA") get() = this + "suitcase rolling"
    @SemanticIconMarker val sun: SemanticIcon
        @JsName("_VA") get() = this + "sun"
    @SemanticIconMarker val sun_outline: SemanticIcon
        @JsName("_WA") get() = this + "sun outline"
    @SemanticIconMarker val superpowers: SemanticIcon
        @JsName("_XA") get() = this + "superpowers"
    @SemanticIconMarker val superscript: SemanticIcon
        @JsName("_YA") get() = this + "superscript"
    @SemanticIconMarker val supple: SemanticIcon
        @JsName("_ZA") get() = this + "supple"
    @SemanticIconMarker val surprise: SemanticIcon
        @JsName("_aB") get() = this + "surprise"
    @SemanticIconMarker val surprise_outline: SemanticIcon
        @JsName("_bB") get() = this + "surprise outline"
    @SemanticIconMarker val suse: SemanticIcon
        @JsName("_cB") get() = this + "suse"
    @SemanticIconMarker val swatchbook: SemanticIcon
        @JsName("_dB") get() = this + "swatchbook"
    @SemanticIconMarker val swift: SemanticIcon
        @JsName("_eB") get() = this + "swift"
    @SemanticIconMarker val swimmer: SemanticIcon
        @JsName("_fB") get() = this + "swimmer"
    @SemanticIconMarker val swimming_pool: SemanticIcon
        @JsName("_gB") get() = this + "swimming pool"
    @SemanticIconMarker val symfony: SemanticIcon
        @JsName("_hB") get() = this + "symfony"
    @SemanticIconMarker val synagogue: SemanticIcon
        @JsName("_iB") get() = this + "synagogue"
    @SemanticIconMarker val sync: SemanticIcon
        @JsName("_jB") get() = this + "sync"
    @SemanticIconMarker val sync_alternate: SemanticIcon
        @JsName("_kB") get() = this + "sync alternate"
    @SemanticIconMarker val syringe: SemanticIcon
        @JsName("_lB") get() = this + "syringe"
    @SemanticIconMarker val table: SemanticIcon
        @JsName("_mB") get() = this + "table"
    @SemanticIconMarker val table_tennis: SemanticIcon
        @JsName("_nB") get() = this + "table tennis"
    @SemanticIconMarker val tablet: SemanticIcon
        @JsName("_oB") get() = this + "tablet"
    @SemanticIconMarker val tablet_alternate: SemanticIcon
        @JsName("_pB") get() = this + "tablet alternate"
    @SemanticIconMarker val tablets: SemanticIcon
        @JsName("_qB") get() = this + "tablets"
    @SemanticIconMarker val tachometer_alternate: SemanticIcon
        @JsName("_rB") get() = this + "tachometer alternate"
    @SemanticIconMarker val tag: SemanticIcon
        @JsName("_sB") get() = this + "tag"
    @SemanticIconMarker val tags: SemanticIcon
        @JsName("_tB") get() = this + "tags"
    @SemanticIconMarker val tape: SemanticIcon
        @JsName("_uB") get() = this + "tape"
    @SemanticIconMarker val tasks: SemanticIcon
        @JsName("_vB") get() = this + "tasks"
    @SemanticIconMarker val taxi: SemanticIcon
        @JsName("_wB") get() = this + "taxi"
    @SemanticIconMarker val teal_users: SemanticIcon
        @JsName("_xB") get() = this + "teal users"
    @SemanticIconMarker val teamspeak: SemanticIcon
        @JsName("_yB") get() = this + "teamspeak"
    @SemanticIconMarker val teeth: SemanticIcon
        @JsName("_zB") get() = this + "teeth"
    @SemanticIconMarker val teeth_open: SemanticIcon
        @JsName("_AB") get() = this + "teeth open"
    @SemanticIconMarker val telegram: SemanticIcon
        @JsName("_BB") get() = this + "telegram"
    @SemanticIconMarker val telegram_plane: SemanticIcon
        @JsName("_CB") get() = this + "telegram plane"
    @SemanticIconMarker val temperature_high: SemanticIcon
        @JsName("_DB") get() = this + "temperature high"
    @SemanticIconMarker val temperature_low: SemanticIcon
        @JsName("_EB") get() = this + "temperature low"
    @SemanticIconMarker val tencent_weibo: SemanticIcon
        @JsName("_FB") get() = this + "tencent weibo"
    @SemanticIconMarker val tenge: SemanticIcon
        @JsName("_GB") get() = this + "tenge"
    @SemanticIconMarker val terminal: SemanticIcon
        @JsName("_HB") get() = this + "terminal"
    @SemanticIconMarker val text_height: SemanticIcon
        @JsName("_IB") get() = this + "text height"
    @SemanticIconMarker val text_width: SemanticIcon
        @JsName("_JB") get() = this + "text width"
    @SemanticIconMarker val th: SemanticIcon
        @JsName("_KB") get() = this + "th"
    @SemanticIconMarker val th_large: SemanticIcon
        @JsName("_LB") get() = this + "th large"
    @SemanticIconMarker val th_list: SemanticIcon
        @JsName("_MB") get() = this + "th list"
    @SemanticIconMarker val theater_masks: SemanticIcon
        @JsName("_NB") get() = this + "theater masks"
    @SemanticIconMarker val themeco: SemanticIcon
        @JsName("_OB") get() = this + "themeco"
    @SemanticIconMarker val themeisle: SemanticIcon
        @JsName("_PB") get() = this + "themeisle"
    @SemanticIconMarker val thermometer: SemanticIcon
        @JsName("_QB") get() = this + "thermometer"
    @SemanticIconMarker val thermometer_empty: SemanticIcon
        @JsName("_RB") get() = this + "thermometer empty"
    @SemanticIconMarker val thermometer_full: SemanticIcon
        @JsName("_SB") get() = this + "thermometer full"
    @SemanticIconMarker val thermometer_half: SemanticIcon
        @JsName("_TB") get() = this + "thermometer half"
    @SemanticIconMarker val thermometer_quarter: SemanticIcon
        @JsName("_UB") get() = this + "thermometer quarter"
    @SemanticIconMarker val thermometer_three_quarters: SemanticIcon
        @JsName("_VB") get() = this + "thermometer three quarters"
    @SemanticIconMarker val think_peaks: SemanticIcon
        @JsName("_WB") get() = this + "think peaks"
    @SemanticIconMarker val thumbs_down: SemanticIcon
        @JsName("_XB") get() = this + "thumbs down"
    @SemanticIconMarker val thumbs_down_outline: SemanticIcon
        @JsName("_YB") get() = this + "thumbs down outline"
    @SemanticIconMarker val thumbs_up: SemanticIcon
        @JsName("_ZB") get() = this + "thumbs up"
    @SemanticIconMarker val thumbs_up_outline: SemanticIcon
        @JsName("_aC") get() = this + "thumbs up outline"
    @SemanticIconMarker val thumbtack: SemanticIcon
        @JsName("_bC") get() = this + "thumbtack"
    @SemanticIconMarker val ticket_alternate: SemanticIcon
        @JsName("_cC") get() = this + "ticket alternate"
    @SemanticIconMarker val times: SemanticIcon
        @JsName("_dC") get() = this + "times"
    @SemanticIconMarker val times_circle: SemanticIcon
        @JsName("_eC") get() = this + "times circle"
    @SemanticIconMarker val times_circle_outline: SemanticIcon
        @JsName("_fC") get() = this + "times circle outline"
    @SemanticIconMarker val tint: SemanticIcon
        @JsName("_gC") get() = this + "tint"
    @SemanticIconMarker val tint_slash: SemanticIcon
        @JsName("_hC") get() = this + "tint slash"
    @SemanticIconMarker val tiny_home: SemanticIcon
        @JsName("_iC") get() = this + "tiny home"
    @SemanticIconMarker val tired: SemanticIcon
        @JsName("_jC") get() = this + "tired"
    @SemanticIconMarker val tired_outline: SemanticIcon
        @JsName("_kC") get() = this + "tired outline"
    @SemanticIconMarker val toggle_off: SemanticIcon
        @JsName("_lC") get() = this + "toggle off"
    @SemanticIconMarker val toggle_on: SemanticIcon
        @JsName("_mC") get() = this + "toggle on"
    @SemanticIconMarker val toilet: SemanticIcon
        @JsName("_nC") get() = this + "toilet"
    @SemanticIconMarker val toilet_paper: SemanticIcon
        @JsName("_oC") get() = this + "toilet paper"
    @SemanticIconMarker val toilet_paper_slash: SemanticIcon
        @JsName("_pC") get() = this + "toilet paper slash"
    @SemanticIconMarker val toolbox: SemanticIcon
        @JsName("_qC") get() = this + "toolbox"
    @SemanticIconMarker val tools: SemanticIcon
        @JsName("_rC") get() = this + "tools"
    @SemanticIconMarker val tooth: SemanticIcon
        @JsName("_sC") get() = this + "tooth"
    @SemanticIconMarker val top_left_corner_add: SemanticIcon
        @JsName("_tC") get() = this + "top left corner add"
    @SemanticIconMarker val top_right_corner_add: SemanticIcon
        @JsName("_uC") get() = this + "top right corner add"
    @SemanticIconMarker val torah: SemanticIcon
        @JsName("_vC") get() = this + "torah"
    @SemanticIconMarker val torii_gate: SemanticIcon
        @JsName("_wC") get() = this + "torii gate"
    @SemanticIconMarker val tractor: SemanticIcon
        @JsName("_xC") get() = this + "tractor"
    @SemanticIconMarker val trade_federation: SemanticIcon
        @JsName("_yC") get() = this + "trade federation"
    @SemanticIconMarker val trademark: SemanticIcon
        @JsName("_zC") get() = this + "trademark"
    @SemanticIconMarker val traffic_light: SemanticIcon
        @JsName("_AC") get() = this + "traffic light"
    @SemanticIconMarker val trailer: SemanticIcon
        @JsName("_BC") get() = this + "trailer"
    @SemanticIconMarker val train: SemanticIcon
        @JsName("_CC") get() = this + "train"
    @SemanticIconMarker val tram: SemanticIcon
        @JsName("_DC") get() = this + "tram"
    @SemanticIconMarker val transgender: SemanticIcon
        @JsName("_EC") get() = this + "transgender"
    @SemanticIconMarker val transgender_alternate: SemanticIcon
        @JsName("_FC") get() = this + "transgender alternate"
    @SemanticIconMarker val trash: SemanticIcon
        @JsName("_GC") get() = this + "trash"
    @SemanticIconMarker val trash_alternate: SemanticIcon
        @JsName("_HC") get() = this + "trash alternate"
    @SemanticIconMarker val trash_alternate_outline: SemanticIcon
        @JsName("_IC") get() = this + "trash alternate outline"
    @SemanticIconMarker val trash_restore: SemanticIcon
        @JsName("_JC") get() = this + "trash restore"
    @SemanticIconMarker val trash_restore_alternate: SemanticIcon
        @JsName("_KC") get() = this + "trash restore alternate"
    @SemanticIconMarker val tree: SemanticIcon
        @JsName("_LC") get() = this + "tree"
    @SemanticIconMarker val trello: SemanticIcon
        @JsName("_MC") get() = this + "trello"
    @SemanticIconMarker val tripadvisor: SemanticIcon
        @JsName("_NC") get() = this + "tripadvisor"
    @SemanticIconMarker val trophy: SemanticIcon
        @JsName("_OC") get() = this + "trophy"
    @SemanticIconMarker val truck: SemanticIcon
        @JsName("_PC") get() = this + "truck"
    @SemanticIconMarker val truck_monster: SemanticIcon
        @JsName("_QC") get() = this + "truck monster"
    @SemanticIconMarker val truck_moving: SemanticIcon
        @JsName("_RC") get() = this + "truck moving"
    @SemanticIconMarker val truck_packing: SemanticIcon
        @JsName("_SC") get() = this + "truck packing"
    @SemanticIconMarker val truck_pickup: SemanticIcon
        @JsName("_TC") get() = this + "truck pickup"
    @SemanticIconMarker val tshirt: SemanticIcon
        @JsName("_UC") get() = this + "tshirt"
    @SemanticIconMarker val tty: SemanticIcon
        @JsName("_VC") get() = this + "tty"
    @SemanticIconMarker val tumblr: SemanticIcon
        @JsName("_WC") get() = this + "tumblr"
    @SemanticIconMarker val tumblr_square: SemanticIcon
        @JsName("_XC") get() = this + "tumblr square"
    @SemanticIconMarker val tv: SemanticIcon
        @JsName("_YC") get() = this + "tv"
    @SemanticIconMarker val twitch: SemanticIcon
        @JsName("_ZC") get() = this + "twitch"
    @SemanticIconMarker val twitter: SemanticIcon
        @JsName("_aD") get() = this + "twitter"
    @SemanticIconMarker val twitter_square: SemanticIcon
        @JsName("_bD") get() = this + "twitter square"
    @SemanticIconMarker val typo3: SemanticIcon
        @JsName("_cD") get() = this + "typo3"
    @SemanticIconMarker val uber: SemanticIcon
        @JsName("_dD") get() = this + "uber"
    @SemanticIconMarker val ubuntu: SemanticIcon
        @JsName("_eD") get() = this + "ubuntu"
    @SemanticIconMarker val uikit: SemanticIcon
        @JsName("_fD") get() = this + "uikit"
    @SemanticIconMarker val umbraco: SemanticIcon
        @JsName("_gD") get() = this + "umbraco"
    @SemanticIconMarker val umbrella: SemanticIcon
        @JsName("_hD") get() = this + "umbrella"
    @SemanticIconMarker val umbrella_beach: SemanticIcon
        @JsName("_iD") get() = this + "umbrella beach"
    @SemanticIconMarker val underline: SemanticIcon
        @JsName("_jD") get() = this + "underline"
    @SemanticIconMarker val undo: SemanticIcon
        @JsName("_kD") get() = this + "undo"
    @SemanticIconMarker val undo_alternate: SemanticIcon
        @JsName("_lD") get() = this + "undo alternate"
    @SemanticIconMarker val uniregistry: SemanticIcon
        @JsName("_mD") get() = this + "uniregistry"
    @SemanticIconMarker val unity: SemanticIcon
        @JsName("_nD") get() = this + "unity"
    @SemanticIconMarker val universal_access: SemanticIcon
        @JsName("_oD") get() = this + "universal access"
    @SemanticIconMarker val university: SemanticIcon
        @JsName("_pD") get() = this + "university"
    @SemanticIconMarker val unlink: SemanticIcon
        @JsName("_qD") get() = this + "unlink"
    @SemanticIconMarker val unlock: SemanticIcon
        @JsName("_rD") get() = this + "unlock"
    @SemanticIconMarker val unlock_alternate: SemanticIcon
        @JsName("_sD") get() = this + "unlock alternate"
    @SemanticIconMarker val untappd: SemanticIcon
        @JsName("_tD") get() = this + "untappd"
    @SemanticIconMarker val upload: SemanticIcon
        @JsName("_uD") get() = this + "upload"
    @SemanticIconMarker val ups: SemanticIcon
        @JsName("_vD") get() = this + "ups"
    @SemanticIconMarker val usb: SemanticIcon
        @JsName("_wD") get() = this + "usb"
    @SemanticIconMarker val user: SemanticIcon
        @JsName("_xD") get() = this + "user"
    @SemanticIconMarker val user_alternate: SemanticIcon
        @JsName("_yD") get() = this + "user alternate"
    @SemanticIconMarker val user_alternate_slash: SemanticIcon
        @JsName("_zD") get() = this + "user alternate slash"
    @SemanticIconMarker val user_astronaut: SemanticIcon
        @JsName("_AD") get() = this + "user astronaut"
    @SemanticIconMarker val user_check: SemanticIcon
        @JsName("_BD") get() = this + "user check"
    @SemanticIconMarker val user_circle: SemanticIcon
        @JsName("_CD") get() = this + "user circle"
    @SemanticIconMarker val user_circle_outline: SemanticIcon
        @JsName("_DD") get() = this + "user circle outline"
    @SemanticIconMarker val user_clock: SemanticIcon
        @JsName("_ED") get() = this + "user clock"
    @SemanticIconMarker val user_cog: SemanticIcon
        @JsName("_FD") get() = this + "user cog"
    @SemanticIconMarker val user_edit: SemanticIcon
        @JsName("_GD") get() = this + "user edit"
    @SemanticIconMarker val user_friends: SemanticIcon
        @JsName("_HD") get() = this + "user friends"
    @SemanticIconMarker val user_graduate: SemanticIcon
        @JsName("_ID") get() = this + "user graduate"
    @SemanticIconMarker val user_injured: SemanticIcon
        @JsName("_JD") get() = this + "user injured"
    @SemanticIconMarker val user_lock: SemanticIcon
        @JsName("_KD") get() = this + "user lock"
    @SemanticIconMarker val user_md: SemanticIcon
        @JsName("_LD") get() = this + "user md"
    @SemanticIconMarker val user_minus: SemanticIcon
        @JsName("_MD") get() = this + "user minus"
    @SemanticIconMarker val user_ninja: SemanticIcon
        @JsName("_ND") get() = this + "user ninja"
    @SemanticIconMarker val user_nurse: SemanticIcon
        @JsName("_OD") get() = this + "user nurse"
    @SemanticIconMarker val user_outline: SemanticIcon
        @JsName("_PD") get() = this + "user outline"
    @SemanticIconMarker val user_plus: SemanticIcon
        @JsName("_QD") get() = this + "user plus"
    @SemanticIconMarker val user_secret: SemanticIcon
        @JsName("_RD") get() = this + "user secret"
    @SemanticIconMarker val user_shield: SemanticIcon
        @JsName("_SD") get() = this + "user shield"
    @SemanticIconMarker val user_slash: SemanticIcon
        @JsName("_TD") get() = this + "user slash"
    @SemanticIconMarker val user_tag: SemanticIcon
        @JsName("_UD") get() = this + "user tag"
    @SemanticIconMarker val user_tie: SemanticIcon
        @JsName("_VD") get() = this + "user tie"
    @SemanticIconMarker val user_times: SemanticIcon
        @JsName("_WD") get() = this + "user times"
    @SemanticIconMarker val users: SemanticIcon
        @JsName("_XD") get() = this + "users"
    @SemanticIconMarker val users_cog: SemanticIcon
        @JsName("_YD") get() = this + "users cog"
    @SemanticIconMarker val usps: SemanticIcon
        @JsName("_ZD") get() = this + "usps"
    @SemanticIconMarker val ussunnah: SemanticIcon
        @JsName("_aE") get() = this + "ussunnah"
    @SemanticIconMarker val utensil_spoon: SemanticIcon
        @JsName("_bE") get() = this + "utensil spoon"
    @SemanticIconMarker val utensils: SemanticIcon
        @JsName("_cE") get() = this + "utensils"
    @SemanticIconMarker val vaadin: SemanticIcon
        @JsName("_dE") get() = this + "vaadin"
    @SemanticIconMarker val vector_square: SemanticIcon
        @JsName("_eE") get() = this + "vector square"
    @SemanticIconMarker val venus: SemanticIcon
        @JsName("_fE") get() = this + "venus"
    @SemanticIconMarker val venus_double: SemanticIcon
        @JsName("_gE") get() = this + "venus double"
    @SemanticIconMarker val venus_mars: SemanticIcon
        @JsName("_hE") get() = this + "venus mars"
    @SemanticIconMarker val vertically_flipped_cloud: SemanticIcon
        @JsName("_iE") get() = this + "vertically flipped cloud"
    @SemanticIconMarker val viacoin: SemanticIcon
        @JsName("_jE") get() = this + "viacoin"
    @SemanticIconMarker val viadeo: SemanticIcon
        @JsName("_kE") get() = this + "viadeo"
    @SemanticIconMarker val viadeo_square: SemanticIcon
        @JsName("_lE") get() = this + "viadeo square"
    @SemanticIconMarker val vial: SemanticIcon
        @JsName("_mE") get() = this + "vial"
    @SemanticIconMarker val vials: SemanticIcon
        @JsName("_nE") get() = this + "vials"
    @SemanticIconMarker val viber: SemanticIcon
        @JsName("_oE") get() = this + "viber"
    @SemanticIconMarker val video: SemanticIcon
        @JsName("_pE") get() = this + "video"
    @SemanticIconMarker val video_slash: SemanticIcon
        @JsName("_qE") get() = this + "video slash"
    @SemanticIconMarker val vihara: SemanticIcon
        @JsName("_rE") get() = this + "vihara"
    @SemanticIconMarker val vimeo: SemanticIcon
        @JsName("_sE") get() = this + "vimeo"
    @SemanticIconMarker val vimeo_square: SemanticIcon
        @JsName("_tE") get() = this + "vimeo square"
    @SemanticIconMarker val vimeo_v: SemanticIcon
        @JsName("_uE") get() = this + "vimeo v"
    @SemanticIconMarker val vine: SemanticIcon
        @JsName("_vE") get() = this + "vine"
    @SemanticIconMarker val violet_users: SemanticIcon
        @JsName("_wE") get() = this + "violet users"
    @SemanticIconMarker val virus: SemanticIcon
        @JsName("_xE") get() = this + "virus"
    @SemanticIconMarker val virus_slash: SemanticIcon
        @JsName("_yE") get() = this + "virus slash"
    @SemanticIconMarker val viruses: SemanticIcon
        @JsName("_zE") get() = this + "viruses"
    @SemanticIconMarker val vk: SemanticIcon
        @JsName("_AE") get() = this + "vk"
    @SemanticIconMarker val vnv: SemanticIcon
        @JsName("_BE") get() = this + "vnv"
    @SemanticIconMarker val voicemail: SemanticIcon
        @JsName("_CE") get() = this + "voicemail"
    @SemanticIconMarker val volleyball_ball: SemanticIcon
        @JsName("_DE") get() = this + "volleyball ball"
    @SemanticIconMarker val volume_down: SemanticIcon
        @JsName("_EE") get() = this + "volume down"
    @SemanticIconMarker val volume_mute: SemanticIcon
        @JsName("_FE") get() = this + "volume mute"
    @SemanticIconMarker val volume_off: SemanticIcon
        @JsName("_GE") get() = this + "volume off"
    @SemanticIconMarker val volume_up: SemanticIcon
        @JsName("_HE") get() = this + "volume up"
    @SemanticIconMarker val vote_yea: SemanticIcon
        @JsName("_IE") get() = this + "vote yea"
    @SemanticIconMarker val vuejs: SemanticIcon
        @JsName("_JE") get() = this + "vuejs"
    @SemanticIconMarker val walking: SemanticIcon
        @JsName("_KE") get() = this + "walking"
    @SemanticIconMarker val wallet: SemanticIcon
        @JsName("_LE") get() = this + "wallet"
    @SemanticIconMarker val warehouse: SemanticIcon
        @JsName("_ME") get() = this + "warehouse"
    @SemanticIconMarker val water: SemanticIcon
        @JsName("_NE") get() = this + "water"
    @SemanticIconMarker val wave_square: SemanticIcon
        @JsName("_OE") get() = this + "wave square"
    @SemanticIconMarker val waze: SemanticIcon
        @JsName("_PE") get() = this + "waze"
    @SemanticIconMarker val weebly: SemanticIcon
        @JsName("_QE") get() = this + "weebly"
    @SemanticIconMarker val weibo: SemanticIcon
        @JsName("_RE") get() = this + "weibo"
    @SemanticIconMarker val weight: SemanticIcon
        @JsName("_SE") get() = this + "weight"
    @SemanticIconMarker val weixin: SemanticIcon
        @JsName("_TE") get() = this + "weixin"
    @SemanticIconMarker val whatsapp: SemanticIcon
        @JsName("_UE") get() = this + "whatsapp"
    @SemanticIconMarker val whatsapp_square: SemanticIcon
        @JsName("_VE") get() = this + "whatsapp square"
    @SemanticIconMarker val wheelchair: SemanticIcon
        @JsName("_WE") get() = this + "wheelchair"
    @SemanticIconMarker val whmcs: SemanticIcon
        @JsName("_XE") get() = this + "whmcs"
    @SemanticIconMarker val wifi: SemanticIcon
        @JsName("_YE") get() = this + "wifi"
    @SemanticIconMarker val wikipedia_w: SemanticIcon
        @JsName("_ZE") get() = this + "wikipedia w"
    @SemanticIconMarker val wind: SemanticIcon
        @JsName("_aF") get() = this + "wind"
    @SemanticIconMarker val window_close: SemanticIcon
        @JsName("_bF") get() = this + "window close"
    @SemanticIconMarker val window_close_outline: SemanticIcon
        @JsName("_cF") get() = this + "window close outline"
    @SemanticIconMarker val window_maximize: SemanticIcon
        @JsName("_dF") get() = this + "window maximize"
    @SemanticIconMarker val window_maximize_outline: SemanticIcon
        @JsName("_eF") get() = this + "window maximize outline"
    @SemanticIconMarker val window_minimize: SemanticIcon
        @JsName("_fF") get() = this + "window minimize"
    @SemanticIconMarker val window_minimize_outline: SemanticIcon
        @JsName("_gF") get() = this + "window minimize outline"
    @SemanticIconMarker val window_restore: SemanticIcon
        @JsName("_hF") get() = this + "window restore"
    @SemanticIconMarker val window_restore_outline: SemanticIcon
        @JsName("_iF") get() = this + "window restore outline"
    @SemanticIconMarker val windows: SemanticIcon
        @JsName("_jF") get() = this + "windows"
    @SemanticIconMarker val wine_bottle: SemanticIcon
        @JsName("_kF") get() = this + "wine bottle"
    @SemanticIconMarker val wine_glass: SemanticIcon
        @JsName("_lF") get() = this + "wine glass"
    @SemanticIconMarker val wine_glass_alternate: SemanticIcon
        @JsName("_mF") get() = this + "wine glass alternate"
    @SemanticIconMarker val wix: SemanticIcon
        @JsName("_nF") get() = this + "wix"
    @SemanticIconMarker val wizards_of_the_coast: SemanticIcon
        @JsName("_oF") get() = this + "wizards of the coast"
    @SemanticIconMarker val wolf_pack_battalion: SemanticIcon
        @JsName("_pF") get() = this + "wolf pack battalion"
    @SemanticIconMarker val won_sign: SemanticIcon
        @JsName("_qF") get() = this + "won sign"
    @SemanticIconMarker val wordpress: SemanticIcon
        @JsName("_rF") get() = this + "wordpress"
    @SemanticIconMarker val wordpress_simple: SemanticIcon
        @JsName("_sF") get() = this + "wordpress simple"
    @SemanticIconMarker val world: SemanticIcon
        @JsName("_tF") get() = this + "world"
    @SemanticIconMarker val wpbeginner: SemanticIcon
        @JsName("_uF") get() = this + "wpbeginner"
    @SemanticIconMarker val wpexplorer: SemanticIcon
        @JsName("_vF") get() = this + "wpexplorer"
    @SemanticIconMarker val wpforms: SemanticIcon
        @JsName("_wF") get() = this + "wpforms"
    @SemanticIconMarker val wpressr: SemanticIcon
        @JsName("_xF") get() = this + "wpressr"
    @SemanticIconMarker val wrench: SemanticIcon
        @JsName("_yF") get() = this + "wrench"
    @SemanticIconMarker val x_ray: SemanticIcon
        @JsName("_zF") get() = this + "x ray"
    @SemanticIconMarker val xbox: SemanticIcon
        @JsName("_AF") get() = this + "xbox"
    @SemanticIconMarker val xing: SemanticIcon
        @JsName("_BF") get() = this + "xing"
    @SemanticIconMarker val xing_square: SemanticIcon
        @JsName("_CF") get() = this + "xing square"
    @SemanticIconMarker val y_combinator: SemanticIcon
        @JsName("_DF") get() = this + "y combinator"
    @SemanticIconMarker val yahoo: SemanticIcon
        @JsName("_EF") get() = this + "yahoo"
    @SemanticIconMarker val yammer: SemanticIcon
        @JsName("_FF") get() = this + "yammer"
    @SemanticIconMarker val yandex: SemanticIcon
        @JsName("_GF") get() = this + "yandex"
    @SemanticIconMarker val yandex_international: SemanticIcon
        @JsName("_HF") get() = this + "yandex international"
    @SemanticIconMarker val yarn: SemanticIcon
        @JsName("_IF") get() = this + "yarn"
    @SemanticIconMarker val yellow_users: SemanticIcon
        @JsName("_JF") get() = this + "yellow users"
    @SemanticIconMarker val yelp: SemanticIcon
        @JsName("_KF") get() = this + "yelp"
    @SemanticIconMarker val yen_sign: SemanticIcon
        @JsName("_LF") get() = this + "yen sign"
    @SemanticIconMarker val yin_yang: SemanticIcon
        @JsName("_MF") get() = this + "yin yang"
    @SemanticIconMarker val yoast: SemanticIcon
        @JsName("_NF") get() = this + "yoast"
    @SemanticIconMarker val youtube: SemanticIcon
        @JsName("_OF") get() = this + "youtube"
    @SemanticIconMarker val youtube_square: SemanticIcon
        @JsName("_PF") get() = this + "youtube square"
    @SemanticIconMarker val zhihu: SemanticIcon
        @JsName("_QF") get() = this + "zhihu"
}
