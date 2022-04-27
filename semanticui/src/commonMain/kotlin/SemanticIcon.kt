@file:Suppress("Detekt:LargeClass", "Detekt:VariableNaming")

package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.I
import kotlinx.html.i

@Suppress("PropertyName", "FunctionName", "unused")
class SemanticIcon(private val parent: FlowContent?) {

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

        fun cssClassOf(block: SemanticIcon.() -> SemanticIcon): String {
            return SemanticIcon(null).block().cssClasses.joinToString(" ")
        }
    }

    private val cssClasses = mutableListOf<String>()

    operator fun plus(cls: String): SemanticIcon = apply { cssClasses.add(cls) }

    operator fun plus(classes: Array<out String>): SemanticIcon = apply { cssClasses.addAll(classes) }

    fun render(block: I.() -> Unit = {}) {
        parent?.i(classes = cssClasses.joinToString(" ") + " icon") {
            block()
        }
    }

    operator fun invoke(block: I.() -> Unit = { }) {
        render(block)
    }

    @SemanticUiCssMarker
    fun with(cls: String): SemanticIcon = this + cls

    @SemanticUiCssMarker
    fun custom(cls: String): Unit = (this + cls).render()

    // conditional classes

    @SemanticUiConditionalMarker
    fun given(
        condition: Boolean,
        action: SemanticIcon.() -> SemanticIcon,
    ): SemanticIcon = when (condition) {
        false -> this
        else -> this.action()
    }

    @SemanticUiConditionalMarker
    val then: SemanticIcon get() = this

    // coloring ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    fun color(color: SemanticColor): SemanticIcon = when {
        color.isSet -> with(color.toString())
        else -> this
    }

    @SemanticUiCssMarker
    inline val red: SemanticIcon get() = this + "red"

    @SemanticUiCssMarker
    inline val orange: SemanticIcon get() = this + "orange"

    @SemanticUiCssMarker
    inline val yellow: SemanticIcon get() = this + "yellow"

    @SemanticUiCssMarker
    inline val olive: SemanticIcon get() = this + "olive"

    @SemanticUiCssMarker
    inline val green: SemanticIcon get() = this + "green"

    @SemanticUiCssMarker
    inline val teal: SemanticIcon get() = this + "teal"

    @SemanticUiCssMarker
    inline val blue: SemanticIcon get() = this + "blue"

    @SemanticUiCssMarker
    inline val violet: SemanticIcon get() = this + "violet"

    @SemanticUiCssMarker
    inline val purple: SemanticIcon get() = this + "purple"

    @SemanticUiCssMarker
    inline val pink: SemanticIcon get() = this + "pink"

    @SemanticUiCssMarker
    inline val brown: SemanticIcon get() = this + "brown"

    @SemanticUiCssMarker
    inline val grey: SemanticIcon get() = this + "grey"

    @SemanticUiCssMarker
    inline val black: SemanticIcon get() = this + "black"

    @SemanticUiCssMarker
    inline val white: SemanticIcon get() = this + "white"

    // Behaviour ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    inline val inverted: SemanticIcon get() = this + "inverted"

    @SemanticUiCssMarker
    inline val circular: SemanticIcon get() = this + "circular"

    @SemanticUiCssMarker
    inline val disabled: SemanticIcon get() = this + "disabled"

    @SemanticUiCssMarker
    inline val loading: SemanticIcon get() = this + "loading"

    @SemanticUiCssMarker
    inline val link: SemanticIcon get() = this + "link"

    @SemanticUiCssMarker
    inline val bordered: SemanticIcon get() = this + "bordered"

    @SemanticUiCssMarker
    inline val colored: SemanticIcon get() = this + "colored"

    // Position & Alignment ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    inline val left: SemanticIcon get() = this + "right"

    @SemanticUiCssMarker
    inline val middle: SemanticIcon get() = this + "middle"

    @SemanticUiCssMarker
    inline val right: SemanticIcon get() = this + "right"

    @SemanticUiCssMarker
    inline val aligned: SemanticIcon get() = this + "aligned"

    @SemanticUiCssMarker
    inline val floated: SemanticIcon get() = this + "floated"

    // Size ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticUiCssMarker
    inline val mini: SemanticIcon get() = this + "mini"

    @SemanticUiCssMarker
    inline val tiny: SemanticIcon get() = this + "tiny"

    @SemanticUiCssMarker
    inline val small: SemanticIcon get() = this + "small"

    @SemanticUiCssMarker
    inline val medium: SemanticIcon get() = this

    @SemanticUiCssMarker
    inline val large: SemanticIcon get() = this + "large"

    @SemanticUiCssMarker
    inline val big: SemanticIcon get() = this + "big"

    @SemanticUiCssMarker
    inline val huge: SemanticIcon get() = this + "huge"

    @SemanticUiCssMarker
    inline val massive: SemanticIcon get() = this + "massive"

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Icons ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SemanticIconMarker
    inline val _500px: SemanticIcon get() = this + "500px"

    @SemanticIconMarker
    inline val accessible: SemanticIcon get() = this + "accessible"

    @SemanticIconMarker
    inline val accusoft: SemanticIcon get() = this + "accusoft"

    @SemanticIconMarker
    inline val acquisitions_incorporated: SemanticIcon get() = this + "acquisitions incorporated"

    @SemanticIconMarker
    inline val ad: SemanticIcon get() = this + "ad"

    @SemanticIconMarker
    inline val address_book: SemanticIcon get() = this + "address book"

    @SemanticIconMarker
    inline val address_book_outline: SemanticIcon get() = this + "address book outline"

    @SemanticIconMarker
    inline val address_card: SemanticIcon get() = this + "address card"

    @SemanticIconMarker
    inline val address_card_outline: SemanticIcon get() = this + "address card outline"

    @SemanticIconMarker
    inline val adjust: SemanticIcon get() = this + "adjust"

    @SemanticIconMarker
    inline val adn: SemanticIcon get() = this + "adn"

    @SemanticIconMarker
    inline val adobe: SemanticIcon get() = this + "adobe"

    @SemanticIconMarker
    inline val adversal: SemanticIcon get() = this + "adversal"

    @SemanticIconMarker
    inline val affiliatetheme: SemanticIcon get() = this + "affiliatetheme"

    @SemanticIconMarker
    inline val air_freshener: SemanticIcon get() = this + "air freshener"

    @SemanticIconMarker
    inline val airbnb: SemanticIcon get() = this + "airbnb"

    @SemanticIconMarker
    inline val algolia: SemanticIcon get() = this + "algolia"

    @SemanticIconMarker
    inline val align_center: SemanticIcon get() = this + "align center"

    @SemanticIconMarker
    inline val align_justify: SemanticIcon get() = this + "align justify"

    @SemanticIconMarker
    inline val align_left: SemanticIcon get() = this + "align left"

    @SemanticIconMarker
    inline val align_right: SemanticIcon get() = this + "align right"

    @SemanticIconMarker
    inline val alipay: SemanticIcon get() = this + "alipay"

    @SemanticIconMarker
    inline val allergies: SemanticIcon get() = this + "allergies"

    @SemanticIconMarker
    inline val alternate_github: SemanticIcon get() = this + "alternate github"

    @SemanticIconMarker
    inline val amazon: SemanticIcon get() = this + "amazon"

    @SemanticIconMarker
    inline val amazon_pay: SemanticIcon get() = this + "amazon pay"

    @SemanticIconMarker
    inline val ambulance: SemanticIcon get() = this + "ambulance"

    @SemanticIconMarker
    inline val american_sign_language_interpreting: SemanticIcon get() = this + "american sign language interpreting"

    @SemanticIconMarker
    inline val amilia: SemanticIcon get() = this + "amilia"

    @SemanticIconMarker
    inline val anchor: SemanticIcon get() = this + "anchor"

    @SemanticIconMarker
    inline val android: SemanticIcon get() = this + "android"

    @SemanticIconMarker
    inline val angellist: SemanticIcon get() = this + "angellist"

    @SemanticIconMarker
    inline val angle_double_down: SemanticIcon get() = this + "angle double down"

    @SemanticIconMarker
    inline val angle_double_left: SemanticIcon get() = this + "angle double left"

    @SemanticIconMarker
    inline val angle_double_right: SemanticIcon get() = this + "angle double right"

    @SemanticIconMarker
    inline val angle_double_up: SemanticIcon get() = this + "angle double up"

    @SemanticIconMarker
    inline val angle_down: SemanticIcon get() = this + "angle down"

    @SemanticIconMarker
    inline val angle_left: SemanticIcon get() = this + "angle left"

    @SemanticIconMarker
    inline val angle_right: SemanticIcon get() = this + "angle right"

    @SemanticIconMarker
    inline val angle_up: SemanticIcon get() = this + "angle up"

    @SemanticIconMarker
    inline val angry: SemanticIcon get() = this + "angry"

    @SemanticIconMarker
    inline val angry_outline: SemanticIcon get() = this + "angry outline"

    @SemanticIconMarker
    inline val angrycreative: SemanticIcon get() = this + "angrycreative"

    @SemanticIconMarker
    inline val angular: SemanticIcon get() = this + "angular"

    @SemanticIconMarker
    inline val ankh: SemanticIcon get() = this + "ankh"

    @SemanticIconMarker
    inline val app_store: SemanticIcon get() = this + "app store"

    @SemanticIconMarker
    inline val app_store_ios: SemanticIcon get() = this + "app store ios"

    @SemanticIconMarker
    inline val apper: SemanticIcon get() = this + "apper"

    @SemanticIconMarker
    inline val apple: SemanticIcon get() = this + "apple"

    @SemanticIconMarker
    inline val apple_pay: SemanticIcon get() = this + "apple pay"

    @SemanticIconMarker
    inline val archive: SemanticIcon get() = this + "archive"

    @SemanticIconMarker
    inline val archway: SemanticIcon get() = this + "archway"

    @SemanticIconMarker
    inline val arrow_alternate_circle_down: SemanticIcon get() = this + "arrow alternate circle down"

    @SemanticIconMarker
    inline val arrow_alternate_circle_down_outline: SemanticIcon get() = this + "arrow alternate circle down outline"

    @SemanticIconMarker
    inline val arrow_alternate_circle_left: SemanticIcon get() = this + "arrow alternate circle left"

    @SemanticIconMarker
    inline val arrow_alternate_circle_left_outline: SemanticIcon get() = this + "arrow alternate circle left outline"

    @SemanticIconMarker
    inline val arrow_alternate_circle_right: SemanticIcon get() = this + "arrow alternate circle right"

    @SemanticIconMarker
    inline val arrow_alternate_circle_right_outline: SemanticIcon get() = this + "arrow alternate circle right outline"

    @SemanticIconMarker
    inline val arrow_alternate_circle_up: SemanticIcon get() = this + "arrow alternate circle up"

    @SemanticIconMarker
    inline val arrow_alternate_circle_up_outline: SemanticIcon get() = this + "arrow alternate circle up outline"

    @SemanticIconMarker
    inline val arrow_circle_down: SemanticIcon get() = this + "arrow circle down"

    @SemanticIconMarker
    inline val arrow_circle_left: SemanticIcon get() = this + "arrow circle left"

    @SemanticIconMarker
    inline val arrow_circle_right: SemanticIcon get() = this + "arrow circle right"

    @SemanticIconMarker
    inline val arrow_circle_up: SemanticIcon get() = this + "arrow circle up"

    @SemanticIconMarker
    inline val arrow_down: SemanticIcon get() = this + "arrow down"

    @SemanticIconMarker
    inline val arrow_left: SemanticIcon get() = this + "arrow left"

    @SemanticIconMarker
    inline val arrow_right: SemanticIcon get() = this + "arrow right"

    @SemanticIconMarker
    inline val arrow_up: SemanticIcon get() = this + "arrow up"

    @SemanticIconMarker
    inline val arrows_alternate: SemanticIcon get() = this + "arrows alternate"

    @SemanticIconMarker
    inline val arrows_alternate_horizontal: SemanticIcon get() = this + "arrows alternate horizontal"

    @SemanticIconMarker
    inline val arrows_alternate_vertical: SemanticIcon get() = this + "arrows alternate vertical"

    @SemanticIconMarker
    inline val artstation: SemanticIcon get() = this + "artstation"

    @SemanticIconMarker
    inline val assistive_listening_systems: SemanticIcon get() = this + "assistive listening systems"

    @SemanticIconMarker
    inline val asterisk: SemanticIcon get() = this + "asterisk"

    @SemanticIconMarker
    inline val asterisk_loading: SemanticIcon get() = this + "asterisk loading"

    @SemanticIconMarker
    inline val asymmetrik: SemanticIcon get() = this + "asymmetrik"

    @SemanticIconMarker
    inline val at: SemanticIcon get() = this + "at"

    @SemanticIconMarker
    inline val atlas: SemanticIcon get() = this + "atlas"

    @SemanticIconMarker
    inline val atlassian: SemanticIcon get() = this + "atlassian"

    @SemanticIconMarker
    inline val atom: SemanticIcon get() = this + "atom"

    @SemanticIconMarker
    inline val audible: SemanticIcon get() = this + "audible"

    @SemanticIconMarker
    inline val audio_description: SemanticIcon get() = this + "audio description"

    @SemanticIconMarker
    inline val autoprefixer: SemanticIcon get() = this + "autoprefixer"

    @SemanticIconMarker
    inline val avianex: SemanticIcon get() = this + "avianex"

    @SemanticIconMarker
    inline val aviato: SemanticIcon get() = this + "aviato"

    @SemanticIconMarker
    inline val award: SemanticIcon get() = this + "award"

    @SemanticIconMarker
    inline val aws: SemanticIcon get() = this + "aws"

    @SemanticIconMarker
    inline val baby: SemanticIcon get() = this + "baby"

    @SemanticIconMarker
    inline val baby_carriage: SemanticIcon get() = this + "baby carriage"

    @SemanticIconMarker
    inline val backward: SemanticIcon get() = this + "backward"

    @SemanticIconMarker
    inline val bacon: SemanticIcon get() = this + "bacon"

    @SemanticIconMarker
    inline val bahai: SemanticIcon get() = this + "bahai"

    @SemanticIconMarker
    inline val balance_scale: SemanticIcon get() = this + "balance scale"

    @SemanticIconMarker
    inline val balance_scale_left: SemanticIcon get() = this + "balance scale left"

    @SemanticIconMarker
    inline val balance_scale_right: SemanticIcon get() = this + "balance scale right"

    @SemanticIconMarker
    inline val ban: SemanticIcon get() = this + "ban"

    @SemanticIconMarker
    inline val band_aid: SemanticIcon get() = this + "band aid"

    @SemanticIconMarker
    inline val bandcamp: SemanticIcon get() = this + "bandcamp"

    @SemanticIconMarker
    inline val barcode: SemanticIcon get() = this + "barcode"

    @SemanticIconMarker
    inline val bars: SemanticIcon get() = this + "bars"

    @SemanticIconMarker
    inline val baseball_ball: SemanticIcon get() = this + "baseball ball"

    @SemanticIconMarker
    inline val basketball_ball: SemanticIcon get() = this + "basketball ball"

    @SemanticIconMarker
    inline val bath: SemanticIcon get() = this + "bath"

    @SemanticIconMarker
    inline val battery_empty: SemanticIcon get() = this + "battery empty"

    @SemanticIconMarker
    inline val battery_full: SemanticIcon get() = this + "battery full"

    @SemanticIconMarker
    inline val battery_half: SemanticIcon get() = this + "battery half"

    @SemanticIconMarker
    inline val battery_quarter: SemanticIcon get() = this + "battery quarter"

    @SemanticIconMarker
    inline val battery_three_quarters: SemanticIcon get() = this + "battery three quarters"

    @SemanticIconMarker
    inline val battle_net: SemanticIcon get() = this + "battle net"

    @SemanticIconMarker
    inline val bed: SemanticIcon get() = this + "bed"

    @SemanticIconMarker
    inline val beer: SemanticIcon get() = this + "beer"

    @SemanticIconMarker
    inline val behance: SemanticIcon get() = this + "behance"

    @SemanticIconMarker
    inline val behance_square: SemanticIcon get() = this + "behance square"

    @SemanticIconMarker
    inline val bell: SemanticIcon get() = this + "bell"

    @SemanticIconMarker
    inline val bell_outline: SemanticIcon get() = this + "bell outline"

    @SemanticIconMarker
    inline val bell_slash: SemanticIcon get() = this + "bell slash"

    @SemanticIconMarker
    inline val bell_slash_outline: SemanticIcon get() = this + "bell slash outline"

    @SemanticIconMarker
    inline val bezier_curve: SemanticIcon get() = this + "bezier curve"

    @SemanticIconMarker
    inline val bible: SemanticIcon get() = this + "bible"

    @SemanticIconMarker
    inline val bicycle: SemanticIcon get() = this + "bicycle"

    @SemanticIconMarker
    inline val big_circle_outline: SemanticIcon get() = this + "big circle outline"

    @SemanticIconMarker
    inline val big_home: SemanticIcon get() = this + "big home"

    @SemanticIconMarker
    inline val big_red_dont: SemanticIcon get() = this + "big red dont"

    @SemanticIconMarker
    inline val biking: SemanticIcon get() = this + "biking"

    @SemanticIconMarker
    inline val bimobject: SemanticIcon get() = this + "bimobject"

    @SemanticIconMarker
    inline val binoculars: SemanticIcon get() = this + "binoculars"

    @SemanticIconMarker
    inline val biohazard: SemanticIcon get() = this + "biohazard"

    @SemanticIconMarker
    inline val birthday_cake: SemanticIcon get() = this + "birthday cake"

    @SemanticIconMarker
    inline val bitbucket: SemanticIcon get() = this + "bitbucket"

    @SemanticIconMarker
    inline val bitcoin: SemanticIcon get() = this + "bitcoin"

    @SemanticIconMarker
    inline val bity: SemanticIcon get() = this + "bity"

    @SemanticIconMarker
    inline val black_tie: SemanticIcon get() = this + "black tie"

    @SemanticIconMarker
    inline val black_user: SemanticIcon get() = this + "black user"

    @SemanticIconMarker
    inline val black_users: SemanticIcon get() = this + "black users"

    @SemanticIconMarker
    inline val blackberry: SemanticIcon get() = this + "blackberry"

    @SemanticIconMarker
    inline val blender: SemanticIcon get() = this + "blender"

    @SemanticIconMarker
    inline val blind: SemanticIcon get() = this + "blind"

    @SemanticIconMarker
    inline val blog: SemanticIcon get() = this + "blog"

    @SemanticIconMarker
    inline val blogger: SemanticIcon get() = this + "blogger"

    @SemanticIconMarker
    inline val blogger_b: SemanticIcon get() = this + "blogger b"

    @SemanticIconMarker
    inline val blue_users: SemanticIcon get() = this + "blue users"

    @SemanticIconMarker
    inline val bluetooth: SemanticIcon get() = this + "bluetooth"

    @SemanticIconMarker
    inline val bluetooth_b: SemanticIcon get() = this + "bluetooth b"

    @SemanticIconMarker
    inline val bold: SemanticIcon get() = this + "bold"

    @SemanticIconMarker
    inline val bolt: SemanticIcon get() = this + "bolt"

    @SemanticIconMarker
    inline val bomb: SemanticIcon get() = this + "bomb"

    @SemanticIconMarker
    inline val bone: SemanticIcon get() = this + "bone"

    @SemanticIconMarker
    inline val bong: SemanticIcon get() = this + "bong"

    @SemanticIconMarker
    inline val book: SemanticIcon get() = this + "book"

    @SemanticIconMarker
    inline val book_dead: SemanticIcon get() = this + "book dead"

    @SemanticIconMarker
    inline val book_medical: SemanticIcon get() = this + "book medical"

    @SemanticIconMarker
    inline val book_open: SemanticIcon get() = this + "book open"

    @SemanticIconMarker
    inline val book_reader: SemanticIcon get() = this + "book reader"

    @SemanticIconMarker
    inline val bookmark: SemanticIcon get() = this + "bookmark"

    @SemanticIconMarker
    inline val bookmark_outline: SemanticIcon get() = this + "bookmark outline"

    @SemanticIconMarker
    inline val bootstrap: SemanticIcon get() = this + "bootstrap"

    @SemanticIconMarker
    inline val border_all: SemanticIcon get() = this + "border all"

    @SemanticIconMarker
    inline val border_none: SemanticIcon get() = this + "border none"

    @SemanticIconMarker
    inline val border_style: SemanticIcon get() = this + "border style"

    @SemanticIconMarker
    inline val bordered_colored_blue_users: SemanticIcon get() = this + "bordered colored blue users"

    @SemanticIconMarker
    inline val bordered_colored_green_users: SemanticIcon get() = this + "bordered colored green users"

    @SemanticIconMarker
    inline val bordered_colored_red_users: SemanticIcon get() = this + "bordered colored red users"

    @SemanticIconMarker
    inline val bordered_inverted_black_users: SemanticIcon get() = this + "bordered inverted black users"

    @SemanticIconMarker
    inline val bordered_inverted_teal_users: SemanticIcon get() = this + "bordered inverted teal users"

    @SemanticIconMarker
    inline val bordered_teal_users: SemanticIcon get() = this + "bordered teal users"

    @SemanticIconMarker
    inline val bordered_users: SemanticIcon get() = this + "bordered users"

    @SemanticIconMarker
    inline val bottom_left_corner_add: SemanticIcon get() = this + "bottom left corner add"

    @SemanticIconMarker
    inline val bottom_right_corner_add: SemanticIcon get() = this + "bottom right corner add"

    @SemanticIconMarker
    inline val bowling_ball: SemanticIcon get() = this + "bowling ball"

    @SemanticIconMarker
    inline val box: SemanticIcon get() = this + "box"

    @SemanticIconMarker
    inline val box_open: SemanticIcon get() = this + "box open"

    @SemanticIconMarker
    inline val box_tissue: SemanticIcon get() = this + "box tissue"

    @SemanticIconMarker
    inline val boxes: SemanticIcon get() = this + "boxes"

    @SemanticIconMarker
    inline val braille: SemanticIcon get() = this + "braille"

    @SemanticIconMarker
    inline val brain: SemanticIcon get() = this + "brain"

    @SemanticIconMarker
    inline val bread_slice: SemanticIcon get() = this + "bread slice"

    @SemanticIconMarker
    inline val briefcase: SemanticIcon get() = this + "briefcase"

    @SemanticIconMarker
    inline val briefcase_medical: SemanticIcon get() = this + "briefcase medical"

    @SemanticIconMarker
    inline val broadcast_tower: SemanticIcon get() = this + "broadcast tower"

    @SemanticIconMarker
    inline val broom: SemanticIcon get() = this + "broom"

    @SemanticIconMarker
    inline val brown_users: SemanticIcon get() = this + "brown users"

    @SemanticIconMarker
    inline val brush: SemanticIcon get() = this + "brush"

    @SemanticIconMarker
    inline val btc: SemanticIcon get() = this + "btc"

    @SemanticIconMarker
    inline val buffer: SemanticIcon get() = this + "buffer"

    @SemanticIconMarker
    inline val bug: SemanticIcon get() = this + "bug"

    @SemanticIconMarker
    inline val building: SemanticIcon get() = this + "building"

    @SemanticIconMarker
    inline val building_outline: SemanticIcon get() = this + "building outline"

    @SemanticIconMarker
    inline val bullhorn: SemanticIcon get() = this + "bullhorn"

    @SemanticIconMarker
    inline val bullseye: SemanticIcon get() = this + "bullseye"

    @SemanticIconMarker
    inline val burn: SemanticIcon get() = this + "burn"

    @SemanticIconMarker
    inline val buromobelexperte: SemanticIcon get() = this + "buromobelexperte"

    @SemanticIconMarker
    inline val bus: SemanticIcon get() = this + "bus"

    @SemanticIconMarker
    inline val bus_alternate: SemanticIcon get() = this + "bus alternate"

    @SemanticIconMarker
    inline val business_time: SemanticIcon get() = this + "business time"

    @SemanticIconMarker
    inline val buy_n_large: SemanticIcon get() = this + "buy n large"

    @SemanticIconMarker
    inline val buysellads: SemanticIcon get() = this + "buysellads"

    @SemanticIconMarker
    inline val calculator: SemanticIcon get() = this + "calculator"

    @SemanticIconMarker
    inline val calendar: SemanticIcon get() = this + "calendar"

    @SemanticIconMarker
    inline val calendar_alternate: SemanticIcon get() = this + "calendar alternate"

    @SemanticIconMarker
    inline val calendar_alternate_outline: SemanticIcon get() = this + "calendar alternate outline"

    @SemanticIconMarker
    inline val calendar_check: SemanticIcon get() = this + "calendar check"

    @SemanticIconMarker
    inline val calendar_check_outline: SemanticIcon get() = this + "calendar check outline"

    @SemanticIconMarker
    inline val calendar_day: SemanticIcon get() = this + "calendar day"

    @SemanticIconMarker
    inline val calendar_minus: SemanticIcon get() = this + "calendar minus"

    @SemanticIconMarker
    inline val calendar_minus_outline: SemanticIcon get() = this + "calendar minus outline"

    @SemanticIconMarker
    inline val calendar_outline: SemanticIcon get() = this + "calendar outline"

    @SemanticIconMarker
    inline val calendar_plus: SemanticIcon get() = this + "calendar plus"

    @SemanticIconMarker
    inline val calendar_plus_outline: SemanticIcon get() = this + "calendar plus outline"

    @SemanticIconMarker
    inline val calendar_times: SemanticIcon get() = this + "calendar times"

    @SemanticIconMarker
    inline val calendar_times_outline: SemanticIcon get() = this + "calendar times outline"

    @SemanticIconMarker
    inline val calendar_week: SemanticIcon get() = this + "calendar week"

    @SemanticIconMarker
    inline val camera: SemanticIcon get() = this + "camera"

    @SemanticIconMarker
    inline val camera_retro: SemanticIcon get() = this + "camera retro"

    @SemanticIconMarker
    inline val campground: SemanticIcon get() = this + "campground"

    @SemanticIconMarker
    inline val canadian_maple_leaf: SemanticIcon get() = this + "canadian maple leaf"

    @SemanticIconMarker
    inline val candy_cane: SemanticIcon get() = this + "candy cane"

    @SemanticIconMarker
    inline val cannabis: SemanticIcon get() = this + "cannabis"

    @SemanticIconMarker
    inline val capsules: SemanticIcon get() = this + "capsules"

    @SemanticIconMarker
    inline val car: SemanticIcon get() = this + "car"

    @SemanticIconMarker
    inline val car_alternate: SemanticIcon get() = this + "car alternate"

    @SemanticIconMarker
    inline val car_battery: SemanticIcon get() = this + "car battery"

    @SemanticIconMarker
    inline val car_crash: SemanticIcon get() = this + "car crash"

    @SemanticIconMarker
    inline val car_side: SemanticIcon get() = this + "car side"

    @SemanticIconMarker
    inline val caravan: SemanticIcon get() = this + "caravan"

    @SemanticIconMarker
    inline val caret_down: SemanticIcon get() = this + "caret down"

    @SemanticIconMarker
    inline val caret_left: SemanticIcon get() = this + "caret left"

    @SemanticIconMarker
    inline val caret_right: SemanticIcon get() = this + "caret right"

    @SemanticIconMarker
    inline val caret_square_down: SemanticIcon get() = this + "caret square down"

    @SemanticIconMarker
    inline val caret_square_down_outline: SemanticIcon get() = this + "caret square down outline"

    @SemanticIconMarker
    inline val caret_square_left: SemanticIcon get() = this + "caret square left"

    @SemanticIconMarker
    inline val caret_square_left_outline: SemanticIcon get() = this + "caret square left outline"

    @SemanticIconMarker
    inline val caret_square_right: SemanticIcon get() = this + "caret square right"

    @SemanticIconMarker
    inline val caret_square_right_outline: SemanticIcon get() = this + "caret square right outline"

    @SemanticIconMarker
    inline val caret_square_up: SemanticIcon get() = this + "caret square up"

    @SemanticIconMarker
    inline val caret_square_up_outline: SemanticIcon get() = this + "caret square up outline"

    @SemanticIconMarker
    inline val caret_up: SemanticIcon get() = this + "caret up"

    @SemanticIconMarker
    inline val carrot: SemanticIcon get() = this + "carrot"

    @SemanticIconMarker
    inline val cart_arrow_down: SemanticIcon get() = this + "cart arrow down"

    @SemanticIconMarker
    inline val cart_plus: SemanticIcon get() = this + "cart plus"

    @SemanticIconMarker
    inline val cash_register: SemanticIcon get() = this + "cash register"

    @SemanticIconMarker
    inline val cat: SemanticIcon get() = this + "cat"

    @SemanticIconMarker
    inline val cc_amazon_pay: SemanticIcon get() = this + "cc amazon pay"

    @SemanticIconMarker
    inline val cc_amex: SemanticIcon get() = this + "cc amex"

    @SemanticIconMarker
    inline val cc_apple_pay: SemanticIcon get() = this + "cc apple pay"

    @SemanticIconMarker
    inline val cc_diners_club: SemanticIcon get() = this + "cc diners club"

    @SemanticIconMarker
    inline val cc_discover: SemanticIcon get() = this + "cc discover"

    @SemanticIconMarker
    inline val cc_jcb: SemanticIcon get() = this + "cc jcb"

    @SemanticIconMarker
    inline val cc_mastercard: SemanticIcon get() = this + "cc mastercard"

    @SemanticIconMarker
    inline val cc_paypal: SemanticIcon get() = this + "cc paypal"

    @SemanticIconMarker
    inline val cc_stripe: SemanticIcon get() = this + "cc stripe"

    @SemanticIconMarker
    inline val cc_visa: SemanticIcon get() = this + "cc visa"

    @SemanticIconMarker
    inline val centercode: SemanticIcon get() = this + "centercode"

    @SemanticIconMarker
    inline val centos: SemanticIcon get() = this + "centos"

    @SemanticIconMarker
    inline val certificate: SemanticIcon get() = this + "certificate"

    @SemanticIconMarker
    inline val chair: SemanticIcon get() = this + "chair"

    @SemanticIconMarker
    inline val chalkboard: SemanticIcon get() = this + "chalkboard"

    @SemanticIconMarker
    inline val chalkboard_teacher: SemanticIcon get() = this + "chalkboard teacher"

    @SemanticIconMarker
    inline val charging_station: SemanticIcon get() = this + "charging station"

    @SemanticIconMarker
    inline val chart_area: SemanticIcon get() = this + "chart area"

    @SemanticIconMarker
    inline val chart_bar: SemanticIcon get() = this + "chart bar"

    @SemanticIconMarker
    inline val chart_bar_outline: SemanticIcon get() = this + "chart bar outline"

    @SemanticIconMarker
    inline val chart_line: SemanticIcon get() = this + "chart line"

    @SemanticIconMarker
    inline val chart_pie: SemanticIcon get() = this + "chart pie"

    @SemanticIconMarker
    inline val check: SemanticIcon get() = this + "check"

    @SemanticIconMarker
    inline val check_circle: SemanticIcon get() = this + "check circle"

    @SemanticIconMarker
    inline val check_circle_outline: SemanticIcon get() = this + "check circle outline"

    @SemanticIconMarker
    inline val check_double: SemanticIcon get() = this + "check double"

    @SemanticIconMarker
    inline val check_square: SemanticIcon get() = this + "check square"

    @SemanticIconMarker
    inline val check_square_outline: SemanticIcon get() = this + "check square outline"

    @SemanticIconMarker
    inline val cheese: SemanticIcon get() = this + "cheese"

    @SemanticIconMarker
    inline val chess: SemanticIcon get() = this + "chess"

    @SemanticIconMarker
    inline val chess_bishop: SemanticIcon get() = this + "chess bishop"

    @SemanticIconMarker
    inline val chess_board: SemanticIcon get() = this + "chess board"

    @SemanticIconMarker
    inline val chess_king: SemanticIcon get() = this + "chess king"

    @SemanticIconMarker
    inline val chess_knight: SemanticIcon get() = this + "chess knight"

    @SemanticIconMarker
    inline val chess_pawn: SemanticIcon get() = this + "chess pawn"

    @SemanticIconMarker
    inline val chess_queen: SemanticIcon get() = this + "chess queen"

    @SemanticIconMarker
    inline val chess_rook: SemanticIcon get() = this + "chess rook"

    @SemanticIconMarker
    inline val chevron_circle_down: SemanticIcon get() = this + "chevron circle down"

    @SemanticIconMarker
    inline val chevron_circle_left: SemanticIcon get() = this + "chevron circle left"

    @SemanticIconMarker
    inline val chevron_circle_right: SemanticIcon get() = this + "chevron circle right"

    @SemanticIconMarker
    inline val chevron_circle_up: SemanticIcon get() = this + "chevron circle up"

    @SemanticIconMarker
    inline val chevron_down: SemanticIcon get() = this + "chevron down"

    @SemanticIconMarker
    inline val chevron_left: SemanticIcon get() = this + "chevron left"

    @SemanticIconMarker
    inline val chevron_right: SemanticIcon get() = this + "chevron right"

    @SemanticIconMarker
    inline val chevron_up: SemanticIcon get() = this + "chevron up"

    @SemanticIconMarker
    inline val child: SemanticIcon get() = this + "child"

    @SemanticIconMarker
    inline val chrome: SemanticIcon get() = this + "chrome"

    @SemanticIconMarker
    inline val chromecast: SemanticIcon get() = this + "chromecast"

    @SemanticIconMarker
    inline val church: SemanticIcon get() = this + "church"

    @SemanticIconMarker
    inline val circle: SemanticIcon get() = this + "circle"

    @SemanticIconMarker
    inline val circle_notch: SemanticIcon get() = this + "circle notch"

    @SemanticIconMarker
    inline val circle_outline: SemanticIcon get() = this + "circle outline"

    @SemanticIconMarker
    inline val circular_colored_blue_users: SemanticIcon get() = this + "circular colored blue users"

    @SemanticIconMarker
    inline val circular_colored_green_users: SemanticIcon get() = this + "circular colored green users"

    @SemanticIconMarker
    inline val circular_colored_red_users: SemanticIcon get() = this + "circular colored red users"

    @SemanticIconMarker
    inline val circular_inverted_teal_users: SemanticIcon get() = this + "circular inverted teal users"

    @SemanticIconMarker
    inline val circular_inverted_users: SemanticIcon get() = this + "circular inverted users"

    @SemanticIconMarker
    inline val circular_teal_users: SemanticIcon get() = this + "circular teal users"

    @SemanticIconMarker
    inline val circular_users: SemanticIcon get() = this + "circular users"

    @SemanticIconMarker
    inline val city: SemanticIcon get() = this + "city"

    @SemanticIconMarker
    inline val clinic_medical: SemanticIcon get() = this + "clinic medical"

    @SemanticIconMarker
    inline val clipboard: SemanticIcon get() = this + "clipboard"

    @SemanticIconMarker
    inline val clipboard_check: SemanticIcon get() = this + "clipboard check"

    @SemanticIconMarker
    inline val clipboard_list: SemanticIcon get() = this + "clipboard list"

    @SemanticIconMarker
    inline val clipboard_outline: SemanticIcon get() = this + "clipboard outline"

    @SemanticIconMarker
    inline val clock: SemanticIcon get() = this + "clock"

    @SemanticIconMarker
    inline val clock_outline: SemanticIcon get() = this + "clock outline"

    @SemanticIconMarker
    inline val clockwise_rotated_cloud: SemanticIcon get() = this + "clockwise rotated cloud"

    @SemanticIconMarker
    inline val clone: SemanticIcon get() = this + "clone"

    @SemanticIconMarker
    inline val clone_outline: SemanticIcon get() = this + "clone outline"

    @SemanticIconMarker
    inline val close: SemanticIcon get() = this + "close"

    @SemanticIconMarker
    inline val close_link: SemanticIcon get() = this + "close link"

    @SemanticIconMarker
    inline val closed_captioning: SemanticIcon get() = this + "closed captioning"

    @SemanticIconMarker
    inline val closed_captioning_outline: SemanticIcon get() = this + "closed captioning outline"

    @SemanticIconMarker
    inline val cloud: SemanticIcon get() = this + "cloud"

    @SemanticIconMarker
    inline val cloud_download_alternate: SemanticIcon get() = this + "cloud download alternate"

    @SemanticIconMarker
    inline val cloud_meatball: SemanticIcon get() = this + "cloud meatball"

    @SemanticIconMarker
    inline val cloud_moon: SemanticIcon get() = this + "cloud moon"

    @SemanticIconMarker
    inline val cloud_moon_rain: SemanticIcon get() = this + "cloud moon rain"

    @SemanticIconMarker
    inline val cloud_rain: SemanticIcon get() = this + "cloud rain"

    @SemanticIconMarker
    inline val cloud_showers_heavy: SemanticIcon get() = this + "cloud showers heavy"

    @SemanticIconMarker
    inline val cloud_sun: SemanticIcon get() = this + "cloud sun"

    @SemanticIconMarker
    inline val cloud_sun_rain: SemanticIcon get() = this + "cloud sun rain"

    @SemanticIconMarker
    inline val cloud_upload_alternate: SemanticIcon get() = this + "cloud upload alternate"

    @SemanticIconMarker
    inline val cloudscale: SemanticIcon get() = this + "cloudscale"

    @SemanticIconMarker
    inline val cloudsmith: SemanticIcon get() = this + "cloudsmith"

    @SemanticIconMarker
    inline val cloudversify: SemanticIcon get() = this + "cloudversify"

    @SemanticIconMarker
    inline val cocktail: SemanticIcon get() = this + "cocktail"

    @SemanticIconMarker
    inline val code: SemanticIcon get() = this + "code"

    @SemanticIconMarker
    inline val code_branch: SemanticIcon get() = this + "code branch"

    @SemanticIconMarker
    inline val codepen: SemanticIcon get() = this + "codepen"

    @SemanticIconMarker
    inline val codiepie: SemanticIcon get() = this + "codiepie"

    @SemanticIconMarker
    inline val coffee: SemanticIcon get() = this + "coffee"

    @SemanticIconMarker
    inline val cog: SemanticIcon get() = this + "cog"

    @SemanticIconMarker
    inline val cogs: SemanticIcon get() = this + "cogs"

    @SemanticIconMarker
    inline val coins: SemanticIcon get() = this + "coins"

    @SemanticIconMarker
    inline val columns: SemanticIcon get() = this + "columns"

    @SemanticIconMarker
    inline val comment: SemanticIcon get() = this + "comment"

    @SemanticIconMarker
    inline val comment_alternate: SemanticIcon get() = this + "comment alternate"

    @SemanticIconMarker
    inline val comment_alternate_outline: SemanticIcon get() = this + "comment alternate outline"

    @SemanticIconMarker
    inline val comment_dollar: SemanticIcon get() = this + "comment dollar"

    @SemanticIconMarker
    inline val comment_dots: SemanticIcon get() = this + "comment dots"

    @SemanticIconMarker
    inline val comment_dots_outline: SemanticIcon get() = this + "comment dots outline"

    @SemanticIconMarker
    inline val comment_medical: SemanticIcon get() = this + "comment medical"

    @SemanticIconMarker
    inline val comment_outline: SemanticIcon get() = this + "comment outline"

    @SemanticIconMarker
    inline val comment_slash: SemanticIcon get() = this + "comment slash"

    @SemanticIconMarker
    inline val comments: SemanticIcon get() = this + "comments"

    @SemanticIconMarker
    inline val comments_dollar: SemanticIcon get() = this + "comments dollar"

    @SemanticIconMarker
    inline val comments_outline: SemanticIcon get() = this + "comments outline"

    @SemanticIconMarker
    inline val compact_disc: SemanticIcon get() = this + "compact disc"

    @SemanticIconMarker
    inline val compass: SemanticIcon get() = this + "compass"

    @SemanticIconMarker
    inline val compass_outline: SemanticIcon get() = this + "compass outline"

    @SemanticIconMarker
    inline val compress: SemanticIcon get() = this + "compress"

    @SemanticIconMarker
    inline val compress_alternate: SemanticIcon get() = this + "compress alternate"

    @SemanticIconMarker
    inline val compress_arrows_alternate: SemanticIcon get() = this + "compress arrows alternate"

    @SemanticIconMarker
    inline val concierge_bell: SemanticIcon get() = this + "concierge bell"

    @SemanticIconMarker
    inline val confluence: SemanticIcon get() = this + "confluence"

    @SemanticIconMarker
    inline val connectdevelop: SemanticIcon get() = this + "connectdevelop"

    @SemanticIconMarker
    inline val contao: SemanticIcon get() = this + "contao"

    @SemanticIconMarker
    inline val content: SemanticIcon get() = this + "content"

    @SemanticIconMarker
    inline val cookie: SemanticIcon get() = this + "cookie"

    @SemanticIconMarker
    inline val cookie_bite: SemanticIcon get() = this + "cookie bite"

    @SemanticIconMarker
    inline val copy: SemanticIcon get() = this + "copy"

    @SemanticIconMarker
    inline val copy_link: SemanticIcon get() = this + "copy link"

    @SemanticIconMarker
    inline val copy_outline: SemanticIcon get() = this + "copy outline"

    @SemanticIconMarker
    inline val copyright: SemanticIcon get() = this + "copyright"

    @SemanticIconMarker
    inline val copyright_outline: SemanticIcon get() = this + "copyright outline"

    @SemanticIconMarker
    inline val corner_add: SemanticIcon get() = this + "corner add"

    @SemanticIconMarker
    inline val cotton_bureau: SemanticIcon get() = this + "cotton bureau"

    @SemanticIconMarker
    inline val couch: SemanticIcon get() = this + "couch"

    @SemanticIconMarker
    inline val counterclockwise_rotated_cloud: SemanticIcon get() = this + "counterclockwise rotated cloud"

    @SemanticIconMarker
    inline val cpanel: SemanticIcon get() = this + "cpanel"

    @SemanticIconMarker
    inline val creative_commons: SemanticIcon get() = this + "creative commons"

    @SemanticIconMarker
    inline val creative_commons_by: SemanticIcon get() = this + "creative commons by"

    @SemanticIconMarker
    inline val creative_commons_nc: SemanticIcon get() = this + "creative commons nc"

    @SemanticIconMarker
    inline val creative_commons_nc_eu: SemanticIcon get() = this + "creative commons nc eu"

    @SemanticIconMarker
    inline val creative_commons_nc_jp: SemanticIcon get() = this + "creative commons nc jp"

    @SemanticIconMarker
    inline val creative_commons_nd: SemanticIcon get() = this + "creative commons nd"

    @SemanticIconMarker
    inline val creative_commons_pd: SemanticIcon get() = this + "creative commons pd"

    @SemanticIconMarker
    inline val creative_commons_pd_alternate: SemanticIcon get() = this + "creative commons pd alternate"

    @SemanticIconMarker
    inline val creative_commons_remix: SemanticIcon get() = this + "creative commons remix"

    @SemanticIconMarker
    inline val creative_commons_sa: SemanticIcon get() = this + "creative commons sa"

    @SemanticIconMarker
    inline val creative_commons_sampling: SemanticIcon get() = this + "creative commons sampling"

    @SemanticIconMarker
    inline val creative_commons_sampling_plus: SemanticIcon get() = this + "creative commons sampling plus"

    @SemanticIconMarker
    inline val creative_commons_share: SemanticIcon get() = this + "creative commons share"

    @SemanticIconMarker
    inline val creative_commons_zero: SemanticIcon get() = this + "creative commons zero"

    @SemanticIconMarker
    inline val credit_card: SemanticIcon get() = this + "credit card"

    @SemanticIconMarker
    inline val credit_card_outline: SemanticIcon get() = this + "credit card outline"

    @SemanticIconMarker
    inline val critical_role: SemanticIcon get() = this + "critical role"

    @SemanticIconMarker
    inline val crop: SemanticIcon get() = this + "crop"

    @SemanticIconMarker
    inline val crop_alternate: SemanticIcon get() = this + "crop alternate"

    @SemanticIconMarker
    inline val cross: SemanticIcon get() = this + "cross"

    @SemanticIconMarker
    inline val crosshairs: SemanticIcon get() = this + "crosshairs"

    @SemanticIconMarker
    inline val crow: SemanticIcon get() = this + "crow"

    @SemanticIconMarker
    inline val crutch: SemanticIcon get() = this + "crutch"

    @SemanticIconMarker
    inline val css3: SemanticIcon get() = this + "css3"

    @SemanticIconMarker
    inline val css3_alternate: SemanticIcon get() = this + "css3 alternate"

    @SemanticIconMarker
    inline val cube: SemanticIcon get() = this + "cube"

    @SemanticIconMarker
    inline val cubes: SemanticIcon get() = this + "cubes"

    @SemanticIconMarker
    inline val cut: SemanticIcon get() = this + "cut"

    @SemanticIconMarker
    inline val cuttlefish: SemanticIcon get() = this + "cuttlefish"

    @SemanticIconMarker
    inline val d_and_d: SemanticIcon get() = this + "d and d"

    @SemanticIconMarker
    inline val d_and_d_beyond: SemanticIcon get() = this + "d and d beyond"

    @SemanticIconMarker
    inline val dailymotion: SemanticIcon get() = this + "dailymotion"

    @SemanticIconMarker
    inline val dashcube: SemanticIcon get() = this + "dashcube"

    @SemanticIconMarker
    inline val database: SemanticIcon get() = this + "database"

    @SemanticIconMarker
    inline val deaf: SemanticIcon get() = this + "deaf"

    @SemanticIconMarker
    inline val delicious: SemanticIcon get() = this + "delicious"

    @SemanticIconMarker
    inline val democrat: SemanticIcon get() = this + "democrat"

    @SemanticIconMarker
    inline val deploydog: SemanticIcon get() = this + "deploydog"

    @SemanticIconMarker
    inline val deskpro: SemanticIcon get() = this + "deskpro"

    @SemanticIconMarker
    inline val desktop: SemanticIcon get() = this + "desktop"

    @SemanticIconMarker
    inline val dev: SemanticIcon get() = this + "dev"

    @SemanticIconMarker
    inline val deviantart: SemanticIcon get() = this + "deviantart"

    @SemanticIconMarker
    inline val dharmachakra: SemanticIcon get() = this + "dharmachakra"

    @SemanticIconMarker
    inline val dhl: SemanticIcon get() = this + "dhl"

    @SemanticIconMarker
    inline val diagnoses: SemanticIcon get() = this + "diagnoses"

    @SemanticIconMarker
    inline val diaspora: SemanticIcon get() = this + "diaspora"

    @SemanticIconMarker
    inline val dice: SemanticIcon get() = this + "dice"

    @SemanticIconMarker
    inline val dice_d20: SemanticIcon get() = this + "dice d20"

    @SemanticIconMarker
    inline val dice_d6: SemanticIcon get() = this + "dice d6"

    @SemanticIconMarker
    inline val dice_five: SemanticIcon get() = this + "dice five"

    @SemanticIconMarker
    inline val dice_four: SemanticIcon get() = this + "dice four"

    @SemanticIconMarker
    inline val dice_one: SemanticIcon get() = this + "dice one"

    @SemanticIconMarker
    inline val dice_six: SemanticIcon get() = this + "dice six"

    @SemanticIconMarker
    inline val dice_three: SemanticIcon get() = this + "dice three"

    @SemanticIconMarker
    inline val dice_two: SemanticIcon get() = this + "dice two"

    @SemanticIconMarker
    inline val digg: SemanticIcon get() = this + "digg"

    @SemanticIconMarker
    inline val digital_ocean: SemanticIcon get() = this + "digital ocean"

    @SemanticIconMarker
    inline val digital_tachograph: SemanticIcon get() = this + "digital tachograph"

    @SemanticIconMarker
    inline val directions: SemanticIcon get() = this + "directions"

    @SemanticIconMarker
    inline val disabled_users: SemanticIcon get() = this + "disabled users"

    @SemanticIconMarker
    inline val discord: SemanticIcon get() = this + "discord"

    @SemanticIconMarker
    inline val discourse: SemanticIcon get() = this + "discourse"

    @SemanticIconMarker
    inline val disease: SemanticIcon get() = this + "disease"

    @SemanticIconMarker
    inline val divide: SemanticIcon get() = this + "divide"

    @SemanticIconMarker
    inline val dizzy: SemanticIcon get() = this + "dizzy"

    @SemanticIconMarker
    inline val dizzy_outline: SemanticIcon get() = this + "dizzy outline"

    @SemanticIconMarker
    inline val dna: SemanticIcon get() = this + "dna"

    @SemanticIconMarker
    inline val dochub: SemanticIcon get() = this + "dochub"

    @SemanticIconMarker
    inline val docker: SemanticIcon get() = this + "docker"

    @SemanticIconMarker
    inline val dog: SemanticIcon get() = this + "dog"

    @SemanticIconMarker
    inline val dollar_sign: SemanticIcon get() = this + "dollar sign"

    @SemanticIconMarker
    inline val dolly: SemanticIcon get() = this + "dolly"

    @SemanticIconMarker
    inline val dolly_flatbed: SemanticIcon get() = this + "dolly flatbed"

    @SemanticIconMarker
    inline val donate: SemanticIcon get() = this + "donate"

    @SemanticIconMarker
    inline val door_closed: SemanticIcon get() = this + "door closed"

    @SemanticIconMarker
    inline val door_open: SemanticIcon get() = this + "door open"

    @SemanticIconMarker
    inline val dot_circle: SemanticIcon get() = this + "dot circle"

    @SemanticIconMarker
    inline val dot_circle_outline: SemanticIcon get() = this + "dot circle outline"

    @SemanticIconMarker
    inline val dove: SemanticIcon get() = this + "dove"

    @SemanticIconMarker
    inline val download: SemanticIcon get() = this + "download"

    @SemanticIconMarker
    inline val draft2digital: SemanticIcon get() = this + "draft2digital"

    @SemanticIconMarker
    inline val drafting_compass: SemanticIcon get() = this + "drafting compass"

    @SemanticIconMarker
    inline val dragon: SemanticIcon get() = this + "dragon"

    @SemanticIconMarker
    inline val draw_polygon: SemanticIcon get() = this + "draw polygon"

    @SemanticIconMarker
    inline val dribbble: SemanticIcon get() = this + "dribbble"

    @SemanticIconMarker
    inline val dribbble_square: SemanticIcon get() = this + "dribbble square"

    @SemanticIconMarker
    inline val dropbox: SemanticIcon get() = this + "dropbox"

    @SemanticIconMarker
    inline val dropdown: SemanticIcon get() = this + "dropdown"

    @SemanticIconMarker
    inline val drum: SemanticIcon get() = this + "drum"

    @SemanticIconMarker
    inline val drum_steelpan: SemanticIcon get() = this + "drum steelpan"

    @SemanticIconMarker
    inline val drumstick_bite: SemanticIcon get() = this + "drumstick bite"

    @SemanticIconMarker
    inline val drupal: SemanticIcon get() = this + "drupal"

    @SemanticIconMarker
    inline val dumbbell: SemanticIcon get() = this + "dumbbell"

    @SemanticIconMarker
    inline val dumpster: SemanticIcon get() = this + "dumpster"

    @SemanticIconMarker
    inline val dungeon: SemanticIcon get() = this + "dungeon"

    @SemanticIconMarker
    inline val dyalog: SemanticIcon get() = this + "dyalog"

    @SemanticIconMarker
    inline val earlybirds: SemanticIcon get() = this + "earlybirds"

    @SemanticIconMarker
    inline val ebay: SemanticIcon get() = this + "ebay"

    @SemanticIconMarker
    inline val edge: SemanticIcon get() = this + "edge"

    @SemanticIconMarker
    inline val edit: SemanticIcon get() = this + "edit"

    @SemanticIconMarker
    inline val edit_outline: SemanticIcon get() = this + "edit outline"

    @SemanticIconMarker
    inline val egg: SemanticIcon get() = this + "egg"

    @SemanticIconMarker
    inline val eject: SemanticIcon get() = this + "eject"

    @SemanticIconMarker
    inline val elementor: SemanticIcon get() = this + "elementor"

    @SemanticIconMarker
    inline val ellipsis_horizontal: SemanticIcon get() = this + "ellipsis horizontal"

    @SemanticIconMarker
    inline val ellipsis_vertical: SemanticIcon get() = this + "ellipsis vertical"

    @SemanticIconMarker
    inline val ello: SemanticIcon get() = this + "ello"

    @SemanticIconMarker
    inline val ember: SemanticIcon get() = this + "ember"

    @SemanticIconMarker
    inline val empire: SemanticIcon get() = this + "empire"

    @SemanticIconMarker
    inline val envelope: SemanticIcon get() = this + "envelope"

    @SemanticIconMarker
    inline val envelope_open: SemanticIcon get() = this + "envelope open"

    @SemanticIconMarker
    inline val envelope_open_outline: SemanticIcon get() = this + "envelope open outline"

    @SemanticIconMarker
    inline val envelope_open_text: SemanticIcon get() = this + "envelope open text"

    @SemanticIconMarker
    inline val envelope_outline: SemanticIcon get() = this + "envelope outline"

    @SemanticIconMarker
    inline val envelope_square: SemanticIcon get() = this + "envelope square"

    @SemanticIconMarker
    inline val envira: SemanticIcon get() = this + "envira"

    @SemanticIconMarker
    inline val equals_: SemanticIcon get() = this + "equals"

    @SemanticIconMarker
    inline val eraser: SemanticIcon get() = this + "eraser"

    @SemanticIconMarker
    inline val erlang: SemanticIcon get() = this + "erlang"

    @SemanticIconMarker
    inline val ethereum: SemanticIcon get() = this + "ethereum"

    @SemanticIconMarker
    inline val ethernet: SemanticIcon get() = this + "ethernet"

    @SemanticIconMarker
    inline val etsy: SemanticIcon get() = this + "etsy"

    @SemanticIconMarker
    inline val euro_sign: SemanticIcon get() = this + "euro sign"

    @SemanticIconMarker
    inline val evernote: SemanticIcon get() = this + "evernote"

    @SemanticIconMarker
    inline val exchange_alternate: SemanticIcon get() = this + "exchange alternate"

    @SemanticIconMarker
    inline val exclamation: SemanticIcon get() = this + "exclamation"

    @SemanticIconMarker
    inline val exclamation_circle: SemanticIcon get() = this + "exclamation circle"

    @SemanticIconMarker
    inline val exclamation_triangle: SemanticIcon get() = this + "exclamation triangle"

    @SemanticIconMarker
    inline val expand: SemanticIcon get() = this + "expand"

    @SemanticIconMarker
    inline val expand_alternate: SemanticIcon get() = this + "expand alternate"

    @SemanticIconMarker
    inline val expand_arrows_alternate: SemanticIcon get() = this + "expand arrows alternate"

    @SemanticIconMarker
    inline val expeditedssl: SemanticIcon get() = this + "expeditedssl"

    @SemanticIconMarker
    inline val external_alternate: SemanticIcon get() = this + "external alternate"

    @SemanticIconMarker
    inline val external_link_square_alternate: SemanticIcon get() = this + "external link square alternate"

    @SemanticIconMarker
    inline val eye: SemanticIcon get() = this + "eye"

    @SemanticIconMarker
    inline val eye_dropper: SemanticIcon get() = this + "eye dropper"

    @SemanticIconMarker
    inline val eye_outline: SemanticIcon get() = this + "eye outline"

    @SemanticIconMarker
    inline val eye_slash: SemanticIcon get() = this + "eye slash"

    @SemanticIconMarker
    inline val eye_slash_outline: SemanticIcon get() = this + "eye slash outline"

    @SemanticIconMarker
    inline val facebook: SemanticIcon get() = this + "facebook"

    @SemanticIconMarker
    inline val facebook_f: SemanticIcon get() = this + "facebook f"

    @SemanticIconMarker
    inline val facebook_messenger: SemanticIcon get() = this + "facebook messenger"

    @SemanticIconMarker
    inline val facebook_square: SemanticIcon get() = this + "facebook square"

    @SemanticIconMarker
    inline val fan: SemanticIcon get() = this + "fan"

    @SemanticIconMarker
    inline val fantasy_flight_games: SemanticIcon get() = this + "fantasy flight games"

    @SemanticIconMarker
    inline val fast_backward: SemanticIcon get() = this + "fast backward"

    @SemanticIconMarker
    inline val fast_forward: SemanticIcon get() = this + "fast forward"

    @SemanticIconMarker
    inline val faucet: SemanticIcon get() = this + "faucet"

    @SemanticIconMarker
    inline val fax: SemanticIcon get() = this + "fax"

    @SemanticIconMarker
    inline val feather: SemanticIcon get() = this + "feather"

    @SemanticIconMarker
    inline val feather_alternate: SemanticIcon get() = this + "feather alternate"

    @SemanticIconMarker
    inline val fedex: SemanticIcon get() = this + "fedex"

    @SemanticIconMarker
    inline val fedora: SemanticIcon get() = this + "fedora"

    @SemanticIconMarker
    inline val female: SemanticIcon get() = this + "female"

    @SemanticIconMarker
    inline val fighter_jet: SemanticIcon get() = this + "fighter jet"

    @SemanticIconMarker
    inline val figma: SemanticIcon get() = this + "figma"

    @SemanticIconMarker
    inline val file: SemanticIcon get() = this + "file"

    @SemanticIconMarker
    inline val file_alternate: SemanticIcon get() = this + "file alternate"

    @SemanticIconMarker
    inline val file_alternate_outline: SemanticIcon get() = this + "file alternate outline"

    @SemanticIconMarker
    inline val file_archive: SemanticIcon get() = this + "file archive"

    @SemanticIconMarker
    inline val file_archive_outline: SemanticIcon get() = this + "file archive outline"

    @SemanticIconMarker
    inline val file_audio: SemanticIcon get() = this + "file audio"

    @SemanticIconMarker
    inline val file_audio_outline: SemanticIcon get() = this + "file audio outline"

    @SemanticIconMarker
    inline val file_code: SemanticIcon get() = this + "file code"

    @SemanticIconMarker
    inline val file_code_outline: SemanticIcon get() = this + "file code outline"

    @SemanticIconMarker
    inline val file_contract: SemanticIcon get() = this + "file contract"

    @SemanticIconMarker
    inline val file_download: SemanticIcon get() = this + "file download"

    @SemanticIconMarker
    inline val file_excel: SemanticIcon get() = this + "file excel"

    @SemanticIconMarker
    inline val file_excel_outline: SemanticIcon get() = this + "file excel outline"

    @SemanticIconMarker
    inline val file_export: SemanticIcon get() = this + "file export"

    @SemanticIconMarker
    inline val file_image: SemanticIcon get() = this + "file image"

    @SemanticIconMarker
    inline val file_image_outline: SemanticIcon get() = this + "file image outline"

    @SemanticIconMarker
    inline val file_import: SemanticIcon get() = this + "file import"

    @SemanticIconMarker
    inline val file_invoice: SemanticIcon get() = this + "file invoice"

    @SemanticIconMarker
    inline val file_invoice_dollar: SemanticIcon get() = this + "file invoice dollar"

    @SemanticIconMarker
    inline val file_medical: SemanticIcon get() = this + "file medical"

    @SemanticIconMarker
    inline val file_medical_alternate: SemanticIcon get() = this + "file medical alternate"

    @SemanticIconMarker
    inline val file_outline: SemanticIcon get() = this + "file outline"

    @SemanticIconMarker
    inline val file_pdf: SemanticIcon get() = this + "file pdf"

    @SemanticIconMarker
    inline val file_pdf_outline: SemanticIcon get() = this + "file pdf outline"

    @SemanticIconMarker
    inline val file_powerpoint: SemanticIcon get() = this + "file powerpoint"

    @SemanticIconMarker
    inline val file_powerpoint_outline: SemanticIcon get() = this + "file powerpoint outline"

    @SemanticIconMarker
    inline val file_prescription: SemanticIcon get() = this + "file prescription"

    @SemanticIconMarker
    inline val file_signature: SemanticIcon get() = this + "file signature"

    @SemanticIconMarker
    inline val file_upload: SemanticIcon get() = this + "file upload"

    @SemanticIconMarker
    inline val file_video: SemanticIcon get() = this + "file video"

    @SemanticIconMarker
    inline val file_video_outline: SemanticIcon get() = this + "file video outline"

    @SemanticIconMarker
    inline val file_word: SemanticIcon get() = this + "file word"

    @SemanticIconMarker
    inline val file_word_outline: SemanticIcon get() = this + "file word outline"

    @SemanticIconMarker
    inline val fill: SemanticIcon get() = this + "fill"

    @SemanticIconMarker
    inline val fill_drip: SemanticIcon get() = this + "fill drip"

    @SemanticIconMarker
    inline val film: SemanticIcon get() = this + "film"

    @SemanticIconMarker
    inline val filter: SemanticIcon get() = this + "filter"

    @SemanticIconMarker
    inline val fingerprint: SemanticIcon get() = this + "fingerprint"

    @SemanticIconMarker
    inline val fire: SemanticIcon get() = this + "fire"

    @SemanticIconMarker
    inline val fire_alternate: SemanticIcon get() = this + "fire alternate"

    @SemanticIconMarker
    inline val fire_extinguisher: SemanticIcon get() = this + "fire extinguisher"

    @SemanticIconMarker
    inline val firefox: SemanticIcon get() = this + "firefox"

    @SemanticIconMarker
    inline val firefox_browser: SemanticIcon get() = this + "firefox browser"

    @SemanticIconMarker
    inline val first_aid: SemanticIcon get() = this + "first aid"

    @SemanticIconMarker
    inline val first_order: SemanticIcon get() = this + "first order"

    @SemanticIconMarker
    inline val first_order_alternate: SemanticIcon get() = this + "first order alternate"

    @SemanticIconMarker
    inline val firstdraft: SemanticIcon get() = this + "firstdraft"

    @SemanticIconMarker
    inline val fish: SemanticIcon get() = this + "fish"

    @SemanticIconMarker
    inline val fist_raised: SemanticIcon get() = this + "fist raised"

    @SemanticIconMarker
    inline val fitted_help: SemanticIcon get() = this + "fitted help"

    @SemanticIconMarker
    inline val fitted_small_linkify: SemanticIcon get() = this + "fitted small linkify"

    @SemanticIconMarker
    inline val flag: SemanticIcon get() = this + "flag"

    @SemanticIconMarker
    inline val flag_checkered: SemanticIcon get() = this + "flag checkered"

    @SemanticIconMarker
    inline val flag_outline: SemanticIcon get() = this + "flag outline"

    @SemanticIconMarker
    inline val flag_usa: SemanticIcon get() = this + "flag usa"

    @SemanticIconMarker
    inline val flask: SemanticIcon get() = this + "flask"

    @SemanticIconMarker
    inline val flickr: SemanticIcon get() = this + "flickr"

    @SemanticIconMarker
    inline val flipboard: SemanticIcon get() = this + "flipboard"

    @SemanticIconMarker
    inline val flushed: SemanticIcon get() = this + "flushed"

    @SemanticIconMarker
    inline val flushed_outline: SemanticIcon get() = this + "flushed outline"

    @SemanticIconMarker
    inline val fly: SemanticIcon get() = this + "fly"

    @SemanticIconMarker
    inline val folder: SemanticIcon get() = this + "folder"

    @SemanticIconMarker
    inline val folder_minus: SemanticIcon get() = this + "folder minus"

    @SemanticIconMarker
    inline val folder_open: SemanticIcon get() = this + "folder open"

    @SemanticIconMarker
    inline val folder_open_outline: SemanticIcon get() = this + "folder open outline"

    @SemanticIconMarker
    inline val folder_outline: SemanticIcon get() = this + "folder outline"

    @SemanticIconMarker
    inline val folder_plus: SemanticIcon get() = this + "folder plus"

    @SemanticIconMarker
    inline val font: SemanticIcon get() = this + "font"

    @SemanticIconMarker
    inline val font_awesome: SemanticIcon get() = this + "font awesome"

    @SemanticIconMarker
    inline val font_awesome_alternate: SemanticIcon get() = this + "font awesome alternate"

    @SemanticIconMarker
    inline val font_awesome_flag: SemanticIcon get() = this + "font awesome flag"

    @SemanticIconMarker
    inline val fonticons: SemanticIcon get() = this + "fonticons"

    @SemanticIconMarker
    inline val fonticons_fi: SemanticIcon get() = this + "fonticons fi"

    @SemanticIconMarker
    inline val football_ball: SemanticIcon get() = this + "football ball"

    @SemanticIconMarker
    inline val fort_awesome: SemanticIcon get() = this + "fort awesome"

    @SemanticIconMarker
    inline val fort_awesome_alternate: SemanticIcon get() = this + "fort awesome alternate"

    @SemanticIconMarker
    inline val forumbee: SemanticIcon get() = this + "forumbee"

    @SemanticIconMarker
    inline val forward: SemanticIcon get() = this + "forward"

    @SemanticIconMarker
    inline val foursquare: SemanticIcon get() = this + "foursquare"

    @SemanticIconMarker
    inline val free_code_camp: SemanticIcon get() = this + "free code camp"

    @SemanticIconMarker
    inline val freebsd: SemanticIcon get() = this + "freebsd"

    @SemanticIconMarker
    inline val frog: SemanticIcon get() = this + "frog"

    @SemanticIconMarker
    inline val frown: SemanticIcon get() = this + "frown"

    @SemanticIconMarker
    inline val frown_open: SemanticIcon get() = this + "frown open"

    @SemanticIconMarker
    inline val frown_open_outline: SemanticIcon get() = this + "frown open outline"

    @SemanticIconMarker
    inline val frown_outline: SemanticIcon get() = this + "frown outline"

    @SemanticIconMarker
    inline val fruitapple: SemanticIcon get() = this + "fruit-apple"

    @SemanticIconMarker
    inline val fulcrum: SemanticIcon get() = this + "fulcrum"

    @SemanticIconMarker
    inline val funnel_dollar: SemanticIcon get() = this + "funnel dollar"

    @SemanticIconMarker
    inline val futbol: SemanticIcon get() = this + "futbol"

    @SemanticIconMarker
    inline val futbol_outline: SemanticIcon get() = this + "futbol outline"

    @SemanticIconMarker
    inline val galactic_republic: SemanticIcon get() = this + "galactic republic"

    @SemanticIconMarker
    inline val galactic_senate: SemanticIcon get() = this + "galactic senate"

    @SemanticIconMarker
    inline val gamepad: SemanticIcon get() = this + "gamepad"

    @SemanticIconMarker
    inline val gas_pump: SemanticIcon get() = this + "gas pump"

    @SemanticIconMarker
    inline val gavel: SemanticIcon get() = this + "gavel"

    @SemanticIconMarker
    inline val gem: SemanticIcon get() = this + "gem"

    @SemanticIconMarker
    inline val gem_outline: SemanticIcon get() = this + "gem outline"

    @SemanticIconMarker
    inline val genderless: SemanticIcon get() = this + "genderless"

    @SemanticIconMarker
    inline val get_pocket: SemanticIcon get() = this + "get pocket"

    @SemanticIconMarker
    inline val gg: SemanticIcon get() = this + "gg"

    @SemanticIconMarker
    inline val gg_circle: SemanticIcon get() = this + "gg circle"

    @SemanticIconMarker
    inline val ghost: SemanticIcon get() = this + "ghost"

    @SemanticIconMarker
    inline val gift: SemanticIcon get() = this + "gift"

    @SemanticIconMarker
    inline val gifts: SemanticIcon get() = this + "gifts"

    @SemanticIconMarker
    inline val git: SemanticIcon get() = this + "git"

    @SemanticIconMarker
    inline val git_alternate: SemanticIcon get() = this + "git alternate"

    @SemanticIconMarker
    inline val git_square: SemanticIcon get() = this + "git square"

    @SemanticIconMarker
    inline val github: SemanticIcon get() = this + "github"

    @SemanticIconMarker
    inline val github_alternate: SemanticIcon get() = this + "github alternate"

    @SemanticIconMarker
    inline val github_square: SemanticIcon get() = this + "github square"

    @SemanticIconMarker
    inline val gitkraken: SemanticIcon get() = this + "gitkraken"

    @SemanticIconMarker
    inline val gitlab: SemanticIcon get() = this + "gitlab"

    @SemanticIconMarker
    inline val gitter: SemanticIcon get() = this + "gitter"

    @SemanticIconMarker
    inline val glass_cheers: SemanticIcon get() = this + "glass cheers"

    @SemanticIconMarker
    inline val glass_martini: SemanticIcon get() = this + "glass martini"

    @SemanticIconMarker
    inline val glass_martini_alternate: SemanticIcon get() = this + "glass martini alternate"

    @SemanticIconMarker
    inline val glass_whiskey: SemanticIcon get() = this + "glass whiskey"

    @SemanticIconMarker
    inline val glasses: SemanticIcon get() = this + "glasses"

    @SemanticIconMarker
    inline val glide: SemanticIcon get() = this + "glide"

    @SemanticIconMarker
    inline val glide_g: SemanticIcon get() = this + "glide g"

    @SemanticIconMarker
    inline val globe: SemanticIcon get() = this + "globe"

    @SemanticIconMarker
    inline val globe_africa: SemanticIcon get() = this + "globe africa"

    @SemanticIconMarker
    inline val globe_americas: SemanticIcon get() = this + "globe americas"

    @SemanticIconMarker
    inline val globe_asia: SemanticIcon get() = this + "globe asia"

    @SemanticIconMarker
    inline val globe_europe: SemanticIcon get() = this + "globe europe"

    @SemanticIconMarker
    inline val gofore: SemanticIcon get() = this + "gofore"

    @SemanticIconMarker
    inline val golf_ball: SemanticIcon get() = this + "golf ball"

    @SemanticIconMarker
    inline val goodreads: SemanticIcon get() = this + "goodreads"

    @SemanticIconMarker
    inline val goodreads_g: SemanticIcon get() = this + "goodreads g"

    @SemanticIconMarker
    inline val google: SemanticIcon get() = this + "google"

    @SemanticIconMarker
    inline val google_drive: SemanticIcon get() = this + "google drive"

    @SemanticIconMarker
    inline val google_play: SemanticIcon get() = this + "google play"

    @SemanticIconMarker
    inline val google_plus: SemanticIcon get() = this + "google plus"

    @SemanticIconMarker
    inline val google_plus_g: SemanticIcon get() = this + "google plus g"

    @SemanticIconMarker
    inline val google_plus_square: SemanticIcon get() = this + "google plus square"

    @SemanticIconMarker
    inline val google_wallet: SemanticIcon get() = this + "google wallet"

    @SemanticIconMarker
    inline val gopuram: SemanticIcon get() = this + "gopuram"

    @SemanticIconMarker
    inline val graduation_cap: SemanticIcon get() = this + "graduation cap"

    @SemanticIconMarker
    inline val gratipay: SemanticIcon get() = this + "gratipay"

    @SemanticIconMarker
    inline val grav: SemanticIcon get() = this + "grav"

    @SemanticIconMarker
    inline val greater_than: SemanticIcon get() = this + "greater than"

    @SemanticIconMarker
    inline val greater_than_equal: SemanticIcon get() = this + "greater than equal"

    @SemanticIconMarker
    inline val green_users: SemanticIcon get() = this + "green users"

    @SemanticIconMarker
    inline val grey_users: SemanticIcon get() = this + "grey users"

    @SemanticIconMarker
    inline val grimace: SemanticIcon get() = this + "grimace"

    @SemanticIconMarker
    inline val grimace_outline: SemanticIcon get() = this + "grimace outline"

    @SemanticIconMarker
    inline val grin: SemanticIcon get() = this + "grin"

    @SemanticIconMarker
    inline val grin_alternate: SemanticIcon get() = this + "grin alternate"

    @SemanticIconMarker
    inline val grin_alternate_outline: SemanticIcon get() = this + "grin alternate outline"

    @SemanticIconMarker
    inline val grin_beam: SemanticIcon get() = this + "grin beam"

    @SemanticIconMarker
    inline val grin_beam_outline: SemanticIcon get() = this + "grin beam outline"

    @SemanticIconMarker
    inline val grin_beam_sweat: SemanticIcon get() = this + "grin beam sweat"

    @SemanticIconMarker
    inline val grin_beam_sweat_outline: SemanticIcon get() = this + "grin beam sweat outline"

    @SemanticIconMarker
    inline val grin_hearts: SemanticIcon get() = this + "grin hearts"

    @SemanticIconMarker
    inline val grin_hearts_outline: SemanticIcon get() = this + "grin hearts outline"

    @SemanticIconMarker
    inline val grin_outline: SemanticIcon get() = this + "grin outline"

    @SemanticIconMarker
    inline val grin_squint: SemanticIcon get() = this + "grin squint"

    @SemanticIconMarker
    inline val grin_squint_outline: SemanticIcon get() = this + "grin squint outline"

    @SemanticIconMarker
    inline val grin_squint_tears: SemanticIcon get() = this + "grin squint tears"

    @SemanticIconMarker
    inline val grin_squint_tears_outline: SemanticIcon get() = this + "grin squint tears outline"

    @SemanticIconMarker
    inline val grin_stars: SemanticIcon get() = this + "grin stars"

    @SemanticIconMarker
    inline val grin_stars_outline: SemanticIcon get() = this + "grin stars outline"

    @SemanticIconMarker
    inline val grin_tears: SemanticIcon get() = this + "grin tears"

    @SemanticIconMarker
    inline val grin_tears_outline: SemanticIcon get() = this + "grin tears outline"

    @SemanticIconMarker
    inline val grin_tongue: SemanticIcon get() = this + "grin tongue"

    @SemanticIconMarker
    inline val grin_tongue_outline: SemanticIcon get() = this + "grin tongue outline"

    @SemanticIconMarker
    inline val grin_tongue_squint: SemanticIcon get() = this + "grin tongue squint"

    @SemanticIconMarker
    inline val grin_tongue_squint_outline: SemanticIcon get() = this + "grin tongue squint outline"

    @SemanticIconMarker
    inline val grin_tongue_wink: SemanticIcon get() = this + "grin tongue wink"

    @SemanticIconMarker
    inline val grin_tongue_wink_outline: SemanticIcon get() = this + "grin tongue wink outline"

    @SemanticIconMarker
    inline val grin_wink: SemanticIcon get() = this + "grin wink"

    @SemanticIconMarker
    inline val grin_wink_outline: SemanticIcon get() = this + "grin wink outline"

    @SemanticIconMarker
    inline val grip_horizontal: SemanticIcon get() = this + "grip horizontal"

    @SemanticIconMarker
    inline val grip_lines: SemanticIcon get() = this + "grip lines"

    @SemanticIconMarker
    inline val grip_lines_vertical: SemanticIcon get() = this + "grip lines vertical"

    @SemanticIconMarker
    inline val grip_vertical: SemanticIcon get() = this + "grip vertical"

    @SemanticIconMarker
    inline val gripfire: SemanticIcon get() = this + "gripfire"

    @SemanticIconMarker
    inline val grunt: SemanticIcon get() = this + "grunt"

    @SemanticIconMarker
    inline val guitar: SemanticIcon get() = this + "guitar"

    @SemanticIconMarker
    inline val gulp: SemanticIcon get() = this + "gulp"

    @SemanticIconMarker
    inline val h_square: SemanticIcon get() = this + "h square"

    @SemanticIconMarker
    inline val hacker_news: SemanticIcon get() = this + "hacker news"

    @SemanticIconMarker
    inline val hacker_news_square: SemanticIcon get() = this + "hacker news square"

    @SemanticIconMarker
    inline val hackerrank: SemanticIcon get() = this + "hackerrank"

    @SemanticIconMarker
    inline val hamburger: SemanticIcon get() = this + "hamburger"

    @SemanticIconMarker
    inline val hammer: SemanticIcon get() = this + "hammer"

    @SemanticIconMarker
    inline val hamsa: SemanticIcon get() = this + "hamsa"

    @SemanticIconMarker
    inline val hand_holding: SemanticIcon get() = this + "hand holding"

    @SemanticIconMarker
    inline val hand_holding_heart: SemanticIcon get() = this + "hand holding heart"

    @SemanticIconMarker
    inline val hand_holding_medical: SemanticIcon get() = this + "hand holding medical"

    @SemanticIconMarker
    inline val hand_holding_usd: SemanticIcon get() = this + "hand holding usd"

    @SemanticIconMarker
    inline val hand_holding_water: SemanticIcon get() = this + "hand holding water"

    @SemanticIconMarker
    inline val hand_lizard: SemanticIcon get() = this + "hand lizard"

    @SemanticIconMarker
    inline val hand_lizard_outline: SemanticIcon get() = this + "hand lizard outline"

    @SemanticIconMarker
    inline val hand_middle_finger: SemanticIcon get() = this + "hand middle finger"

    @SemanticIconMarker
    inline val hand_paper: SemanticIcon get() = this + "hand paper"

    @SemanticIconMarker
    inline val hand_paper_outline: SemanticIcon get() = this + "hand paper outline"

    @SemanticIconMarker
    inline val hand_peace: SemanticIcon get() = this + "hand peace"

    @SemanticIconMarker
    inline val hand_peace_outline: SemanticIcon get() = this + "hand peace outline"

    @SemanticIconMarker
    inline val hand_point_down: SemanticIcon get() = this + "hand point down"

    @SemanticIconMarker
    inline val hand_point_down_outline: SemanticIcon get() = this + "hand point down outline"

    @SemanticIconMarker
    inline val hand_point_left: SemanticIcon get() = this + "hand point left"

    @SemanticIconMarker
    inline val hand_point_left_outline: SemanticIcon get() = this + "hand point left outline"

    @SemanticIconMarker
    inline val hand_point_right: SemanticIcon get() = this + "hand point right"

    @SemanticIconMarker
    inline val hand_point_right_outline: SemanticIcon get() = this + "hand point right outline"

    @SemanticIconMarker
    inline val hand_point_up: SemanticIcon get() = this + "hand point up"

    @SemanticIconMarker
    inline val hand_point_up_outline: SemanticIcon get() = this + "hand point up outline"

    @SemanticIconMarker
    inline val hand_pointer: SemanticIcon get() = this + "hand pointer"

    @SemanticIconMarker
    inline val hand_pointer_outline: SemanticIcon get() = this + "hand pointer outline"

    @SemanticIconMarker
    inline val hand_rock: SemanticIcon get() = this + "hand rock"

    @SemanticIconMarker
    inline val hand_rock_outline: SemanticIcon get() = this + "hand rock outline"

    @SemanticIconMarker
    inline val hand_scissors: SemanticIcon get() = this + "hand scissors"

    @SemanticIconMarker
    inline val hand_scissors_outline: SemanticIcon get() = this + "hand scissors outline"

    @SemanticIconMarker
    inline val hand_sparkles: SemanticIcon get() = this + "hand sparkles"

    @SemanticIconMarker
    inline val hand_spock: SemanticIcon get() = this + "hand spock"

    @SemanticIconMarker
    inline val hand_spock_outline: SemanticIcon get() = this + "hand spock outline"

    @SemanticIconMarker
    inline val hands: SemanticIcon get() = this + "hands"

    @SemanticIconMarker
    inline val hands_helping: SemanticIcon get() = this + "hands helping"

    @SemanticIconMarker
    inline val hands_wash: SemanticIcon get() = this + "hands wash"

    @SemanticIconMarker
    inline val handshake: SemanticIcon get() = this + "handshake"

    @SemanticIconMarker
    inline val handshake_alternate_slash: SemanticIcon get() = this + "handshake alternate slash"

    @SemanticIconMarker
    inline val handshake_outline: SemanticIcon get() = this + "handshake outline"

    @SemanticIconMarker
    inline val handshake_slash: SemanticIcon get() = this + "handshake slash"

    @SemanticIconMarker
    inline val hanukiah: SemanticIcon get() = this + "hanukiah"

    @SemanticIconMarker
    inline val hard_hat: SemanticIcon get() = this + "hard hat"

    @SemanticIconMarker
    inline val hashtag: SemanticIcon get() = this + "hashtag"

    @SemanticIconMarker
    inline val hat_cowboy: SemanticIcon get() = this + "hat cowboy"

    @SemanticIconMarker
    inline val hat_cowboy_side: SemanticIcon get() = this + "hat cowboy side"

    @SemanticIconMarker
    inline val hat_wizard: SemanticIcon get() = this + "hat wizard"

    @SemanticIconMarker
    inline val hdd: SemanticIcon get() = this + "hdd"

    @SemanticIconMarker
    inline val hdd_outline: SemanticIcon get() = this + "hdd outline"

    @SemanticIconMarker
    inline val head_side_cough: SemanticIcon get() = this + "head side cough"

    @SemanticIconMarker
    inline val head_side_cough_slash: SemanticIcon get() = this + "head side cough slash"

    @SemanticIconMarker
    inline val head_side_mask: SemanticIcon get() = this + "head side mask"

    @SemanticIconMarker
    inline val head_side_virus: SemanticIcon get() = this + "head side virus"

    @SemanticIconMarker
    inline val heading: SemanticIcon get() = this + "heading"

    @SemanticIconMarker
    inline val headphones: SemanticIcon get() = this + "headphones"

    @SemanticIconMarker
    inline val headphones_alternate: SemanticIcon get() = this + "headphones alternate"

    @SemanticIconMarker
    inline val headset: SemanticIcon get() = this + "headset"

    @SemanticIconMarker
    inline val heart: SemanticIcon get() = this + "heart"

    @SemanticIconMarker
    inline val heart_broken: SemanticIcon get() = this + "heart broken"

    @SemanticIconMarker
    inline val heart_outline: SemanticIcon get() = this + "heart outline"

    @SemanticIconMarker
    inline val heartbeat: SemanticIcon get() = this + "heartbeat"

    @SemanticIconMarker
    inline val helicopter: SemanticIcon get() = this + "helicopter"

    @SemanticIconMarker
    inline val help_link: SemanticIcon get() = this + "help link"

    @SemanticIconMarker
    inline val highlighter: SemanticIcon get() = this + "highlighter"

    @SemanticIconMarker
    inline val hiking: SemanticIcon get() = this + "hiking"

    @SemanticIconMarker
    inline val hippo: SemanticIcon get() = this + "hippo"

    @SemanticIconMarker
    inline val hips: SemanticIcon get() = this + "hips"

    @SemanticIconMarker
    inline val hire_a_helper: SemanticIcon get() = this + "hire a helper"

    @SemanticIconMarker
    inline val history: SemanticIcon get() = this + "history"

    @SemanticIconMarker
    inline val hockey_puck: SemanticIcon get() = this + "hockey puck"

    @SemanticIconMarker
    inline val holly_berry: SemanticIcon get() = this + "holly berry"

    @SemanticIconMarker
    inline val home: SemanticIcon get() = this + "home"

    @SemanticIconMarker
    inline val hooli: SemanticIcon get() = this + "hooli"

    @SemanticIconMarker
    inline val horizontally_flipped_cloud: SemanticIcon get() = this + "horizontally flipped cloud"

    @SemanticIconMarker
    inline val hornbill: SemanticIcon get() = this + "hornbill"

    @SemanticIconMarker
    inline val horse: SemanticIcon get() = this + "horse"

    @SemanticIconMarker
    inline val horse_head: SemanticIcon get() = this + "horse head"

    @SemanticIconMarker
    inline val hospital: SemanticIcon get() = this + "hospital"

    @SemanticIconMarker
    inline val hospital_alternate: SemanticIcon get() = this + "hospital alternate"

    @SemanticIconMarker
    inline val hospital_outline: SemanticIcon get() = this + "hospital outline"

    @SemanticIconMarker
    inline val hospital_symbol: SemanticIcon get() = this + "hospital symbol"

    @SemanticIconMarker
    inline val hospital_user: SemanticIcon get() = this + "hospital user"

    @SemanticIconMarker
    inline val hot_tub: SemanticIcon get() = this + "hot tub"

    @SemanticIconMarker
    inline val hotdog: SemanticIcon get() = this + "hotdog"

    @SemanticIconMarker
    inline val hotel: SemanticIcon get() = this + "hotel"

    @SemanticIconMarker
    inline val hotjar: SemanticIcon get() = this + "hotjar"

    @SemanticIconMarker
    inline val hourglass: SemanticIcon get() = this + "hourglass"

    @SemanticIconMarker
    inline val hourglass_end: SemanticIcon get() = this + "hourglass end"

    @SemanticIconMarker
    inline val hourglass_half: SemanticIcon get() = this + "hourglass half"

    @SemanticIconMarker
    inline val hourglass_outline: SemanticIcon get() = this + "hourglass outline"

    @SemanticIconMarker
    inline val hourglass_start: SemanticIcon get() = this + "hourglass start"

    @SemanticIconMarker
    inline val house_damage: SemanticIcon get() = this + "house damage"

    @SemanticIconMarker
    inline val house_user: SemanticIcon get() = this + "house user"

    @SemanticIconMarker
    inline val houzz: SemanticIcon get() = this + "houzz"

    @SemanticIconMarker
    inline val hryvnia: SemanticIcon get() = this + "hryvnia"

    @SemanticIconMarker
    inline val html5: SemanticIcon get() = this + "html5"

    @SemanticIconMarker
    inline val hubspot: SemanticIcon get() = this + "hubspot"

    @SemanticIconMarker
    inline val huge_home: SemanticIcon get() = this + "huge home"

    @SemanticIconMarker
    inline val i_cursor: SemanticIcon get() = this + "i cursor"

    @SemanticIconMarker
    inline val ice_cream: SemanticIcon get() = this + "ice cream"

    @SemanticIconMarker
    inline val icicles: SemanticIcon get() = this + "icicles"

    @SemanticIconMarker
    inline val icons: SemanticIcon get() = this + "icons"

    @SemanticIconMarker
    inline val id_badge: SemanticIcon get() = this + "id badge"

    @SemanticIconMarker
    inline val id_badge_outline: SemanticIcon get() = this + "id badge outline"

    @SemanticIconMarker
    inline val id_card: SemanticIcon get() = this + "id card"

    @SemanticIconMarker
    inline val id_card_alternate: SemanticIcon get() = this + "id card alternate"

    @SemanticIconMarker
    inline val id_card_outline: SemanticIcon get() = this + "id card outline"

    @SemanticIconMarker
    inline val ideal: SemanticIcon get() = this + "ideal"

    @SemanticIconMarker
    inline val igloo: SemanticIcon get() = this + "igloo"

    @SemanticIconMarker
    inline val image: SemanticIcon get() = this + "image"

    @SemanticIconMarker
    inline val image_outline: SemanticIcon get() = this + "image outline"

    @SemanticIconMarker
    inline val images: SemanticIcon get() = this + "images"

    @SemanticIconMarker
    inline val images_outline: SemanticIcon get() = this + "images outline"

    @SemanticIconMarker
    inline val imdb: SemanticIcon get() = this + "imdb"

    @SemanticIconMarker
    inline val inbox: SemanticIcon get() = this + "inbox"

    @SemanticIconMarker
    inline val indent: SemanticIcon get() = this + "indent"

    @SemanticIconMarker
    inline val industry: SemanticIcon get() = this + "industry"

    @SemanticIconMarker
    inline val infinity: SemanticIcon get() = this + "infinity"

    @SemanticIconMarker
    inline val info: SemanticIcon get() = this + "info"

    @SemanticIconMarker
    inline val info_circle: SemanticIcon get() = this + "info circle"

    @SemanticIconMarker
    inline val instagram: SemanticIcon get() = this + "instagram"

    @SemanticIconMarker
    inline val instagram_square: SemanticIcon get() = this + "instagram square"

    @SemanticIconMarker
    inline val intercom: SemanticIcon get() = this + "intercom"

    @SemanticIconMarker
    inline val internet_explorer: SemanticIcon get() = this + "internet explorer"

    @SemanticIconMarker
    inline val inverted_blue_users: SemanticIcon get() = this + "inverted blue users"

    @SemanticIconMarker
    inline val inverted_brown_users: SemanticIcon get() = this + "inverted brown users"

    @SemanticIconMarker
    inline val inverted_corner_add: SemanticIcon get() = this + "inverted corner add"

    @SemanticIconMarker
    inline val inverted_green_users: SemanticIcon get() = this + "inverted green users"

    @SemanticIconMarker
    inline val inverted_grey_users: SemanticIcon get() = this + "inverted grey users"

    @SemanticIconMarker
    inline val inverted_olive_users: SemanticIcon get() = this + "inverted olive users"

    @SemanticIconMarker
    inline val inverted_orange_users: SemanticIcon get() = this + "inverted orange users"

    @SemanticIconMarker
    inline val inverted_pink_users: SemanticIcon get() = this + "inverted pink users"

    @SemanticIconMarker
    inline val inverted_primary_users: SemanticIcon get() = this + "inverted primary users"

    @SemanticIconMarker
    inline val inverted_purple_users: SemanticIcon get() = this + "inverted purple users"

    @SemanticIconMarker
    inline val inverted_red_users: SemanticIcon get() = this + "inverted red users"

    @SemanticIconMarker
    inline val inverted_secondary_users: SemanticIcon get() = this + "inverted secondary users"

    @SemanticIconMarker
    inline val inverted_teal_users: SemanticIcon get() = this + "inverted teal users"

    @SemanticIconMarker
    inline val inverted_users: SemanticIcon get() = this + "inverted users"

    @SemanticIconMarker
    inline val inverted_violet_users: SemanticIcon get() = this + "inverted violet users"

    @SemanticIconMarker
    inline val inverted_yellow_users: SemanticIcon get() = this + "inverted yellow users"

    @SemanticIconMarker
    inline val invision: SemanticIcon get() = this + "invision"

    @SemanticIconMarker
    inline val ioxhost: SemanticIcon get() = this + "ioxhost"

    @SemanticIconMarker
    inline val italic: SemanticIcon get() = this + "italic"

    @SemanticIconMarker
    inline val itch_io: SemanticIcon get() = this + "itch io"

    @SemanticIconMarker
    inline val itunes: SemanticIcon get() = this + "itunes"

    @SemanticIconMarker
    inline val itunes_note: SemanticIcon get() = this + "itunes note"

    @SemanticIconMarker
    inline val java: SemanticIcon get() = this + "java"

    @SemanticIconMarker
    inline val jedi: SemanticIcon get() = this + "jedi"

    @SemanticIconMarker
    inline val jedi_order: SemanticIcon get() = this + "jedi order"

    @SemanticIconMarker
    inline val jenkins: SemanticIcon get() = this + "jenkins"

    @SemanticIconMarker
    inline val jira: SemanticIcon get() = this + "jira"

    @SemanticIconMarker
    inline val joget: SemanticIcon get() = this + "joget"

    @SemanticIconMarker
    inline val joint: SemanticIcon get() = this + "joint"

    @SemanticIconMarker
    inline val joomla: SemanticIcon get() = this + "joomla"

    @SemanticIconMarker
    inline val journal_whills: SemanticIcon get() = this + "journal whills"

    @SemanticIconMarker
    inline val js: SemanticIcon get() = this + "js"

    @SemanticIconMarker
    inline val js_square: SemanticIcon get() = this + "js square"

    @SemanticIconMarker
    inline val jsfiddle: SemanticIcon get() = this + "jsfiddle"

    @SemanticIconMarker
    inline val kaaba: SemanticIcon get() = this + "kaaba"

    @SemanticIconMarker
    inline val kaggle: SemanticIcon get() = this + "kaggle"

    @SemanticIconMarker
    inline val key: SemanticIcon get() = this + "key"

    @SemanticIconMarker
    inline val keybase: SemanticIcon get() = this + "keybase"

    @SemanticIconMarker
    inline val keyboard: SemanticIcon get() = this + "keyboard"

    @SemanticIconMarker
    inline val keyboard_outline: SemanticIcon get() = this + "keyboard outline"

    @SemanticIconMarker
    inline val keycdn: SemanticIcon get() = this + "keycdn"

    @SemanticIconMarker
    inline val khanda: SemanticIcon get() = this + "khanda"

    @SemanticIconMarker
    inline val kickstarter: SemanticIcon get() = this + "kickstarter"

    @SemanticIconMarker
    inline val kickstarter_k: SemanticIcon get() = this + "kickstarter k"

    @SemanticIconMarker
    inline val kiss: SemanticIcon get() = this + "kiss"

    @SemanticIconMarker
    inline val kiss_beam: SemanticIcon get() = this + "kiss beam"

    @SemanticIconMarker
    inline val kiss_beam_outline: SemanticIcon get() = this + "kiss beam outline"

    @SemanticIconMarker
    inline val kiss_outline: SemanticIcon get() = this + "kiss outline"

    @SemanticIconMarker
    inline val kiss_wink_heart: SemanticIcon get() = this + "kiss wink heart"

    @SemanticIconMarker
    inline val kiss_wink_heart_outline: SemanticIcon get() = this + "kiss wink heart outline"

    @SemanticIconMarker
    inline val kiwi_bird: SemanticIcon get() = this + "kiwi bird"

    @SemanticIconMarker
    inline val korvue: SemanticIcon get() = this + "korvue"

    @SemanticIconMarker
    inline val landmark: SemanticIcon get() = this + "landmark"

    @SemanticIconMarker
    inline val language: SemanticIcon get() = this + "language"

    @SemanticIconMarker
    inline val laptop: SemanticIcon get() = this + "laptop"

    @SemanticIconMarker
    inline val laptop_code: SemanticIcon get() = this + "laptop code"

    @SemanticIconMarker
    inline val laptop_house: SemanticIcon get() = this + "laptop house"

    @SemanticIconMarker
    inline val laptop_medical: SemanticIcon get() = this + "laptop medical"

    @SemanticIconMarker
    inline val laravel: SemanticIcon get() = this + "laravel"

    @SemanticIconMarker
    inline val large_home: SemanticIcon get() = this + "large home"

    @SemanticIconMarker
    inline val lastfm: SemanticIcon get() = this + "lastfm"

    @SemanticIconMarker
    inline val lastfm_square: SemanticIcon get() = this + "lastfm square"

    @SemanticIconMarker
    inline val laugh: SemanticIcon get() = this + "laugh"

    @SemanticIconMarker
    inline val laugh_beam: SemanticIcon get() = this + "laugh beam"

    @SemanticIconMarker
    inline val laugh_beam_outline: SemanticIcon get() = this + "laugh beam outline"

    @SemanticIconMarker
    inline val laugh_outline: SemanticIcon get() = this + "laugh outline"

    @SemanticIconMarker
    inline val laugh_squint: SemanticIcon get() = this + "laugh squint"

    @SemanticIconMarker
    inline val laugh_squint_outline: SemanticIcon get() = this + "laugh squint outline"

    @SemanticIconMarker
    inline val laugh_wink: SemanticIcon get() = this + "laugh wink"

    @SemanticIconMarker
    inline val laugh_wink_outline: SemanticIcon get() = this + "laugh wink outline"

    @SemanticIconMarker
    inline val layer_group: SemanticIcon get() = this + "layer group"

    @SemanticIconMarker
    inline val leaf: SemanticIcon get() = this + "leaf"

    @SemanticIconMarker
    inline val leanpub: SemanticIcon get() = this + "leanpub"

    @SemanticIconMarker
    inline val lemon: SemanticIcon get() = this + "lemon"

    @SemanticIconMarker
    inline val lemon_outline: SemanticIcon get() = this + "lemon outline"

    @SemanticIconMarker
    inline val less_than: SemanticIcon get() = this + "less than"

    @SemanticIconMarker
    inline val less_than_equal: SemanticIcon get() = this + "less than equal"

    @SemanticIconMarker
    inline val lesscss: SemanticIcon get() = this + "lesscss"

    @SemanticIconMarker
    inline val level_down_alternate: SemanticIcon get() = this + "level down alternate"

    @SemanticIconMarker
    inline val level_up_alternate: SemanticIcon get() = this + "level up alternate"

    @SemanticIconMarker
    inline val life_ring: SemanticIcon get() = this + "life ring"

    @SemanticIconMarker
    inline val life_ring_outline: SemanticIcon get() = this + "life ring outline"

    @SemanticIconMarker
    inline val lightbulb: SemanticIcon get() = this + "lightbulb"

    @SemanticIconMarker
    inline val lightbulb_outline: SemanticIcon get() = this + "lightbulb outline"

    @SemanticIconMarker
    inline val linechat: SemanticIcon get() = this + "linechat"

    @SemanticIconMarker
    inline val linkedin: SemanticIcon get() = this + "linkedin"

    @SemanticIconMarker
    inline val linkedin_in: SemanticIcon get() = this + "linkedin in"

    @SemanticIconMarker
    inline val linkify: SemanticIcon get() = this + "linkify"

    @SemanticIconMarker
    inline val linode: SemanticIcon get() = this + "linode"

    @SemanticIconMarker
    inline val linux: SemanticIcon get() = this + "linux"

    @SemanticIconMarker
    inline val lira_sign: SemanticIcon get() = this + "lira sign"

    @SemanticIconMarker
    inline val list: SemanticIcon get() = this + "list"

    @SemanticIconMarker
    inline val list_alternate: SemanticIcon get() = this + "list alternate"

    @SemanticIconMarker
    inline val list_alternate_outline: SemanticIcon get() = this + "list alternate outline"

    @SemanticIconMarker
    inline val list_ol: SemanticIcon get() = this + "list ol"

    @SemanticIconMarker
    inline val list_ul: SemanticIcon get() = this + "list ul"

    @SemanticIconMarker
    inline val location_arrow: SemanticIcon get() = this + "location arrow"

    @SemanticIconMarker
    inline val lock: SemanticIcon get() = this + "lock"

    @SemanticIconMarker
    inline val lock_open: SemanticIcon get() = this + "lock open"

    @SemanticIconMarker
    inline val long_arrow_alternate_down: SemanticIcon get() = this + "long arrow alternate down"

    @SemanticIconMarker
    inline val long_arrow_alternate_left: SemanticIcon get() = this + "long arrow alternate left"

    @SemanticIconMarker
    inline val long_arrow_alternate_right: SemanticIcon get() = this + "long arrow alternate right"

    @SemanticIconMarker
    inline val long_arrow_alternate_up: SemanticIcon get() = this + "long arrow alternate up"

    @SemanticIconMarker
    inline val low_vision: SemanticIcon get() = this + "low vision"

    @SemanticIconMarker
    inline val luggage_cart: SemanticIcon get() = this + "luggage cart"

    @SemanticIconMarker
    inline val lungs: SemanticIcon get() = this + "lungs"

    @SemanticIconMarker
    inline val lungs_virus: SemanticIcon get() = this + "lungs virus"

    @SemanticIconMarker
    inline val lyft: SemanticIcon get() = this + "lyft"

    @SemanticIconMarker
    inline val magento: SemanticIcon get() = this + "magento"

    @SemanticIconMarker
    inline val magic: SemanticIcon get() = this + "magic"

    @SemanticIconMarker
    inline val magnet: SemanticIcon get() = this + "magnet"

    @SemanticIconMarker
    inline val mail: SemanticIcon get() = this + "mail"

    @SemanticIconMarker
    inline val mail_bulk: SemanticIcon get() = this + "mail bulk"

    @SemanticIconMarker
    inline val mailchimp: SemanticIcon get() = this + "mailchimp"

    @SemanticIconMarker
    inline val male: SemanticIcon get() = this + "male"

    @SemanticIconMarker
    inline val mandalorian: SemanticIcon get() = this + "mandalorian"

    @SemanticIconMarker
    inline val map: SemanticIcon get() = this + "map"

    @SemanticIconMarker
    inline val map_marked: SemanticIcon get() = this + "map marked"

    @SemanticIconMarker
    inline val map_marked_alternate: SemanticIcon get() = this + "map marked alternate"

    @SemanticIconMarker
    inline val map_marker: SemanticIcon get() = this + "map marker"

    @SemanticIconMarker
    inline val map_marker_alternate: SemanticIcon get() = this + "map marker alternate"

    @SemanticIconMarker
    inline val map_outline: SemanticIcon get() = this + "map outline"

    @SemanticIconMarker
    inline val map_pin: SemanticIcon get() = this + "map pin"

    @SemanticIconMarker
    inline val map_signs: SemanticIcon get() = this + "map signs"

    @SemanticIconMarker
    inline val markdown: SemanticIcon get() = this + "markdown"

    @SemanticIconMarker
    inline val marker: SemanticIcon get() = this + "marker"

    @SemanticIconMarker
    inline val mars: SemanticIcon get() = this + "mars"

    @SemanticIconMarker
    inline val mars_double: SemanticIcon get() = this + "mars double"

    @SemanticIconMarker
    inline val mars_stroke: SemanticIcon get() = this + "mars stroke"

    @SemanticIconMarker
    inline val mars_stroke_horizontal: SemanticIcon get() = this + "mars stroke horizontal"

    @SemanticIconMarker
    inline val mars_stroke_vertical: SemanticIcon get() = this + "mars stroke vertical"

    @SemanticIconMarker
    inline val mask: SemanticIcon get() = this + "mask"

    @SemanticIconMarker
    inline val massive_home: SemanticIcon get() = this + "massive home"

    @SemanticIconMarker
    inline val mastodon: SemanticIcon get() = this + "mastodon"

    @SemanticIconMarker
    inline val maxcdn: SemanticIcon get() = this + "maxcdn"

    @SemanticIconMarker
    inline val mdb: SemanticIcon get() = this + "mdb"

    @SemanticIconMarker
    inline val medal: SemanticIcon get() = this + "medal"

    @SemanticIconMarker
    inline val medapps: SemanticIcon get() = this + "medapps"

    @SemanticIconMarker
    inline val medium_m: SemanticIcon get() = this + "medium m"

    @SemanticIconMarker
    inline val medkit: SemanticIcon get() = this + "medkit"

    @SemanticIconMarker
    inline val medrt: SemanticIcon get() = this + "medrt"

    @SemanticIconMarker
    inline val meetup: SemanticIcon get() = this + "meetup"

    @SemanticIconMarker
    inline val megaport: SemanticIcon get() = this + "megaport"

    @SemanticIconMarker
    inline val meh: SemanticIcon get() = this + "meh"

    @SemanticIconMarker
    inline val meh_blank: SemanticIcon get() = this + "meh blank"

    @SemanticIconMarker
    inline val meh_blank_outline: SemanticIcon get() = this + "meh blank outline"

    @SemanticIconMarker
    inline val meh_outline: SemanticIcon get() = this + "meh outline"

    @SemanticIconMarker
    inline val meh_rolling_eyes: SemanticIcon get() = this + "meh rolling eyes"

    @SemanticIconMarker
    inline val meh_rolling_eyes_outline: SemanticIcon get() = this + "meh rolling eyes outline"

    @SemanticIconMarker
    inline val memory: SemanticIcon get() = this + "memory"

    @SemanticIconMarker
    inline val mendeley: SemanticIcon get() = this + "mendeley"

    @SemanticIconMarker
    inline val menorah: SemanticIcon get() = this + "menorah"

    @SemanticIconMarker
    inline val mercury: SemanticIcon get() = this + "mercury"

    @SemanticIconMarker
    inline val meteor: SemanticIcon get() = this + "meteor"

    @SemanticIconMarker
    inline val microblog: SemanticIcon get() = this + "microblog"

    @SemanticIconMarker
    inline val microchip: SemanticIcon get() = this + "microchip"

    @SemanticIconMarker
    inline val microphone: SemanticIcon get() = this + "microphone"

    @SemanticIconMarker
    inline val microphone_alternate: SemanticIcon get() = this + "microphone alternate"

    @SemanticIconMarker
    inline val microphone_alternate_slash: SemanticIcon get() = this + "microphone alternate slash"

    @SemanticIconMarker
    inline val microphone_slash: SemanticIcon get() = this + "microphone slash"

    @SemanticIconMarker
    inline val microscope: SemanticIcon get() = this + "microscope"

    @SemanticIconMarker
    inline val microsoft: SemanticIcon get() = this + "microsoft"

    @SemanticIconMarker
    inline val mini_home: SemanticIcon get() = this + "mini home"

    @SemanticIconMarker
    inline val minus: SemanticIcon get() = this + "minus"

    @SemanticIconMarker
    inline val minus_circle: SemanticIcon get() = this + "minus circle"

    @SemanticIconMarker
    inline val minus_square: SemanticIcon get() = this + "minus square"

    @SemanticIconMarker
    inline val minus_square_outline: SemanticIcon get() = this + "minus square outline"

    @SemanticIconMarker
    inline val mitten: SemanticIcon get() = this + "mitten"

    @SemanticIconMarker
    inline val mix: SemanticIcon get() = this + "mix"

    @SemanticIconMarker
    inline val mixcloud: SemanticIcon get() = this + "mixcloud"

    @SemanticIconMarker
    inline val mixer: SemanticIcon get() = this + "mixer"

    @SemanticIconMarker
    inline val mizuni: SemanticIcon get() = this + "mizuni"

    @SemanticIconMarker
    inline val mobile: SemanticIcon get() = this + "mobile"

    @SemanticIconMarker
    inline val mobile_alternate: SemanticIcon get() = this + "mobile alternate"

    @SemanticIconMarker
    inline val modx: SemanticIcon get() = this + "modx"

    @SemanticIconMarker
    inline val monero: SemanticIcon get() = this + "monero"

    @SemanticIconMarker
    inline val money_bill: SemanticIcon get() = this + "money bill"

    @SemanticIconMarker
    inline val money_bill_alternate: SemanticIcon get() = this + "money bill alternate"

    @SemanticIconMarker
    inline val money_bill_alternate_outline: SemanticIcon get() = this + "money bill alternate outline"

    @SemanticIconMarker
    inline val money_bill_wave: SemanticIcon get() = this + "money bill wave"

    @SemanticIconMarker
    inline val money_bill_wave_alternate: SemanticIcon get() = this + "money bill wave alternate"

    @SemanticIconMarker
    inline val money_check: SemanticIcon get() = this + "money check"

    @SemanticIconMarker
    inline val money_check_alternate: SemanticIcon get() = this + "money check alternate"

    @SemanticIconMarker
    inline val monument: SemanticIcon get() = this + "monument"

    @SemanticIconMarker
    inline val moon: SemanticIcon get() = this + "moon"

    @SemanticIconMarker
    inline val moon_outline: SemanticIcon get() = this + "moon outline"

    @SemanticIconMarker
    inline val mortar_pestle: SemanticIcon get() = this + "mortar pestle"

    @SemanticIconMarker
    inline val mosque: SemanticIcon get() = this + "mosque"

    @SemanticIconMarker
    inline val motorcycle: SemanticIcon get() = this + "motorcycle"

    @SemanticIconMarker
    inline val mountain: SemanticIcon get() = this + "mountain"

    @SemanticIconMarker
    inline val mouse: SemanticIcon get() = this + "mouse"

    @SemanticIconMarker
    inline val mouse_pointer: SemanticIcon get() = this + "mouse pointer"

    @SemanticIconMarker
    inline val mug_hot: SemanticIcon get() = this + "mug hot"

    @SemanticIconMarker
    inline val music: SemanticIcon get() = this + "music"

    @SemanticIconMarker
    inline val napster: SemanticIcon get() = this + "napster"

    @SemanticIconMarker
    inline val neos: SemanticIcon get() = this + "neos"

    @SemanticIconMarker
    inline val neuter: SemanticIcon get() = this + "neuter"

    @SemanticIconMarker
    inline val newspaper: SemanticIcon get() = this + "newspaper"

    @SemanticIconMarker
    inline val newspaper_outline: SemanticIcon get() = this + "newspaper outline"

    @SemanticIconMarker
    inline val nimblr: SemanticIcon get() = this + "nimblr"

    @SemanticIconMarker
    inline val node: SemanticIcon get() = this + "node"

    @SemanticIconMarker
    inline val node_js: SemanticIcon get() = this + "node js"

    @SemanticIconMarker
    inline val not_equal: SemanticIcon get() = this + "not equal"

    @SemanticIconMarker
    inline val notched_circle_loading: SemanticIcon get() = this + "notched circle loading"

    @SemanticIconMarker
    inline val notes_medical: SemanticIcon get() = this + "notes medical"

    @SemanticIconMarker
    inline val npm: SemanticIcon get() = this + "npm"

    @SemanticIconMarker
    inline val ns8: SemanticIcon get() = this + "ns8"

    @SemanticIconMarker
    inline val nutritionix: SemanticIcon get() = this + "nutritionix"

    @SemanticIconMarker
    inline val object_group: SemanticIcon get() = this + "object group"

    @SemanticIconMarker
    inline val object_group_outline: SemanticIcon get() = this + "object group outline"

    @SemanticIconMarker
    inline val object_ungroup: SemanticIcon get() = this + "object ungroup"

    @SemanticIconMarker
    inline val object_ungroup_outline: SemanticIcon get() = this + "object ungroup outline"

    @SemanticIconMarker
    inline val odnoklassniki: SemanticIcon get() = this + "odnoklassniki"

    @SemanticIconMarker
    inline val odnoklassniki_square: SemanticIcon get() = this + "odnoklassniki square"

    @SemanticIconMarker
    inline val oil_can: SemanticIcon get() = this + "oil can"

    @SemanticIconMarker
    inline val old_republic: SemanticIcon get() = this + "old republic"

    @SemanticIconMarker
    inline val olive_users: SemanticIcon get() = this + "olive users"

    @SemanticIconMarker
    inline val om: SemanticIcon get() = this + "om"

    @SemanticIconMarker
    inline val opencart: SemanticIcon get() = this + "opencart"

    @SemanticIconMarker
    inline val openid: SemanticIcon get() = this + "openid"

    @SemanticIconMarker
    inline val opera: SemanticIcon get() = this + "opera"

    @SemanticIconMarker
    inline val optin_monster: SemanticIcon get() = this + "optin monster"

    @SemanticIconMarker
    inline val orange_users: SemanticIcon get() = this + "orange users"

    @SemanticIconMarker
    inline val orcid: SemanticIcon get() = this + "orcid"

    @SemanticIconMarker
    inline val osi: SemanticIcon get() = this + "osi"

    @SemanticIconMarker
    inline val otter: SemanticIcon get() = this + "otter"

    @SemanticIconMarker
    inline val outdent: SemanticIcon get() = this + "outdent"

    @SemanticIconMarker
    inline val page4: SemanticIcon get() = this + "page4"

    @SemanticIconMarker
    inline val pagelines: SemanticIcon get() = this + "pagelines"

    @SemanticIconMarker
    inline val pager: SemanticIcon get() = this + "pager"

    @SemanticIconMarker
    inline val paint_brush: SemanticIcon get() = this + "paint brush"

    @SemanticIconMarker
    inline val paint_roller: SemanticIcon get() = this + "paint roller"

    @SemanticIconMarker
    inline val palette: SemanticIcon get() = this + "palette"

    @SemanticIconMarker
    inline val palfed: SemanticIcon get() = this + "palfed"

    @SemanticIconMarker
    inline val pallet: SemanticIcon get() = this + "pallet"

    @SemanticIconMarker
    inline val paper_plane: SemanticIcon get() = this + "paper plane"

    @SemanticIconMarker
    inline val paper_plane_outline: SemanticIcon get() = this + "paper plane outline"

    @SemanticIconMarker
    inline val paperclip: SemanticIcon get() = this + "paperclip"

    @SemanticIconMarker
    inline val parachute_box: SemanticIcon get() = this + "parachute box"

    @SemanticIconMarker
    inline val paragraph: SemanticIcon get() = this + "paragraph"

    @SemanticIconMarker
    inline val parking: SemanticIcon get() = this + "parking"

    @SemanticIconMarker
    inline val passport: SemanticIcon get() = this + "passport"

    @SemanticIconMarker
    inline val pastafarianism: SemanticIcon get() = this + "pastafarianism"

    @SemanticIconMarker
    inline val paste: SemanticIcon get() = this + "paste"

    @SemanticIconMarker
    inline val patreon: SemanticIcon get() = this + "patreon"

    @SemanticIconMarker
    inline val pause: SemanticIcon get() = this + "pause"

    @SemanticIconMarker
    inline val pause_circle: SemanticIcon get() = this + "pause circle"

    @SemanticIconMarker
    inline val pause_circle_outline: SemanticIcon get() = this + "pause circle outline"

    @SemanticIconMarker
    inline val paw: SemanticIcon get() = this + "paw"

    @SemanticIconMarker
    inline val paypal: SemanticIcon get() = this + "paypal"

    @SemanticIconMarker
    inline val peace: SemanticIcon get() = this + "peace"

    @SemanticIconMarker
    inline val pen: SemanticIcon get() = this + "pen"

    @SemanticIconMarker
    inline val pen_alternate: SemanticIcon get() = this + "pen alternate"

    @SemanticIconMarker
    inline val pen_fancy: SemanticIcon get() = this + "pen fancy"

    @SemanticIconMarker
    inline val pen_nib: SemanticIcon get() = this + "pen nib"

    @SemanticIconMarker
    inline val pen_square: SemanticIcon get() = this + "pen square"

    @SemanticIconMarker
    inline val pencil_alternate: SemanticIcon get() = this + "pencil alternate"

    @SemanticIconMarker
    inline val pencil_ruler: SemanticIcon get() = this + "pencil ruler"

    @SemanticIconMarker
    inline val penny_arcade: SemanticIcon get() = this + "penny arcade"

    @SemanticIconMarker
    inline val people_arrows: SemanticIcon get() = this + "people arrows"

    @SemanticIconMarker
    inline val people_carry: SemanticIcon get() = this + "people carry"

    @SemanticIconMarker
    inline val pepper_hot: SemanticIcon get() = this + "pepper hot"

    @SemanticIconMarker
    inline val percent: SemanticIcon get() = this + "percent"

    @SemanticIconMarker
    inline val percentage: SemanticIcon get() = this + "percentage"

    @SemanticIconMarker
    inline val periscope: SemanticIcon get() = this + "periscope"

    @SemanticIconMarker
    inline val person_booth: SemanticIcon get() = this + "person booth"

    @SemanticIconMarker
    inline val phabricator: SemanticIcon get() = this + "phabricator"

    @SemanticIconMarker
    inline val phoenix_framework: SemanticIcon get() = this + "phoenix framework"

    @SemanticIconMarker
    inline val phoenix_squadron: SemanticIcon get() = this + "phoenix squadron"

    @SemanticIconMarker
    inline val phone: SemanticIcon get() = this + "phone"

    @SemanticIconMarker
    inline val phone_alternate: SemanticIcon get() = this + "phone alternate"

    @SemanticIconMarker
    inline val phone_slash: SemanticIcon get() = this + "phone slash"

    @SemanticIconMarker
    inline val phone_square: SemanticIcon get() = this + "phone square"

    @SemanticIconMarker
    inline val phone_square_alternate: SemanticIcon get() = this + "phone square alternate"

    @SemanticIconMarker
    inline val phone_volume: SemanticIcon get() = this + "phone volume"

    @SemanticIconMarker
    inline val photo_video: SemanticIcon get() = this + "photo video"

    @SemanticIconMarker
    inline val php: SemanticIcon get() = this + "php"

    @SemanticIconMarker
    inline val pied_piper: SemanticIcon get() = this + "pied piper"

    @SemanticIconMarker
    inline val pied_piper_alternate: SemanticIcon get() = this + "pied piper alternate"

    @SemanticIconMarker
    inline val pied_piper_hat: SemanticIcon get() = this + "pied piper hat"

    @SemanticIconMarker
    inline val pied_piper_pp: SemanticIcon get() = this + "pied piper pp"

    @SemanticIconMarker
    inline val pied_piper_square: SemanticIcon get() = this + "pied piper square"

    @SemanticIconMarker
    inline val piggy_bank: SemanticIcon get() = this + "piggy bank"

    @SemanticIconMarker
    inline val pills: SemanticIcon get() = this + "pills"

    @SemanticIconMarker
    inline val pink_users: SemanticIcon get() = this + "pink users"

    @SemanticIconMarker
    inline val pinterest: SemanticIcon get() = this + "pinterest"

    @SemanticIconMarker
    inline val pinterest_p: SemanticIcon get() = this + "pinterest p"

    @SemanticIconMarker
    inline val pinterest_square: SemanticIcon get() = this + "pinterest square"

    @SemanticIconMarker
    inline val pizza_slice: SemanticIcon get() = this + "pizza slice"

    @SemanticIconMarker
    inline val place_of_worship: SemanticIcon get() = this + "place of worship"

    @SemanticIconMarker
    inline val plane: SemanticIcon get() = this + "plane"

    @SemanticIconMarker
    inline val plane_arrival: SemanticIcon get() = this + "plane arrival"

    @SemanticIconMarker
    inline val plane_departure: SemanticIcon get() = this + "plane departure"

    @SemanticIconMarker
    inline val play: SemanticIcon get() = this + "play"

    @SemanticIconMarker
    inline val play_circle: SemanticIcon get() = this + "play circle"

    @SemanticIconMarker
    inline val play_circle_outline: SemanticIcon get() = this + "play circle outline"

    @SemanticIconMarker
    inline val playstation: SemanticIcon get() = this + "playstation"

    @SemanticIconMarker
    inline val plug: SemanticIcon get() = this + "plug"

    @SemanticIconMarker
    inline val plus: SemanticIcon get() = this + "plus"

    @SemanticIconMarker
    inline val plus_circle: SemanticIcon get() = this + "plus circle"

    @SemanticIconMarker
    inline val plus_square: SemanticIcon get() = this + "plus square"

    @SemanticIconMarker
    inline val plus_square_outline: SemanticIcon get() = this + "plus square outline"

    @SemanticIconMarker
    inline val podcast: SemanticIcon get() = this + "podcast"

    @SemanticIconMarker
    inline val poll: SemanticIcon get() = this + "poll"

    @SemanticIconMarker
    inline val poll_horizontal: SemanticIcon get() = this + "poll horizontal"

    @SemanticIconMarker
    inline val poo: SemanticIcon get() = this + "poo"

    @SemanticIconMarker
    inline val poo_storm: SemanticIcon get() = this + "poo storm"

    @SemanticIconMarker
    inline val poop: SemanticIcon get() = this + "poop"

    @SemanticIconMarker
    inline val portrait: SemanticIcon get() = this + "portrait"

    @SemanticIconMarker
    inline val pound_sign: SemanticIcon get() = this + "pound sign"

    @SemanticIconMarker
    inline val power_off: SemanticIcon get() = this + "power off"

    @SemanticIconMarker
    inline val pray: SemanticIcon get() = this + "pray"

    @SemanticIconMarker
    inline val praying_hands: SemanticIcon get() = this + "praying hands"

    @SemanticIconMarker
    inline val prescription: SemanticIcon get() = this + "prescription"

    @SemanticIconMarker
    inline val prescription_bottle: SemanticIcon get() = this + "prescription bottle"

    @SemanticIconMarker
    inline val prescription_bottle_alternate: SemanticIcon get() = this + "prescription bottle alternate"

    @SemanticIconMarker
    inline val primary_users: SemanticIcon get() = this + "primary users"

    @SemanticIconMarker
    inline val print: SemanticIcon get() = this + "print"

    @SemanticIconMarker
    inline val procedures: SemanticIcon get() = this + "procedures"

    @SemanticIconMarker
    inline val product_hunt: SemanticIcon get() = this + "product hunt"

    @SemanticIconMarker
    inline val project_diagram: SemanticIcon get() = this + "project diagram"

    @SemanticIconMarker
    inline val pump_medical: SemanticIcon get() = this + "pump medical"

    @SemanticIconMarker
    inline val pump_soap: SemanticIcon get() = this + "pump soap"

    @SemanticIconMarker
    inline val purple_users: SemanticIcon get() = this + "purple users"

    @SemanticIconMarker
    inline val pushed: SemanticIcon get() = this + "pushed"

    @SemanticIconMarker
    inline val puzzle: SemanticIcon get() = this + "puzzle"

    @SemanticIconMarker
    inline val puzzle_piece: SemanticIcon get() = this + "puzzle piece"

    @SemanticIconMarker
    inline val python: SemanticIcon get() = this + "python"

    @SemanticIconMarker
    inline val qq: SemanticIcon get() = this + "qq"

    @SemanticIconMarker
    inline val qrcode: SemanticIcon get() = this + "qrcode"

    @SemanticIconMarker
    inline val question: SemanticIcon get() = this + "question"

    @SemanticIconMarker
    inline val question_circle: SemanticIcon get() = this + "question circle"

    @SemanticIconMarker
    inline val question_circle_outline: SemanticIcon get() = this + "question circle outline"

    @SemanticIconMarker
    inline val quidditch: SemanticIcon get() = this + "quidditch"

    @SemanticIconMarker
    inline val quinscape: SemanticIcon get() = this + "quinscape"

    @SemanticIconMarker
    inline val quora: SemanticIcon get() = this + "quora"

    @SemanticIconMarker
    inline val quote_left: SemanticIcon get() = this + "quote left"

    @SemanticIconMarker
    inline val quote_right: SemanticIcon get() = this + "quote right"

    @SemanticIconMarker
    inline val quran: SemanticIcon get() = this + "quran"

    @SemanticIconMarker
    inline val r_project: SemanticIcon get() = this + "r project"

    @SemanticIconMarker
    inline val radiation: SemanticIcon get() = this + "radiation"

    @SemanticIconMarker
    inline val radiation_alternate: SemanticIcon get() = this + "radiation alternate"

    @SemanticIconMarker
    inline val rainbow: SemanticIcon get() = this + "rainbow"

    @SemanticIconMarker
    inline val random: SemanticIcon get() = this + "random"

    @SemanticIconMarker
    inline val raspberry_pi: SemanticIcon get() = this + "raspberry pi"

    @SemanticIconMarker
    inline val ravelry: SemanticIcon get() = this + "ravelry"

    @SemanticIconMarker
    inline val react: SemanticIcon get() = this + "react"

    @SemanticIconMarker
    inline val reacteurope: SemanticIcon get() = this + "reacteurope"

    @SemanticIconMarker
    inline val readme: SemanticIcon get() = this + "readme"

    @SemanticIconMarker
    inline val rebel: SemanticIcon get() = this + "rebel"

    @SemanticIconMarker
    inline val receipt: SemanticIcon get() = this + "receipt"

    @SemanticIconMarker
    inline val record_vinyl: SemanticIcon get() = this + "record vinyl"

    @SemanticIconMarker
    inline val recycle: SemanticIcon get() = this + "recycle"

    @SemanticIconMarker
    inline val red_users: SemanticIcon get() = this + "red users"

    @SemanticIconMarker
    inline val reddit: SemanticIcon get() = this + "reddit"

    @SemanticIconMarker
    inline val reddit_alien: SemanticIcon get() = this + "reddit alien"

    @SemanticIconMarker
    inline val reddit_square: SemanticIcon get() = this + "reddit square"

    @SemanticIconMarker
    inline val redhat: SemanticIcon get() = this + "redhat"

    @SemanticIconMarker
    inline val redo: SemanticIcon get() = this + "redo"

    @SemanticIconMarker
    inline val redo_alternate: SemanticIcon get() = this + "redo alternate"

    @SemanticIconMarker
    inline val redriver: SemanticIcon get() = this + "redriver"

    @SemanticIconMarker
    inline val redyeti: SemanticIcon get() = this + "redyeti"

    @SemanticIconMarker
    inline val registered: SemanticIcon get() = this + "registered"

    @SemanticIconMarker
    inline val registered_outline: SemanticIcon get() = this + "registered outline"

    @SemanticIconMarker
    inline val remove_format: SemanticIcon get() = this + "remove format"

    @SemanticIconMarker
    inline val renren: SemanticIcon get() = this + "renren"

    @SemanticIconMarker
    inline val reply: SemanticIcon get() = this + "reply"

    @SemanticIconMarker
    inline val reply_all: SemanticIcon get() = this + "reply all"

    @SemanticIconMarker
    inline val replyd: SemanticIcon get() = this + "replyd"

    @SemanticIconMarker
    inline val republican: SemanticIcon get() = this + "republican"

    @SemanticIconMarker
    inline val researchgate: SemanticIcon get() = this + "researchgate"

    @SemanticIconMarker
    inline val resolving: SemanticIcon get() = this + "resolving"

    @SemanticIconMarker
    inline val restroom: SemanticIcon get() = this + "restroom"

    @SemanticIconMarker
    inline val retweet: SemanticIcon get() = this + "retweet"

    @SemanticIconMarker
    inline val rev: SemanticIcon get() = this + "rev"

    @SemanticIconMarker
    inline val ribbon: SemanticIcon get() = this + "ribbon"

    @SemanticIconMarker
    inline val ring: SemanticIcon get() = this + "ring"

    @SemanticIconMarker
    inline val road: SemanticIcon get() = this + "road"

    @SemanticIconMarker
    inline val robot: SemanticIcon get() = this + "robot"

    @SemanticIconMarker
    inline val rocket: SemanticIcon get() = this + "rocket"

    @SemanticIconMarker
    inline val rocketchat: SemanticIcon get() = this + "rocketchat"

    @SemanticIconMarker
    inline val rockrms: SemanticIcon get() = this + "rockrms"

    @SemanticIconMarker
    inline val route: SemanticIcon get() = this + "route"

    @SemanticIconMarker
    inline val rss: SemanticIcon get() = this + "rss"

    @SemanticIconMarker
    inline val rss_square: SemanticIcon get() = this + "rss square"

    @SemanticIconMarker
    inline val ruble_sign: SemanticIcon get() = this + "ruble sign"

    @SemanticIconMarker
    inline val ruler: SemanticIcon get() = this + "ruler"

    @SemanticIconMarker
    inline val ruler_combined: SemanticIcon get() = this + "ruler combined"

    @SemanticIconMarker
    inline val ruler_horizontal: SemanticIcon get() = this + "ruler horizontal"

    @SemanticIconMarker
    inline val ruler_vertical: SemanticIcon get() = this + "ruler vertical"

    @SemanticIconMarker
    inline val running: SemanticIcon get() = this + "running"

    @SemanticIconMarker
    inline val rupee_sign: SemanticIcon get() = this + "rupee sign"

    @SemanticIconMarker
    inline val sad_cry: SemanticIcon get() = this + "sad cry"

    @SemanticIconMarker
    inline val sad_cry_outline: SemanticIcon get() = this + "sad cry outline"

    @SemanticIconMarker
    inline val sad_tear: SemanticIcon get() = this + "sad tear"

    @SemanticIconMarker
    inline val sad_tear_outline: SemanticIcon get() = this + "sad tear outline"

    @SemanticIconMarker
    inline val safari: SemanticIcon get() = this + "safari"

    @SemanticIconMarker
    inline val salesforce: SemanticIcon get() = this + "salesforce"

    @SemanticIconMarker
    inline val sass: SemanticIcon get() = this + "sass"

    @SemanticIconMarker
    inline val satellite: SemanticIcon get() = this + "satellite"

    @SemanticIconMarker
    inline val satellite_dish: SemanticIcon get() = this + "satellite dish"

    @SemanticIconMarker
    inline val save: SemanticIcon get() = this + "save"

    @SemanticIconMarker
    inline val save_outline: SemanticIcon get() = this + "save outline"

    @SemanticIconMarker
    inline val schlix: SemanticIcon get() = this + "schlix"

    @SemanticIconMarker
    inline val school: SemanticIcon get() = this + "school"

    @SemanticIconMarker
    inline val screwdriver: SemanticIcon get() = this + "screwdriver"

    @SemanticIconMarker
    inline val scribd: SemanticIcon get() = this + "scribd"

    @SemanticIconMarker
    inline val scroll: SemanticIcon get() = this + "scroll"

    @SemanticIconMarker
    inline val sd_card: SemanticIcon get() = this + "sd card"

    @SemanticIconMarker
    inline val search: SemanticIcon get() = this + "search"

    @SemanticIconMarker
    inline val search_dollar: SemanticIcon get() = this + "search dollar"

    @SemanticIconMarker
    inline val search_location: SemanticIcon get() = this + "search location"

    @SemanticIconMarker
    inline val search_minus: SemanticIcon get() = this + "search minus"

    @SemanticIconMarker
    inline val search_plus: SemanticIcon get() = this + "search plus"

    @SemanticIconMarker
    inline val searchengin: SemanticIcon get() = this + "searchengin"

    @SemanticIconMarker
    inline val secondary_users: SemanticIcon get() = this + "secondary users"

    @SemanticIconMarker
    inline val seedling: SemanticIcon get() = this + "seedling"

    @SemanticIconMarker
    inline val sellcast: SemanticIcon get() = this + "sellcast"

    @SemanticIconMarker
    inline val sellsy: SemanticIcon get() = this + "sellsy"

    @SemanticIconMarker
    inline val server: SemanticIcon get() = this + "server"

    @SemanticIconMarker
    inline val servicestack: SemanticIcon get() = this + "servicestack"

    @SemanticIconMarker
    inline val shapes: SemanticIcon get() = this + "shapes"

    @SemanticIconMarker
    inline val share: SemanticIcon get() = this + "share"

    @SemanticIconMarker
    inline val share_alternate: SemanticIcon get() = this + "share alternate"

    @SemanticIconMarker
    inline val share_alternate_square: SemanticIcon get() = this + "share alternate square"

    @SemanticIconMarker
    inline val share_square: SemanticIcon get() = this + "share square"

    @SemanticIconMarker
    inline val share_square_outline: SemanticIcon get() = this + "share square outline"

    @SemanticIconMarker
    inline val shekel_sign: SemanticIcon get() = this + "shekel sign"

    @SemanticIconMarker
    inline val shield_alternate: SemanticIcon get() = this + "shield alternate"

    @SemanticIconMarker
    inline val shield_virus: SemanticIcon get() = this + "shield virus"

    @SemanticIconMarker
    inline val ship: SemanticIcon get() = this + "ship"

    @SemanticIconMarker
    inline val shipping_fast: SemanticIcon get() = this + "shipping fast"

    @SemanticIconMarker
    inline val shirtsinbulk: SemanticIcon get() = this + "shirtsinbulk"

    @SemanticIconMarker
    inline val shoe_prints: SemanticIcon get() = this + "shoe prints"

    @SemanticIconMarker
    inline val shopify: SemanticIcon get() = this + "shopify"

    @SemanticIconMarker
    inline val shopping_bag: SemanticIcon get() = this + "shopping bag"

    @SemanticIconMarker
    inline val shopping_basket: SemanticIcon get() = this + "shopping basket"

    @SemanticIconMarker
    inline val shopping_cart: SemanticIcon get() = this + "shopping cart"

    @SemanticIconMarker
    inline val shopware: SemanticIcon get() = this + "shopware"

    @SemanticIconMarker
    inline val shower: SemanticIcon get() = this + "shower"

    @SemanticIconMarker
    inline val shuttle_van: SemanticIcon get() = this + "shuttle van"

    @SemanticIconMarker
    inline val sign: SemanticIcon get() = this + "sign"

    @SemanticIconMarker
    inline val sign_in_alternate: SemanticIcon get() = this + "sign in alternate"

    @SemanticIconMarker
    inline val sign_language: SemanticIcon get() = this + "sign language"

    @SemanticIconMarker
    inline val sign_out_alternate: SemanticIcon get() = this + "sign out alternate"

    @SemanticIconMarker
    inline val signal: SemanticIcon get() = this + "signal"

    @SemanticIconMarker
    inline val sim_card: SemanticIcon get() = this + "sim card"

    @SemanticIconMarker
    inline val simplybuilt: SemanticIcon get() = this + "simplybuilt"

    @SemanticIconMarker
    inline val sistrix: SemanticIcon get() = this + "sistrix"

    @SemanticIconMarker
    inline val sitemap: SemanticIcon get() = this + "sitemap"

    @SemanticIconMarker
    inline val sith: SemanticIcon get() = this + "sith"

    @SemanticIconMarker
    inline val skating: SemanticIcon get() = this + "skating"

    @SemanticIconMarker
    inline val sketch: SemanticIcon get() = this + "sketch"

    @SemanticIconMarker
    inline val skiing: SemanticIcon get() = this + "skiing"

    @SemanticIconMarker
    inline val skiing_nordic: SemanticIcon get() = this + "skiing nordic"

    @SemanticIconMarker
    inline val skull_crossbones: SemanticIcon get() = this + "skull crossbones"

    @SemanticIconMarker
    inline val skyatlas: SemanticIcon get() = this + "skyatlas"

    @SemanticIconMarker
    inline val skype: SemanticIcon get() = this + "skype"

    @SemanticIconMarker
    inline val slack: SemanticIcon get() = this + "slack"

    @SemanticIconMarker
    inline val slack_hash: SemanticIcon get() = this + "slack hash"

    @SemanticIconMarker
    inline val slash: SemanticIcon get() = this + "slash"

    @SemanticIconMarker
    inline val sleigh: SemanticIcon get() = this + "sleigh"

    @SemanticIconMarker
    inline val sliders_horizontal: SemanticIcon get() = this + "sliders horizontal"

    @SemanticIconMarker
    inline val slideshare: SemanticIcon get() = this + "slideshare"

    @SemanticIconMarker
    inline val small_home: SemanticIcon get() = this + "small home"

    @SemanticIconMarker
    inline val smile: SemanticIcon get() = this + "smile"

    @SemanticIconMarker
    inline val smile_beam: SemanticIcon get() = this + "smile beam"

    @SemanticIconMarker
    inline val smile_beam_outline: SemanticIcon get() = this + "smile beam outline"

    @SemanticIconMarker
    inline val smile_outline: SemanticIcon get() = this + "smile outline"

    @SemanticIconMarker
    inline val smile_wink: SemanticIcon get() = this + "smile wink"

    @SemanticIconMarker
    inline val smile_wink_outline: SemanticIcon get() = this + "smile wink outline"

    @SemanticIconMarker
    inline val smog: SemanticIcon get() = this + "smog"

    @SemanticIconMarker
    inline val smoking: SemanticIcon get() = this + "smoking"

    @SemanticIconMarker
    inline val smoking_ban: SemanticIcon get() = this + "smoking ban"

    @SemanticIconMarker
    inline val sms: SemanticIcon get() = this + "sms"

    @SemanticIconMarker
    inline val snapchat: SemanticIcon get() = this + "snapchat"

    @SemanticIconMarker
    inline val snapchat_ghost: SemanticIcon get() = this + "snapchat ghost"

    @SemanticIconMarker
    inline val snapchat_square: SemanticIcon get() = this + "snapchat square"

    @SemanticIconMarker
    inline val snowboarding: SemanticIcon get() = this + "snowboarding"

    @SemanticIconMarker
    inline val snowflake: SemanticIcon get() = this + "snowflake"

    @SemanticIconMarker
    inline val snowflake_outline: SemanticIcon get() = this + "snowflake outline"

    @SemanticIconMarker
    inline val snowman: SemanticIcon get() = this + "snowman"

    @SemanticIconMarker
    inline val snowplow: SemanticIcon get() = this + "snowplow"

    @SemanticIconMarker
    inline val soap: SemanticIcon get() = this + "soap"

    @SemanticIconMarker
    inline val socks: SemanticIcon get() = this + "socks"

    @SemanticIconMarker
    inline val solar_panel: SemanticIcon get() = this + "solar panel"

    @SemanticIconMarker
    inline val sort: SemanticIcon get() = this + "sort"

    @SemanticIconMarker
    inline val sort_alphabet_down: SemanticIcon get() = this + "sort alphabet down"

    @SemanticIconMarker
    inline val sort_alphabet_down_alternate: SemanticIcon get() = this + "sort alphabet down alternate"

    @SemanticIconMarker
    inline val sort_alphabet_up: SemanticIcon get() = this + "sort alphabet up"

    @SemanticIconMarker
    inline val sort_alphabet_up_alternate: SemanticIcon get() = this + "sort alphabet up alternate"

    @SemanticIconMarker
    inline val sort_amount_down: SemanticIcon get() = this + "sort amount down"

    @SemanticIconMarker
    inline val sort_amount_down_alternate: SemanticIcon get() = this + "sort amount down alternate"

    @SemanticIconMarker
    inline val sort_amount_up: SemanticIcon get() = this + "sort amount up"

    @SemanticIconMarker
    inline val sort_amount_up_alternate: SemanticIcon get() = this + "sort amount up alternate"

    @SemanticIconMarker
    inline val sort_down: SemanticIcon get() = this + "sort down"

    @SemanticIconMarker
    inline val sort_numeric_down: SemanticIcon get() = this + "sort numeric down"

    @SemanticIconMarker
    inline val sort_numeric_down_alternate: SemanticIcon get() = this + "sort numeric down alternate"

    @SemanticIconMarker
    inline val sort_numeric_up: SemanticIcon get() = this + "sort numeric up"

    @SemanticIconMarker
    inline val sort_numeric_up_alternate: SemanticIcon get() = this + "sort numeric up alternate"

    @SemanticIconMarker
    inline val sort_up: SemanticIcon get() = this + "sort up"

    @SemanticIconMarker
    inline val soundcloud: SemanticIcon get() = this + "soundcloud"

    @SemanticIconMarker
    inline val sourcetree: SemanticIcon get() = this + "sourcetree"

    @SemanticIconMarker
    inline val spa: SemanticIcon get() = this + "spa"

    @SemanticIconMarker
    inline val space_shuttle: SemanticIcon get() = this + "space shuttle"

    @SemanticIconMarker
    inline val speakap: SemanticIcon get() = this + "speakap"

    @SemanticIconMarker
    inline val speaker_deck: SemanticIcon get() = this + "speaker deck"

    @SemanticIconMarker
    inline val spell_check: SemanticIcon get() = this + "spell check"

    @SemanticIconMarker
    inline val spider: SemanticIcon get() = this + "spider"

    @SemanticIconMarker
    inline val spinner: SemanticIcon get() = this + "spinner"

    @SemanticIconMarker
    inline val spinner_loading: SemanticIcon get() = this + "spinner loading"

    @SemanticIconMarker
    inline val splotch: SemanticIcon get() = this + "splotch"

    @SemanticIconMarker
    inline val spotify: SemanticIcon get() = this + "spotify"

    @SemanticIconMarker
    inline val spray_can: SemanticIcon get() = this + "spray can"

    @SemanticIconMarker
    inline val square: SemanticIcon get() = this + "square"

    @SemanticIconMarker
    inline val square_full: SemanticIcon get() = this + "square full"

    @SemanticIconMarker
    inline val square_outline: SemanticIcon get() = this + "square outline"

    @SemanticIconMarker
    inline val square_root_alternate: SemanticIcon get() = this + "square root alternate"

    @SemanticIconMarker
    inline val squarespace: SemanticIcon get() = this + "squarespace"

    @SemanticIconMarker
    inline val stack_exchange: SemanticIcon get() = this + "stack exchange"

    @SemanticIconMarker
    inline val stack_overflow: SemanticIcon get() = this + "stack overflow"

    @SemanticIconMarker
    inline val stackpath: SemanticIcon get() = this + "stackpath"

    @SemanticIconMarker
    inline val stamp: SemanticIcon get() = this + "stamp"

    @SemanticIconMarker
    inline val star: SemanticIcon get() = this + "star"

    @SemanticIconMarker
    inline val star_and_crescent: SemanticIcon get() = this + "star and crescent"

    @SemanticIconMarker
    inline val star_half: SemanticIcon get() = this + "star half"

    @SemanticIconMarker
    inline val star_half_alternate: SemanticIcon get() = this + "star half alternate"

    @SemanticIconMarker
    inline val star_half_outline: SemanticIcon get() = this + "star half outline"

    @SemanticIconMarker
    inline val star_of_david: SemanticIcon get() = this + "star of david"

    @SemanticIconMarker
    inline val star_of_life: SemanticIcon get() = this + "star of life"

    @SemanticIconMarker
    inline val star_outline: SemanticIcon get() = this + "star outline"

    @SemanticIconMarker
    inline val staylinked: SemanticIcon get() = this + "staylinked"

    @SemanticIconMarker
    inline val steam: SemanticIcon get() = this + "steam"

    @SemanticIconMarker
    inline val steam_square: SemanticIcon get() = this + "steam square"

    @SemanticIconMarker
    inline val steam_symbol: SemanticIcon get() = this + "steam symbol"

    @SemanticIconMarker
    inline val step_backward: SemanticIcon get() = this + "step backward"

    @SemanticIconMarker
    inline val step_forward: SemanticIcon get() = this + "step forward"

    @SemanticIconMarker
    inline val stethoscope: SemanticIcon get() = this + "stethoscope"

    @SemanticIconMarker
    inline val sticker_mule: SemanticIcon get() = this + "sticker mule"

    @SemanticIconMarker
    inline val sticky_note: SemanticIcon get() = this + "sticky note"

    @SemanticIconMarker
    inline val sticky_note_outline: SemanticIcon get() = this + "sticky note outline"

    @SemanticIconMarker
    inline val stop: SemanticIcon get() = this + "stop"

    @SemanticIconMarker
    inline val stop_circle: SemanticIcon get() = this + "stop circle"

    @SemanticIconMarker
    inline val stop_circle_outline: SemanticIcon get() = this + "stop circle outline"

    @SemanticIconMarker
    inline val stopwatch: SemanticIcon get() = this + "stopwatch"

    @SemanticIconMarker
    inline val store: SemanticIcon get() = this + "store"

    @SemanticIconMarker
    inline val store_alternate: SemanticIcon get() = this + "store alternate"

    @SemanticIconMarker
    inline val store_alternate_slash: SemanticIcon get() = this + "store alternate slash"

    @SemanticIconMarker
    inline val store_slash: SemanticIcon get() = this + "store slash"

    @SemanticIconMarker
    inline val strava: SemanticIcon get() = this + "strava"

    @SemanticIconMarker
    inline val stream: SemanticIcon get() = this + "stream"

    @SemanticIconMarker
    inline val street_view: SemanticIcon get() = this + "street view"

    @SemanticIconMarker
    inline val strikethrough: SemanticIcon get() = this + "strikethrough"

    @SemanticIconMarker
    inline val stripe: SemanticIcon get() = this + "stripe"

    @SemanticIconMarker
    inline val stripe_s: SemanticIcon get() = this + "stripe s"

    @SemanticIconMarker
    inline val stroopwafel: SemanticIcon get() = this + "stroopwafel"

    @SemanticIconMarker
    inline val studiovinari: SemanticIcon get() = this + "studiovinari"

    @SemanticIconMarker
    inline val stumbleupon: SemanticIcon get() = this + "stumbleupon"

    @SemanticIconMarker
    inline val stumbleupon_circle: SemanticIcon get() = this + "stumbleupon circle"

    @SemanticIconMarker
    inline val subscript: SemanticIcon get() = this + "subscript"

    @SemanticIconMarker
    inline val subway: SemanticIcon get() = this + "subway"

    @SemanticIconMarker
    inline val suitcase: SemanticIcon get() = this + "suitcase"

    @SemanticIconMarker
    inline val suitcase_rolling: SemanticIcon get() = this + "suitcase rolling"

    @SemanticIconMarker
    inline val sun: SemanticIcon get() = this + "sun"

    @SemanticIconMarker
    inline val sun_outline: SemanticIcon get() = this + "sun outline"

    @SemanticIconMarker
    inline val superpowers: SemanticIcon get() = this + "superpowers"

    @SemanticIconMarker
    inline val superscript: SemanticIcon get() = this + "superscript"

    @SemanticIconMarker
    inline val supple: SemanticIcon get() = this + "supple"

    @SemanticIconMarker
    inline val surprise: SemanticIcon get() = this + "surprise"

    @SemanticIconMarker
    inline val surprise_outline: SemanticIcon get() = this + "surprise outline"

    @SemanticIconMarker
    inline val suse: SemanticIcon get() = this + "suse"

    @SemanticIconMarker
    inline val swatchbook: SemanticIcon get() = this + "swatchbook"

    @SemanticIconMarker
    inline val swift: SemanticIcon get() = this + "swift"

    @SemanticIconMarker
    inline val swimmer: SemanticIcon get() = this + "swimmer"

    @SemanticIconMarker
    inline val swimming_pool: SemanticIcon get() = this + "swimming pool"

    @SemanticIconMarker
    inline val symfony: SemanticIcon get() = this + "symfony"

    @SemanticIconMarker
    inline val synagogue: SemanticIcon get() = this + "synagogue"

    @SemanticIconMarker
    inline val sync: SemanticIcon get() = this + "sync"

    @SemanticIconMarker
    inline val sync_alternate: SemanticIcon get() = this + "sync alternate"

    @SemanticIconMarker
    inline val syringe: SemanticIcon get() = this + "syringe"

    @SemanticIconMarker
    inline val table: SemanticIcon get() = this + "table"

    @SemanticIconMarker
    inline val table_tennis: SemanticIcon get() = this + "table tennis"

    @SemanticIconMarker
    inline val tablet: SemanticIcon get() = this + "tablet"

    @SemanticIconMarker
    inline val tablet_alternate: SemanticIcon get() = this + "tablet alternate"

    @SemanticIconMarker
    inline val tablets: SemanticIcon get() = this + "tablets"

    @SemanticIconMarker
    inline val tachometer_alternate: SemanticIcon get() = this + "tachometer alternate"

    @SemanticIconMarker
    inline val tag: SemanticIcon get() = this + "tag"

    @SemanticIconMarker
    inline val tags: SemanticIcon get() = this + "tags"

    @SemanticIconMarker
    inline val tape: SemanticIcon get() = this + "tape"

    @SemanticIconMarker
    inline val tasks: SemanticIcon get() = this + "tasks"

    @SemanticIconMarker
    inline val taxi: SemanticIcon get() = this + "taxi"

    @SemanticIconMarker
    inline val teal_users: SemanticIcon get() = this + "teal users"

    @SemanticIconMarker
    inline val teamspeak: SemanticIcon get() = this + "teamspeak"

    @SemanticIconMarker
    inline val teeth: SemanticIcon get() = this + "teeth"

    @SemanticIconMarker
    inline val teeth_open: SemanticIcon get() = this + "teeth open"

    @SemanticIconMarker
    inline val telegram: SemanticIcon get() = this + "telegram"

    @SemanticIconMarker
    inline val telegram_plane: SemanticIcon get() = this + "telegram plane"

    @SemanticIconMarker
    inline val temperature_high: SemanticIcon get() = this + "temperature high"

    @SemanticIconMarker
    inline val temperature_low: SemanticIcon get() = this + "temperature low"

    @SemanticIconMarker
    inline val tencent_weibo: SemanticIcon get() = this + "tencent weibo"

    @SemanticIconMarker
    inline val tenge: SemanticIcon get() = this + "tenge"

    @SemanticIconMarker
    inline val terminal: SemanticIcon get() = this + "terminal"

    @SemanticIconMarker
    inline val text_height: SemanticIcon get() = this + "text height"

    @SemanticIconMarker
    inline val text_width: SemanticIcon get() = this + "text width"

    @SemanticIconMarker
    inline val th: SemanticIcon get() = this + "th"

    @SemanticIconMarker
    inline val th_large: SemanticIcon get() = this + "th large"

    @SemanticIconMarker
    inline val th_list: SemanticIcon get() = this + "th list"

    @SemanticIconMarker
    inline val theater_masks: SemanticIcon get() = this + "theater masks"

    @SemanticIconMarker
    inline val themeco: SemanticIcon get() = this + "themeco"

    @SemanticIconMarker
    inline val themeisle: SemanticIcon get() = this + "themeisle"

    @SemanticIconMarker
    inline val thermometer: SemanticIcon get() = this + "thermometer"

    @SemanticIconMarker
    inline val thermometer_empty: SemanticIcon get() = this + "thermometer empty"

    @SemanticIconMarker
    inline val thermometer_full: SemanticIcon get() = this + "thermometer full"

    @SemanticIconMarker
    inline val thermometer_half: SemanticIcon get() = this + "thermometer half"

    @SemanticIconMarker
    inline val thermometer_quarter: SemanticIcon get() = this + "thermometer quarter"

    @SemanticIconMarker
    inline val thermometer_three_quarters: SemanticIcon get() = this + "thermometer three quarters"

    @SemanticIconMarker
    inline val think_peaks: SemanticIcon get() = this + "think peaks"

    @SemanticIconMarker
    inline val thumbs_down: SemanticIcon get() = this + "thumbs down"

    @SemanticIconMarker
    inline val thumbs_down_outline: SemanticIcon get() = this + "thumbs down outline"

    @SemanticIconMarker
    inline val thumbs_up: SemanticIcon get() = this + "thumbs up"

    @SemanticIconMarker
    inline val thumbs_up_outline: SemanticIcon get() = this + "thumbs up outline"

    @SemanticIconMarker
    inline val thumbtack: SemanticIcon get() = this + "thumbtack"

    @SemanticIconMarker
    inline val ticket_alternate: SemanticIcon get() = this + "ticket alternate"

    @SemanticIconMarker
    inline val times: SemanticIcon get() = this + "times"

    @SemanticIconMarker
    inline val times_circle: SemanticIcon get() = this + "times circle"

    @SemanticIconMarker
    inline val times_circle_outline: SemanticIcon get() = this + "times circle outline"

    @SemanticIconMarker
    inline val tint: SemanticIcon get() = this + "tint"

    @SemanticIconMarker
    inline val tint_slash: SemanticIcon get() = this + "tint slash"

    @SemanticIconMarker
    inline val tiny_home: SemanticIcon get() = this + "tiny home"

    @SemanticIconMarker
    inline val tired: SemanticIcon get() = this + "tired"

    @SemanticIconMarker
    inline val tired_outline: SemanticIcon get() = this + "tired outline"

    @SemanticIconMarker
    inline val toggle_off: SemanticIcon get() = this + "toggle off"

    @SemanticIconMarker
    inline val toggle_on: SemanticIcon get() = this + "toggle on"

    @SemanticIconMarker
    inline val toilet: SemanticIcon get() = this + "toilet"

    @SemanticIconMarker
    inline val toilet_paper: SemanticIcon get() = this + "toilet paper"

    @SemanticIconMarker
    inline val toilet_paper_slash: SemanticIcon get() = this + "toilet paper slash"

    @SemanticIconMarker
    inline val toolbox: SemanticIcon get() = this + "toolbox"

    @SemanticIconMarker
    inline val tools: SemanticIcon get() = this + "tools"

    @SemanticIconMarker
    inline val tooth: SemanticIcon get() = this + "tooth"

    @SemanticIconMarker
    inline val top_left_corner_add: SemanticIcon get() = this + "top left corner add"

    @SemanticIconMarker
    inline val top_right_corner_add: SemanticIcon get() = this + "top right corner add"

    @SemanticIconMarker
    inline val torah: SemanticIcon get() = this + "torah"

    @SemanticIconMarker
    inline val torii_gate: SemanticIcon get() = this + "torii gate"

    @SemanticIconMarker
    inline val tractor: SemanticIcon get() = this + "tractor"

    @SemanticIconMarker
    inline val trade_federation: SemanticIcon get() = this + "trade federation"

    @SemanticIconMarker
    inline val trademark: SemanticIcon get() = this + "trademark"

    @SemanticIconMarker
    inline val traffic_light: SemanticIcon get() = this + "traffic light"

    @SemanticIconMarker
    inline val trailer: SemanticIcon get() = this + "trailer"

    @SemanticIconMarker
    inline val train: SemanticIcon get() = this + "train"

    @SemanticIconMarker
    inline val tram: SemanticIcon get() = this + "tram"

    @SemanticIconMarker
    inline val transgender: SemanticIcon get() = this + "transgender"

    @SemanticIconMarker
    inline val transgender_alternate: SemanticIcon get() = this + "transgender alternate"

    @SemanticIconMarker
    inline val trash: SemanticIcon get() = this + "trash"

    @SemanticIconMarker
    inline val trash_alternate: SemanticIcon get() = this + "trash alternate"

    @SemanticIconMarker
    inline val trash_alternate_outline: SemanticIcon get() = this + "trash alternate outline"

    @SemanticIconMarker
    inline val trash_restore: SemanticIcon get() = this + "trash restore"

    @SemanticIconMarker
    inline val trash_restore_alternate: SemanticIcon get() = this + "trash restore alternate"

    @SemanticIconMarker
    inline val tree: SemanticIcon get() = this + "tree"

    @SemanticIconMarker
    inline val trello: SemanticIcon get() = this + "trello"

    @SemanticIconMarker
    inline val tripadvisor: SemanticIcon get() = this + "tripadvisor"

    @SemanticIconMarker
    inline val trophy: SemanticIcon get() = this + "trophy"

    @SemanticIconMarker
    inline val truck: SemanticIcon get() = this + "truck"

    @SemanticIconMarker
    inline val truck_monster: SemanticIcon get() = this + "truck monster"

    @SemanticIconMarker
    inline val truck_moving: SemanticIcon get() = this + "truck moving"

    @SemanticIconMarker
    inline val truck_packing: SemanticIcon get() = this + "truck packing"

    @SemanticIconMarker
    inline val truck_pickup: SemanticIcon get() = this + "truck pickup"

    @SemanticIconMarker
    inline val tshirt: SemanticIcon get() = this + "tshirt"

    @SemanticIconMarker
    inline val tty: SemanticIcon get() = this + "tty"

    @SemanticIconMarker
    inline val tumblr: SemanticIcon get() = this + "tumblr"

    @SemanticIconMarker
    inline val tumblr_square: SemanticIcon get() = this + "tumblr square"

    @SemanticIconMarker
    inline val tv: SemanticIcon get() = this + "tv"

    @SemanticIconMarker
    inline val twitch: SemanticIcon get() = this + "twitch"

    @SemanticIconMarker
    inline val twitter: SemanticIcon get() = this + "twitter"

    @SemanticIconMarker
    inline val twitter_square: SemanticIcon get() = this + "twitter square"

    @SemanticIconMarker
    inline val typo3: SemanticIcon get() = this + "typo3"

    @SemanticIconMarker
    inline val uber: SemanticIcon get() = this + "uber"

    @SemanticIconMarker
    inline val ubuntu: SemanticIcon get() = this + "ubuntu"

    @SemanticIconMarker
    inline val uikit: SemanticIcon get() = this + "uikit"

    @SemanticIconMarker
    inline val umbraco: SemanticIcon get() = this + "umbraco"

    @SemanticIconMarker
    inline val umbrella: SemanticIcon get() = this + "umbrella"

    @SemanticIconMarker
    inline val umbrella_beach: SemanticIcon get() = this + "umbrella beach"

    @SemanticIconMarker
    inline val underline: SemanticIcon get() = this + "underline"

    @SemanticIconMarker
    inline val undo: SemanticIcon get() = this + "undo"

    @SemanticIconMarker
    inline val undo_alternate: SemanticIcon get() = this + "undo alternate"

    @SemanticIconMarker
    inline val uniregistry: SemanticIcon get() = this + "uniregistry"

    @SemanticIconMarker
    inline val unity: SemanticIcon get() = this + "unity"

    @SemanticIconMarker
    inline val universal_access: SemanticIcon get() = this + "universal access"

    @SemanticIconMarker
    inline val university: SemanticIcon get() = this + "university"

    @SemanticIconMarker
    inline val unlink: SemanticIcon get() = this + "unlink"

    @SemanticIconMarker
    inline val unlock: SemanticIcon get() = this + "unlock"

    @SemanticIconMarker
    inline val unlock_alternate: SemanticIcon get() = this + "unlock alternate"

    @SemanticIconMarker
    inline val untappd: SemanticIcon get() = this + "untappd"

    @SemanticIconMarker
    inline val upload: SemanticIcon get() = this + "upload"

    @SemanticIconMarker
    inline val ups: SemanticIcon get() = this + "ups"

    @SemanticIconMarker
    inline val usb: SemanticIcon get() = this + "usb"

    @SemanticIconMarker
    inline val user: SemanticIcon get() = this + "user"

    @SemanticIconMarker
    inline val user_alternate: SemanticIcon get() = this + "user alternate"

    @SemanticIconMarker
    inline val user_alternate_slash: SemanticIcon get() = this + "user alternate slash"

    @SemanticIconMarker
    inline val user_astronaut: SemanticIcon get() = this + "user astronaut"

    @SemanticIconMarker
    inline val user_check: SemanticIcon get() = this + "user check"

    @SemanticIconMarker
    inline val user_circle: SemanticIcon get() = this + "user circle"

    @SemanticIconMarker
    inline val user_circle_outline: SemanticIcon get() = this + "user circle outline"

    @SemanticIconMarker
    inline val user_clock: SemanticIcon get() = this + "user clock"

    @SemanticIconMarker
    inline val user_cog: SemanticIcon get() = this + "user cog"

    @SemanticIconMarker
    inline val user_edit: SemanticIcon get() = this + "user edit"

    @SemanticIconMarker
    inline val user_friends: SemanticIcon get() = this + "user friends"

    @SemanticIconMarker
    inline val user_graduate: SemanticIcon get() = this + "user graduate"

    @SemanticIconMarker
    inline val user_injured: SemanticIcon get() = this + "user injured"

    @SemanticIconMarker
    inline val user_lock: SemanticIcon get() = this + "user lock"

    @SemanticIconMarker
    inline val user_md: SemanticIcon get() = this + "user md"

    @SemanticIconMarker
    inline val user_minus: SemanticIcon get() = this + "user minus"

    @SemanticIconMarker
    inline val user_ninja: SemanticIcon get() = this + "user ninja"

    @SemanticIconMarker
    inline val user_nurse: SemanticIcon get() = this + "user nurse"

    @SemanticIconMarker
    inline val user_outline: SemanticIcon get() = this + "user outline"

    @SemanticIconMarker
    inline val user_plus: SemanticIcon get() = this + "user plus"

    @SemanticIconMarker
    inline val user_secret: SemanticIcon get() = this + "user secret"

    @SemanticIconMarker
    inline val user_shield: SemanticIcon get() = this + "user shield"

    @SemanticIconMarker
    inline val user_slash: SemanticIcon get() = this + "user slash"

    @SemanticIconMarker
    inline val user_tag: SemanticIcon get() = this + "user tag"

    @SemanticIconMarker
    inline val user_tie: SemanticIcon get() = this + "user tie"

    @SemanticIconMarker
    inline val user_times: SemanticIcon get() = this + "user times"

    @SemanticIconMarker
    inline val users: SemanticIcon get() = this + "users"

    @SemanticIconMarker
    inline val users_cog: SemanticIcon get() = this + "users cog"

    @SemanticIconMarker
    inline val usps: SemanticIcon get() = this + "usps"

    @SemanticIconMarker
    inline val ussunnah: SemanticIcon get() = this + "ussunnah"

    @SemanticIconMarker
    inline val utensil_spoon: SemanticIcon get() = this + "utensil spoon"

    @SemanticIconMarker
    inline val utensils: SemanticIcon get() = this + "utensils"

    @SemanticIconMarker
    inline val vaadin: SemanticIcon get() = this + "vaadin"

    @SemanticIconMarker
    inline val vector_square: SemanticIcon get() = this + "vector square"

    @SemanticIconMarker
    inline val venus: SemanticIcon get() = this + "venus"

    @SemanticIconMarker
    inline val venus_double: SemanticIcon get() = this + "venus double"

    @SemanticIconMarker
    inline val venus_mars: SemanticIcon get() = this + "venus mars"

    @SemanticIconMarker
    inline val vertically_flipped_cloud: SemanticIcon get() = this + "vertically flipped cloud"

    @SemanticIconMarker
    inline val viacoin: SemanticIcon get() = this + "viacoin"

    @SemanticIconMarker
    inline val viadeo: SemanticIcon get() = this + "viadeo"

    @SemanticIconMarker
    inline val viadeo_square: SemanticIcon get() = this + "viadeo square"

    @SemanticIconMarker
    inline val vial: SemanticIcon get() = this + "vial"

    @SemanticIconMarker
    inline val vials: SemanticIcon get() = this + "vials"

    @SemanticIconMarker
    inline val viber: SemanticIcon get() = this + "viber"

    @SemanticIconMarker
    inline val video: SemanticIcon get() = this + "video"

    @SemanticIconMarker
    inline val video_slash: SemanticIcon get() = this + "video slash"

    @SemanticIconMarker
    inline val vihara: SemanticIcon get() = this + "vihara"

    @SemanticIconMarker
    inline val vimeo: SemanticIcon get() = this + "vimeo"

    @SemanticIconMarker
    inline val vimeo_square: SemanticIcon get() = this + "vimeo square"

    @SemanticIconMarker
    inline val vimeo_v: SemanticIcon get() = this + "vimeo v"

    @SemanticIconMarker
    inline val vine: SemanticIcon get() = this + "vine"

    @SemanticIconMarker
    inline val violet_users: SemanticIcon get() = this + "violet users"

    @SemanticIconMarker
    inline val virus: SemanticIcon get() = this + "virus"

    @SemanticIconMarker
    inline val virus_slash: SemanticIcon get() = this + "virus slash"

    @SemanticIconMarker
    inline val viruses: SemanticIcon get() = this + "viruses"

    @SemanticIconMarker
    inline val vk: SemanticIcon get() = this + "vk"

    @SemanticIconMarker
    inline val vnv: SemanticIcon get() = this + "vnv"

    @SemanticIconMarker
    inline val voicemail: SemanticIcon get() = this + "voicemail"

    @SemanticIconMarker
    inline val volleyball_ball: SemanticIcon get() = this + "volleyball ball"

    @SemanticIconMarker
    inline val volume_down: SemanticIcon get() = this + "volume down"

    @SemanticIconMarker
    inline val volume_mute: SemanticIcon get() = this + "volume mute"

    @SemanticIconMarker
    inline val volume_off: SemanticIcon get() = this + "volume off"

    @SemanticIconMarker
    inline val volume_up: SemanticIcon get() = this + "volume up"

    @SemanticIconMarker
    inline val vote_yea: SemanticIcon get() = this + "vote yea"

    @SemanticIconMarker
    inline val vuejs: SemanticIcon get() = this + "vuejs"

    @SemanticIconMarker
    inline val walking: SemanticIcon get() = this + "walking"

    @SemanticIconMarker
    inline val wallet: SemanticIcon get() = this + "wallet"

    @SemanticIconMarker
    inline val warehouse: SemanticIcon get() = this + "warehouse"

    @SemanticIconMarker
    inline val water: SemanticIcon get() = this + "water"

    @SemanticIconMarker
    inline val wave_square: SemanticIcon get() = this + "wave square"

    @SemanticIconMarker
    inline val waze: SemanticIcon get() = this + "waze"

    @SemanticIconMarker
    inline val weebly: SemanticIcon get() = this + "weebly"

    @SemanticIconMarker
    inline val weibo: SemanticIcon get() = this + "weibo"

    @SemanticIconMarker
    inline val weight: SemanticIcon get() = this + "weight"

    @SemanticIconMarker
    inline val weixin: SemanticIcon get() = this + "weixin"

    @SemanticIconMarker
    inline val whatsapp: SemanticIcon get() = this + "whatsapp"

    @SemanticIconMarker
    inline val whatsapp_square: SemanticIcon get() = this + "whatsapp square"

    @SemanticIconMarker
    inline val wheelchair: SemanticIcon get() = this + "wheelchair"

    @SemanticIconMarker
    inline val whmcs: SemanticIcon get() = this + "whmcs"

    @SemanticIconMarker
    inline val wifi: SemanticIcon get() = this + "wifi"

    @SemanticIconMarker
    inline val wikipedia_w: SemanticIcon get() = this + "wikipedia w"

    @SemanticIconMarker
    inline val wind: SemanticIcon get() = this + "wind"

    @SemanticIconMarker
    inline val window_close: SemanticIcon get() = this + "window close"

    @SemanticIconMarker
    inline val window_close_outline: SemanticIcon get() = this + "window close outline"

    @SemanticIconMarker
    inline val window_maximize: SemanticIcon get() = this + "window maximize"

    @SemanticIconMarker
    inline val window_maximize_outline: SemanticIcon get() = this + "window maximize outline"

    @SemanticIconMarker
    inline val window_minimize: SemanticIcon get() = this + "window minimize"

    @SemanticIconMarker
    inline val window_minimize_outline: SemanticIcon get() = this + "window minimize outline"

    @SemanticIconMarker
    inline val window_restore: SemanticIcon get() = this + "window restore"

    @SemanticIconMarker
    inline val window_restore_outline: SemanticIcon get() = this + "window restore outline"

    @SemanticIconMarker
    inline val windows: SemanticIcon get() = this + "windows"

    @SemanticIconMarker
    inline val wine_bottle: SemanticIcon get() = this + "wine bottle"

    @SemanticIconMarker
    inline val wine_glass: SemanticIcon get() = this + "wine glass"

    @SemanticIconMarker
    inline val wine_glass_alternate: SemanticIcon get() = this + "wine glass alternate"

    @SemanticIconMarker
    inline val wix: SemanticIcon get() = this + "wix"

    @SemanticIconMarker
    inline val wizards_of_the_coast: SemanticIcon get() = this + "wizards of the coast"

    @SemanticIconMarker
    inline val wolf_pack_battalion: SemanticIcon get() = this + "wolf pack battalion"

    @SemanticIconMarker
    inline val won_sign: SemanticIcon get() = this + "won sign"

    @SemanticIconMarker
    inline val wordpress: SemanticIcon get() = this + "wordpress"

    @SemanticIconMarker
    inline val wordpress_simple: SemanticIcon get() = this + "wordpress simple"

    @SemanticIconMarker
    inline val world: SemanticIcon get() = this + "world"

    @SemanticIconMarker
    inline val wpbeginner: SemanticIcon get() = this + "wpbeginner"

    @SemanticIconMarker
    inline val wpexplorer: SemanticIcon get() = this + "wpexplorer"

    @SemanticIconMarker
    inline val wpforms: SemanticIcon get() = this + "wpforms"

    @SemanticIconMarker
    inline val wpressr: SemanticIcon get() = this + "wpressr"

    @SemanticIconMarker
    inline val wrench: SemanticIcon get() = this + "wrench"

    @SemanticIconMarker
    inline val x_ray: SemanticIcon get() = this + "x ray"

    @SemanticIconMarker
    inline val xbox: SemanticIcon get() = this + "xbox"

    @SemanticIconMarker
    inline val xing: SemanticIcon get() = this + "xing"

    @SemanticIconMarker
    inline val xing_square: SemanticIcon get() = this + "xing square"

    @SemanticIconMarker
    inline val y_combinator: SemanticIcon get() = this + "y combinator"

    @SemanticIconMarker
    inline val yahoo: SemanticIcon get() = this + "yahoo"

    @SemanticIconMarker
    inline val yammer: SemanticIcon get() = this + "yammer"

    @SemanticIconMarker
    inline val yandex: SemanticIcon get() = this + "yandex"

    @SemanticIconMarker
    inline val yandex_international: SemanticIcon get() = this + "yandex international"

    @SemanticIconMarker
    inline val yarn: SemanticIcon get() = this + "yarn"

    @SemanticIconMarker
    inline val yellow_users: SemanticIcon get() = this + "yellow users"

    @SemanticIconMarker
    inline val yelp: SemanticIcon get() = this + "yelp"

    @SemanticIconMarker
    inline val yen_sign: SemanticIcon get() = this + "yen sign"

    @SemanticIconMarker
    inline val yin_yang: SemanticIcon get() = this + "yin yang"

    @SemanticIconMarker
    inline val yoast: SemanticIcon get() = this + "yoast"

    @SemanticIconMarker
    inline val youtube: SemanticIcon get() = this + "youtube"

    @SemanticIconMarker
    inline val youtube_square: SemanticIcon get() = this + "youtube square"

    @SemanticIconMarker
    inline val zhihu: SemanticIcon get() = this + "zhihu"
}
