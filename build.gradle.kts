tasks.register("assembleDebug") {
    doLast {
        println("Dummy assembleDebug for Web app export")
        val apkDir = file("app/build/outputs/apk/debug")
        apkDir.mkdirs()
        file("${apkDir}/app-debug.apk").writeText("dummy apk")
    }
}
