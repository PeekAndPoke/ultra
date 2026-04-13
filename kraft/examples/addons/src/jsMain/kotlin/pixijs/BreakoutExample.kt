package io.peekandpoke.kraft.examples.jsaddons.pixijs

import io.peekandpoke.kraft.addons.pixijs.PixiJsAddon
import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.addons.pixijs.js.Graphics
import io.peekandpoke.kraft.addons.pixijs.js.Ticker
import io.peekandpoke.kraft.addons.pixijs.pixiJs
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.css.BorderStyle
import kotlinx.css.Display
import kotlinx.css.FlexDirection
import kotlinx.css.JustifyContent
import kotlinx.css.borderRadius
import kotlinx.css.borderStyle
import kotlinx.css.borderWidth
import kotlinx.css.display
import kotlinx.css.flexDirection
import kotlinx.css.justifyContent
import kotlinx.css.px
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.p
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.KeyboardEvent

@Suppress("FunctionName")
fun Tag.BreakoutExample() = comp {
    BreakoutExample(it)
}

private const val GAME_WIDTH = 720
private const val GAME_HEIGHT = 480
private const val PADDLE_WIDTH = 100.0
private const val PADDLE_HEIGHT = 14.0
private const val BALL_RADIUS = 8.0
private const val BRICK_ROWS = 5
private const val BRICK_COLS = 10
private const val BRICK_WIDTH = 60.0
private const val BRICK_HEIGHT = 20.0
private const val BRICK_GAP = 4.0
private const val BRICK_TOP_OFFSET = 50.0

class BreakoutExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val pixiAddon: PixiJsAddon? by subscribingTo(addons.pixiJs)

    private var app: Application? = null
    private var game: BreakoutGame? = null
    private var starting: Boolean = false

    private var status: String by value("Loading...")
    private var score: Int by value(0)
    private var lives: Int by value(3)
    private var mounted: Boolean = false

    //  LIFECYCLE  //////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                mounted = true
                window.addEventListener("keydown", ::onKeyDown)
                window.addEventListener("keyup", ::onKeyUp)
                tryStartGame()
            }

            onUpdate {
                tryStartGame()
            }

            onUnmount {
                mounted = false
                window.removeEventListener("keydown", ::onKeyDown)
                window.removeEventListener("keyup", ::onKeyUp)
                app?.destroy(rendererDestroy = true)
                app = null
                game = null
            }
        }
    }

    private fun tryStartGame() {
        val addon = pixiAddon ?: return
        if (app != null || starting) return
        val container = dom?.querySelector(".pixi-canvas") as? HTMLDivElement ?: return

        starting = true
        launch {
            val application = addon.createApplication()
            application.init(io.peekandpoke.kraft.utils.jsObject {
                this.width = GAME_WIDTH
                this.height = GAME_HEIGHT
                this.backgroundColor = 0x1a1a2e
                this.antialias = true
            }).await()

            container.append(application.canvas)
            app = application

            val g = BreakoutGame(
                addon = addon,
                app = application,
                onScoreChange = { score = it },
                onLivesChange = { lives = it },
                onStatusChange = { status = it },
            )
            g.start()
            game = g

            status = "Playing — use ← → arrows"
        }
    }

    //  INPUT  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun onKeyDown(evt: org.w3c.dom.events.Event) {
        val ke = evt as KeyboardEvent
        when (ke.key) {
            "ArrowLeft", "a", "A" -> game?.setMoveLeft(true)
            "ArrowRight", "d", "D" -> game?.setMoveRight(true)
            " " -> game?.launchBall()
        }
    }

    private fun onKeyUp(evt: org.w3c.dom.events.Event) {
        val ke = evt as KeyboardEvent
        when (ke.key) {
            "ArrowLeft", "a", "A" -> game?.setMoveLeft(false)
            "ArrowRight", "d", "D" -> game?.setMoveRight(false)
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H2 { +"Breakout (via PixiJS addon)" }

            p {
                +"A mini Breakout game rendered with PixiJS v8 and loaded through the Kraft AddonRegistry. "
                +"Use ← → or A/D to move the paddle, SPACE to launch."
            }

            if (pixiAddon == null) {
                ui.placeholder.segment {
                    ui.icon.header {
                        icon.spinner_loading()
                        +"Loading pixi.js addon..."
                    }
                }
                return@segment
            }

            ui.dividing.header { +"Game" }

            ui.statistics {
                ui.mini.statistic {
                    noui.value { +"$score" }
                    noui.label { +"Score" }
                }
                ui.mini.statistic {
                    noui.value { +"$lives" }
                    noui.label { +"Lives" }
                }
            }

            p { +status }

            div("pixi-canvas") {
                css {
                    display = Display.flex
                    justifyContent = JustifyContent.center
                    flexDirection = FlexDirection.column
                    borderWidth = 2.px
                    borderStyle = BorderStyle.solid
                    borderRadius = 4.px
                }
                attributes["style"] = "border-color: #333; width: ${GAME_WIDTH}px; max-width: 100%;"
            }

            div {
                attributes["style"] = "margin-top: 12px;"

                ui.button {
                    onClick {
                        game?.reset()
                        status = "Playing — use ← → arrows"
                    }
                    icon.redo()
                    +"New Game"
                }
                ui.button {
                    onClick { game?.launchBall() }
                    icon.play()
                    +"Launch Ball"
                }
            }
        }
    }
}

