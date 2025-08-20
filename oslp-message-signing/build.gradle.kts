dependencies {
    implementation(libs.springBoot)
    implementation(libs.springContext)
    implementation(libs.slf4jApi)

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}