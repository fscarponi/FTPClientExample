import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

application.mainClass.set("ApplicationKt")
group = "com.github.fscarponi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}

dependencies {
    testImplementation(kotlin("test"))
// https://mvnrepository.com/artifact/it.sauronsoftware/ftp4j
//    implementation("it.sauronsoftware:ftp4j:1.7.2")
    implementation("commons-net:commons-net:3.8.0")

}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
