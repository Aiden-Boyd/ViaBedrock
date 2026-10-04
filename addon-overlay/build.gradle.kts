plugins {
    id("base.java")
    id("base.fabric")
    id("configuration.transitive_jar_in_jar")
    id("via.maven_publish")
}

dependencies {
    implementation(libs.viafabricplus)
    // Keep its existing nested engines intact; users install only our outer jar.
    jarInJar(libs.viafabricplus) { isTransitive = false }

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

if (System.getenv("VIA_BEDROCK_STANDALONE_SMOKE") == "true") {
    tasks.named<JavaExec>("runClient") {
        doFirst {
            val launch = groovy.json.JsonOutput.toJson(mapOf(
                "java" to javaLauncher.get().executablePath.asFile.absolutePath,
                "classpath" to classpath.files.map { it.absolutePath },
                "vanillaLibraries" to listOf(
                    "minecraftLibraries", "minecraftRuntimeLibraries",
                    "minecraftClientLibraries", "minecraftClientRuntimeLibraries",
                    "loaderLibraries"
                ).flatMap { configurations.getByName(it).files }.map { it.absolutePath },
                "jvmArgs" to allJvmArgs,
                "args" to (args ?: emptyList<String>())
            ))
            layout.buildDirectory.file("standalone-launch.json").get().asFile.writeText(launch)
            // The smoke script launches Knot directly against the packaged mod.
            throw GradleException("STANDALONE_LAUNCH_EXPORTED")
        }
    }
}
