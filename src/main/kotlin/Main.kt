package com.dergruenkohl

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.resolve

object Main {
    enum class Mode {
        BLOCK, ITEM
    }
    val baseOutput = Path("output")
    val outputDir = Path("output/assets/minecraft/textures")
    val blockImg = File("assets/block.png")
    val itemImg = File("assets/item.png")
    val packPng = File("assets/pack.png")
    @JvmStatic
    fun main(args: Array<String>) {
        val baseDir = Path("assets/vanilla/assets/minecraft/textures")
        val blockImages = loadImages(baseDir.resolve("block"))
        val itemImages = loadImages(baseDir.resolve("item"))
        prepareOutputDir()
        blockImages.forEach {
            replaceImage(it, Mode.BLOCK)
        }
        itemImages.forEach {
            replaceImage(it, Mode.ITEM)
        }
        zipOutput()
    }
    fun replaceImage(image: File, mode: Mode) {
        when(mode) {
           Mode.BLOCK -> {
                replaceImage(image, blockImg, outputDir.resolve("block").toFile())
           }
            Mode.ITEM -> {
                replaceImage(image, itemImg, outputDir.resolve("item").toFile())
            }
        }
    }

    fun replaceImage(original: File, replacement: File, output: File) {
        val originalName = original.name
        replacement.copyTo(output.resolve(originalName), overwrite = true)
        println("Copied $originalName to $output")
    }
    fun loadImages(path: Path): List<File> {
        val imageFiles = path.toFile().walkBottomUp().filter { it.isFile && it.extension == "png" }.toList()
        println("Found ${imageFiles.count()} images")
        return imageFiles
    }

    fun prepareOutputDir() {
        packPng.copyTo(baseOutput.resolve("pack.png").toFile(), overwrite = true)
        baseOutput.resolve("pack.mcmeta").toFile().writeText("""
            {
              "pack": {
                "pack_format": 55,
                "description": "Wheat, made by salami"
              }
            }

        """.trimIndent())
    }
    fun zipOutput() {
        val outputZip = Path("output/wheat.zip")
        if (outputZip.toFile().exists()) {
            outputZip.toFile().delete()
        }

        println("Zipping output to $outputZip")

        outputZip.toFile().outputStream().use { fileOut ->
            java.util.zip.ZipOutputStream(fileOut).use { zipOut ->
                addFileToZip(zipOut, baseOutput.resolve("pack.png").toFile(), "pack.png")
                addFileToZip(zipOut, baseOutput.resolve("pack.mcmeta").toFile(), "pack.mcmeta")
                addDirectoryToZip(zipOut, baseOutput.resolve("assets").toFile(), "assets")
            }
        }

        println("Zipping completed")
    }

    private fun addFileToZip(zipOut: java.util.zip.ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return

        val entry = java.util.zip.ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        file.inputStream().use { it.copyTo(zipOut) }
        zipOut.closeEntry()
    }

    private fun addDirectoryToZip(zipOut: java.util.zip.ZipOutputStream, dir: File, basePath: String) {
        if (!dir.exists()) return

        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = "$basePath/${dir.toPath().relativize(file.toPath())}"
                addFileToZip(zipOut, file, relativePath.replace("\\", "/"))
            }
        }
    }
}
