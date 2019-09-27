package com.microsoft.tfs

import com.jetbrains.rd.util.CommonsLoggingLoggerFactory
import com.jetbrains.rd.util.ILoggerFactory
import com.jetbrains.rd.util.Statics
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.FileAppender
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logging {
    val factory = CommonsLoggingLoggerFactory
    val layout = PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n")

    fun initialize(logDirectory: Path?, level: Level) {
        org.apache.log4j.Logger.getRootLogger().apply {
            removeAllAppenders()

            addAppender(ConsoleAppender(layout))
            logDirectory?.let { directory ->
                val startDate = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                val currentProcessId = ManagementFactory.getRuntimeMXBean().name
                val logFilePath = directory.resolve("$startDate.$currentProcessId.log")
                addAppender(FileAppender(layout, logFilePath.toString(), true))
                println("Logs are saved to $logFilePath")
            }

            this.level = level
            println("Log level enabled: $level")
        }

        Statics<ILoggerFactory>().push(factory)
    }

    fun getLogger(category: String) = factory.getLogger(category)
    inline fun <reified T> getLogger() = factory.getLogger(T::class.java.name)
}