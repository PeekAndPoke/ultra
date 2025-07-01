# How to publish

How to publish to Maven Central:

```
./gradlew publishAllPublicationsToMavenCentralRepository
```

Maven User and password need to be put into ~/.gradle/gradle.properties:

```
mavenCentralUsername=
mavenCentralPassword=

signing.password=
```

How to publish to Maven local:

```
./gradlew publishToMavenLocal
```

## Some Details

Now we can finally publish to Maven Central.

- https://search.maven.org
- https://repo1.maven.org/maven2/de/devpeak/ultra/

## Nexus

The nexus is here

- https://s01.oss.sonatype.org/

## Helpful Resources

### Links that helped

Sonatype JIRA "peekandpoke.io":
- https://issues.sonatype.org/browse/OSSRH-65427

Sonatype JIRA "devpeak.de":

- https://issues.sonatype.org/browse/OSSRH-65096
- https://issues.sonatype.org/browse/OSSRH-65332

Internet:

- https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/
- https://dev.to/jillesvangurp/publish-kotlin-multiplaform-jars-to-a-private-maven-2bdh
- https://stackoverflow.com/questions/63176482/publish-kotlin-mpp-metadata-with-gradle-kotlin-dsl
- https://kotlinfrompython.com/2018/01/18/how-to-publish-artifacts-to-maven-central-via-gradle/
