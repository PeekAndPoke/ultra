# How to publish

```
./gradlew clean build publish -Psigning.password= -PmavenCentralRepositoryPassword=
```

Now we can finally publish to Maven Central.

- https://search.maven.org
- https://repo1.maven.org/maven2/de/devpeak/ultra/

## Nexus

The nexus is here

- https://s01.oss.sonatype.org/

## Helpful Resources

### Links that helped

Sonatype JIRA:

- https://issues.sonatype.org/browse/OSSRH-65096
- https://issues.sonatype.org/browse/OSSRH-65332

Internet:

- https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/
- https://dev.to/jillesvangurp/publish-kotlin-multiplaform-jars-to-a-private-maven-2bdh
- https://stackoverflow.com/questions/63176482/publish-kotlin-mpp-metadata-with-gradle-kotlin-dsl
- https://kotlinfrompython.com/2018/01/18/how-to-publish-artifacts-to-maven-central-via-gradle/

### Video Series

- [How to Publish a Java Library to Maven Central](https://www.youtube.com/watch?v=bxP9IuJbcDQ)


- [01 Claim Your Namespace Easy Publishing to Central Repository](https://www.youtube.com/watch?v=MmNg0E_Pr64)
- [02 Applying for Access to OSS RH Easy Publishing to Central Repository](https://www.youtube.com/watch?v=DXn6JoiYtEM)
- [03 Requirements and Signing Tips for OSS RH Easy Publishing to Central Repos](https://www.youtube.com/watch?v=C-kIh0Mt6sg)
- [04 Accessing OSS RH Easy Publishing to Central Repository](https://www.youtube.com/watch?v=_EbFme_5hM8)
- [05 First Deployments Easy Publishing to Central Repository](https://www.youtube.com/watch?v=ZuuV2cUdrSk)
- [06 Project Object Model POM Easy Publishing to Central Repository](https://www.youtube.com/watch?v=pFPGPOKgzm0)
- [07 Javadoc, Sources and Signing Easy Publishing to Central Repository](https://www.youtube.com/watch?v=lsfMxKZWtpM)

## Maven publish with bintray

All
> ./gradlew clean build && ./gradlew --parallel bintrayUpload -PbintrayUser=peekandpoke -PbintrayApiKey=

Single subproject
> ./gradlew clean build :SUB-PROJECT:bintrayUpload -PbintrayUser=peekandpoke -PbintrayApiKey=

## Long road to maven publish

Documents I read to get publishing working:

1. Setting up the project with gradle kotlin dsl

> https://guides.gradle.org/building-kotlin-jvm-libraries

2. Setting up upload to bintray

> https://plugins.gradle.org/plugin/com.jfrog.bintray
 
> https://github.com/bintray/bintray-examples/tree/master/gradle-bintray-plugin-examples

... finally found something useful

> http://bastienpaul.fr/wordpress/2019/02/08/publish-a-kotlin-lib-with-gradle-kotlin-dsl/


