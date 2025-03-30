package de.peekandpoke.kraft.examples.jsaddons.prismjs

import de.peekandpoke.kraft.addons.prismjs.*
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.InlineColor.Companion.inlineColor
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.PrismJsExample() = comp {
    PrismJsExample(it)
}

class PrismJsExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"PrismJs syntax highlighter" }

            ui.header H3 { +"Plugin Examples" }
            pluginExamples()

            ui.header H3 { +"Language Examples" }
            languageExamples()
        }
    }

    private fun FlowContent.pluginExamples() {
        ui.header { +"Line numbers" }

        p {
            a(href = "https://prismjs.com/plugins/line-numbers/") {
                +"PrismJs docs"
            }
        }

        p { +"Usage" }
        PrismKotlinScript(
            """
                PrismKotlin("... code ...") {
                    lineNumbers(
                        // line number of the first line, default = 1
                        start = -5,
                        // should soft wraps be used, default = false
                        softWrap = true,
                    )
                }            
            """.trimIndent()
        )

        PrismHtml(getPluginExampleHtmlCode()) {
            lineNumbers(start = -5, softWrap = true)
        }
    }

    private fun FlowContent.languageExamples() {

        val configure: PrismOptsBuilder = {
            inlineColor()
            lineNumbers()
            copyToClipboard()
        }

        ui.header { +"atom" }
        PrismAtom(
            """
                Hello World!
            """.trimIndent(),
            configure,
        )

        ui.header { +"clike" }
        PrismCLike(
            """
                #include <stdio.h>
                int main() {
                    printf("Hello, World!");
                    return 0;
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"css" }
        PrismCss(
            """
                /* All in red */
                body {
                    color: #FF0000;
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"dart" }
        PrismDart(
            """
                /* Hello world from dart */
                void main() {
                    print('Hello, World!');
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"html" }
        PrismHtml(
            """
                <!-- Hello world from html -->
                <div>
                    Hello, World!
                </div>
            """.trimIndent(),
            configure,
        )

        ui.header { +"kotlin" }
        PrismKotlin(
            """
                /** Hello world from kotlin */
                fun main() {
                    println("Hello, World!")
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"java" }
        PrismJava(
            """
                /** Hello world from java */
                class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!"); 
                    }
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"javascript" }
        PrismJavascript(
            """
                /** Hello world from javascript */
                function helloWorld() {
                    alert( 'Hello, world!' );
                }
                
                helloWorld();
            """.trimIndent(),
            configure,
        )

        ui.header { +"json" }
        PrismJson(
            """
                {
                    "json": "Hello, World!"                        
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"json5" }
        PrismJson5(
            """
                {
                  // comments
                  unquoted: 'and you can quote me on that',
                  singleQuotes: 'I can use "double quotes" here',
                  lineBreaks: "Look, Mom! \
                      No \\n's!",
                  hexadecimal: 0xdecaf,
                  leadingDecimalPoint: .8675309, andTrailing: 8675309.,
                  positiveSign: +1,
                  trailingComma: 'in objects', andIn: ['arrays',],
                  "backwardsCompatible": "with JSON",
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"jsonp" }
        PrismJsonp(
            """
                myFunc(
                    '{ "name":"John", "age":30, "city":"New York" }'
                );
            """.trimIndent(),
            configure,
        )

        ui.header { +"less" }
        PrismLess(
            """
                @header-color: #FFAA88;
                
                #header {
                  color: @header-color;
                  .navigation {
                    font-size: 12px;
                  }
                  .logo {
                    width: 300px;
                  }
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"markup" }
        PrismMarkup(
            """
                <!-- Hello world from markup -->
                <div>
                    Hello, World!
                </div>
            """.trimIndent(),
            configure,
        )

        ui.header { +"php" }
        PrismPhp(
            """
                <?php
                    // Hello world from PHP
                    phpinfo(); 
                
            """.trimIndent(),
            configure,
        )

        ui.header { +"plain" }
        PrismPlain(
            """
                This is just a plain text.
                Hello World!
            """.trimIndent(),
            configure,
        )

        ui.header { +"regex" }
        PrismRegex(
            """
                [-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)
            """.trimIndent(),
            configure,
        )

        ui.header { +"ruby" }
        PrismRuby(
            """
                # Hello world from ruby
                puts 'Hello, world!'
            """.trimIndent(),
            configure,
        )

        ui.header { +"rust" }
        PrismRust(
            """
                // This is the main function
                fn main() {
                    // Statements here are executed when the compiled binary is called

                    // Print text to the console
                    println!("Hello World!");
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"rss" }
        PrismRss(
            """
                <?xml version="1.0" encoding="UTF-8" ?>
                <rss version="2.0">
                  <channel>
                    <title>W3Schools Home Page</title>
                    <link>https://www.w3schools.com</link>
                    <description>Free web building tutorials</description>
                    <item>
                      <title>RSS Tutorial</title>
                      <link>https://www.w3schools.com/xml/xml_rss.asp</link>
                      <description>New RSS tutorial on W3Schools</description>
                    </item>
                    <item>
                      <title>XML Tutorial</title>
                      <link>https://www.w3schools.com/xml</link>
                      <description>New XML tutorial on W3Schools</description>
                    </item>
                  </channel>
                </rss>
            """.trimIndent(),
            configure,
        )

        ui.header { +"sass" }
        PrismSass(
            """
                ${"$"}font-stack: Helvetica, sans-serif
                ${"$"}primary-color: #333
                
                body                      
                  div                      
                    font: 100% ${'$'}font-stack
                    color: ${'$'}primary-color
            """.trimIndent(),
            configure,
        )

        ui.header { +"scss" }
        PrismScss(
            """
                ${"$"}font-stack: Helvetica, sans-serif;
                ${"$"}primary-color: #333;
                
                body {
                  div {                      
                    font: 100% ${'$'}font-stack;
                    color: ${'$'}primary-color;
                  }
                }
            """.trimIndent(),
            configure,
        )

        ui.header { +"ssml" }
        PrismSsml(
            """
                <speak>
                  Here are <say-as interpret-as="characters">SSML</say-as> samples.
                  I can pause <break time="3s"/>.
                  I can play a sound
                  <audio src="https://www.example.com/MY_MP3_FILE.mp3">didn't get your MP3 audio file</audio>.
                  I can speak in cardinals. Your number is <say-as interpret-as="cardinal">10</say-as>.
                  Or I can speak in ordinals. You are <say-as interpret-as="ordinal">10</say-as> in line.
                  Or I can even speak in digits. The digits for ten are <say-as interpret-as="characters">10</say-as>.
                  I can also substitute phrases, like the <sub alias="World Wide Web Consortium">W3C</sub>.
                  Finally, I can speak a paragraph with two sentences.
                  <p><s>This is sentence one.</s><s>This is sentence two.</s></p>
                </speak>
            """.trimIndent(),
            configure,
        )

        ui.header { +"svg" }
        PrismSvg(
            """
                <svg width="400" height="180">
                  <rect x="50" y="20" rx="20" ry="20" width="150" height="150" style="fill:red;stroke:black;stroke-width:5;opacity:0.5" />
                  Sorry, your browser does not support inline SVG.
                </svg>
            """.trimIndent(),
            configure,
        )

        ui.header { +"text" }
        PrismText(
            """
                This is just a plain text.
                Hello World!
            """.trimIndent(),
            configure,
        )

        ui.header { +"typescript" }
        PrismTypescript(
            """
                let message: string = 'Hello, World!';
                // create a new heading 1 element
                let heading = document.createElement('h1');
                heading.textContent = message;
                // add the heading the document
                document.body.appendChild(heading);
            """.trimIndent(),
            configure,
        )

        ui.header { +"xml" }
        PrismXml(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <text>
                  <para>hello world</para>
                </text>
            """.trimIndent(),
            configure,
        )
    }

    private fun getPluginExampleHtmlCode(): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>

        <meta charset="utf-8" />
        <link rel="icon" href="assets/favicon.png" />
        <title>Line Numbers ▲ Prism plugins</title>
        <base href="../.." />
        <link rel="stylesheet" href="assets/style.css" />
        <link rel="stylesheet" href="themes/prism.css" data-noprefix />
        <link rel="stylesheet" href="plugins/line-numbers/prism-line-numbers.css" data-noprefix />
        <script src="assets/vendor/prefixfree.min.js"></script>

        <script>var _gaq = [['_setAccount', 'UA-33746269-1'], ['_trackPageview']];</script>
        <script src="https://www.google-analytics.com/ga.js" async></script>
        </head>
        <body class="language-none">

        <header data-plugin-header="line-numbers"></header>

        <section class="language-markup">
          <h1>How to use</h1>

          <p>Obviously, this is supposed to work only for code blocks (<code>&lt;pre>&lt;code></code>) and not for inline code.</p>
          <p>Add the <code>line-numbers</code> class to your desired <code>&lt;pre></code> or any of its ancestors, and the Line Numbers plugin will take care of the rest. To give all code blocks line numbers, add the <code>line-numbers</code> class to the <code>&lt;body></code> of the page. This is part of a general activation mechanism where adding the <code>line-numbers</code> (or <code>no-line-numbers</code>) class to any element will enable (or disable) the Line Numbers plugin for all code blocks in that element. <br> Example:</p>

          <pre><code>&lt;body class="line-numbers"> &lt;!-- enabled for the whole page -->

            &lt;!-- with line numbers -->
            &lt;pre>&lt;code>...&lt;/code>&lt;/pre>
            &lt;!-- disabled for a specific element - without line numbers -->
            &lt;pre class="no-line-numbers">&lt;code>...&lt;/code>&lt;/pre>

            &lt;div class="no-line-numbers"> &lt;!-- disabled for this subtree -->

                &lt;!-- without line numbers -->
                &lt;pre>&lt;code>...&lt;/code>&lt;/pre>
                &lt;!-- enabled for a specific element - with line numbers -->
                &lt;pre class="line-numbers">&lt;code>...&lt;/code>&lt;/pre>

            &lt;/div>
        &lt;/body></code></pre>

          <p>Optional: You can specify the <code>data-start</code> (Number) attribute on the <code>&lt;pre></code> element. It will shift the line counter.</p>
          <p>Optional: To support multiline line numbers using soft wrap, apply the CSS <code>white-space: pre-line;</code> or <code>white-space: pre-wrap;</code> to your desired <code>&lt;pre></code>.</p>
        </section>

        <section class="line-numbers">
          <h1>Examples</h1>

          <h2>JavaScript</h2>
          <pre class="line-numbers" data-src="plugins/line-numbers/prism-line-numbers.js"></pre>

          <h2>CSS</h2>
          <p>Please note that this <code class="language-markup">&lt;pre></code> does not have the <code>line-numbers</code> class but its parent does.</p>
          <pre data-src="plugins/line-numbers/prism-line-numbers.css"></pre>

          <h2>HTML</h2>
          <p>Please note the <code>data-start="-5"</code> in the code below.</p>
          <pre class="line-numbers" data-src="plugins/line-numbers/index.html" data-start="-5"></pre>

          <h2>Unknown languages</h2>
          <pre class="language-none line-numbers"><code>This raw text
        is not highlighted
        but it still has
        line numbers</code></pre>

          <h2>Soft wrap support</h2>
          <p>Please note the <code>style="white-space:pre-wrap;"</code> in the code below.</p>
          <pre class="line-numbers" data-src="plugins/line-numbers/index.html" data-start="-5" style="white-space:pre-wrap;"></pre>

        </section>

        <footer data-src="assets/templates/footer.html" data-type="text/html"></footer>

        <script src="prism.js"></script>
        <script src="plugins/line-numbers/prism-line-numbers.js"></script>
        <script src="assets/vendor/utopia.js"></script>
        <script src="components.js"></script>
        <script src="assets/code.js"></script>

        </body>
        </html>
    """.trimIndent()
}
