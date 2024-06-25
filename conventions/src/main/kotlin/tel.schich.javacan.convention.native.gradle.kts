plugins {
    id("tel.schich.javacan.convention.base")
}

dependencies {
    "tel.schich:jni-access-generator:1.1.2".let {
        annotationProcessor(it)
        compileOnly(it)
        implementation(it)
        api(it)
    }
}

