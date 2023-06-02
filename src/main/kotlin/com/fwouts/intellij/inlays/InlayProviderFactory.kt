package com.fwouts.intellij.inlays

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class InlayProviderFactory : InlayHintsProviderFactory {
    @Deprecated("Use getProvidersInfo without project", replaceWith = ReplaceWith("getProvidersInfo()"))
    override fun getProvidersInfo(project: Project): List<ProviderInfo<out Any>> {
        return getLanguages().map { l -> ProviderInfo(l, InlayProvider()) }
    }

    override fun getProvidersInfo(): List<ProviderInfo<out Any>> {
        return getLanguages().map { l -> ProviderInfo(l, InlayProvider()) }
    }

    override fun getLanguages(): Iterable<Language> {
        return Language.getRegisteredLanguages()
    }

    override fun getProvidersInfoForLanguage(language: Language): List<InlayHintsProvider<out Any>> {
        return listOf(InlayProvider())
    }

    class InlayProvider : InlayHintsProvider<NoSettings> {
        companion object {
            var counter = 0

            init {
                val app = ApplicationManager.getApplication()
                val projectManager = ProjectManager.getInstance()

                Timer().scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        app.invokeLater {
                            counter += 1
                            ParameterHintsPassFactory.forceHintsUpdateOnNextPass()
                            projectManager.openProjects.forEach { project ->
                                DaemonCodeAnalyzer.getInstance(project).restart()
                            }
                        }
                    }
                }, 1_000, 1_000)
            }
        }

        override val key = SettingsKey<NoSettings>(InlayProvider::class.qualifiedName!!)
        override val name = "Inlays Refresh"
        override val previewText = null
        override fun createSettings() = NoSettings()
        override val isVisibleInSettings = false

        override fun isLanguageSupported(language: Language): Boolean {
            return true
        }

        override fun getCollectorFor(
            file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
        ) = object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                sink.addBlockElement(
                    offset = 0,
                    relatesToPrecedingText = false,
                    showAbove = false,
                    priority = 0,
                    presentation = factory.text("Counter = $counter")
                )
                return false
            }
        }

        override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
            return object : ImmediateConfigurable {
                override fun createComponent(listener: ChangeListener) = JPanel()
            }
        }
    }
}
