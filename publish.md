# How to publish

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


