package com.intellij.i18n

import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jacoco.core.analysis.ISourceFileCoverage
import org.jacoco.core.data.ExecutionData
import org.jacoco.core.data.SessionInfo
import org.jacoco.core.internal.analysis.BundleCoverageImpl
import org.jacoco.core.internal.analysis.CounterImpl
import org.jacoco.core.internal.analysis.SourceFileCoverageImpl
import org.jacoco.report.DirectorySourceFileLocator
import org.jacoco.report.xml.XMLFormatter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths

/**
 * @author traff
 */
open class CoverageI18N: DefaultTask() {
    lateinit var dir: String

    @TaskAction
    fun main() {
        val formatter = XMLFormatter()

        val file = File(dir, "reports/jacoco/test/report.xml")
        file.parentFile.mkdirs()
        file.createNewFile()

        val fileOutputStream = FileOutputStream(file)

        val visitor = formatter.createVisitor(fileOutputStream)

        val started = System.currentTimeMillis()


        val resources = File(dir)

        val executionData = mutableListOf<ExecutionData>()

        val sourceFiles = mutableListOf<ISourceFileCoverage>()

        for (f in resources.walk()) {
            if (f.isFile && f.extension == "properties") {
                println(f.absolutePath)
                val params = Parameters()
                val builder =
                        FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration::class.java)
                                .configure(params.properties()
                                        .setFileName(f.absolutePath)
                                        .setListDelimiterHandler(DefaultListDelimiterHandler(',')))
                val config = builder.configuration


                val source = SourceFileCoverageImpl(f.name, Paths.get(dir).relativize(Paths.get(f.parent)).toString().replace("/", "."))
                source.ensureCapacity(0, config.size())


                for ((i, k) in config.keys.withIndex()) {
                    val str = config.getString(k)
                    val cnt = if (!notTranslated(str)) CounterImpl.COUNTER_0_1 else CounterImpl.COUNTER_0_1

                    source.increment(cnt, CounterImpl.COUNTER_0_0, i)
                }

                sourceFiles.add(source)
            }
        }
        val si = SessionInfo("id", started, System.currentTimeMillis())


        visitor.visitInfo(arrayListOf(si), arrayListOf())


        visitor.visitBundle(BundleCoverageImpl("", arrayListOf(), sourceFiles), DirectorySourceFileLocator(File(dir), "UTF-8", 4))



        visitor.visitInfo(arrayListOf(si), executionData)



        visitor.visitEnd()

        fileOutputStream.close()
    }

    private fun notTranslated(translatedStr: String) = translatedStr.matches(Regex("\\p{ASCII}+"))
}

fun main(args: Array<String>) {
    val coverageI18N = CoverageI18N()
    coverageI18N.dir = "/Users/traff/Projects/pycharm_kr/build/resources/main"
    coverageI18N.main()
}