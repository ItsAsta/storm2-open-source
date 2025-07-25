rootProject.name = "asta-storm2-open-source"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    "cannonballs"
)

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}
