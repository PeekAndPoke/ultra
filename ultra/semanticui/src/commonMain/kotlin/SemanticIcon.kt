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

@Suppress("PropertyName", "FunctionName", "unused")
@SemanticIconMarker
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
            .plus(
                arrayOf(
                    "dont",
                    "delete",
                ),
            )

        fun cssClassOf(block: SemanticIcon.() -> SemanticIcon): String {
            return cssClassOfAsList(block).joinToString(" ")
        }

        fun cssClassOfAsList(block: SemanticIcon.() -> SemanticIcon): List<String> {
            lateinit var classes: List<String>

            createHTML().body {
                classes = icon.block().cssClasses.filter { it.isNotBlank() }
            }

            return classes
        }
    }

    private val cssClasses = mutableListOf<String>()

    private fun attrs() = attributesMapOf("class", cssClasses.joinToString(" ") + " icon")

    operator fun plus(cls: String): SemanticIcon = apply { cssClasses.add(cls) }

    operator fun plus(classes: Array<out String>): SemanticIcon = apply { cssClasses.addAll(classes) }

    fun render(block: I.() -> Unit = {}): Unit = I(attrs(), parent.consumer).visitNoInline(block)

    operator fun invoke(): Unit = render()

    operator fun invoke(block: I.() -> Unit): Unit = render(block)

    fun with(cls: String): SemanticIcon = this + cls

    fun custom(cls: String): Unit = (this + cls).render()

    // conditional classes

    fun given(
        condition: Boolean,
        action: SemanticIcon.() -> SemanticIcon,
    ): SemanticIcon = when (condition) {
        false -> this
        else -> this.action()
    }

    fun givenNot(
        condition: Boolean,
        action: SemanticIcon.() -> SemanticIcon,
    ): SemanticIcon = given(!condition, action)

    inline val then: SemanticIcon get() = this

    // coloring ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun color(color: SemanticColor): SemanticIcon = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    val red: SemanticIcon get() = this + "red"

    val orange: SemanticIcon get() = this + "orange"

    val yellow: SemanticIcon get() = this + "yellow"

    val olive: SemanticIcon get() = this + "olive"

    val green: SemanticIcon get() = this + "green"

    val teal: SemanticIcon get() = this + "teal"

    val blue: SemanticIcon get() = this + "blue"

    val violet: SemanticIcon get() = this + "violet"

    val purple: SemanticIcon get() = this + "purple"

    val pink: SemanticIcon get() = this + "pink"

    val brown: SemanticIcon get() = this + "brown"

    val grey: SemanticIcon get() = this + "grey"

    val black: SemanticIcon get() = this + "black"

    val white: SemanticIcon get() = this + "white"

    // Behaviour ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val bordered: SemanticIcon get() = this + "bordered"

    val circular: SemanticIcon get() = this + "circular"

    val clockwise: SemanticIcon get() = this + "clockwise"

    val counterclockwise: SemanticIcon get() = this + "counterclockwise"

    val colored: SemanticIcon get() = this + "colored"

    val corner: SemanticIcon get() = this + "corner"

    val disabled: SemanticIcon get() = this + "disabled"

    val divider: SemanticIcon get() = this + "divider"

    val fitted: SemanticIcon get() = this + "fitted"

    val flipped: SemanticIcon get() = this + "flipped"

    val horizontally: SemanticIcon get() = this + "horizontally"

    val link: SemanticIcon get() = this + "link"

    val loading: SemanticIcon get() = this + "loading"

    val inverted: SemanticIcon get() = this + "inverted"

    val rotated: SemanticIcon get() = this + "rotated"

    val vertically: SemanticIcon get() = this + "vertically"

    // Position & Alignment ////////////////////////////////////////////////////////////////////////////////////////////////////////

    val aligned: SemanticIcon get() = this + "aligned"

    val bottom: SemanticIcon get() = this + "bottom"

    val floated: SemanticIcon get() = this + "floated"

    val left: SemanticIcon get() = this + "right"

    val middle: SemanticIcon get() = this + "middle"

    val right: SemanticIcon get() = this + "right"

    val top: SemanticIcon get() = this + "top"

    // Size ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val mini: SemanticIcon get() = this + "mini"

    val tiny: SemanticIcon get() = this + "tiny"

    val small: SemanticIcon get() = this + "small"

    val medium: SemanticIcon get() = this

    val large: SemanticIcon get() = this + "large"

    val big: SemanticIcon get() = this + "big"

    val huge: SemanticIcon get() = this + "huge"

    val massive: SemanticIcon get() = this + "massive"

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Custom //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    inline val dont: SemanticIcon get() = this + "dont"

    inline val delete: SemanticIcon get() = this + "delete"

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Icons ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    inline val _500px: SemanticIcon
        get() = this + "500px"

    inline val accessible: SemanticIcon
        get() = this + "accessible"

    inline val accusoft: SemanticIcon
        get() = this + "accusoft"

    inline val acquisitions_incorporated: SemanticIcon
        get() = this + "acquisitions incorporated"

    inline val ad: SemanticIcon
        get() = this + "ad"

    inline val address_book: SemanticIcon
        get() = this + "address book"

    inline val address_book_outline: SemanticIcon
        get() = this + "address book outline"

    inline val address_card: SemanticIcon
        get() = this + "address card"

    inline val address_card_outline: SemanticIcon
        get() = this + "address card outline"

    inline val adjust: SemanticIcon
        get() = this + "adjust"

    inline val adn: SemanticIcon
        get() = this + "adn"

    inline val adobe: SemanticIcon
        get() = this + "adobe"

    inline val adversal: SemanticIcon
        get() = this + "adversal"

    inline val affiliatetheme: SemanticIcon
        get() = this + "affiliatetheme"

    inline val air_freshener: SemanticIcon
        get() = this + "air freshener"

    inline val airbnb: SemanticIcon
        get() = this + "airbnb"

    inline val algolia: SemanticIcon
        get() = this + "algolia"

    inline val align_center: SemanticIcon
        get() = this + "align center"

    inline val align_justify: SemanticIcon
        get() = this + "align justify"

    inline val align_left: SemanticIcon
        get() = this + "align left"

    inline val align_right: SemanticIcon
        get() = this + "align right"

    inline val alipay: SemanticIcon
        get() = this + "alipay"

    inline val allergies: SemanticIcon
        get() = this + "allergies"

    inline val alternate_github: SemanticIcon
        get() = this + "alternate github"

    inline val amazon: SemanticIcon
        get() = this + "amazon"

    inline val amazon_pay: SemanticIcon
        get() = this + "amazon pay"

    inline val ambulance: SemanticIcon
        get() = this + "ambulance"

    inline val american_sign_language_interpreting: SemanticIcon
        get() = this + "american sign language interpreting"

    inline val amilia: SemanticIcon
        get() = this + "amilia"

    inline val anchor: SemanticIcon
        get() = this + "anchor"

    inline val android: SemanticIcon
        get() = this + "android"

    inline val angellist: SemanticIcon
        get() = this + "angellist"

    inline val angle_double_down: SemanticIcon
        get() = this + "angle double down"

    inline val angle_double_left: SemanticIcon
        get() = this + "angle double left"

    inline val angle_double_right: SemanticIcon
        get() = this + "angle double right"

    inline val angle_double_up: SemanticIcon
        get() = this + "angle double up"

    inline val angle_down: SemanticIcon
        get() = this + "angle down"

    inline val angle_left: SemanticIcon
        get() = this + "angle left"

    inline val angle_right: SemanticIcon
        get() = this + "angle right"

    inline val angle_up: SemanticIcon
        get() = this + "angle up"

    inline val angry: SemanticIcon
        get() = this + "angry"

    inline val angry_outline: SemanticIcon
        get() = this + "angry outline"

    inline val angrycreative: SemanticIcon
        get() = this + "angrycreative"

    inline val angular: SemanticIcon
        get() = this + "angular"

    inline val ankh: SemanticIcon
        get() = this + "ankh"

    inline val app_store: SemanticIcon
        get() = this + "app store"

    inline val app_store_ios: SemanticIcon
        get() = this + "app store ios"

    inline val apper: SemanticIcon
        get() = this + "apper"

    inline val apple: SemanticIcon
        get() = this + "apple"

    inline val apple_pay: SemanticIcon
        get() = this + "apple pay"

    inline val archive: SemanticIcon
        get() = this + "archive"

    inline val archway: SemanticIcon
        get() = this + "archway"

    inline val arrow_alternate_circle_down: SemanticIcon
        get() = this + "arrow alternate circle down"

    inline val arrow_alternate_circle_down_outline: SemanticIcon
        get() = this + "arrow alternate circle down outline"

    inline val arrow_alternate_circle_left: SemanticIcon
        get() = this + "arrow alternate circle left"

    inline val arrow_alternate_circle_left_outline: SemanticIcon
        get() = this + "arrow alternate circle left outline"

    inline val arrow_alternate_circle_right: SemanticIcon
        get() = this + "arrow alternate circle right"

    inline val arrow_alternate_circle_right_outline: SemanticIcon
        get() = this + "arrow alternate circle right outline"

    inline val arrow_alternate_circle_up: SemanticIcon
        get() = this + "arrow alternate circle up"

    inline val arrow_alternate_circle_up_outline: SemanticIcon
        get() = this + "arrow alternate circle up outline"

    inline val arrow_circle_down: SemanticIcon
        get() = this + "arrow circle down"

    inline val arrow_circle_left: SemanticIcon
        get() = this + "arrow circle left"

    inline val arrow_circle_right: SemanticIcon
        get() = this + "arrow circle right"

    inline val arrow_circle_up: SemanticIcon
        get() = this + "arrow circle up"

    inline val arrow_down: SemanticIcon
        get() = this + "arrow down"

    inline val arrow_left: SemanticIcon
        get() = this + "arrow left"

    inline val arrow_right: SemanticIcon
        get() = this + "arrow right"

    inline val arrow_up: SemanticIcon
        get() = this + "arrow up"

    inline val arrows_alternate: SemanticIcon
        get() = this + "arrows alternate"

    inline val arrows_alternate_horizontal: SemanticIcon
        get() = this + "arrows alternate horizontal"

    inline val arrows_alternate_vertical: SemanticIcon
        get() = this + "arrows alternate vertical"

    inline val artstation: SemanticIcon
        get() = this + "artstation"

    inline val assistive_listening_systems: SemanticIcon
        get() = this + "assistive listening systems"

    inline val asterisk: SemanticIcon
        get() = this + "asterisk"

    inline val asterisk_loading: SemanticIcon
        get() = this + "asterisk loading"

    inline val asymmetrik: SemanticIcon
        get() = this + "asymmetrik"

    inline val at: SemanticIcon
        get() = this + "at"

    inline val atlas: SemanticIcon
        get() = this + "atlas"

    inline val atlassian: SemanticIcon
        get() = this + "atlassian"

    inline val atom: SemanticIcon
        get() = this + "atom"

    inline val audible: SemanticIcon
        get() = this + "audible"

    inline val audio_description: SemanticIcon
        get() = this + "audio description"

    inline val autoprefixer: SemanticIcon
        get() = this + "autoprefixer"

    inline val avianex: SemanticIcon
        get() = this + "avianex"

    inline val aviato: SemanticIcon
        get() = this + "aviato"

    inline val award: SemanticIcon
        get() = this + "award"

    inline val aws: SemanticIcon
        get() = this + "aws"

    inline val baby: SemanticIcon
        get() = this + "baby"

    inline val baby_carriage: SemanticIcon
        get() = this + "baby carriage"

    inline val backward: SemanticIcon
        get() = this + "backward"

    inline val bacon: SemanticIcon
        get() = this + "bacon"

    inline val bahai: SemanticIcon
        get() = this + "bahai"

    inline val balance_scale: SemanticIcon
        get() = this + "balance scale"

    inline val balance_scale_left: SemanticIcon
        get() = this + "balance scale left"

    inline val balance_scale_right: SemanticIcon
        get() = this + "balance scale right"

    inline val ban: SemanticIcon
        get() = this + "ban"

    inline val band_aid: SemanticIcon
        get() = this + "band aid"

    inline val bandcamp: SemanticIcon
        get() = this + "bandcamp"

    inline val barcode: SemanticIcon
        get() = this + "barcode"

    inline val bars: SemanticIcon
        get() = this + "bars"

    inline val baseball_ball: SemanticIcon
        get() = this + "baseball ball"

    inline val basketball_ball: SemanticIcon
        get() = this + "basketball ball"

    inline val bath: SemanticIcon
        get() = this + "bath"

    inline val battery_empty: SemanticIcon
        get() = this + "battery empty"

    inline val battery_full: SemanticIcon
        get() = this + "battery full"

    inline val battery_half: SemanticIcon
        get() = this + "battery half"

    inline val battery_quarter: SemanticIcon
        get() = this + "battery quarter"

    inline val battery_three_quarters: SemanticIcon
        get() = this + "battery three quarters"

    inline val battle_net: SemanticIcon
        get() = this + "battle net"

    inline val bed: SemanticIcon
        get() = this + "bed"

    inline val beer: SemanticIcon
        get() = this + "beer"

    inline val behance: SemanticIcon
        get() = this + "behance"

    inline val behance_square: SemanticIcon
        get() = this + "behance square"

    inline val bell: SemanticIcon
        get() = this + "bell"

    inline val bell_outline: SemanticIcon
        get() = this + "bell outline"

    inline val bell_slash: SemanticIcon
        get() = this + "bell slash"

    inline val bell_slash_outline: SemanticIcon
        get() = this + "bell slash outline"

    inline val bezier_curve: SemanticIcon
        get() = this + "bezier curve"

    inline val bible: SemanticIcon
        get() = this + "bible"

    inline val bicycle: SemanticIcon
        get() = this + "bicycle"

    inline val big_circle_outline: SemanticIcon
        get() = this + "big circle outline"

    inline val big_home: SemanticIcon
        get() = this + "big home"

    inline val big_red_dont: SemanticIcon
        get() = this + "big red dont"

    inline val biking: SemanticIcon
        get() = this + "biking"

    inline val bimobject: SemanticIcon
        get() = this + "bimobject"

    inline val binoculars: SemanticIcon
        get() = this + "binoculars"

    inline val biohazard: SemanticIcon
        get() = this + "biohazard"

    inline val birthday_cake: SemanticIcon
        get() = this + "birthday cake"

    inline val bitbucket: SemanticIcon
        get() = this + "bitbucket"

    inline val bitcoin: SemanticIcon
        get() = this + "bitcoin"

    inline val bity: SemanticIcon
        get() = this + "bity"

    inline val black_tie: SemanticIcon
        get() = this + "black tie"

    inline val black_user: SemanticIcon
        get() = this + "black user"

    inline val black_users: SemanticIcon
        get() = this + "black users"

    inline val blackberry: SemanticIcon
        get() = this + "blackberry"

    inline val blender: SemanticIcon
        get() = this + "blender"

    inline val blind: SemanticIcon
        get() = this + "blind"

    inline val blog: SemanticIcon
        get() = this + "blog"

    inline val blogger: SemanticIcon
        get() = this + "blogger"

    inline val blogger_b: SemanticIcon
        get() = this + "blogger b"

    inline val blue_users: SemanticIcon
        get() = this + "blue users"

    inline val bluetooth: SemanticIcon
        get() = this + "bluetooth"

    inline val bluetooth_b: SemanticIcon
        get() = this + "bluetooth b"

    inline val bold: SemanticIcon
        get() = this + "bold"

    inline val bolt: SemanticIcon
        get() = this + "bolt"

    inline val bomb: SemanticIcon
        get() = this + "bomb"

    inline val bone: SemanticIcon
        get() = this + "bone"

    inline val bong: SemanticIcon
        get() = this + "bong"

    inline val book: SemanticIcon
        get() = this + "book"

    inline val book_dead: SemanticIcon
        get() = this + "book dead"

    inline val book_medical: SemanticIcon
        get() = this + "book medical"

    inline val book_open: SemanticIcon
        get() = this + "book open"

    inline val book_reader: SemanticIcon
        get() = this + "book reader"

    inline val bookmark: SemanticIcon
        get() = this + "bookmark"

    inline val bookmark_outline: SemanticIcon
        get() = this + "bookmark outline"

    inline val bootstrap: SemanticIcon
        get() = this + "bootstrap"

    inline val border_all: SemanticIcon
        get() = this + "border all"

    inline val border_none: SemanticIcon
        get() = this + "border none"

    inline val border_style: SemanticIcon
        get() = this + "border style"

    inline val bordered_colored_blue_users: SemanticIcon
        get() = this + "bordered colored blue users"

    inline val bordered_colored_green_users: SemanticIcon
        get() = this + "bordered colored green users"

    inline val bordered_colored_red_users: SemanticIcon
        get() = this + "bordered colored red users"

    inline val bordered_inverted_black_users: SemanticIcon
        get() = this + "bordered inverted black users"

    inline val bordered_inverted_teal_users: SemanticIcon
        get() = this + "bordered inverted teal users"

    inline val bordered_teal_users: SemanticIcon
        get() = this + "bordered teal users"

    inline val bordered_users: SemanticIcon
        get() = this + "bordered users"

    inline val bottom_left_corner_add: SemanticIcon
        get() = this + "bottom left corner add"

    inline val bottom_right_corner_add: SemanticIcon
        get() = this + "bottom right corner add"

    inline val bowling_ball: SemanticIcon
        get() = this + "bowling ball"

    inline val box: SemanticIcon
        get() = this + "box"

    inline val box_open: SemanticIcon
        get() = this + "box open"

    inline val box_tissue: SemanticIcon
        get() = this + "box tissue"

    inline val boxes: SemanticIcon
        get() = this + "boxes"

    inline val braille: SemanticIcon
        get() = this + "braille"

    inline val brain: SemanticIcon
        get() = this + "brain"

    inline val bread_slice: SemanticIcon
        get() = this + "bread slice"

    inline val briefcase: SemanticIcon
        get() = this + "briefcase"

    inline val briefcase_medical: SemanticIcon
        get() = this + "briefcase medical"

    inline val broadcast_tower: SemanticIcon
        get() = this + "broadcast tower"

    inline val broom: SemanticIcon
        get() = this + "broom"

    inline val brown_users: SemanticIcon
        get() = this + "brown users"

    inline val brush: SemanticIcon
        get() = this + "brush"

    inline val btc: SemanticIcon
        get() = this + "btc"

    inline val buffer: SemanticIcon
        get() = this + "buffer"

    inline val bug: SemanticIcon
        get() = this + "bug"

    inline val building: SemanticIcon
        get() = this + "building"

    inline val building_outline: SemanticIcon
        get() = this + "building outline"

    inline val bullhorn: SemanticIcon
        get() = this + "bullhorn"

    inline val bullseye: SemanticIcon
        get() = this + "bullseye"

    inline val burn: SemanticIcon
        get() = this + "burn"

    inline val buromobelexperte: SemanticIcon
        get() = this + "buromobelexperte"

    inline val bus: SemanticIcon
        get() = this + "bus"

    inline val bus_alternate: SemanticIcon
        get() = this + "bus alternate"

    inline val business_time: SemanticIcon
        get() = this + "business time"

    inline val buy_n_large: SemanticIcon
        get() = this + "buy n large"

    inline val buysellads: SemanticIcon
        get() = this + "buysellads"

    inline val calculator: SemanticIcon
        get() = this + "calculator"

    inline val calendar: SemanticIcon
        get() = this + "calendar"

    inline val calendar_alternate: SemanticIcon
        get() = this + "calendar alternate"

    inline val calendar_alternate_outline: SemanticIcon
        get() = this + "calendar alternate outline"

    inline val calendar_check: SemanticIcon
        get() = this + "calendar check"

    inline val calendar_check_outline: SemanticIcon
        get() = this + "calendar check outline"

    inline val calendar_day: SemanticIcon
        get() = this + "calendar day"

    inline val calendar_minus: SemanticIcon
        get() = this + "calendar minus"

    inline val calendar_minus_outline: SemanticIcon
        get() = this + "calendar minus outline"

    inline val calendar_outline: SemanticIcon
        get() = this + "calendar outline"

    inline val calendar_plus: SemanticIcon
        get() = this + "calendar plus"

    inline val calendar_plus_outline: SemanticIcon
        get() = this + "calendar plus outline"

    inline val calendar_times: SemanticIcon
        get() = this + "calendar times"

    inline val calendar_times_outline: SemanticIcon
        get() = this + "calendar times outline"

    inline val calendar_week: SemanticIcon
        get() = this + "calendar week"

    inline val camera: SemanticIcon
        get() = this + "camera"

    inline val camera_retro: SemanticIcon
        get() = this + "camera retro"

    inline val campground: SemanticIcon
        get() = this + "campground"

    inline val canadian_maple_leaf: SemanticIcon
        get() = this + "canadian maple leaf"

    inline val candy_cane: SemanticIcon
        get() = this + "candy cane"

    inline val cannabis: SemanticIcon
        get() = this + "cannabis"

    inline val capsules: SemanticIcon
        get() = this + "capsules"

    inline val car: SemanticIcon
        get() = this + "car"

    inline val car_alternate: SemanticIcon
        get() = this + "car alternate"

    inline val car_battery: SemanticIcon
        get() = this + "car battery"

    inline val car_crash: SemanticIcon
        get() = this + "car crash"

    inline val car_side: SemanticIcon
        get() = this + "car side"

    inline val caravan: SemanticIcon
        get() = this + "caravan"

    inline val caret_down: SemanticIcon
        get() = this + "caret down"

    inline val caret_left: SemanticIcon
        get() = this + "caret left"

    inline val caret_right: SemanticIcon
        get() = this + "caret right"

    inline val caret_square_down: SemanticIcon
        get() = this + "caret square down"

    inline val caret_square_down_outline: SemanticIcon
        get() = this + "caret square down outline"

    inline val caret_square_left: SemanticIcon
        get() = this + "caret square left"

    inline val caret_square_left_outline: SemanticIcon
        get() = this + "caret square left outline"

    inline val caret_square_right: SemanticIcon
        get() = this + "caret square right"

    inline val caret_square_right_outline: SemanticIcon
        get() = this + "caret square right outline"

    inline val caret_square_up: SemanticIcon
        get() = this + "caret square up"

    inline val caret_square_up_outline: SemanticIcon
        get() = this + "caret square up outline"

    inline val caret_up: SemanticIcon
        get() = this + "caret up"

    inline val carrot: SemanticIcon
        get() = this + "carrot"

    inline val cart_arrow_down: SemanticIcon
        get() = this + "cart arrow down"

    inline val cart_plus: SemanticIcon
        get() = this + "cart plus"

    inline val cash_register: SemanticIcon
        get() = this + "cash register"

    inline val cat: SemanticIcon
        get() = this + "cat"

    inline val cc_amazon_pay: SemanticIcon
        get() = this + "cc amazon pay"

    inline val cc_amex: SemanticIcon
        get() = this + "cc amex"

    inline val cc_apple_pay: SemanticIcon
        get() = this + "cc apple pay"

    inline val cc_diners_club: SemanticIcon
        get() = this + "cc diners club"

    inline val cc_discover: SemanticIcon
        get() = this + "cc discover"

    inline val cc_jcb: SemanticIcon
        get() = this + "cc jcb"

    inline val cc_mastercard: SemanticIcon
        get() = this + "cc mastercard"

    inline val cc_paypal: SemanticIcon
        get() = this + "cc paypal"

    inline val cc_stripe: SemanticIcon
        get() = this + "cc stripe"

    inline val cc_visa: SemanticIcon
        get() = this + "cc visa"

    inline val centercode: SemanticIcon
        get() = this + "centercode"

    inline val centos: SemanticIcon
        get() = this + "centos"

    inline val certificate: SemanticIcon
        get() = this + "certificate"

    inline val chair: SemanticIcon
        get() = this + "chair"

    inline val chalkboard: SemanticIcon
        get() = this + "chalkboard"

    inline val chalkboard_teacher: SemanticIcon
        get() = this + "chalkboard teacher"

    inline val charging_station: SemanticIcon
        get() = this + "charging station"

    inline val chart_area: SemanticIcon
        get() = this + "chart area"

    inline val chart_bar: SemanticIcon
        get() = this + "chart bar"

    inline val chart_bar_outline: SemanticIcon
        get() = this + "chart bar outline"

    inline val chart_line: SemanticIcon
        get() = this + "chart line"

    inline val chart_pie: SemanticIcon
        get() = this + "chart pie"

    inline val check: SemanticIcon
        get() = this + "check"

    inline val check_circle: SemanticIcon
        get() = this + "check circle"

    inline val check_circle_outline: SemanticIcon
        get() = this + "check circle outline"

    inline val check_double: SemanticIcon
        get() = this + "check double"

    inline val check_square: SemanticIcon
        get() = this + "check square"

    inline val check_square_outline: SemanticIcon
        get() = this + "check square outline"

    inline val cheese: SemanticIcon
        get() = this + "cheese"

    inline val chess: SemanticIcon
        get() = this + "chess"

    inline val chess_bishop: SemanticIcon
        get() = this + "chess bishop"

    inline val chess_board: SemanticIcon
        get() = this + "chess board"

    inline val chess_king: SemanticIcon
        get() = this + "chess king"

    inline val chess_knight: SemanticIcon
        get() = this + "chess knight"

    inline val chess_pawn: SemanticIcon
        get() = this + "chess pawn"

    inline val chess_queen: SemanticIcon
        get() = this + "chess queen"

    inline val chess_rook: SemanticIcon
        get() = this + "chess rook"

    inline val chevron_circle_down: SemanticIcon
        get() = this + "chevron circle down"

    inline val chevron_circle_left: SemanticIcon
        get() = this + "chevron circle left"

    inline val chevron_circle_right: SemanticIcon
        get() = this + "chevron circle right"

    inline val chevron_circle_up: SemanticIcon
        get() = this + "chevron circle up"

    inline val chevron_down: SemanticIcon
        get() = this + "chevron down"

    inline val chevron_left: SemanticIcon
        get() = this + "chevron left"

    inline val chevron_right: SemanticIcon
        get() = this + "chevron right"

    inline val chevron_up: SemanticIcon
        get() = this + "chevron up"

    inline val child: SemanticIcon
        get() = this + "child"

    inline val chrome: SemanticIcon
        get() = this + "chrome"

    inline val chromecast: SemanticIcon
        get() = this + "chromecast"

    inline val church: SemanticIcon
        get() = this + "church"

    inline val circle: SemanticIcon
        get() = this + "circle"

    inline val circle_notch: SemanticIcon
        get() = this + "circle notch"

    inline val circle_outline: SemanticIcon
        get() = this + "circle outline"

    inline val circular_colored_blue_users: SemanticIcon
        get() = this + "circular colored blue users"

    inline val circular_colored_green_users: SemanticIcon
        get() = this + "circular colored green users"

    inline val circular_colored_red_users: SemanticIcon
        get() = this + "circular colored red users"

    inline val circular_inverted_teal_users: SemanticIcon
        get() = this + "circular inverted teal users"

    inline val circular_inverted_users: SemanticIcon
        get() = this + "circular inverted users"

    inline val circular_teal_users: SemanticIcon
        get() = this + "circular teal users"

    inline val circular_users: SemanticIcon
        get() = this + "circular users"

    inline val city: SemanticIcon
        get() = this + "city"

    inline val clinic_medical: SemanticIcon
        get() = this + "clinic medical"

    inline val clipboard: SemanticIcon
        get() = this + "clipboard"

    inline val clipboard_check: SemanticIcon
        get() = this + "clipboard check"

    inline val clipboard_list: SemanticIcon
        get() = this + "clipboard list"

    inline val clipboard_outline: SemanticIcon
        get() = this + "clipboard outline"

    inline val clock: SemanticIcon
        get() = this + "clock"

    inline val clock_outline: SemanticIcon
        get() = this + "clock outline"

    inline val clockwise_rotated_cloud: SemanticIcon
        get() = this + "clockwise rotated cloud"

    inline val clone: SemanticIcon
        get() = this + "clone"

    inline val clone_outline: SemanticIcon
        get() = this + "clone outline"

    inline val close: SemanticIcon
        get() = this + "close"

    inline val close_link: SemanticIcon
        get() = this + "close link"

    inline val closed_captioning: SemanticIcon
        get() = this + "closed captioning"

    inline val closed_captioning_outline: SemanticIcon
        get() = this + "closed captioning outline"

    inline val cloud: SemanticIcon
        get() = this + "cloud"

    inline val cloud_download_alternate: SemanticIcon
        get() = this + "cloud download alternate"

    inline val cloud_meatball: SemanticIcon
        get() = this + "cloud meatball"

    inline val cloud_moon: SemanticIcon
        get() = this + "cloud moon"

    inline val cloud_moon_rain: SemanticIcon
        get() = this + "cloud moon rain"

    inline val cloud_rain: SemanticIcon
        get() = this + "cloud rain"

    inline val cloud_showers_heavy: SemanticIcon
        get() = this + "cloud showers heavy"

    inline val cloud_sun: SemanticIcon
        get() = this + "cloud sun"

    inline val cloud_sun_rain: SemanticIcon
        get() = this + "cloud sun rain"

    inline val cloud_upload_alternate: SemanticIcon
        get() = this + "cloud upload alternate"

    inline val cloudscale: SemanticIcon
        get() = this + "cloudscale"

    inline val cloudsmith: SemanticIcon
        get() = this + "cloudsmith"

    inline val cloudversify: SemanticIcon
        get() = this + "cloudversify"

    inline val cocktail: SemanticIcon
        get() = this + "cocktail"

    inline val code: SemanticIcon
        get() = this + "code"

    inline val code_branch: SemanticIcon
        get() = this + "code branch"

    inline val codepen: SemanticIcon
        get() = this + "codepen"

    inline val codiepie: SemanticIcon
        get() = this + "codiepie"

    inline val coffee: SemanticIcon
        get() = this + "coffee"

    inline val cog: SemanticIcon
        get() = this + "cog"

    inline val cogs: SemanticIcon
        get() = this + "cogs"

    inline val coins: SemanticIcon
        get() = this + "coins"

    inline val columns: SemanticIcon
        get() = this + "columns"

    inline val comment: SemanticIcon
        get() = this + "comment"

    inline val comment_alternate: SemanticIcon
        get() = this + "comment alternate"

    inline val comment_alternate_outline: SemanticIcon
        get() = this + "comment alternate outline"

    inline val comment_dollar: SemanticIcon
        get() = this + "comment dollar"

    inline val comment_dots: SemanticIcon
        get() = this + "comment dots"

    inline val comment_dots_outline: SemanticIcon
        get() = this + "comment dots outline"

    inline val comment_medical: SemanticIcon
        get() = this + "comment medical"

    inline val comment_outline: SemanticIcon
        get() = this + "comment outline"

    inline val comment_slash: SemanticIcon
        get() = this + "comment slash"

    inline val comments: SemanticIcon
        get() = this + "comments"

    inline val comments_dollar: SemanticIcon
        get() = this + "comments dollar"

    inline val comments_outline: SemanticIcon
        get() = this + "comments outline"

    inline val compact_disc: SemanticIcon
        get() = this + "compact disc"

    inline val compass: SemanticIcon
        get() = this + "compass"

    inline val compass_outline: SemanticIcon
        get() = this + "compass outline"

    inline val compress: SemanticIcon
        get() = this + "compress"

    inline val compress_alternate: SemanticIcon
        get() = this + "compress alternate"

    inline val compress_arrows_alternate: SemanticIcon
        get() = this + "compress arrows alternate"

    inline val concierge_bell: SemanticIcon
        get() = this + "concierge bell"

    inline val confluence: SemanticIcon
        get() = this + "confluence"

    inline val connectdevelop: SemanticIcon
        get() = this + "connectdevelop"

    inline val contao: SemanticIcon
        get() = this + "contao"

    inline val content: SemanticIcon
        get() = this + "content"

    inline val cookie: SemanticIcon
        get() = this + "cookie"

    inline val cookie_bite: SemanticIcon
        get() = this + "cookie bite"

    inline val copy: SemanticIcon
        get() = this + "copy"

    inline val copy_outline: SemanticIcon
        get() = this + "copy outline"

    inline val copyright: SemanticIcon
        get() = this + "copyright"

    inline val copyright_outline: SemanticIcon
        get() = this + "copyright outline"

    inline val corner_add: SemanticIcon
        get() = this + "corner add"

    inline val cotton_bureau: SemanticIcon
        get() = this + "cotton bureau"

    inline val couch: SemanticIcon
        get() = this + "couch"

    inline val counterclockwise_rotated_cloud: SemanticIcon
        get() = this + "counterclockwise rotated cloud"

    inline val cpanel: SemanticIcon
        get() = this + "cpanel"

    inline val creative_commons: SemanticIcon
        get() = this + "creative commons"

    inline val creative_commons_by: SemanticIcon
        get() = this + "creative commons by"

    inline val creative_commons_nc: SemanticIcon
        get() = this + "creative commons nc"

    inline val creative_commons_nc_eu: SemanticIcon
        get() = this + "creative commons nc eu"

    inline val creative_commons_nc_jp: SemanticIcon
        get() = this + "creative commons nc jp"

    inline val creative_commons_nd: SemanticIcon
        get() = this + "creative commons nd"

    inline val creative_commons_pd: SemanticIcon
        get() = this + "creative commons pd"

    inline val creative_commons_pd_alternate: SemanticIcon
        get() = this + "creative commons pd alternate"

    inline val creative_commons_remix: SemanticIcon
        get() = this + "creative commons remix"

    inline val creative_commons_sa: SemanticIcon
        get() = this + "creative commons sa"

    inline val creative_commons_sampling: SemanticIcon
        get() = this + "creative commons sampling"

    inline val creative_commons_sampling_plus: SemanticIcon
        get() = this + "creative commons sampling plus"

    inline val creative_commons_share: SemanticIcon
        get() = this + "creative commons share"

    inline val creative_commons_zero: SemanticIcon
        get() = this + "creative commons zero"

    inline val credit_card: SemanticIcon
        get() = this + "credit card"

    inline val credit_card_outline: SemanticIcon
        get() = this + "credit card outline"

    inline val critical_role: SemanticIcon
        get() = this + "critical role"

    inline val crop: SemanticIcon
        get() = this + "crop"

    inline val crop_alternate: SemanticIcon
        get() = this + "crop alternate"

    inline val cross: SemanticIcon
        get() = this + "cross"

    inline val crosshairs: SemanticIcon
        get() = this + "crosshairs"

    inline val crow: SemanticIcon
        get() = this + "crow"

    inline val crutch: SemanticIcon
        get() = this + "crutch"

    inline val css3: SemanticIcon
        get() = this + "css3"

    inline val css3_alternate: SemanticIcon
        get() = this + "css3 alternate"

    inline val cube: SemanticIcon
        get() = this + "cube"

    inline val cubes: SemanticIcon
        get() = this + "cubes"

    inline val cut: SemanticIcon
        get() = this + "cut"

    inline val cuttlefish: SemanticIcon
        get() = this + "cuttlefish"

    inline val d_and_d: SemanticIcon
        get() = this + "d and d"

    inline val d_and_d_beyond: SemanticIcon
        get() = this + "d and d beyond"

    inline val dailymotion: SemanticIcon
        get() = this + "dailymotion"

    inline val dashcube: SemanticIcon
        get() = this + "dashcube"

    inline val database: SemanticIcon
        get() = this + "database"

    inline val deaf: SemanticIcon
        get() = this + "deaf"

    inline val delicious: SemanticIcon
        get() = this + "delicious"

    inline val democrat: SemanticIcon
        get() = this + "democrat"

    inline val deploydog: SemanticIcon
        get() = this + "deploydog"

    inline val deskpro: SemanticIcon
        get() = this + "deskpro"

    inline val desktop: SemanticIcon
        get() = this + "desktop"

    inline val dev: SemanticIcon
        get() = this + "dev"

    inline val deviantart: SemanticIcon
        get() = this + "deviantart"

    inline val dharmachakra: SemanticIcon
        get() = this + "dharmachakra"

    inline val dhl: SemanticIcon
        get() = this + "dhl"

    inline val diagnoses: SemanticIcon
        get() = this + "diagnoses"

    inline val diaspora: SemanticIcon
        get() = this + "diaspora"

    inline val dice: SemanticIcon
        get() = this + "dice"

    inline val dice_d20: SemanticIcon
        get() = this + "dice d20"

    inline val dice_d6: SemanticIcon
        get() = this + "dice d6"

    inline val dice_five: SemanticIcon
        get() = this + "dice five"

    inline val dice_four: SemanticIcon
        get() = this + "dice four"

    inline val dice_one: SemanticIcon
        get() = this + "dice one"

    inline val dice_six: SemanticIcon
        get() = this + "dice six"

    inline val dice_three: SemanticIcon
        get() = this + "dice three"

    inline val dice_two: SemanticIcon
        get() = this + "dice two"

    inline val digg: SemanticIcon
        get() = this + "digg"

    inline val digital_ocean: SemanticIcon
        get() = this + "digital ocean"

    inline val digital_tachograph: SemanticIcon
        get() = this + "digital tachograph"

    inline val directions: SemanticIcon
        get() = this + "directions"

    inline val disabled_users: SemanticIcon
        get() = this + "disabled users"

    inline val discord: SemanticIcon
        get() = this + "discord"

    inline val discourse: SemanticIcon
        get() = this + "discourse"

    inline val disease: SemanticIcon
        get() = this + "disease"

    inline val divide: SemanticIcon
        get() = this + "divide"

    inline val dizzy: SemanticIcon
        get() = this + "dizzy"

    inline val dizzy_outline: SemanticIcon
        get() = this + "dizzy outline"

    inline val dna: SemanticIcon
        get() = this + "dna"

    inline val dochub: SemanticIcon
        get() = this + "dochub"

    inline val docker: SemanticIcon
        get() = this + "docker"

    inline val dog: SemanticIcon
        get() = this + "dog"

    inline val dollar_sign: SemanticIcon
        get() = this + "dollar sign"

    inline val dolly: SemanticIcon
        get() = this + "dolly"

    inline val dolly_flatbed: SemanticIcon
        get() = this + "dolly flatbed"

    inline val donate: SemanticIcon
        get() = this + "donate"

    inline val door_closed: SemanticIcon
        get() = this + "door closed"

    inline val door_open: SemanticIcon
        get() = this + "door open"

    inline val dot_circle: SemanticIcon
        get() = this + "dot circle"

    inline val dot_circle_outline: SemanticIcon
        get() = this + "dot circle outline"

    inline val dove: SemanticIcon
        get() = this + "dove"

    inline val download: SemanticIcon
        get() = this + "download"

    inline val draft2digital: SemanticIcon
        get() = this + "draft2digital"

    inline val drafting_compass: SemanticIcon
        get() = this + "drafting compass"

    inline val dragon: SemanticIcon
        get() = this + "dragon"

    inline val draw_polygon: SemanticIcon
        get() = this + "draw polygon"

    inline val dribbble: SemanticIcon
        get() = this + "dribbble"

    inline val dribbble_square: SemanticIcon
        get() = this + "dribbble square"

    inline val dropbox: SemanticIcon
        get() = this + "dropbox"

    inline val dropdown: SemanticIcon
        get() = this + "dropdown"

    inline val drum: SemanticIcon
        get() = this + "drum"

    inline val drum_steelpan: SemanticIcon
        get() = this + "drum steelpan"

    inline val drumstick_bite: SemanticIcon
        get() = this + "drumstick bite"

    inline val drupal: SemanticIcon
        get() = this + "drupal"

    inline val dumbbell: SemanticIcon
        get() = this + "dumbbell"

    inline val dumpster: SemanticIcon
        get() = this + "dumpster"

    inline val dungeon: SemanticIcon
        get() = this + "dungeon"

    inline val dyalog: SemanticIcon
        get() = this + "dyalog"

    inline val earlybirds: SemanticIcon
        get() = this + "earlybirds"

    inline val ebay: SemanticIcon
        get() = this + "ebay"

    inline val edge: SemanticIcon
        get() = this + "edge"

    inline val edit: SemanticIcon
        get() = this + "edit"

    inline val edit_outline: SemanticIcon
        get() = this + "edit outline"

    inline val egg: SemanticIcon
        get() = this + "egg"

    inline val eject: SemanticIcon
        get() = this + "eject"

    inline val elementor: SemanticIcon
        get() = this + "elementor"

    inline val ellipsis_horizontal: SemanticIcon
        get() = this + "ellipsis horizontal"

    inline val ellipsis_vertical: SemanticIcon
        get() = this + "ellipsis vertical"

    inline val ello: SemanticIcon
        get() = this + "ello"

    inline val ember: SemanticIcon
        get() = this + "ember"

    inline val empire: SemanticIcon
        get() = this + "empire"

    inline val envelope: SemanticIcon
        get() = this + "envelope"

    inline val envelope_open: SemanticIcon
        get() = this + "envelope open"

    inline val envelope_open_outline: SemanticIcon
        get() = this + "envelope open outline"

    inline val envelope_open_text: SemanticIcon
        get() = this + "envelope open text"

    inline val envelope_outline: SemanticIcon
        get() = this + "envelope outline"

    inline val envelope_square: SemanticIcon
        get() = this + "envelope square"

    inline val envira: SemanticIcon
        get() = this + "envira"

    inline val equals_: SemanticIcon
        get() = this + "equals"

    inline val eraser: SemanticIcon
        get() = this + "eraser"

    inline val erlang: SemanticIcon
        get() = this + "erlang"

    inline val ethereum: SemanticIcon
        get() = this + "ethereum"

    inline val ethernet: SemanticIcon
        get() = this + "ethernet"

    inline val etsy: SemanticIcon
        get() = this + "etsy"

    inline val euro_sign: SemanticIcon
        get() = this + "euro sign"

    inline val evernote: SemanticIcon
        get() = this + "evernote"

    inline val exchange_alternate: SemanticIcon
        get() = this + "exchange alternate"

    inline val exclamation: SemanticIcon
        get() = this + "exclamation"

    inline val exclamation_circle: SemanticIcon
        get() = this + "exclamation circle"

    inline val exclamation_triangle: SemanticIcon
        get() = this + "exclamation triangle"

    inline val expand: SemanticIcon
        get() = this + "expand"

    inline val expand_alternate: SemanticIcon
        get() = this + "expand alternate"

    inline val expand_arrows_alternate: SemanticIcon
        get() = this + "expand arrows alternate"

    inline val expeditedssl: SemanticIcon
        get() = this + "expeditedssl"

    inline val external_alternate: SemanticIcon
        get() = this + "external alternate"

    inline val external_link_square_alternate: SemanticIcon
        get() = this + "external link square alternate"

    inline val eye: SemanticIcon
        get() = this + "eye"

    inline val eye_dropper: SemanticIcon
        get() = this + "eye dropper"

    inline val eye_outline: SemanticIcon
        get() = this + "eye outline"

    inline val eye_slash: SemanticIcon
        get() = this + "eye slash"

    inline val eye_slash_outline: SemanticIcon
        get() = this + "eye slash outline"

    inline val facebook: SemanticIcon
        get() = this + "facebook"

    inline val facebook_f: SemanticIcon
        get() = this + "facebook f"

    inline val facebook_messenger: SemanticIcon
        get() = this + "facebook messenger"

    inline val facebook_square: SemanticIcon
        get() = this + "facebook square"

    inline val fan: SemanticIcon
        get() = this + "fan"

    inline val fantasy_flight_games: SemanticIcon
        get() = this + "fantasy flight games"

    inline val fast_backward: SemanticIcon
        get() = this + "fast backward"

    inline val fast_forward: SemanticIcon
        get() = this + "fast forward"

    inline val faucet: SemanticIcon
        get() = this + "faucet"

    inline val fax: SemanticIcon
        get() = this + "fax"

    inline val feather: SemanticIcon
        get() = this + "feather"

    inline val feather_alternate: SemanticIcon
        get() = this + "feather alternate"

    inline val fedex: SemanticIcon
        get() = this + "fedex"

    inline val fedora: SemanticIcon
        get() = this + "fedora"

    inline val female: SemanticIcon
        get() = this + "female"

    inline val fighter_jet: SemanticIcon
        get() = this + "fighter jet"

    inline val figma: SemanticIcon
        get() = this + "figma"

    inline val file: SemanticIcon
        get() = this + "file"

    inline val file_alternate: SemanticIcon
        get() = this + "file alternate"

    inline val file_alternate_outline: SemanticIcon
        get() = this + "file alternate outline"

    inline val file_archive: SemanticIcon
        get() = this + "file archive"

    inline val file_archive_outline: SemanticIcon
        get() = this + "file archive outline"

    inline val file_audio: SemanticIcon
        get() = this + "file audio"

    inline val file_audio_outline: SemanticIcon
        get() = this + "file audio outline"

    inline val file_code: SemanticIcon
        get() = this + "file code"

    inline val file_code_outline: SemanticIcon
        get() = this + "file code outline"

    inline val file_contract: SemanticIcon
        get() = this + "file contract"

    inline val file_download: SemanticIcon
        get() = this + "file download"

    inline val file_excel: SemanticIcon
        get() = this + "file excel"

    inline val file_excel_outline: SemanticIcon
        get() = this + "file excel outline"

    inline val file_export: SemanticIcon
        get() = this + "file export"

    inline val file_image: SemanticIcon
        get() = this + "file image"

    inline val file_image_outline: SemanticIcon
        get() = this + "file image outline"

    inline val file_import: SemanticIcon
        get() = this + "file import"

    inline val file_invoice: SemanticIcon
        get() = this + "file invoice"

    inline val file_invoice_dollar: SemanticIcon
        get() = this + "file invoice dollar"

    inline val file_medical: SemanticIcon
        get() = this + "file medical"

    inline val file_medical_alternate: SemanticIcon
        get() = this + "file medical alternate"

    inline val file_outline: SemanticIcon
        get() = this + "file outline"

    inline val file_pdf: SemanticIcon
        get() = this + "file pdf"

    inline val file_pdf_outline: SemanticIcon
        get() = this + "file pdf outline"

    inline val file_powerpoint: SemanticIcon
        get() = this + "file powerpoint"

    inline val file_powerpoint_outline: SemanticIcon
        get() = this + "file powerpoint outline"

    inline val file_prescription: SemanticIcon
        get() = this + "file prescription"

    inline val file_signature: SemanticIcon
        get() = this + "file signature"

    inline val file_upload: SemanticIcon
        get() = this + "file upload"

    inline val file_video: SemanticIcon
        get() = this + "file video"

    inline val file_video_outline: SemanticIcon
        get() = this + "file video outline"

    inline val file_word: SemanticIcon
        get() = this + "file word"

    inline val file_word_outline: SemanticIcon
        get() = this + "file word outline"

    inline val fill: SemanticIcon
        get() = this + "fill"

    inline val fill_drip: SemanticIcon
        get() = this + "fill drip"

    inline val film: SemanticIcon
        get() = this + "film"

    inline val filter: SemanticIcon
        get() = this + "filter"

    inline val fingerprint: SemanticIcon
        get() = this + "fingerprint"

    inline val fire: SemanticIcon
        get() = this + "fire"

    inline val fire_alternate: SemanticIcon
        get() = this + "fire alternate"

    inline val fire_extinguisher: SemanticIcon
        get() = this + "fire extinguisher"

    inline val firefox: SemanticIcon
        get() = this + "firefox"

    inline val firefox_browser: SemanticIcon
        get() = this + "firefox browser"

    inline val first_aid: SemanticIcon
        get() = this + "first aid"

    inline val first_order: SemanticIcon
        get() = this + "first order"

    inline val first_order_alternate: SemanticIcon
        get() = this + "first order alternate"

    inline val firstdraft: SemanticIcon
        get() = this + "firstdraft"

    inline val fish: SemanticIcon
        get() = this + "fish"

    inline val fist_raised: SemanticIcon
        get() = this + "fist raised"

    inline val fitted_help: SemanticIcon
        get() = this + "fitted help"

    inline val fitted_small_linkify: SemanticIcon
        get() = this + "fitted small linkify"

    inline val flag: SemanticIcon
        get() = this + "flag"

    inline val flag_checkered: SemanticIcon
        get() = this + "flag checkered"

    inline val flag_outline: SemanticIcon
        get() = this + "flag outline"

    inline val flag_usa: SemanticIcon
        get() = this + "flag usa"

    inline val flask: SemanticIcon
        get() = this + "flask"

    inline val flickr: SemanticIcon
        get() = this + "flickr"

    inline val flipboard: SemanticIcon
        get() = this + "flipboard"

    inline val flushed: SemanticIcon
        get() = this + "flushed"

    inline val flushed_outline: SemanticIcon
        get() = this + "flushed outline"

    inline val fly: SemanticIcon
        get() = this + "fly"

    inline val folder: SemanticIcon
        get() = this + "folder"

    inline val folder_minus: SemanticIcon
        get() = this + "folder minus"

    inline val folder_open: SemanticIcon
        get() = this + "folder open"

    inline val folder_open_outline: SemanticIcon
        get() = this + "folder open outline"

    inline val folder_outline: SemanticIcon
        get() = this + "folder outline"

    inline val folder_plus: SemanticIcon
        get() = this + "folder plus"

    inline val font: SemanticIcon
        get() = this + "font"

    inline val font_awesome: SemanticIcon
        get() = this + "font awesome"

    inline val font_awesome_alternate: SemanticIcon
        get() = this + "font awesome alternate"

    inline val font_awesome_flag: SemanticIcon
        get() = this + "font awesome flag"

    inline val fonticons: SemanticIcon
        get() = this + "fonticons"

    inline val fonticons_fi: SemanticIcon
        get() = this + "fonticons fi"

    inline val football_ball: SemanticIcon
        get() = this + "football ball"

    inline val fort_awesome: SemanticIcon
        get() = this + "fort awesome"

    inline val fort_awesome_alternate: SemanticIcon
        get() = this + "fort awesome alternate"

    inline val forumbee: SemanticIcon
        get() = this + "forumbee"

    inline val forward: SemanticIcon
        get() = this + "forward"

    inline val foursquare: SemanticIcon
        get() = this + "foursquare"

    inline val free_code_camp: SemanticIcon
        get() = this + "free code camp"

    inline val freebsd: SemanticIcon
        get() = this + "freebsd"

    inline val frog: SemanticIcon
        get() = this + "frog"

    inline val frown: SemanticIcon
        get() = this + "frown"

    inline val frown_open: SemanticIcon
        get() = this + "frown open"

    inline val frown_open_outline: SemanticIcon
        get() = this + "frown open outline"

    inline val frown_outline: SemanticIcon
        get() = this + "frown outline"

    inline val fruitapple: SemanticIcon
        get() = this + "fruit-apple"

    inline val fulcrum: SemanticIcon
        get() = this + "fulcrum"

    inline val funnel_dollar: SemanticIcon
        get() = this + "funnel dollar"

    inline val futbol: SemanticIcon
        get() = this + "futbol"

    inline val futbol_outline: SemanticIcon
        get() = this + "futbol outline"

    inline val galactic_republic: SemanticIcon
        get() = this + "galactic republic"

    inline val galactic_senate: SemanticIcon
        get() = this + "galactic senate"

    inline val gamepad: SemanticIcon
        get() = this + "gamepad"

    inline val gas_pump: SemanticIcon
        get() = this + "gas pump"

    inline val gavel: SemanticIcon
        get() = this + "gavel"

    inline val gem: SemanticIcon
        get() = this + "gem"

    inline val gem_outline: SemanticIcon
        get() = this + "gem outline"

    inline val genderless: SemanticIcon
        get() = this + "genderless"

    inline val get_pocket: SemanticIcon
        get() = this + "get pocket"

    inline val gg: SemanticIcon
        get() = this + "gg"

    inline val gg_circle: SemanticIcon
        get() = this + "gg circle"

    inline val ghost: SemanticIcon
        get() = this + "ghost"

    inline val gift: SemanticIcon
        get() = this + "gift"

    inline val gifts: SemanticIcon
        get() = this + "gifts"

    inline val git: SemanticIcon
        get() = this + "git"

    inline val git_alternate: SemanticIcon
        get() = this + "git alternate"

    inline val git_square: SemanticIcon
        get() = this + "git square"

    inline val github: SemanticIcon
        get() = this + "github"

    inline val github_alternate: SemanticIcon
        get() = this + "github alternate"

    inline val github_square: SemanticIcon
        get() = this + "github square"

    inline val gitkraken: SemanticIcon
        get() = this + "gitkraken"

    inline val gitlab: SemanticIcon
        get() = this + "gitlab"

    inline val gitter: SemanticIcon
        get() = this + "gitter"

    inline val glass_cheers: SemanticIcon
        get() = this + "glass cheers"

    inline val glass_martini: SemanticIcon
        get() = this + "glass martini"

    inline val glass_martini_alternate: SemanticIcon
        get() = this + "glass martini alternate"

    inline val glass_whiskey: SemanticIcon
        get() = this + "glass whiskey"

    inline val glasses: SemanticIcon
        get() = this + "glasses"

    inline val glide: SemanticIcon
        get() = this + "glide"

    inline val glide_g: SemanticIcon
        get() = this + "glide g"

    inline val globe: SemanticIcon
        get() = this + "globe"

    inline val globe_africa: SemanticIcon
        get() = this + "globe africa"

    inline val globe_americas: SemanticIcon
        get() = this + "globe americas"

    inline val globe_asia: SemanticIcon
        get() = this + "globe asia"

    inline val globe_europe: SemanticIcon
        get() = this + "globe europe"

    inline val gofore: SemanticIcon
        get() = this + "gofore"

    inline val golf_ball: SemanticIcon
        get() = this + "golf ball"

    inline val goodreads: SemanticIcon
        get() = this + "goodreads"

    inline val goodreads_g: SemanticIcon
        get() = this + "goodreads g"

    inline val google: SemanticIcon
        get() = this + "google"

    inline val google_drive: SemanticIcon
        get() = this + "google drive"

    inline val google_play: SemanticIcon
        get() = this + "google play"

    inline val google_plus: SemanticIcon
        get() = this + "google plus"

    inline val google_plus_g: SemanticIcon
        get() = this + "google plus g"

    inline val google_plus_square: SemanticIcon
        get() = this + "google plus square"

    inline val google_wallet: SemanticIcon
        get() = this + "google wallet"

    inline val gopuram: SemanticIcon
        get() = this + "gopuram"

    inline val graduation_cap: SemanticIcon
        get() = this + "graduation cap"

    inline val gratipay: SemanticIcon
        get() = this + "gratipay"

    inline val grav: SemanticIcon
        get() = this + "grav"

    inline val greater_than: SemanticIcon
        get() = this + "greater than"

    inline val greater_than_equal: SemanticIcon
        get() = this + "greater than equal"

    inline val green_users: SemanticIcon
        get() = this + "green users"

    inline val grey_users: SemanticIcon
        get() = this + "grey users"

    inline val grimace: SemanticIcon
        get() = this + "grimace"

    inline val grimace_outline: SemanticIcon
        get() = this + "grimace outline"

    inline val grin: SemanticIcon
        get() = this + "grin"

    inline val grin_alternate: SemanticIcon
        get() = this + "grin alternate"

    inline val grin_alternate_outline: SemanticIcon
        get() = this + "grin alternate outline"

    inline val grin_beam: SemanticIcon
        get() = this + "grin beam"

    inline val grin_beam_outline: SemanticIcon
        get() = this + "grin beam outline"

    inline val grin_beam_sweat: SemanticIcon
        get() = this + "grin beam sweat"

    inline val grin_beam_sweat_outline: SemanticIcon
        get() = this + "grin beam sweat outline"

    inline val grin_hearts: SemanticIcon
        get() = this + "grin hearts"

    inline val grin_hearts_outline: SemanticIcon
        get() = this + "grin hearts outline"

    inline val grin_outline: SemanticIcon
        get() = this + "grin outline"

    inline val grin_squint: SemanticIcon
        get() = this + "grin squint"

    inline val grin_squint_outline: SemanticIcon
        get() = this + "grin squint outline"

    inline val grin_squint_tears: SemanticIcon
        get() = this + "grin squint tears"

    inline val grin_squint_tears_outline: SemanticIcon
        get() = this + "grin squint tears outline"

    inline val grin_stars: SemanticIcon
        get() = this + "grin stars"

    inline val grin_stars_outline: SemanticIcon
        get() = this + "grin stars outline"

    inline val grin_tears: SemanticIcon
        get() = this + "grin tears"

    inline val grin_tears_outline: SemanticIcon
        get() = this + "grin tears outline"

    inline val grin_tongue: SemanticIcon
        get() = this + "grin tongue"

    inline val grin_tongue_outline: SemanticIcon
        get() = this + "grin tongue outline"

    inline val grin_tongue_squint: SemanticIcon
        get() = this + "grin tongue squint"

    inline val grin_tongue_squint_outline: SemanticIcon
        get() = this + "grin tongue squint outline"

    inline val grin_tongue_wink: SemanticIcon
        get() = this + "grin tongue wink"

    inline val grin_tongue_wink_outline: SemanticIcon
        get() = this + "grin tongue wink outline"

    inline val grin_wink: SemanticIcon
        get() = this + "grin wink"

    inline val grin_wink_outline: SemanticIcon
        get() = this + "grin wink outline"

    inline val grip_horizontal: SemanticIcon
        get() = this + "grip horizontal"

    inline val grip_lines: SemanticIcon
        get() = this + "grip lines"

    inline val grip_lines_vertical: SemanticIcon
        get() = this + "grip lines vertical"

    inline val grip_vertical: SemanticIcon
        get() = this + "grip vertical"

    inline val gripfire: SemanticIcon
        get() = this + "gripfire"

    inline val grunt: SemanticIcon
        get() = this + "grunt"

    inline val guitar: SemanticIcon
        get() = this + "guitar"

    inline val gulp: SemanticIcon
        get() = this + "gulp"

    inline val h_square: SemanticIcon
        get() = this + "h square"

    inline val hacker_news: SemanticIcon
        get() = this + "hacker news"

    inline val hacker_news_square: SemanticIcon
        get() = this + "hacker news square"

    inline val hackerrank: SemanticIcon
        get() = this + "hackerrank"

    inline val hamburger: SemanticIcon
        get() = this + "hamburger"

    inline val hammer: SemanticIcon
        get() = this + "hammer"

    inline val hamsa: SemanticIcon
        get() = this + "hamsa"

    inline val hand_holding: SemanticIcon
        get() = this + "hand holding"

    inline val hand_holding_heart: SemanticIcon
        get() = this + "hand holding heart"

    inline val hand_holding_medical: SemanticIcon
        get() = this + "hand holding medical"

    inline val hand_holding_usd: SemanticIcon
        get() = this + "hand holding usd"

    inline val hand_holding_water: SemanticIcon
        get() = this + "hand holding water"

    inline val hand_lizard: SemanticIcon
        get() = this + "hand lizard"

    inline val hand_lizard_outline: SemanticIcon
        get() = this + "hand lizard outline"

    inline val hand_middle_finger: SemanticIcon
        get() = this + "hand middle finger"

    inline val hand_paper: SemanticIcon
        get() = this + "hand paper"

    inline val hand_paper_outline: SemanticIcon
        get() = this + "hand paper outline"

    inline val hand_peace: SemanticIcon
        get() = this + "hand peace"

    inline val hand_peace_outline: SemanticIcon
        get() = this + "hand peace outline"

    inline val hand_point_down: SemanticIcon
        get() = this + "hand point down"

    inline val hand_point_down_outline: SemanticIcon
        get() = this + "hand point down outline"

    inline val hand_point_left: SemanticIcon
        get() = this + "hand point left"

    inline val hand_point_left_outline: SemanticIcon
        get() = this + "hand point left outline"

    inline val hand_point_right: SemanticIcon
        get() = this + "hand point right"

    inline val hand_point_right_outline: SemanticIcon
        get() = this + "hand point right outline"

    inline val hand_point_up: SemanticIcon
        get() = this + "hand point up"

    inline val hand_point_up_outline: SemanticIcon
        get() = this + "hand point up outline"

    inline val hand_pointer: SemanticIcon
        get() = this + "hand pointer"

    inline val hand_pointer_outline: SemanticIcon
        get() = this + "hand pointer outline"

    inline val hand_rock: SemanticIcon
        get() = this + "hand rock"

    inline val hand_rock_outline: SemanticIcon
        get() = this + "hand rock outline"

    inline val hand_scissors: SemanticIcon
        get() = this + "hand scissors"

    inline val hand_scissors_outline: SemanticIcon
        get() = this + "hand scissors outline"

    inline val hand_sparkles: SemanticIcon
        get() = this + "hand sparkles"

    inline val hand_spock: SemanticIcon
        get() = this + "hand spock"

    inline val hand_spock_outline: SemanticIcon
        get() = this + "hand spock outline"

    inline val hands: SemanticIcon
        get() = this + "hands"

    inline val hands_helping: SemanticIcon
        get() = this + "hands helping"

    inline val hands_wash: SemanticIcon
        get() = this + "hands wash"

    inline val handshake: SemanticIcon
        get() = this + "handshake"

    inline val handshake_alternate_slash: SemanticIcon
        get() = this + "handshake alternate slash"

    inline val handshake_outline: SemanticIcon
        get() = this + "handshake outline"

    inline val handshake_slash: SemanticIcon
        get() = this + "handshake slash"

    inline val hanukiah: SemanticIcon
        get() = this + "hanukiah"

    inline val hard_hat: SemanticIcon
        get() = this + "hard hat"

    inline val hashtag: SemanticIcon
        get() = this + "hashtag"

    inline val hat_cowboy: SemanticIcon
        get() = this + "hat cowboy"

    inline val hat_cowboy_side: SemanticIcon
        get() = this + "hat cowboy side"

    inline val hat_wizard: SemanticIcon
        get() = this + "hat wizard"

    inline val hdd: SemanticIcon
        get() = this + "hdd"

    inline val hdd_outline: SemanticIcon
        get() = this + "hdd outline"

    inline val head_side_cough: SemanticIcon
        get() = this + "head side cough"

    inline val head_side_cough_slash: SemanticIcon
        get() = this + "head side cough slash"

    inline val head_side_mask: SemanticIcon
        get() = this + "head side mask"

    inline val head_side_virus: SemanticIcon
        get() = this + "head side virus"

    inline val heading: SemanticIcon
        get() = this + "heading"

    inline val headphones: SemanticIcon
        get() = this + "headphones"

    inline val headphones_alternate: SemanticIcon
        get() = this + "headphones alternate"

    inline val headset: SemanticIcon
        get() = this + "headset"

    inline val heart: SemanticIcon
        get() = this + "heart"

    inline val heart_broken: SemanticIcon
        get() = this + "heart broken"

    inline val heart_outline: SemanticIcon
        get() = this + "heart outline"

    inline val heartbeat: SemanticIcon
        get() = this + "heartbeat"

    inline val helicopter: SemanticIcon
        get() = this + "helicopter"

    inline val help_link: SemanticIcon
        get() = this + "help link"

    inline val highlighter: SemanticIcon
        get() = this + "highlighter"

    inline val hiking: SemanticIcon
        get() = this + "hiking"

    inline val hippo: SemanticIcon
        get() = this + "hippo"

    inline val hips: SemanticIcon
        get() = this + "hips"

    inline val hire_a_helper: SemanticIcon
        get() = this + "hire a helper"

    inline val history: SemanticIcon
        get() = this + "history"

    inline val hockey_puck: SemanticIcon
        get() = this + "hockey puck"

    inline val holly_berry: SemanticIcon
        get() = this + "holly berry"

    inline val home: SemanticIcon
        get() = this + "home"

    inline val hooli: SemanticIcon
        get() = this + "hooli"

    inline val horizontally_flipped_cloud: SemanticIcon
        get() = this + "horizontally flipped cloud"

    inline val hornbill: SemanticIcon
        get() = this + "hornbill"

    inline val horse: SemanticIcon
        get() = this + "horse"

    inline val horse_head: SemanticIcon
        get() = this + "horse head"

    inline val hospital: SemanticIcon
        get() = this + "hospital"

    inline val hospital_alternate: SemanticIcon
        get() = this + "hospital alternate"

    inline val hospital_outline: SemanticIcon
        get() = this + "hospital outline"

    inline val hospital_symbol: SemanticIcon
        get() = this + "hospital symbol"

    inline val hospital_user: SemanticIcon
        get() = this + "hospital user"

    inline val hot_tub: SemanticIcon
        get() = this + "hot tub"

    inline val hotdog: SemanticIcon
        get() = this + "hotdog"

    inline val hotel: SemanticIcon
        get() = this + "hotel"

    inline val hotjar: SemanticIcon
        get() = this + "hotjar"

    inline val hourglass: SemanticIcon
        get() = this + "hourglass"

    inline val hourglass_end: SemanticIcon
        get() = this + "hourglass end"

    inline val hourglass_half: SemanticIcon
        get() = this + "hourglass half"

    inline val hourglass_outline: SemanticIcon
        get() = this + "hourglass outline"

    inline val hourglass_start: SemanticIcon
        get() = this + "hourglass start"

    inline val house_damage: SemanticIcon
        get() = this + "house damage"

    inline val house_user: SemanticIcon
        get() = this + "house user"

    inline val houzz: SemanticIcon
        get() = this + "houzz"

    inline val hryvnia: SemanticIcon
        get() = this + "hryvnia"

    inline val html5: SemanticIcon
        get() = this + "html5"

    inline val hubspot: SemanticIcon
        get() = this + "hubspot"

    inline val huge_home: SemanticIcon
        get() = this + "huge home"

    inline val i_cursor: SemanticIcon
        get() = this + "i cursor"

    inline val ice_cream: SemanticIcon
        get() = this + "ice cream"

    inline val icicles: SemanticIcon
        get() = this + "icicles"

    inline val icons: SemanticIcon
        get() = this + "icons"

    inline val id_badge: SemanticIcon
        get() = this + "id badge"

    inline val id_badge_outline: SemanticIcon
        get() = this + "id badge outline"

    inline val id_card: SemanticIcon
        get() = this + "id card"

    inline val id_card_alternate: SemanticIcon
        get() = this + "id card alternate"

    inline val id_card_outline: SemanticIcon
        get() = this + "id card outline"

    inline val ideal: SemanticIcon
        get() = this + "ideal"

    inline val igloo: SemanticIcon
        get() = this + "igloo"

    inline val image: SemanticIcon
        get() = this + "image"

    inline val image_outline: SemanticIcon
        get() = this + "image outline"

    inline val images: SemanticIcon
        get() = this + "images"

    inline val images_outline: SemanticIcon
        get() = this + "images outline"

    inline val imdb: SemanticIcon
        get() = this + "imdb"

    inline val inbox: SemanticIcon
        get() = this + "inbox"

    inline val indent: SemanticIcon
        get() = this + "indent"

    inline val industry: SemanticIcon
        get() = this + "industry"

    inline val infinity: SemanticIcon
        get() = this + "infinity"

    inline val info: SemanticIcon
        get() = this + "info"

    inline val info_circle: SemanticIcon
        get() = this + "info circle"

    inline val instagram: SemanticIcon
        get() = this + "instagram"

    inline val instagram_square: SemanticIcon
        get() = this + "instagram square"

    inline val intercom: SemanticIcon
        get() = this + "intercom"

    inline val internet_explorer: SemanticIcon
        get() = this + "internet explorer"

    inline val inverted_blue_users: SemanticIcon
        get() = this + "inverted blue users"

    inline val inverted_brown_users: SemanticIcon
        get() = this + "inverted brown users"

    inline val inverted_corner_add: SemanticIcon
        get() = this + "inverted corner add"

    inline val inverted_green_users: SemanticIcon
        get() = this + "inverted green users"

    inline val inverted_grey_users: SemanticIcon
        get() = this + "inverted grey users"

    inline val inverted_olive_users: SemanticIcon
        get() = this + "inverted olive users"

    inline val inverted_orange_users: SemanticIcon
        get() = this + "inverted orange users"

    inline val inverted_pink_users: SemanticIcon
        get() = this + "inverted pink users"

    inline val inverted_primary_users: SemanticIcon
        get() = this + "inverted primary users"

    inline val inverted_purple_users: SemanticIcon
        get() = this + "inverted purple users"

    inline val inverted_red_users: SemanticIcon
        get() = this + "inverted red users"

    inline val inverted_secondary_users: SemanticIcon
        get() = this + "inverted secondary users"

    inline val inverted_teal_users: SemanticIcon
        get() = this + "inverted teal users"

    inline val inverted_users: SemanticIcon
        get() = this + "inverted users"

    inline val inverted_violet_users: SemanticIcon
        get() = this + "inverted violet users"

    inline val inverted_yellow_users: SemanticIcon
        get() = this + "inverted yellow users"

    inline val invision: SemanticIcon
        get() = this + "invision"

    inline val ioxhost: SemanticIcon
        get() = this + "ioxhost"

    inline val italic: SemanticIcon
        get() = this + "italic"

    inline val itch_io: SemanticIcon
        get() = this + "itch io"

    inline val itunes: SemanticIcon
        get() = this + "itunes"

    inline val itunes_note: SemanticIcon
        get() = this + "itunes note"

    inline val java: SemanticIcon
        get() = this + "java"

    inline val jedi: SemanticIcon
        get() = this + "jedi"

    inline val jedi_order: SemanticIcon
        get() = this + "jedi order"

    inline val jenkins: SemanticIcon
        get() = this + "jenkins"

    inline val jira: SemanticIcon
        get() = this + "jira"

    inline val joget: SemanticIcon
        get() = this + "joget"

    inline val joint: SemanticIcon
        get() = this + "joint"

    inline val joomla: SemanticIcon
        get() = this + "joomla"

    inline val journal_whills: SemanticIcon
        get() = this + "journal whills"

    inline val js: SemanticIcon
        get() = this + "js"

    inline val js_square: SemanticIcon
        get() = this + "js square"

    inline val jsfiddle: SemanticIcon
        get() = this + "jsfiddle"

    inline val kaaba: SemanticIcon
        get() = this + "kaaba"

    inline val kaggle: SemanticIcon
        get() = this + "kaggle"

    inline val key: SemanticIcon
        get() = this + "key"

    inline val keybase: SemanticIcon
        get() = this + "keybase"

    inline val keyboard: SemanticIcon
        get() = this + "keyboard"

    inline val keyboard_outline: SemanticIcon
        get() = this + "keyboard outline"

    inline val keycdn: SemanticIcon
        get() = this + "keycdn"

    inline val khanda: SemanticIcon
        get() = this + "khanda"

    inline val kickstarter: SemanticIcon
        get() = this + "kickstarter"

    inline val kickstarter_k: SemanticIcon
        get() = this + "kickstarter k"

    inline val kiss: SemanticIcon
        get() = this + "kiss"

    inline val kiss_beam: SemanticIcon
        get() = this + "kiss beam"

    inline val kiss_beam_outline: SemanticIcon
        get() = this + "kiss beam outline"

    inline val kiss_outline: SemanticIcon
        get() = this + "kiss outline"

    inline val kiss_wink_heart: SemanticIcon
        get() = this + "kiss wink heart"

    inline val kiss_wink_heart_outline: SemanticIcon
        get() = this + "kiss wink heart outline"

    inline val kiwi_bird: SemanticIcon
        get() = this + "kiwi bird"

    inline val korvue: SemanticIcon
        get() = this + "korvue"

    inline val landmark: SemanticIcon
        get() = this + "landmark"

    inline val language: SemanticIcon
        get() = this + "language"

    inline val laptop: SemanticIcon
        get() = this + "laptop"

    inline val laptop_code: SemanticIcon
        get() = this + "laptop code"

    inline val laptop_house: SemanticIcon
        get() = this + "laptop house"

    inline val laptop_medical: SemanticIcon
        get() = this + "laptop medical"

    inline val laravel: SemanticIcon
        get() = this + "laravel"

    inline val large_home: SemanticIcon
        get() = this + "large home"

    inline val lastfm: SemanticIcon
        get() = this + "lastfm"

    inline val lastfm_square: SemanticIcon
        get() = this + "lastfm square"

    inline val laugh: SemanticIcon
        get() = this + "laugh"

    inline val laugh_beam: SemanticIcon
        get() = this + "laugh beam"

    inline val laugh_beam_outline: SemanticIcon
        get() = this + "laugh beam outline"

    inline val laugh_outline: SemanticIcon
        get() = this + "laugh outline"

    inline val laugh_squint: SemanticIcon
        get() = this + "laugh squint"

    inline val laugh_squint_outline: SemanticIcon
        get() = this + "laugh squint outline"

    inline val laugh_wink: SemanticIcon
        get() = this + "laugh wink"

    inline val laugh_wink_outline: SemanticIcon
        get() = this + "laugh wink outline"

    inline val layer_group: SemanticIcon
        get() = this + "layer group"

    inline val leaf: SemanticIcon
        get() = this + "leaf"

    inline val leanpub: SemanticIcon
        get() = this + "leanpub"

    inline val lemon: SemanticIcon
        get() = this + "lemon"

    inline val lemon_outline: SemanticIcon
        get() = this + "lemon outline"

    inline val less_than: SemanticIcon
        get() = this + "less than"

    inline val less_than_equal: SemanticIcon
        get() = this + "less than equal"

    inline val lesscss: SemanticIcon
        get() = this + "lesscss"

    inline val level_down_alternate: SemanticIcon
        get() = this + "level down alternate"

    inline val level_up_alternate: SemanticIcon
        get() = this + "level up alternate"

    inline val life_ring: SemanticIcon
        get() = this + "life ring"

    inline val life_ring_outline: SemanticIcon
        get() = this + "life ring outline"

    inline val lightbulb: SemanticIcon
        get() = this + "lightbulb"

    inline val lightbulb_outline: SemanticIcon
        get() = this + "lightbulb outline"

    inline val linechat: SemanticIcon
        get() = this + "linechat"

    inline val linkedin: SemanticIcon
        get() = this + "linkedin"

    inline val linkedin_in: SemanticIcon
        get() = this + "linkedin in"

    inline val linkify: SemanticIcon
        get() = this + "linkify"

    inline val linode: SemanticIcon
        get() = this + "linode"

    inline val linux: SemanticIcon
        get() = this + "linux"

    inline val lira_sign: SemanticIcon
        get() = this + "lira sign"

    inline val list: SemanticIcon
        get() = this + "list"

    inline val list_alternate: SemanticIcon
        get() = this + "list alternate"

    inline val list_alternate_outline: SemanticIcon
        get() = this + "list alternate outline"

    inline val list_ol: SemanticIcon
        get() = this + "list ol"

    inline val list_ul: SemanticIcon
        get() = this + "list ul"

    inline val location_arrow: SemanticIcon
        get() = this + "location arrow"

    inline val lock: SemanticIcon
        get() = this + "lock"

    inline val lock_open: SemanticIcon
        get() = this + "lock open"

    inline val long_arrow_alternate_down: SemanticIcon
        get() = this + "long arrow alternate down"

    inline val long_arrow_alternate_left: SemanticIcon
        get() = this + "long arrow alternate left"

    inline val long_arrow_alternate_right: SemanticIcon
        get() = this + "long arrow alternate right"

    inline val long_arrow_alternate_up: SemanticIcon
        get() = this + "long arrow alternate up"

    inline val low_vision: SemanticIcon
        get() = this + "low vision"

    inline val luggage_cart: SemanticIcon
        get() = this + "luggage cart"

    inline val lungs: SemanticIcon
        get() = this + "lungs"

    inline val lungs_virus: SemanticIcon
        get() = this + "lungs virus"

    inline val lyft: SemanticIcon
        get() = this + "lyft"

    inline val magento: SemanticIcon
        get() = this + "magento"

    inline val magic: SemanticIcon
        get() = this + "magic"

    inline val magnet: SemanticIcon
        get() = this + "magnet"

    inline val mail: SemanticIcon
        get() = this + "mail"

    inline val mail_bulk: SemanticIcon
        get() = this + "mail bulk"

    inline val mailchimp: SemanticIcon
        get() = this + "mailchimp"

    inline val male: SemanticIcon
        get() = this + "male"

    inline val mandalorian: SemanticIcon
        get() = this + "mandalorian"

    inline val map: SemanticIcon
        get() = this + "map"

    inline val map_marked: SemanticIcon
        get() = this + "map marked"

    inline val map_marked_alternate: SemanticIcon
        get() = this + "map marked alternate"

    inline val map_marker: SemanticIcon
        get() = this + "map marker"

    inline val map_marker_alternate: SemanticIcon
        get() = this + "map marker alternate"

    inline val map_outline: SemanticIcon
        get() = this + "map outline"

    inline val map_pin: SemanticIcon
        get() = this + "map pin"

    inline val map_signs: SemanticIcon
        get() = this + "map signs"

    inline val markdown: SemanticIcon
        get() = this + "markdown"

    inline val marker: SemanticIcon
        get() = this + "marker"

    inline val mars: SemanticIcon
        get() = this + "mars"

    inline val mars_double: SemanticIcon
        get() = this + "mars double"

    inline val mars_stroke: SemanticIcon
        get() = this + "mars stroke"

    inline val mars_stroke_horizontal: SemanticIcon
        get() = this + "mars stroke horizontal"

    inline val mars_stroke_vertical: SemanticIcon
        get() = this + "mars stroke vertical"

    inline val mask: SemanticIcon
        get() = this + "mask"

    inline val massive_home: SemanticIcon
        get() = this + "massive home"

    inline val mastodon: SemanticIcon
        get() = this + "mastodon"

    inline val maxcdn: SemanticIcon
        get() = this + "maxcdn"

    inline val mdb: SemanticIcon
        get() = this + "mdb"

    inline val medal: SemanticIcon
        get() = this + "medal"

    inline val medapps: SemanticIcon
        get() = this + "medapps"

    inline val medium_: SemanticIcon
        get() = this + "medium"

    inline val medium_m: SemanticIcon
        get() = this + "medium m"

    inline val medkit: SemanticIcon
        get() = this + "medkit"

    inline val medrt: SemanticIcon
        get() = this + "medrt"

    inline val meetup: SemanticIcon
        get() = this + "meetup"

    inline val megaport: SemanticIcon
        get() = this + "megaport"

    inline val meh: SemanticIcon
        get() = this + "meh"

    inline val meh_blank: SemanticIcon
        get() = this + "meh blank"

    inline val meh_blank_outline: SemanticIcon
        get() = this + "meh blank outline"

    inline val meh_outline: SemanticIcon
        get() = this + "meh outline"

    inline val meh_rolling_eyes: SemanticIcon
        get() = this + "meh rolling eyes"

    inline val meh_rolling_eyes_outline: SemanticIcon
        get() = this + "meh rolling eyes outline"

    inline val memory: SemanticIcon
        get() = this + "memory"

    inline val mendeley: SemanticIcon
        get() = this + "mendeley"

    inline val menorah: SemanticIcon
        get() = this + "menorah"

    inline val mercury: SemanticIcon
        get() = this + "mercury"

    inline val meteor: SemanticIcon
        get() = this + "meteor"

    inline val microblog: SemanticIcon
        get() = this + "microblog"

    inline val microchip: SemanticIcon
        get() = this + "microchip"

    inline val microphone: SemanticIcon
        get() = this + "microphone"

    inline val microphone_alternate: SemanticIcon
        get() = this + "microphone alternate"

    inline val microphone_alternate_slash: SemanticIcon
        get() = this + "microphone alternate slash"

    inline val microphone_slash: SemanticIcon
        get() = this + "microphone slash"

    inline val microscope: SemanticIcon
        get() = this + "microscope"

    inline val microsoft: SemanticIcon
        get() = this + "microsoft"

    inline val mini_home: SemanticIcon
        get() = this + "mini home"

    inline val minus: SemanticIcon
        get() = this + "minus"

    inline val minus_circle: SemanticIcon
        get() = this + "minus circle"

    inline val minus_square: SemanticIcon
        get() = this + "minus square"

    inline val minus_square_outline: SemanticIcon
        get() = this + "minus square outline"

    inline val mitten: SemanticIcon
        get() = this + "mitten"

    inline val mix: SemanticIcon
        get() = this + "mix"

    inline val mixcloud: SemanticIcon
        get() = this + "mixcloud"

    inline val mixer: SemanticIcon
        get() = this + "mixer"

    inline val mizuni: SemanticIcon
        get() = this + "mizuni"

    inline val mobile: SemanticIcon
        get() = this + "mobile"

    inline val mobile_alternate: SemanticIcon
        get() = this + "mobile alternate"

    inline val modx: SemanticIcon
        get() = this + "modx"

    inline val monero: SemanticIcon
        get() = this + "monero"

    inline val money_bill: SemanticIcon
        get() = this + "money bill"

    inline val money_bill_alternate: SemanticIcon
        get() = this + "money bill alternate"

    inline val money_bill_alternate_outline: SemanticIcon
        get() = this + "money bill alternate outline"

    inline val money_bill_wave: SemanticIcon
        get() = this + "money bill wave"

    inline val money_bill_wave_alternate: SemanticIcon
        get() = this + "money bill wave alternate"

    inline val money_check: SemanticIcon
        get() = this + "money check"

    inline val money_check_alternate: SemanticIcon
        get() = this + "money check alternate"

    inline val monument: SemanticIcon
        get() = this + "monument"

    inline val moon: SemanticIcon
        get() = this + "moon"

    inline val moon_outline: SemanticIcon
        get() = this + "moon outline"

    inline val mortar_pestle: SemanticIcon
        get() = this + "mortar pestle"

    inline val mosque: SemanticIcon
        get() = this + "mosque"

    inline val motorcycle: SemanticIcon
        get() = this + "motorcycle"

    inline val mountain: SemanticIcon
        get() = this + "mountain"

    inline val mouse: SemanticIcon
        get() = this + "mouse"

    inline val mouse_pointer: SemanticIcon
        get() = this + "mouse pointer"

    inline val mug_hot: SemanticIcon
        get() = this + "mug hot"

    inline val music: SemanticIcon
        get() = this + "music"

    inline val napster: SemanticIcon
        get() = this + "napster"

    inline val neos: SemanticIcon
        get() = this + "neos"

    inline val neuter: SemanticIcon
        get() = this + "neuter"

    inline val newspaper: SemanticIcon
        get() = this + "newspaper"

    inline val newspaper_outline: SemanticIcon
        get() = this + "newspaper outline"

    inline val nimblr: SemanticIcon
        get() = this + "nimblr"

    inline val node: SemanticIcon
        get() = this + "node"

    inline val node_js: SemanticIcon
        get() = this + "node js"

    inline val not_equal: SemanticIcon
        get() = this + "not equal"

    inline val notched_circle_loading: SemanticIcon
        get() = this + "notched circle loading"

    inline val notes_medical: SemanticIcon
        get() = this + "notes medical"

    inline val npm: SemanticIcon
        get() = this + "npm"

    inline val ns8: SemanticIcon
        get() = this + "ns8"

    inline val nutritionix: SemanticIcon
        get() = this + "nutritionix"

    inline val object_group: SemanticIcon
        get() = this + "object group"

    inline val object_group_outline: SemanticIcon
        get() = this + "object group outline"

    inline val object_ungroup: SemanticIcon
        get() = this + "object ungroup"

    inline val object_ungroup_outline: SemanticIcon
        get() = this + "object ungroup outline"

    inline val odnoklassniki: SemanticIcon
        get() = this + "odnoklassniki"

    inline val odnoklassniki_square: SemanticIcon
        get() = this + "odnoklassniki square"

    inline val oil_can: SemanticIcon
        get() = this + "oil can"

    inline val old_republic: SemanticIcon
        get() = this + "old republic"

    inline val olive_users: SemanticIcon
        get() = this + "olive users"

    inline val om: SemanticIcon
        get() = this + "om"

    inline val opencart: SemanticIcon
        get() = this + "opencart"

    inline val openid: SemanticIcon
        get() = this + "openid"

    inline val opera: SemanticIcon
        get() = this + "opera"

    inline val optin_monster: SemanticIcon
        get() = this + "optin monster"

    inline val orange_users: SemanticIcon
        get() = this + "orange users"

    inline val orcid: SemanticIcon
        get() = this + "orcid"

    inline val osi: SemanticIcon
        get() = this + "osi"

    inline val otter: SemanticIcon
        get() = this + "otter"

    inline val outdent: SemanticIcon
        get() = this + "outdent"

    inline val page4: SemanticIcon
        get() = this + "page4"

    inline val pagelines: SemanticIcon
        get() = this + "pagelines"

    inline val pager: SemanticIcon
        get() = this + "pager"

    inline val paint_brush: SemanticIcon
        get() = this + "paint brush"

    inline val paint_roller: SemanticIcon
        get() = this + "paint roller"

    inline val palette: SemanticIcon
        get() = this + "palette"

    inline val palfed: SemanticIcon
        get() = this + "palfed"

    inline val pallet: SemanticIcon
        get() = this + "pallet"

    inline val paper_plane: SemanticIcon
        get() = this + "paper plane"

    inline val paper_plane_outline: SemanticIcon
        get() = this + "paper plane outline"

    inline val paperclip: SemanticIcon
        get() = this + "paperclip"

    inline val parachute_box: SemanticIcon
        get() = this + "parachute box"

    inline val paragraph: SemanticIcon
        get() = this + "paragraph"

    inline val parking: SemanticIcon
        get() = this + "parking"

    inline val passport: SemanticIcon
        get() = this + "passport"

    inline val pastafarianism: SemanticIcon
        get() = this + "pastafarianism"

    inline val paste: SemanticIcon
        get() = this + "paste"

    inline val patreon: SemanticIcon
        get() = this + "patreon"

    inline val pause: SemanticIcon
        get() = this + "pause"

    inline val pause_circle: SemanticIcon
        get() = this + "pause circle"

    inline val pause_circle_outline: SemanticIcon
        get() = this + "pause circle outline"

    inline val paw: SemanticIcon
        get() = this + "paw"

    inline val paypal: SemanticIcon
        get() = this + "paypal"

    inline val peace: SemanticIcon
        get() = this + "peace"

    inline val pen: SemanticIcon
        get() = this + "pen"

    inline val pen_alternate: SemanticIcon
        get() = this + "pen alternate"

    inline val pen_fancy: SemanticIcon
        get() = this + "pen fancy"

    inline val pen_nib: SemanticIcon
        get() = this + "pen nib"

    inline val pen_square: SemanticIcon
        get() = this + "pen square"

    inline val pencil_alternate: SemanticIcon
        get() = this + "pencil alternate"

    inline val pencil_ruler: SemanticIcon
        get() = this + "pencil ruler"

    inline val penny_arcade: SemanticIcon
        get() = this + "penny arcade"

    inline val people_arrows: SemanticIcon
        get() = this + "people arrows"

    inline val people_carry: SemanticIcon
        get() = this + "people carry"

    inline val pepper_hot: SemanticIcon
        get() = this + "pepper hot"

    inline val percent: SemanticIcon
        get() = this + "percent"

    inline val percentage: SemanticIcon
        get() = this + "percentage"

    inline val periscope: SemanticIcon
        get() = this + "periscope"

    inline val person_booth: SemanticIcon
        get() = this + "person booth"

    inline val phabricator: SemanticIcon
        get() = this + "phabricator"

    inline val phoenix_framework: SemanticIcon
        get() = this + "phoenix framework"

    inline val phoenix_squadron: SemanticIcon
        get() = this + "phoenix squadron"

    inline val phone: SemanticIcon
        get() = this + "phone"

    inline val phone_alternate: SemanticIcon
        get() = this + "phone alternate"

    inline val phone_slash: SemanticIcon
        get() = this + "phone slash"

    inline val phone_square: SemanticIcon
        get() = this + "phone square"

    inline val phone_square_alternate: SemanticIcon
        get() = this + "phone square alternate"

    inline val phone_volume: SemanticIcon
        get() = this + "phone volume"

    inline val photo_video: SemanticIcon
        get() = this + "photo video"

    inline val php: SemanticIcon
        get() = this + "php"

    inline val pied_piper: SemanticIcon
        get() = this + "pied piper"

    inline val pied_piper_alternate: SemanticIcon
        get() = this + "pied piper alternate"

    inline val pied_piper_hat: SemanticIcon
        get() = this + "pied piper hat"

    inline val pied_piper_pp: SemanticIcon
        get() = this + "pied piper pp"

    inline val pied_piper_square: SemanticIcon
        get() = this + "pied piper square"

    inline val piggy_bank: SemanticIcon
        get() = this + "piggy bank"

    inline val pills: SemanticIcon
        get() = this + "pills"

    inline val pink_users: SemanticIcon
        get() = this + "pink users"

    inline val pinterest: SemanticIcon
        get() = this + "pinterest"

    inline val pinterest_p: SemanticIcon
        get() = this + "pinterest p"

    inline val pinterest_square: SemanticIcon
        get() = this + "pinterest square"

    inline val pizza_slice: SemanticIcon
        get() = this + "pizza slice"

    inline val place_of_worship: SemanticIcon
        get() = this + "place of worship"

    inline val plane: SemanticIcon
        get() = this + "plane"

    inline val plane_arrival: SemanticIcon
        get() = this + "plane arrival"

    inline val plane_departure: SemanticIcon
        get() = this + "plane departure"

    inline val play: SemanticIcon
        get() = this + "play"

    inline val play_circle: SemanticIcon
        get() = this + "play circle"

    inline val play_circle_outline: SemanticIcon
        get() = this + "play circle outline"

    inline val playstation: SemanticIcon
        get() = this + "playstation"

    inline val plug: SemanticIcon
        get() = this + "plug"

    inline val plus: SemanticIcon
        get() = this + "plus"

    inline val plus_circle: SemanticIcon
        get() = this + "plus circle"

    inline val plus_square: SemanticIcon
        get() = this + "plus square"

    inline val plus_square_outline: SemanticIcon
        get() = this + "plus square outline"

    inline val podcast: SemanticIcon
        get() = this + "podcast"

    inline val poll: SemanticIcon
        get() = this + "poll"

    inline val poll_horizontal: SemanticIcon
        get() = this + "poll horizontal"

    inline val poo: SemanticIcon
        get() = this + "poo"

    inline val poo_storm: SemanticIcon
        get() = this + "poo storm"

    inline val poop: SemanticIcon
        get() = this + "poop"

    inline val portrait: SemanticIcon
        get() = this + "portrait"

    inline val pound_sign: SemanticIcon
        get() = this + "pound sign"

    inline val power_off: SemanticIcon
        get() = this + "power off"

    inline val pray: SemanticIcon
        get() = this + "pray"

    inline val praying_hands: SemanticIcon
        get() = this + "praying hands"

    inline val prescription: SemanticIcon
        get() = this + "prescription"

    inline val prescription_bottle: SemanticIcon
        get() = this + "prescription bottle"

    inline val prescription_bottle_alternate: SemanticIcon
        get() = this + "prescription bottle alternate"

    inline val primary_users: SemanticIcon
        get() = this + "primary users"

    inline val print: SemanticIcon
        get() = this + "print"

    inline val procedures: SemanticIcon
        get() = this + "procedures"

    inline val product_hunt: SemanticIcon
        get() = this + "product hunt"

    inline val project_diagram: SemanticIcon
        get() = this + "project diagram"

    inline val pump_medical: SemanticIcon
        get() = this + "pump medical"

    inline val pump_soap: SemanticIcon
        get() = this + "pump soap"

    inline val purple_users: SemanticIcon
        get() = this + "purple users"

    inline val pushed: SemanticIcon
        get() = this + "pushed"

    inline val puzzle: SemanticIcon
        get() = this + "puzzle"

    inline val puzzle_piece: SemanticIcon
        get() = this + "puzzle piece"

    inline val python: SemanticIcon
        get() = this + "python"

    inline val qq: SemanticIcon
        get() = this + "qq"

    inline val qrcode: SemanticIcon
        get() = this + "qrcode"

    inline val question: SemanticIcon
        get() = this + "question"

    inline val question_circle: SemanticIcon
        get() = this + "question circle"

    inline val question_circle_outline: SemanticIcon
        get() = this + "question circle outline"

    inline val quidditch: SemanticIcon
        get() = this + "quidditch"

    inline val quinscape: SemanticIcon
        get() = this + "quinscape"

    inline val quora: SemanticIcon
        get() = this + "quora"

    inline val quote_left: SemanticIcon
        get() = this + "quote left"

    inline val quote_right: SemanticIcon
        get() = this + "quote right"

    inline val quran: SemanticIcon
        get() = this + "quran"

    inline val r_project: SemanticIcon
        get() = this + "r project"

    inline val radiation: SemanticIcon
        get() = this + "radiation"

    inline val radiation_alternate: SemanticIcon
        get() = this + "radiation alternate"

    inline val rainbow: SemanticIcon
        get() = this + "rainbow"

    inline val random: SemanticIcon
        get() = this + "random"

    inline val raspberry_pi: SemanticIcon
        get() = this + "raspberry pi"

    inline val ravelry: SemanticIcon
        get() = this + "ravelry"

    inline val react: SemanticIcon
        get() = this + "react"

    inline val reacteurope: SemanticIcon
        get() = this + "reacteurope"

    inline val readme: SemanticIcon
        get() = this + "readme"

    inline val rebel: SemanticIcon
        get() = this + "rebel"

    inline val receipt: SemanticIcon
        get() = this + "receipt"

    inline val record_vinyl: SemanticIcon
        get() = this + "record vinyl"

    inline val recycle: SemanticIcon
        get() = this + "recycle"

    inline val red_users: SemanticIcon
        get() = this + "red users"

    inline val reddit: SemanticIcon
        get() = this + "reddit"

    inline val reddit_alien: SemanticIcon
        get() = this + "reddit alien"

    inline val reddit_square: SemanticIcon
        get() = this + "reddit square"

    inline val redhat: SemanticIcon
        get() = this + "redhat"

    inline val redo: SemanticIcon
        get() = this + "redo"

    inline val redo_alternate: SemanticIcon
        get() = this + "redo alternate"

    inline val redriver: SemanticIcon
        get() = this + "redriver"

    inline val redyeti: SemanticIcon
        get() = this + "redyeti"

    inline val registered: SemanticIcon
        get() = this + "registered"

    inline val registered_outline: SemanticIcon
        get() = this + "registered outline"

    inline val remove_format: SemanticIcon
        get() = this + "remove format"

    inline val renren: SemanticIcon
        get() = this + "renren"

    inline val reply: SemanticIcon
        get() = this + "reply"

    inline val reply_all: SemanticIcon
        get() = this + "reply all"

    inline val replyd: SemanticIcon
        get() = this + "replyd"

    inline val republican: SemanticIcon
        get() = this + "republican"

    inline val researchgate: SemanticIcon
        get() = this + "researchgate"

    inline val resolving: SemanticIcon
        get() = this + "resolving"

    inline val restroom: SemanticIcon
        get() = this + "restroom"

    inline val retweet: SemanticIcon
        get() = this + "retweet"

    inline val rev: SemanticIcon
        get() = this + "rev"

    inline val ribbon: SemanticIcon
        get() = this + "ribbon"

    inline val ring: SemanticIcon
        get() = this + "ring"

    inline val road: SemanticIcon
        get() = this + "road"

    inline val robot: SemanticIcon
        get() = this + "robot"

    inline val rocket: SemanticIcon
        get() = this + "rocket"

    inline val rocketchat: SemanticIcon
        get() = this + "rocketchat"

    inline val rockrms: SemanticIcon
        get() = this + "rockrms"

    inline val route: SemanticIcon
        get() = this + "route"

    inline val rss: SemanticIcon
        get() = this + "rss"

    inline val rss_square: SemanticIcon
        get() = this + "rss square"

    inline val ruble_sign: SemanticIcon
        get() = this + "ruble sign"

    inline val ruler: SemanticIcon
        get() = this + "ruler"

    inline val ruler_combined: SemanticIcon
        get() = this + "ruler combined"

    inline val ruler_horizontal: SemanticIcon
        get() = this + "ruler horizontal"

    inline val ruler_vertical: SemanticIcon
        get() = this + "ruler vertical"

    inline val running: SemanticIcon
        get() = this + "running"

    inline val rupee_sign: SemanticIcon
        get() = this + "rupee sign"

    inline val sad_cry: SemanticIcon
        get() = this + "sad cry"

    inline val sad_cry_outline: SemanticIcon
        get() = this + "sad cry outline"

    inline val sad_tear: SemanticIcon
        get() = this + "sad tear"

    inline val sad_tear_outline: SemanticIcon
        get() = this + "sad tear outline"

    inline val safari: SemanticIcon
        get() = this + "safari"

    inline val salesforce: SemanticIcon
        get() = this + "salesforce"

    inline val sass: SemanticIcon
        get() = this + "sass"

    inline val satellite: SemanticIcon
        get() = this + "satellite"

    inline val satellite_dish: SemanticIcon
        get() = this + "satellite dish"

    inline val save: SemanticIcon
        get() = this + "save"

    inline val save_outline: SemanticIcon
        get() = this + "save outline"

    inline val schlix: SemanticIcon
        get() = this + "schlix"

    inline val school: SemanticIcon
        get() = this + "school"

    inline val screwdriver: SemanticIcon
        get() = this + "screwdriver"

    inline val scribd: SemanticIcon
        get() = this + "scribd"

    inline val scroll: SemanticIcon
        get() = this + "scroll"

    inline val sd_card: SemanticIcon
        get() = this + "sd card"

    inline val search: SemanticIcon
        get() = this + "search"

    inline val search_dollar: SemanticIcon
        get() = this + "search dollar"

    inline val search_location: SemanticIcon
        get() = this + "search location"

    inline val search_minus: SemanticIcon
        get() = this + "search minus"

    inline val search_plus: SemanticIcon
        get() = this + "search plus"

    inline val searchengin: SemanticIcon
        get() = this + "searchengin"

    inline val secondary_users: SemanticIcon
        get() = this + "secondary users"

    inline val seedling: SemanticIcon
        get() = this + "seedling"

    inline val sellcast: SemanticIcon
        get() = this + "sellcast"

    inline val sellsy: SemanticIcon
        get() = this + "sellsy"

    inline val server: SemanticIcon
        get() = this + "server"

    inline val servicestack: SemanticIcon
        get() = this + "servicestack"

    inline val shapes: SemanticIcon
        get() = this + "shapes"

    inline val share: SemanticIcon
        get() = this + "share"

    inline val share_alternate: SemanticIcon
        get() = this + "share alternate"

    inline val share_alternate_square: SemanticIcon
        get() = this + "share alternate square"

    inline val share_square: SemanticIcon
        get() = this + "share square"

    inline val share_square_outline: SemanticIcon
        get() = this + "share square outline"

    inline val shekel_sign: SemanticIcon
        get() = this + "shekel sign"

    inline val shield_alternate: SemanticIcon
        get() = this + "shield alternate"

    inline val shield_virus: SemanticIcon
        get() = this + "shield virus"

    inline val ship: SemanticIcon
        get() = this + "ship"

    inline val shipping_fast: SemanticIcon
        get() = this + "shipping fast"

    inline val shirtsinbulk: SemanticIcon
        get() = this + "shirtsinbulk"

    inline val shoe_prints: SemanticIcon
        get() = this + "shoe prints"

    inline val shopify: SemanticIcon
        get() = this + "shopify"

    inline val shopping_bag: SemanticIcon
        get() = this + "shopping bag"

    inline val shopping_basket: SemanticIcon
        get() = this + "shopping basket"

    inline val shopping_cart: SemanticIcon
        get() = this + "shopping cart"

    inline val shopware: SemanticIcon
        get() = this + "shopware"

    inline val shower: SemanticIcon
        get() = this + "shower"

    inline val shuttle_van: SemanticIcon
        get() = this + "shuttle van"

    inline val sign: SemanticIcon
        get() = this + "sign"

    inline val sign_in_alternate: SemanticIcon
        get() = this + "sign in alternate"

    inline val sign_language: SemanticIcon
        get() = this + "sign language"

    inline val sign_out_alternate: SemanticIcon
        get() = this + "sign out alternate"

    inline val signal: SemanticIcon
        get() = this + "signal"

    inline val sim_card: SemanticIcon
        get() = this + "sim card"

    inline val simplybuilt: SemanticIcon
        get() = this + "simplybuilt"

    inline val sistrix: SemanticIcon
        get() = this + "sistrix"

    inline val sitemap: SemanticIcon
        get() = this + "sitemap"

    inline val sith: SemanticIcon
        get() = this + "sith"

    inline val skating: SemanticIcon
        get() = this + "skating"

    inline val sketch: SemanticIcon
        get() = this + "sketch"

    inline val skiing: SemanticIcon
        get() = this + "skiing"

    inline val skiing_nordic: SemanticIcon
        get() = this + "skiing nordic"

    inline val skull_crossbones: SemanticIcon
        get() = this + "skull crossbones"

    inline val skyatlas: SemanticIcon
        get() = this + "skyatlas"

    inline val skype: SemanticIcon
        get() = this + "skype"

    inline val slack: SemanticIcon
        get() = this + "slack"

    inline val slack_hash: SemanticIcon
        get() = this + "slack hash"

    inline val slash: SemanticIcon
        get() = this + "slash"

    inline val sleigh: SemanticIcon
        get() = this + "sleigh"

    inline val sliders_horizontal: SemanticIcon
        get() = this + "sliders horizontal"

    inline val slideshare: SemanticIcon
        get() = this + "slideshare"

    inline val small_home: SemanticIcon
        get() = this + "small home"

    inline val smile: SemanticIcon
        get() = this + "smile"

    inline val smile_beam: SemanticIcon
        get() = this + "smile beam"

    inline val smile_beam_outline: SemanticIcon
        get() = this + "smile beam outline"

    inline val smile_outline: SemanticIcon
        get() = this + "smile outline"

    inline val smile_wink: SemanticIcon
        get() = this + "smile wink"

    inline val smile_wink_outline: SemanticIcon
        get() = this + "smile wink outline"

    inline val smog: SemanticIcon
        get() = this + "smog"

    inline val smoking: SemanticIcon
        get() = this + "smoking"

    inline val smoking_ban: SemanticIcon
        get() = this + "smoking ban"

    inline val sms: SemanticIcon
        get() = this + "sms"

    inline val snapchat: SemanticIcon
        get() = this + "snapchat"

    inline val snapchat_ghost: SemanticIcon
        get() = this + "snapchat ghost"

    inline val snapchat_square: SemanticIcon
        get() = this + "snapchat square"

    inline val snowboarding: SemanticIcon
        get() = this + "snowboarding"

    inline val snowflake: SemanticIcon
        get() = this + "snowflake"

    inline val snowflake_outline: SemanticIcon
        get() = this + "snowflake outline"

    inline val snowman: SemanticIcon
        get() = this + "snowman"

    inline val snowplow: SemanticIcon
        get() = this + "snowplow"

    inline val soap: SemanticIcon
        get() = this + "soap"

    inline val socks: SemanticIcon
        get() = this + "socks"

    inline val solar_panel: SemanticIcon
        get() = this + "solar panel"

    inline val sort: SemanticIcon
        get() = this + "sort"

    inline val sort_alphabet_down: SemanticIcon
        get() = this + "sort alphabet down"

    inline val sort_alphabet_down_alternate: SemanticIcon
        get() = this + "sort alphabet down alternate"

    inline val sort_alphabet_up: SemanticIcon
        get() = this + "sort alphabet up"

    inline val sort_alphabet_up_alternate: SemanticIcon
        get() = this + "sort alphabet up alternate"

    inline val sort_amount_down: SemanticIcon
        get() = this + "sort amount down"

    inline val sort_amount_down_alternate: SemanticIcon
        get() = this + "sort amount down alternate"

    inline val sort_amount_up: SemanticIcon
        get() = this + "sort amount up"

    inline val sort_amount_up_alternate: SemanticIcon
        get() = this + "sort amount up alternate"

    inline val sort_down: SemanticIcon
        get() = this + "sort down"

    inline val sort_numeric_down: SemanticIcon
        get() = this + "sort numeric down"

    inline val sort_numeric_down_alternate: SemanticIcon
        get() = this + "sort numeric down alternate"

    inline val sort_numeric_up: SemanticIcon
        get() = this + "sort numeric up"

    inline val sort_numeric_up_alternate: SemanticIcon
        get() = this + "sort numeric up alternate"

    inline val sort_up: SemanticIcon
        get() = this + "sort up"

    inline val soundcloud: SemanticIcon
        get() = this + "soundcloud"

    inline val sourcetree: SemanticIcon
        get() = this + "sourcetree"

    inline val spa: SemanticIcon
        get() = this + "spa"

    inline val space_shuttle: SemanticIcon
        get() = this + "space shuttle"

    inline val speakap: SemanticIcon
        get() = this + "speakap"

    inline val speaker_deck: SemanticIcon
        get() = this + "speaker deck"

    inline val spell_check: SemanticIcon
        get() = this + "spell check"

    inline val spider: SemanticIcon
        get() = this + "spider"

    inline val spinner: SemanticIcon
        get() = this + "spinner"

    inline val spinner_loading: SemanticIcon
        get() = this + "spinner loading"

    inline val splotch: SemanticIcon
        get() = this + "splotch"

    inline val spotify: SemanticIcon
        get() = this + "spotify"

    inline val spray_can: SemanticIcon
        get() = this + "spray can"

    inline val square: SemanticIcon
        get() = this + "square"

    inline val square_full: SemanticIcon
        get() = this + "square full"

    inline val square_outline: SemanticIcon
        get() = this + "square outline"

    inline val square_root_alternate: SemanticIcon
        get() = this + "square root alternate"

    inline val squarespace: SemanticIcon
        get() = this + "squarespace"

    inline val stack_exchange: SemanticIcon
        get() = this + "stack exchange"

    inline val stack_overflow: SemanticIcon
        get() = this + "stack overflow"

    inline val stackpath: SemanticIcon
        get() = this + "stackpath"

    inline val stamp: SemanticIcon
        get() = this + "stamp"

    inline val star: SemanticIcon
        get() = this + "star"

    inline val star_and_crescent: SemanticIcon
        get() = this + "star and crescent"

    inline val star_half: SemanticIcon
        get() = this + "star half"

    inline val star_half_alternate: SemanticIcon
        get() = this + "star half alternate"

    inline val star_half_outline: SemanticIcon
        get() = this + "star half outline"

    inline val star_of_david: SemanticIcon
        get() = this + "star of david"

    inline val star_of_life: SemanticIcon
        get() = this + "star of life"

    inline val star_outline: SemanticIcon
        get() = this + "star outline"

    inline val staylinked: SemanticIcon
        get() = this + "staylinked"

    inline val steam: SemanticIcon
        get() = this + "steam"

    inline val steam_square: SemanticIcon
        get() = this + "steam square"

    inline val steam_symbol: SemanticIcon
        get() = this + "steam symbol"

    inline val step_backward: SemanticIcon
        get() = this + "step backward"

    inline val step_forward: SemanticIcon
        get() = this + "step forward"

    inline val stethoscope: SemanticIcon
        get() = this + "stethoscope"

    inline val sticker_mule: SemanticIcon
        get() = this + "sticker mule"

    inline val sticky_note: SemanticIcon
        get() = this + "sticky note"

    inline val sticky_note_outline: SemanticIcon
        get() = this + "sticky note outline"

    inline val stop: SemanticIcon
        get() = this + "stop"

    inline val stop_circle: SemanticIcon
        get() = this + "stop circle"

    inline val stop_circle_outline: SemanticIcon
        get() = this + "stop circle outline"

    inline val stopwatch: SemanticIcon
        get() = this + "stopwatch"

    inline val store: SemanticIcon
        get() = this + "store"

    inline val store_alternate: SemanticIcon
        get() = this + "store alternate"

    inline val store_alternate_slash: SemanticIcon
        get() = this + "store alternate slash"

    inline val store_slash: SemanticIcon
        get() = this + "store slash"

    inline val strava: SemanticIcon
        get() = this + "strava"

    inline val stream: SemanticIcon
        get() = this + "stream"

    inline val street_view: SemanticIcon
        get() = this + "street view"

    inline val strikethrough: SemanticIcon
        get() = this + "strikethrough"

    inline val stripe: SemanticIcon
        get() = this + "stripe"

    inline val stripe_s: SemanticIcon
        get() = this + "stripe s"

    inline val stroopwafel: SemanticIcon
        get() = this + "stroopwafel"

    inline val studiovinari: SemanticIcon
        get() = this + "studiovinari"

    inline val stumbleupon: SemanticIcon
        get() = this + "stumbleupon"

    inline val stumbleupon_circle: SemanticIcon
        get() = this + "stumbleupon circle"

    inline val subscript: SemanticIcon
        get() = this + "subscript"

    inline val subway: SemanticIcon
        get() = this + "subway"

    inline val suitcase: SemanticIcon
        get() = this + "suitcase"

    inline val suitcase_rolling: SemanticIcon
        get() = this + "suitcase rolling"

    inline val sun: SemanticIcon
        get() = this + "sun"

    inline val sun_outline: SemanticIcon
        get() = this + "sun outline"

    inline val superpowers: SemanticIcon
        get() = this + "superpowers"

    inline val superscript: SemanticIcon
        get() = this + "superscript"

    inline val supple: SemanticIcon
        get() = this + "supple"

    inline val surprise: SemanticIcon
        get() = this + "surprise"

    inline val surprise_outline: SemanticIcon
        get() = this + "surprise outline"

    inline val suse: SemanticIcon
        get() = this + "suse"

    inline val swatchbook: SemanticIcon
        get() = this + "swatchbook"

    inline val swift: SemanticIcon
        get() = this + "swift"

    inline val swimmer: SemanticIcon
        get() = this + "swimmer"

    inline val swimming_pool: SemanticIcon
        get() = this + "swimming pool"

    inline val symfony: SemanticIcon
        get() = this + "symfony"

    inline val synagogue: SemanticIcon
        get() = this + "synagogue"

    inline val sync: SemanticIcon
        get() = this + "sync"

    inline val sync_alternate: SemanticIcon
        get() = this + "sync alternate"

    inline val syringe: SemanticIcon
        get() = this + "syringe"

    inline val table: SemanticIcon
        get() = this + "table"

    inline val table_tennis: SemanticIcon
        get() = this + "table tennis"

    inline val tablet: SemanticIcon
        get() = this + "tablet"

    inline val tablet_alternate: SemanticIcon
        get() = this + "tablet alternate"

    inline val tablets: SemanticIcon
        get() = this + "tablets"

    inline val tachometer_alternate: SemanticIcon
        get() = this + "tachometer alternate"

    inline val tag: SemanticIcon
        get() = this + "tag"

    inline val tags: SemanticIcon
        get() = this + "tags"

    inline val tape: SemanticIcon
        get() = this + "tape"

    inline val tasks: SemanticIcon
        get() = this + "tasks"

    inline val taxi: SemanticIcon
        get() = this + "taxi"

    inline val teal_users: SemanticIcon
        get() = this + "teal users"

    inline val teamspeak: SemanticIcon
        get() = this + "teamspeak"

    inline val teeth: SemanticIcon
        get() = this + "teeth"

    inline val teeth_open: SemanticIcon
        get() = this + "teeth open"

    inline val telegram: SemanticIcon
        get() = this + "telegram"

    inline val telegram_plane: SemanticIcon
        get() = this + "telegram plane"

    inline val temperature_high: SemanticIcon
        get() = this + "temperature high"

    inline val temperature_low: SemanticIcon
        get() = this + "temperature low"

    inline val tencent_weibo: SemanticIcon
        get() = this + "tencent weibo"

    inline val tenge: SemanticIcon
        get() = this + "tenge"

    inline val terminal: SemanticIcon
        get() = this + "terminal"

    inline val text_height: SemanticIcon
        get() = this + "text height"

    inline val text_width: SemanticIcon
        get() = this + "text width"

    inline val th: SemanticIcon
        get() = this + "th"

    inline val th_large: SemanticIcon
        get() = this + "th large"

    inline val th_list: SemanticIcon
        get() = this + "th list"

    inline val theater_masks: SemanticIcon
        get() = this + "theater masks"

    inline val themeco: SemanticIcon
        get() = this + "themeco"

    inline val themeisle: SemanticIcon
        get() = this + "themeisle"

    inline val thermometer: SemanticIcon
        get() = this + "thermometer"

    inline val thermometer_empty: SemanticIcon
        get() = this + "thermometer empty"

    inline val thermometer_full: SemanticIcon
        get() = this + "thermometer full"

    inline val thermometer_half: SemanticIcon
        get() = this + "thermometer half"

    inline val thermometer_quarter: SemanticIcon
        get() = this + "thermometer quarter"

    inline val thermometer_three_quarters: SemanticIcon
        get() = this + "thermometer three quarters"

    inline val think_peaks: SemanticIcon
        get() = this + "think peaks"

    inline val thumbs_down: SemanticIcon
        get() = this + "thumbs down"

    inline val thumbs_down_outline: SemanticIcon
        get() = this + "thumbs down outline"

    inline val thumbs_up: SemanticIcon
        get() = this + "thumbs up"

    inline val thumbs_up_outline: SemanticIcon
        get() = this + "thumbs up outline"

    inline val thumbtack: SemanticIcon
        get() = this + "thumbtack"

    inline val ticket_alternate: SemanticIcon
        get() = this + "ticket alternate"

    inline val times: SemanticIcon
        get() = this + "times"

    inline val times_circle: SemanticIcon
        get() = this + "times circle"

    inline val times_circle_outline: SemanticIcon
        get() = this + "times circle outline"

    inline val tint: SemanticIcon
        get() = this + "tint"

    inline val tint_slash: SemanticIcon
        get() = this + "tint slash"

    inline val tiny_home: SemanticIcon
        get() = this + "tiny home"

    inline val tired: SemanticIcon
        get() = this + "tired"

    inline val tired_outline: SemanticIcon
        get() = this + "tired outline"

    inline val toggle_off: SemanticIcon
        get() = this + "toggle off"

    inline val toggle_on: SemanticIcon
        get() = this + "toggle on"

    inline val toilet: SemanticIcon
        get() = this + "toilet"

    inline val toilet_paper: SemanticIcon
        get() = this + "toilet paper"

    inline val toilet_paper_slash: SemanticIcon
        get() = this + "toilet paper slash"

    inline val toolbox: SemanticIcon
        get() = this + "toolbox"

    inline val tools: SemanticIcon
        get() = this + "tools"

    inline val tooth: SemanticIcon
        get() = this + "tooth"

    inline val top_left_corner_add: SemanticIcon
        get() = this + "top left corner add"

    inline val top_right_corner_add: SemanticIcon
        get() = this + "top right corner add"

    inline val torah: SemanticIcon
        get() = this + "torah"

    inline val torii_gate: SemanticIcon
        get() = this + "torii gate"

    inline val tractor: SemanticIcon
        get() = this + "tractor"

    inline val trade_federation: SemanticIcon
        get() = this + "trade federation"

    inline val trademark: SemanticIcon
        get() = this + "trademark"

    inline val traffic_light: SemanticIcon
        get() = this + "traffic light"

    inline val trailer: SemanticIcon
        get() = this + "trailer"

    inline val train: SemanticIcon
        get() = this + "train"

    inline val tram: SemanticIcon
        get() = this + "tram"

    inline val transgender: SemanticIcon
        get() = this + "transgender"

    inline val transgender_alternate: SemanticIcon
        get() = this + "transgender alternate"

    inline val trash: SemanticIcon
        get() = this + "trash"

    inline val trash_alternate: SemanticIcon
        get() = this + "trash alternate"

    inline val trash_alternate_outline: SemanticIcon
        get() = this + "trash alternate outline"

    inline val trash_restore: SemanticIcon
        get() = this + "trash restore"

    inline val trash_restore_alternate: SemanticIcon
        get() = this + "trash restore alternate"

    inline val tree: SemanticIcon
        get() = this + "tree"

    inline val trello: SemanticIcon
        get() = this + "trello"

    inline val tripadvisor: SemanticIcon
        get() = this + "tripadvisor"

    inline val trophy: SemanticIcon
        get() = this + "trophy"

    inline val truck: SemanticIcon
        get() = this + "truck"

    inline val truck_monster: SemanticIcon
        get() = this + "truck monster"

    inline val truck_moving: SemanticIcon
        get() = this + "truck moving"

    inline val truck_packing: SemanticIcon
        get() = this + "truck packing"

    inline val truck_pickup: SemanticIcon
        get() = this + "truck pickup"

    inline val tshirt: SemanticIcon
        get() = this + "tshirt"

    inline val tty: SemanticIcon
        get() = this + "tty"

    inline val tumblr: SemanticIcon
        get() = this + "tumblr"

    inline val tumblr_square: SemanticIcon
        get() = this + "tumblr square"

    inline val tv: SemanticIcon
        get() = this + "tv"

    inline val twitch: SemanticIcon
        get() = this + "twitch"

    inline val twitter: SemanticIcon
        get() = this + "twitter"

    inline val twitter_square: SemanticIcon
        get() = this + "twitter square"

    inline val typo3: SemanticIcon
        get() = this + "typo3"

    inline val uber: SemanticIcon
        get() = this + "uber"

    inline val ubuntu: SemanticIcon
        get() = this + "ubuntu"

    inline val uikit: SemanticIcon
        get() = this + "uikit"

    inline val umbraco: SemanticIcon
        get() = this + "umbraco"

    inline val umbrella: SemanticIcon
        get() = this + "umbrella"

    inline val umbrella_beach: SemanticIcon
        get() = this + "umbrella beach"

    inline val underline: SemanticIcon
        get() = this + "underline"

    inline val undo: SemanticIcon
        get() = this + "undo"

    inline val undo_alternate: SemanticIcon
        get() = this + "undo alternate"

    inline val uniregistry: SemanticIcon
        get() = this + "uniregistry"

    inline val unity: SemanticIcon
        get() = this + "unity"

    inline val universal_access: SemanticIcon
        get() = this + "universal access"

    inline val university: SemanticIcon
        get() = this + "university"

    inline val unlink: SemanticIcon
        get() = this + "unlink"

    inline val unlock: SemanticIcon
        get() = this + "unlock"

    inline val unlock_alternate: SemanticIcon
        get() = this + "unlock alternate"

    inline val untappd: SemanticIcon
        get() = this + "untappd"

    inline val upload: SemanticIcon
        get() = this + "upload"

    inline val ups: SemanticIcon
        get() = this + "ups"

    inline val usb: SemanticIcon
        get() = this + "usb"

    inline val user: SemanticIcon
        get() = this + "user"

    inline val user_alternate: SemanticIcon
        get() = this + "user alternate"

    inline val user_alternate_slash: SemanticIcon
        get() = this + "user alternate slash"

    inline val user_astronaut: SemanticIcon
        get() = this + "user astronaut"

    inline val user_check: SemanticIcon
        get() = this + "user check"

    inline val user_circle: SemanticIcon
        get() = this + "user circle"

    inline val user_circle_outline: SemanticIcon
        get() = this + "user circle outline"

    inline val user_clock: SemanticIcon
        get() = this + "user clock"

    inline val user_cog: SemanticIcon
        get() = this + "user cog"

    inline val user_edit: SemanticIcon
        get() = this + "user edit"

    inline val user_friends: SemanticIcon
        get() = this + "user friends"

    inline val user_graduate: SemanticIcon
        get() = this + "user graduate"

    inline val user_injured: SemanticIcon
        get() = this + "user injured"

    inline val user_lock: SemanticIcon
        get() = this + "user lock"

    inline val user_md: SemanticIcon
        get() = this + "user md"

    inline val user_minus: SemanticIcon
        get() = this + "user minus"

    inline val user_ninja: SemanticIcon
        get() = this + "user ninja"

    inline val user_nurse: SemanticIcon
        get() = this + "user nurse"

    inline val user_outline: SemanticIcon
        get() = this + "user outline"

    inline val user_plus: SemanticIcon
        get() = this + "user plus"

    inline val user_secret: SemanticIcon
        get() = this + "user secret"

    inline val user_shield: SemanticIcon
        get() = this + "user shield"

    inline val user_slash: SemanticIcon
        get() = this + "user slash"

    inline val user_tag: SemanticIcon
        get() = this + "user tag"

    inline val user_tie: SemanticIcon
        get() = this + "user tie"

    inline val user_times: SemanticIcon
        get() = this + "user times"

    inline val users: SemanticIcon
        get() = this + "users"

    inline val users_cog: SemanticIcon
        get() = this + "users cog"

    inline val usps: SemanticIcon
        get() = this + "usps"

    inline val ussunnah: SemanticIcon
        get() = this + "ussunnah"

    inline val utensil_spoon: SemanticIcon
        get() = this + "utensil spoon"

    inline val utensils: SemanticIcon
        get() = this + "utensils"

    inline val vaadin: SemanticIcon
        get() = this + "vaadin"

    inline val vector_square: SemanticIcon
        get() = this + "vector square"

    inline val venus: SemanticIcon
        get() = this + "venus"

    inline val venus_double: SemanticIcon
        get() = this + "venus double"

    inline val venus_mars: SemanticIcon
        get() = this + "venus mars"

    inline val vertically_flipped_cloud: SemanticIcon
        get() = this + "vertically flipped cloud"

    inline val viacoin: SemanticIcon
        get() = this + "viacoin"

    inline val viadeo: SemanticIcon
        get() = this + "viadeo"

    inline val viadeo_square: SemanticIcon
        get() = this + "viadeo square"

    inline val vial: SemanticIcon
        get() = this + "vial"

    inline val vials: SemanticIcon
        get() = this + "vials"

    inline val viber: SemanticIcon
        get() = this + "viber"

    inline val video: SemanticIcon
        get() = this + "video"

    inline val video_slash: SemanticIcon
        get() = this + "video slash"

    inline val vihara: SemanticIcon
        get() = this + "vihara"

    inline val vimeo: SemanticIcon
        get() = this + "vimeo"

    inline val vimeo_square: SemanticIcon
        get() = this + "vimeo square"

    inline val vimeo_v: SemanticIcon
        get() = this + "vimeo v"

    inline val vine: SemanticIcon
        get() = this + "vine"

    inline val violet_users: SemanticIcon
        get() = this + "violet users"

    inline val virus: SemanticIcon
        get() = this + "virus"

    inline val virus_slash: SemanticIcon
        get() = this + "virus slash"

    inline val viruses: SemanticIcon
        get() = this + "viruses"

    inline val vk: SemanticIcon
        get() = this + "vk"

    inline val vnv: SemanticIcon
        get() = this + "vnv"

    inline val voicemail: SemanticIcon
        get() = this + "voicemail"

    inline val volleyball_ball: SemanticIcon
        get() = this + "volleyball ball"

    inline val volume_down: SemanticIcon
        get() = this + "volume down"

    inline val volume_mute: SemanticIcon
        get() = this + "volume mute"

    inline val volume_off: SemanticIcon
        get() = this + "volume off"

    inline val volume_up: SemanticIcon
        get() = this + "volume up"

    inline val vote_yea: SemanticIcon
        get() = this + "vote yea"

    inline val vuejs: SemanticIcon
        get() = this + "vuejs"

    inline val walking: SemanticIcon
        get() = this + "walking"

    inline val wallet: SemanticIcon
        get() = this + "wallet"

    inline val warehouse: SemanticIcon
        get() = this + "warehouse"

    inline val water: SemanticIcon
        get() = this + "water"

    inline val wave_square: SemanticIcon
        get() = this + "wave square"

    inline val waze: SemanticIcon
        get() = this + "waze"

    inline val weebly: SemanticIcon
        get() = this + "weebly"

    inline val weibo: SemanticIcon
        get() = this + "weibo"

    inline val weight: SemanticIcon
        get() = this + "weight"

    inline val weixin: SemanticIcon
        get() = this + "weixin"

    inline val whatsapp: SemanticIcon
        get() = this + "whatsapp"

    inline val whatsapp_square: SemanticIcon
        get() = this + "whatsapp square"

    inline val wheelchair: SemanticIcon
        get() = this + "wheelchair"

    inline val whmcs: SemanticIcon
        get() = this + "whmcs"

    inline val wifi: SemanticIcon
        get() = this + "wifi"

    inline val wikipedia_w: SemanticIcon
        get() = this + "wikipedia w"

    inline val wind: SemanticIcon
        get() = this + "wind"

    inline val window_close: SemanticIcon
        get() = this + "window close"

    inline val window_close_outline: SemanticIcon
        get() = this + "window close outline"

    inline val window_maximize: SemanticIcon
        get() = this + "window maximize"

    inline val window_maximize_outline: SemanticIcon
        get() = this + "window maximize outline"

    inline val window_minimize: SemanticIcon
        get() = this + "window minimize"

    inline val window_minimize_outline: SemanticIcon
        get() = this + "window minimize outline"

    inline val window_restore: SemanticIcon
        get() = this + "window restore"

    inline val window_restore_outline: SemanticIcon
        get() = this + "window restore outline"

    inline val windows: SemanticIcon
        get() = this + "windows"

    inline val wine_bottle: SemanticIcon
        get() = this + "wine bottle"

    inline val wine_glass: SemanticIcon
        get() = this + "wine glass"

    inline val wine_glass_alternate: SemanticIcon
        get() = this + "wine glass alternate"

    inline val wix: SemanticIcon
        get() = this + "wix"

    inline val wizards_of_the_coast: SemanticIcon
        get() = this + "wizards of the coast"

    inline val wolf_pack_battalion: SemanticIcon
        get() = this + "wolf pack battalion"

    inline val won_sign: SemanticIcon
        get() = this + "won sign"

    inline val wordpress: SemanticIcon
        get() = this + "wordpress"

    inline val wordpress_simple: SemanticIcon
        get() = this + "wordpress simple"

    inline val world: SemanticIcon
        get() = this + "world"

    inline val wpbeginner: SemanticIcon
        get() = this + "wpbeginner"

    inline val wpexplorer: SemanticIcon
        get() = this + "wpexplorer"

    inline val wpforms: SemanticIcon
        get() = this + "wpforms"

    inline val wpressr: SemanticIcon
        get() = this + "wpressr"

    inline val wrench: SemanticIcon
        get() = this + "wrench"

    inline val x_ray: SemanticIcon
        get() = this + "x ray"

    inline val xbox: SemanticIcon
        get() = this + "xbox"

    inline val xing: SemanticIcon
        get() = this + "xing"

    inline val xing_square: SemanticIcon
        get() = this + "xing square"

    inline val y_combinator: SemanticIcon
        get() = this + "y combinator"

    inline val yahoo: SemanticIcon
        get() = this + "yahoo"

    inline val yammer: SemanticIcon
        get() = this + "yammer"

    inline val yandex: SemanticIcon
        get() = this + "yandex"

    inline val yandex_international: SemanticIcon
        get() = this + "yandex international"

    inline val yarn: SemanticIcon
        get() = this + "yarn"

    inline val yellow_users: SemanticIcon
        get() = this + "yellow users"

    inline val yelp: SemanticIcon
        get() = this + "yelp"

    inline val yen_sign: SemanticIcon
        get() = this + "yen sign"

    inline val yin_yang: SemanticIcon
        get() = this + "yin yang"

    inline val yoast: SemanticIcon
        get() = this + "yoast"

    inline val youtube: SemanticIcon
        get() = this + "youtube"

    inline val youtube_square: SemanticIcon
        get() = this + "youtube square"

    inline val zhihu: SemanticIcon
        get() = this + "zhihu"
}
