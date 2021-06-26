abstract class EmbedShadersTask : DefaultTask() {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val defaultPackage: Property<String>

    @get:Input
    abstract val defaultVisibility: Property<String>

    @get:Input
    abstract val namePrefix: Property<String>

    init {
        defaultVisibility.set("")
        namePrefix.set("")
    }

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            val name = "${namePrefix.get()}${change.file.nameWithoutExtension.replace("-","_")}"
            val targetFile = outputDir.file(change.normalizedPath.replace(".","_")+".kt").get().asFile
            if (change.changeType == ChangeType.REMOVED) {
                targetFile.delete()
            } else {
                val contents = change.file.readText()
                val lines = contents.split("\n")
                var packageStatement = "package ${defaultPackage.get()}\n"
                val visibilityStatement = if (defaultVisibility.get().isNotBlank()) "${defaultVisibility.get()} " else ""

                val r = Regex("#pragma package ([a-z.]+)")
                for (line in lines) {
                    val m = r.find(line.trim())
                    if (m != null) {
                        packageStatement = "package ${m.groupValues[1]}\n"
                    }
                }
                val text = "${packageStatement}${visibilityStatement}const val $name = ${"\"\"\""}${contents}${"\"\"\""}"
                targetFile.writeText(text)
            }
        }
    }
}