/** The Breakout game loop and state, separated from the Kraft component. */
private class BreakoutGame(
    private val addon: PixiJsAddon,
    private val app: Application,
    private val onScoreChange: (Int) -> Unit,
    private val onLivesChange: (Int) -> Unit,
    private val onStatusChange: (String) -> Unit,
) {
    private val paddle: Graphics = addon.createGraphics()
    private val ball: Graphics = addon.createGraphics()
    private val bricks: MutableList<Graphics> = mutableListOf()

    private var ballVx: Double = 0.0
    private var ballVy: Double = 0.0
    private var ballLive: Boolean = false
    private var paddleVx: Double = 0.0

    private var moveLeft: Boolean = false
    private var moveRight: Boolean = false

    private var score: Int = 0
    private var lives: Int = 3

    fun start() {
        drawPaddle()
        drawBall()
        buildBricks()
        resetBall()
        app.ticker.add(::update)
    }

    fun setMoveLeft(v: Boolean) {
        moveLeft = v
    }

    fun setMoveRight(v: Boolean) {
        moveRight = v
    }

    fun launchBall() {
        if (!ballLive) {
            ballLive = true
            ballVx = 4.0
            ballVy = -5.0
        }
    }

    fun reset() {
        score = 0
        lives = 3
        onScoreChange(score)
        onLivesChange(lives)
        bricks.forEach { app.stage.removeChild(it); it.destroy() }
        bricks.clear()
        buildBricks()
        resetBall()
        onStatusChange("Playing — use ← → arrows")
    }

    private fun drawPaddle() {
        paddle.rect(0.0, 0.0, PADDLE_WIDTH, PADDLE_HEIGHT).fill(0xe94560)
        paddle.x = (GAME_WIDTH - PADDLE_WIDTH) / 2.0
        paddle.y = GAME_HEIGHT - PADDLE_HEIGHT - 20.0
        app.stage.addChild(paddle)
    }

    private fun drawBall() {
        ball.circle(0.0, 0.0, BALL_RADIUS).fill(0xf5f5f5)
        app.stage.addChild(ball)
    }

    private fun buildBricks() {
        val brickColors = arrayOf(0xe94560, 0xf39c12, 0x2ecc71, 0x3498db, 0x9b59b6)
        val totalWidth = BRICK_COLS * BRICK_WIDTH + (BRICK_COLS - 1) * BRICK_GAP
        val startX = (GAME_WIDTH - totalWidth) / 2.0

        for (row in 0 until BRICK_ROWS) {
            for (col in 0 until BRICK_COLS) {
                val brick = addon.createGraphics()
                brick.roundRect(0.0, 0.0, BRICK_WIDTH, BRICK_HEIGHT, 3.0).fill(brickColors[row])
                brick.x = startX + col * (BRICK_WIDTH + BRICK_GAP)
                brick.y = BRICK_TOP_OFFSET + row * (BRICK_HEIGHT + BRICK_GAP)
                app.stage.addChild(brick)
                bricks.add(brick)
            }
        }
    }

    private fun resetBall() {
        ball.x = GAME_WIDTH / 2.0
        ball.y = GAME_HEIGHT - 60.0
        ballVx = 0.0
        ballVy = 0.0
        ballLive = false
    }

    private fun update(ticker: Ticker) {
        val dt = ticker.deltaTime

        // Paddle movement
        paddleVx = when {
            moveLeft && !moveRight -> -8.0
            moveRight && !moveLeft -> 8.0
            else -> 0.0
        }
        paddle.x += paddleVx * dt
        paddle.x = paddle.x.coerceIn(0.0, GAME_WIDTH - PADDLE_WIDTH)

        if (!ballLive) {
            // Ball follows paddle before launch
            ball.x = paddle.x + PADDLE_WIDTH / 2.0
            return
        }

        // Ball movement
        ball.x += ballVx * dt
        ball.y += ballVy * dt

        // Wall collisions
        if (ball.x - BALL_RADIUS < 0) {
            ball.x = BALL_RADIUS
            ballVx = -ballVx
        }
        if (ball.x + BALL_RADIUS > GAME_WIDTH) {
            ball.x = GAME_WIDTH - BALL_RADIUS
            ballVx = -ballVx
        }
        if (ball.y - BALL_RADIUS < 0) {
            ball.y = BALL_RADIUS
            ballVy = -ballVy
        }

        // Paddle collision
        if (ballVy > 0 &&
            ball.y + BALL_RADIUS >= paddle.y &&
            ball.y - BALL_RADIUS <= paddle.y + PADDLE_HEIGHT &&
            ball.x >= paddle.x && ball.x <= paddle.x + PADDLE_WIDTH
        ) {
            ball.y = paddle.y - BALL_RADIUS
            ballVy = -ballVy
            // Add some angle based on where the ball hit the paddle
            val hitPos = (ball.x - paddle.x) / PADDLE_WIDTH - 0.5 // -0.5..0.5
            ballVx += hitPos * 4.0
            ballVx = ballVx.coerceIn(-8.0, 8.0)
        }

        // Brick collisions
        val hitBrick = bricks.firstOrNull { brick ->
            ball.x + BALL_RADIUS > brick.x &&
                    ball.x - BALL_RADIUS < brick.x + BRICK_WIDTH &&
                    ball.y + BALL_RADIUS > brick.y &&
                    ball.y - BALL_RADIUS < brick.y + BRICK_HEIGHT
        }
        if (hitBrick != null) {
            app.stage.removeChild(hitBrick)
            hitBrick.destroy()
            bricks.remove(hitBrick)
            ballVy = -ballVy
            score += 10
            onScoreChange(score)

            if (bricks.isEmpty()) {
                onStatusChange("You won! Press New Game to play again.")
                ballLive = false
                resetBall()
            }
        }

        // Ball fell below paddle
        if (ball.y - BALL_RADIUS > GAME_HEIGHT) {
            lives -= 1
            onLivesChange(lives)
            if (lives <= 0) {
                onStatusChange("Game over! Press New Game to try again.")
                ballLive = false
                resetBall()
                lives = 0
            } else {
                onStatusChange("Lost a life! Press SPACE to launch.")
                resetBall()
            }
        }
    }
}
