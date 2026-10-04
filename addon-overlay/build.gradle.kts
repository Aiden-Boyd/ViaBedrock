plugins {
    id("base.java")
    id("base.fabric")
    id("configuration.transitive_jar_in_jar")
    id("via.maven_publish")
}

dependencies {
    implementation(libs.viafabricplus)

    jarInJar(libs.viabedrock) {
        exclude(group = "com.mojang", module = "brigadier")
        exclude(group = "at.yawk.lz4", module = "lz4-java")
        exclude(group = "io.netty")
    }
    jarInJar(libs.minecraftauth) {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    jarInJar(libs.netty.transport.raknet) {
        exclude(group = "io.netty")
    }
    jarInJar(libs.netty.transport.nethernet) {
        exclude(group = "io.netty")
        exclude(group = "org.bouncycastle")
        exclude(group = "dev.opencollab", module = "libdatachannel-java")
    }
    jarInJar(libs.libdatachannel.java.arch.detect)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.test {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

if (System.getenv("VIA_BEDROCK_MENU_SMOKE") == "true") {
    tasks.named<JavaExec>("runClient") {
        systemProperty("viaBedrock.menuSmoke", "true")
    }
}
