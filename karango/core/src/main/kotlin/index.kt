package de.peekandpoke.karango

import com.arangodb.ArangoDB
import com.arangodb.ArangoDatabaseAsync
import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.slumber.KarangoCodec
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

fun KontainerBuilder.karango(config: ArangoDbConfig) = module(KarangoModule, config)

fun getKarangoDefaultSlumberConfig() = SlumberConfig.default.prependModules(VaultSlumberModule)

/**
 * Karango kontainer module
 *
 * When using this you need to provide the following:
 *
 * ```
 *   instance(ArangoDatabase::class, myArangoDatabase)
 * ```
 */
val KarangoModule = module { config: ArangoDbConfig ->

    val arangoDb: ArangoDatabaseAsync = config.toArangoDb()

    instance(ArangoDatabaseAsync::class, arangoDb)

    dynamic(KarangoDriver::class)

    // We initialize the SlumberConfig once, so we can re-use it.
    // This is important so for caching the codec lookups
    val codecConfig = getKarangoDefaultSlumberConfig()

    dynamic(KarangoCodec::class) { database: Database, cache: EntityCache ->
        KarangoCodec(
            config = codecConfig,
            database = database,
            entityCache = cache
        )
    }
}

private val dbCache = mutableMapOf<ArangoDbConfig, ArangoDatabaseAsync>()
private val lock = Any()


fun ArangoDbConfig.toArangoDb(): ArangoDatabaseAsync = synchronized(lock) {
    // TODO: check that the DB was not yet shut down
    dbCache[this]?.let { return it }

    return toArangoDbWithoutCache().also { dbCache[this] = it }
}

fun ArangoDbConfig.toArangoDbWithoutCache(): ArangoDatabaseAsync {
    val db = ArangoDB.Builder().apply {
        user(user)
        password(password)
        host(host, port)

        // Trying new settings
//        .keepAliveInterval(5)
//        .acquireHostList(true)
        // seem not to work on live
//        .maxConnections(50)
//        .timeout(30.seconds.inWholeMilliseconds.toInt())
//        .connectionTtl(1.hours.inWholeMilliseconds)

        if (useSsl && caCertX509 != null) {
            useSsl(true)
            sslContext(createSsl(caCertX509))
        }
    }.build().async()

    return db.db(database)
}

private fun createSsl(encodedCA: String): SSLContext {

    val inputStream: InputStream = ByteArrayInputStream(Base64.getDecoder().decode(encodedCA))

    val cf = CertificateFactory.getInstance("X.509")
    val caCert: X509Certificate = cf.generateCertificate(inputStream) as X509Certificate

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    ks.load(null)
    ks.setCertificateEntry("caCert", caCert)

    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.trustManagers, null)

    return sslContext
}
