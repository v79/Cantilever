plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "Cantilever"
include("FileUploadHandler")
include("MarkdownProcessor")
include("SharedModels")
include("TemplateProcessor")
include("API")
include("OpenAPISchemaGenerator")
include("OpenAPISchemaAnnotations")
include("ImageProcessor")
