import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.Duration
import java.util.*

plugins {
    java
}

group = "me.marcdoesntexist"
version = "3.1.0"
description = "Mine management plugin for Paper servers"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io")
}

val papiStub = sourceSets.create("papiStub") {
    java.srcDir("src/papiStub/java")
}

dependencies {
    add(
        papiStub.compileOnlyConfigurationName,
        "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
    )
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.19")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly(files(papiStub.output))
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.sk89q.worldedit:worldedit-bukkit:7.3.19")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(papiStub.classesTaskName)
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("GalaxyMines-${version}.jar")
}









val testServerRoot = layout.buildDirectory.dir("test-server")
val testServerDownloads = testServerRoot.map { it.dir("downloads") }

val testServerArtifacts = linkedMapOf(
    "paper.jar" to
            "https://fill-data.papermc.io/v1/objects/" +
            "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/" +
            "paper-1.21.11-132.jar",
    "worldedit-bukkit-7.4.6.jar" to
            "https://cdn.modrinth.com/data/1u6JkXh5/versions/YFnqiUlL/worldedit-bukkit-7.4.6.jar",
    "worldguard-bukkit-7.0.17.jar" to
            "https://cdn.modrinth.com/data/DKY9btbd/versions/pI4UHLJL/worldguard-bukkit-7.0.17.jar",
    "Vault.jar" to
            "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar",
    "PlaceholderAPI-2.12.3.jar" to
            "https://cdn.modrinth.com/data/lKEzGugV/versions/pIvQcXW8/PlaceholderAPI-2.12.3.jar"
)

fun downloadFile(url: String, target: File) {
    if (target.isFile && target.length() > 0L) {
        return
    }
    target.parentFile.mkdirs()
    val connection = URI.create(url).toURL().openConnection()
    connection.setRequestProperty("User-Agent", "GalaxyMines-TestServer/1.0")
    connection.connectTimeout = 30_000
    connection.readTimeout = 120_000
    connection.getInputStream().use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
    check(target.length() > 0L) { "Downloaded an empty file from $url" }
}

val downloadTestServerArtifacts = tasks.register("downloadTestServerArtifacts") {
    group = "verification"
    description = "Downloads Paper and the plugins used by the local test server."
    testServerArtifacts.forEach { (name, _) ->
        outputs.file(testServerDownloads.map { it.file(name) })
    }
    doLast {
        val directory = testServerDownloads.get().asFile
        directory.mkdirs()
        testServerArtifacts.forEach { (name, url) ->
            val target = File(directory, name)
            downloadFile(url, target)
            logger.lifecycle("Test server dependency ready: ${target.name} (${target.length()} bytes)")
        }
    }
}

val testServerHost = "127.0.0.1"
val testServerPort = 25565
val testServerRconPort = 25575
val testServerRconPassword = "galaxymines-test-server"
val testServerMarker = "-Dgalaxymines.testserver=true"

fun isTcpPortOpen(host: String, port: Int): Boolean {
    return try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 500)
        }
        true
    } catch (_: IOException) {
        false
    }
}

fun waitForTestServerStop(timeout: Duration): Boolean {
    val deadline = System.nanoTime() + timeout.toNanos()
    while (System.nanoTime() < deadline) {
        if (!isTcpPortOpen(testServerHost, testServerPort) &&
            !isTcpPortOpen(testServerHost, testServerRconPort)
        ) {
            return true
        }
        Thread.sleep(500)
    }
    return !isTcpPortOpen(testServerHost, testServerPort) &&
            !isTcpPortOpen(testServerHost, testServerRconPort)
}

