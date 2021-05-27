val VERSION_NAME: String by project

val mavenCentralUsername: String by project
val mavenCentralPassword: String by project

apply(plugin = "com.vanniktech.maven.publish")

configure<PublishingExtension> {

    repositories {

        withType<MavenArtifactRepository> {

            if (url.host == "oss.sonatype.org") {

                val before = url

                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"

                url = if (VERSION_NAME.endsWith("SNAPSHOT")) {
                    uri(snapshotsRepoUrl)
                } else {
                    uri(releasesRepoUrl)
                }

//                println("renamed maven repo ${name} url from")
//                println(before)
//                println("to")
//                println(url)

                credentials {
                    username = mavenCentralUsername
                    password = mavenCentralPassword
                }
            }
        }
    }
}
