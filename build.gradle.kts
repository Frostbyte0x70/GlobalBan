plugins {
    kotlin("jvm") version "2.2.0"
}

group = "org.globalban"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.6.1") {
        exclude(module="opus-java") // required for encoding audio into opus, not needed if audio is already provided in opus encoding
        exclude(module="tink") // required for encrypting and decrypting audio
    }
    implementation("club.minnced:jda-ktx:0.12.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.25.2")
	implementation("com.mysql:mysql-connector-j:9.4.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}