fun sendRconCommand(command: String): String {
    Socket().use { socket ->
        socket.connect(InetSocketAddress(testServerHost, testServerRconPort), 2_000)
        socket.soTimeout = 3_000
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        fun readLittleEndianInt(): Int {
            val b0 = input.readUnsignedByte()
            val b1 = input.readUnsignedByte()
            val b2 = input.readUnsignedByte()
            val b3 = input.readUnsignedByte()
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        fun writePacket(requestId: Int, type: Int, payload: String) {
            val data = payload.toByteArray(Charsets.UTF_8)
            val packet = ByteBuffer.allocate(data.size + 14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(data.size + 10)
                .putInt(requestId)
                .putInt(type)
                .put(data)
                .put(0)
                .put(0)
                .array()
            output.write(packet)
            output.flush()
        }

        fun readPacket(): Triple<Int, Int, String> {
            val length = readLittleEndianInt()
            require(length in 10..(4 * 1024 * 1024)) {
                "Invalid RCON packet length: $length"
            }
            val requestId = readLittleEndianInt()
            val type = readLittleEndianInt()
            val payload = ByteArray(length - 10)
            input.readFully(payload)
            input.readUnsignedByte()
            input.readUnsignedByte()
            return Triple(requestId, type, String(payload, Charsets.UTF_8))
        }

        writePacket(1, 3, testServerRconPassword)
        val auth = readPacket()
        check(auth.first == 1) {
            "RCON authentication failed (response id ${auth.first})."
        }

        writePacket(2, 2, command)
        return try {
            readPacket().third
        } catch (_: IOException) {
            ""
        }
    }
}

val prepareTestServer = tasks.register("prepareTestServer") {
    group = "verification"
    description = "Prepares a persistent Paper test server on 127.0.0.1:25565."
    dependsOn(downloadTestServerArtifacts, tasks.named("jar"))
    doLast {
        val server = testServerRoot.get().asFile
        val downloads = testServerDownloads.get().asFile
        val plugins = File(server, "plugins")
        server.mkdirs()
        plugins.mkdirs()

        project.copy {
            from(downloads) { exclude("paper.jar") }
            into(plugins)
        }
        project.copy {
            from(tasks.jar.get().archiveFile)
            into(plugins)
        }
        project.copy {
            from(File(downloads, "paper.jar"))
            into(server)
            rename { "paper.jar" }
        }

        File(server, "eula.txt").writeText("eula=true\n")
        File(server, "server.properties").writeText(
            """
            server-ip=$testServerHost
            server-port=$testServerPort
            online-mode=false
            white-list=true
            enforce-secure-profile=false
            level-name=test-world
            level-type=flat
            generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
            generate-structures=false
            spawn-protection=0
            max-players=10
            view-distance=8
            simulation-distance=6
            allow-nether=false
            allow-end=false
            spawn-monsters=false
            spawn-animals=false
            spawn-npcs=false
            difficulty=peaceful
            gamemode=creative
            enable-command-block=false
            enable-rcon=true
            rcon.port=$testServerRconPort
            rcon.password=$testServerRconPassword
            broadcast-rcon-to-ops=false
            max-tick-time=-1
            pause-when-empty-seconds=0
            motd=GalaxyMines test server
            """.trimIndent() + "\n"
        )

        val devId = UUID.nameUUIDFromBytes("OfflinePlayer:Dev".toByteArray(Charsets.UTF_8))
        val testerId = UUID.nameUUIDFromBytes("OfflinePlayer:Tester".toByteArray(Charsets.UTF_8))
        File(server, "ops.json").writeText(
            """[{"uuid":"$devId","name":"Dev","level":4,"bypassesPlayerLimit":true}]""" +
                    "\n"
        )
        File(server, "whitelist.json").writeText(
            "[\n" +
                    "  {\"uuid\":\"$devId\",\"name\":\"Dev\"},\n" +
                    "  {\"uuid\":\"$testerId\",\"name\":\"Tester\"}\n" +
                    "]\n"
        )

        val pluginData = File(plugins, "GalaxyMines")
        pluginData.mkdirs()
        val config = File(pluginData, "config.yml")
        if (!config.isFile) {
            val base = file("src/main/resources/config.yml").readText()
            config.writeText(
                base.replace("economy:\n  enabled: false", "economy:\n  enabled: true")
            )
        }
        val prices = File(pluginData, "prices.yml")
        if (!prices.isFile) {
            prices.writeText(file("src/main/resources/prices.yml").readText())
        }
        project.copy {
            from(file("src/test/resources/test-server/mines"))
            into(File(pluginData, "mines"))
        }
    }
}

val stopTestServer = tasks.register("stopTestServer") {
    group = "verification"
    description = "Stops the persistent Paper test server and waits for 127.0.0.1:25565 to be free."
    doLast {
        val rconError = runCatching { sendRconCommand("stop") }.exceptionOrNull()
        if (rconError != null) {
            if (isTcpPortOpen(testServerHost, testServerPort)) {
                throw GradleException(
                    "Port $testServerPort is in use, but the test-server RCON console is " +
                            "unavailable. This usually means the running instance predates RCON " +
                            "support. Stop it once from its console or terminal, then rerun this " +
                            "task. RCON error: ${rconError.message}"
                )
            }
            logger.lifecycle("The persistent test server is not running.")
            return@doLast
        }

        logger.lifecycle("Requested a graceful stop through the test-server RCON console.")
        if (!waitForTestServerStop(Duration.ofSeconds(30))) {
            throw GradleException(
                "The test server did not release $testServerHost:$testServerPort and " +
                        "$testServerHost:$testServerRconPort within 30 seconds."
            )
        }
        Thread.sleep(1_000)
        logger.lifecycle("Persistent test server stopped; $testServerHost:$testServerPort is free.")
    }
}

prepareTestServer.configure { mustRunAfter(stopTestServer) }

val runTestServer = tasks.register<Exec>("runTestServer") {
    group = "verification"
    description = "Runs the persistent Paper test server on 127.0.0.1:25565."
    dependsOn(prepareTestServer)
    mustRunAfter(stopTestServer)
    workingDir(testServerRoot.get().asFile)
    val javaName = if (System.getProperty("os.name").lowercase().contains("win")) {
        "java.exe"
    } else {
        "java"
    }
    val javaExecutable = File(System.getProperty("java.home"), "bin/$javaName")
    commandLine(
        javaExecutable.absolutePath,
        "-Xms1G",
        "-Xmx2G",
        testServerMarker,
        "-jar",
        "paper.jar",
        "--nogui"
    )
    standardInput = System.`in`
    isIgnoreExitValue = true
}

val restartTestServer = tasks.register("restartTestServer") {
    group = "verification"
    description = "Stops, prepares and restarts the persistent Paper test server in the foreground."
    dependsOn(stopTestServer, runTestServer)
}

val providerApiRoot = layout.buildDirectory.dir("provider-api")

val providerApiArtifacts = linkedMapOf(
    "nexo-1.29.1.jar" to
            "https://repo.nexomc.com/releases/com/nexomc/nexo/1.29.1/nexo-1.29.1.jar",
    "oraxen-1.220.1.jar" to
            "https://repo.oraxen.com/releases/io/th0rgal/oraxen/1.220.1/oraxen-1.220.1.jar",
    "itemsadder-api-4.0.18-beta-10.jar" to
            "https://repo1.maven.org/maven2/beer/devs/itemsadder-api/4.0.18-beta-10/" +
            "itemsadder-api-4.0.18-beta-10.jar"
)



val providerApiSha256 = mapOf(
    "nexo-1.29.1.jar" to "3f0f6578d49c9c24c6bbdcd3dafb2bb43980d9929d069345ab5ae9a646209cbf",
    "oraxen-1.220.1.jar" to "f41367a231c06442b0ad931cbc829007a83475b2d93b8d3e08c55a160cb81f1b",
    "itemsadder-api-4.0.18-beta-10.jar" to
            "576115ae3ac6ad54a932ef861820486b268bd11b020abc9c04103021626f4c68"
)

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

val downloadProviderApiArtifacts = tasks.register("downloadProviderApiArtifacts") {
    group = "verification"
    description = "Downloads the official artifacts used by providerApiCheck."
    providerApiArtifacts.keys.forEach { name ->
        outputs.file(providerApiRoot.map { it.file(name) })
    }
    doLast {
        val directory = providerApiRoot.get().asFile
        directory.mkdirs()
        providerApiArtifacts.forEach { (name, url) ->
            val target = File(directory, name)
            downloadFile(url, target)
            val expected = providerApiSha256[target.name]
            if (expected != null) {
                val actual = sha256(target)
                check(actual.equals(expected, ignoreCase = true)) {
                    "Checksum mismatch for ${target.name}: expected $expected, got $actual. " +
                            "Delete the cached file and update the expected hash only after verifying " +
                            "the new artifact."
                }
            }
            logger.lifecycle("Provider API artifact ready: ${target.name} (${target.length()} bytes)")
        }
    }
}

val providerApiExpectations = linkedMapOf(
    "nexo-1.29.1.jar" to linkedMapOf(
        "com.nexomc.nexo.api.NexoBlocks" to listOf(
            "public static final void place(java.lang.String, org.bukkit.Location);",
            "public static final boolean isCustomBlock(org.bukkit.block.Block);",
            "public static final com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic " +
                    "customBlockMechanic(org.bukkit.block.Block);"
        ),
        "com.nexomc.nexo.api.NexoItems" to listOf(
            "public static final com.nexomc.nexo.items.ItemBuilder itemFromId(java.lang.String);",
            "public static final java.lang.String idFromItem(org.bukkit.inventory.ItemStack);"
        ),
        "com.nexomc.nexo.mechanics.Mechanic" to listOf(
            "public final java.lang.String getItemID();"
        ),
        "com.nexomc.nexo.items.ItemBuilder" to listOf(
            "public final org.bukkit.inventory.ItemStack build();"
        )
    ),
    "oraxen-1.220.1.jar" to linkedMapOf(
        "io.th0rgal.oraxen.api.OraxenBlocks" to listOf(
            "public static void place(java.lang.String, org.bukkit.Location);",
            "public static boolean isOraxenBlock(org.bukkit.block.Block);",
            "public static io.th0rgal.oraxen.mechanics.Mechanic " +
                    "getOraxenBlock(org.bukkit.Location);"
        ),
        "io.th0rgal.oraxen.api.OraxenItems" to listOf(
            "public static io.th0rgal.oraxen.items.ItemBuilder getItemById(java.lang.String);",
            "public static java.lang.String getIdByItem(org.bukkit.inventory.ItemStack);"
        ),
        "io.th0rgal.oraxen.mechanics.Mechanic" to listOf(
            "public java.lang.String getItemID();"
        ),
        "io.th0rgal.oraxen.items.ItemBuilder" to listOf(
            "public org.bukkit.inventory.ItemStack build();"
        )
    ),
    "itemsadder-api-4.0.18-beta-10.jar" to linkedMapOf(
        "dev.lone.itemsadder.api.CustomBlock" to listOf(
            "public static dev.lone.itemsadder.api.CustomBlock place(java.lang.String, " +
                    "org.bukkit.Location);",
            "public static dev.lone.itemsadder.api.CustomBlock " +
                    "byAlreadyPlaced(org.bukkit.block.Block);",
            "public static dev.lone.itemsadder.api.CustomBlock getInstance(java.lang.String);"
        ),
        "dev.lone.itemsadder.api.CustomStack" to listOf(
            "public static dev.lone.itemsadder.api.CustomStack getInstance(java.lang.String);",
            "public static dev.lone.itemsadder.api.CustomStack " +
                    "byItemStack(org.bukkit.inventory.ItemStack);",
            "public org.bukkit.inventory.ItemStack getItemStack();",
            "public java.lang.String getNamespacedID();"
        )
    )
)

val providerApiCheck = tasks.register("providerApiCheck") {
    group = "verification"
    description = "Checks the reflective provider adapters against official provider artifacts."
    dependsOn(downloadProviderApiArtifacts)
    doLast {
        val javaHome = System.getProperty("java.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val javap = File(javaHome, "bin/javap${if (isWindows) ".exe" else ""}")
        if (!javap.isFile) {
            throw GradleException("javap not found at ${javap.absolutePath}")
        }
        val artifacts = providerApiRoot.get().asFile
        providerApiExpectations.forEach { (jarName, classes) ->
            val jar = File(artifacts, jarName)
            if (!jar.isFile) {
                throw GradleException("Provider artifact missing: ${jar.absolutePath}")
            }
            classes.forEach { (className, expectedMethods) ->
                val process = ProcessBuilder(
                    javap.absolutePath,
                    "-classpath",
                    jar.absolutePath,
                    "-public",
                    className
                ).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw GradleException(
                        "javap failed for $className in ${jar.name} (exit $exitCode):\n$output"
                    )
                }
                val actualMethods = output.lineSequence().map { it.trim() }.toSet()
                val missing = expectedMethods.filterNot(actualMethods::contains)
                if (missing.isNotEmpty()) {
                    throw GradleException(
                        "Provider API mismatch in ${jar.name} / $className. Missing:\n" +
                                missing.joinToString("\n") +
                                "\nActual output:\n$output"
                    )
                }
            }
        }
        logger.lifecycle(
            "Provider API check passed: Nexo 1.29.1, Oraxen 1.220.1, ItemsAdder API 4.0.18-beta-10"
        )
    }
}
