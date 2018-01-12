/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.scratch.ui


import com.intellij.application.options.ModulesComboBox
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.components.panels.HorizontalLayout
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.psi.UserDataProperty
import javax.swing.*

var FileEditor.scratchTopPanel: ScratchTopPanel? by UserDataProperty<FileEditor, ScratchTopPanel>(Key.create("scratch.panel"))
val ScratchFile.scratchTopPanel: ScratchTopPanel?
    get() {
        val fileEditorManager = FileEditorManager.getInstance(psiFile.project)
        val editor = fileEditorManager.getAllEditors(psiFile.virtualFile).firstOrNull()
        return editor?.scratchTopPanel
    }

class ScratchTopPanel(project: Project) : JPanel(HorizontalLayout(5)) {
    val progressBar: JProgressBar

    private val moduleChooser: ModulesComboBox
    private val isReplCheckbox: JCheckBox
    private val isMakeBeforeRunCheckbox: JCheckBox

    init {
        add(createActionsToolbar())

        isReplCheckbox = JCheckBox("Use REPL", false)
        isReplCheckbox.verticalTextPosition = SwingConstants.BOTTOM
        isReplCheckbox.horizontalTextPosition = SwingConstants.LEADING
        add(isReplCheckbox)

        add(JSeparator(SwingConstants.VERTICAL))

        isMakeBeforeRunCheckbox = JCheckBox("Make before Run", false)
        isMakeBeforeRunCheckbox.verticalTextPosition = SwingConstants.BOTTOM
        isMakeBeforeRunCheckbox.horizontalTextPosition = SwingConstants.LEADING
        add(isMakeBeforeRunCheckbox)

        add(JSeparator(SwingConstants.VERTICAL))

        moduleChooser = createModuleChooser(project)
        add(JLabel("Use classpath of module"))
        add(moduleChooser)

        progressBar = JProgressBar()
        progressBar.isVisible = false
        add(progressBar)
    }

    fun getModule(): Module? = moduleChooser.selectedModule

    fun addModuleListener(f: (Module) -> Unit) {
        moduleChooser.addActionListener {
            moduleChooser.selectedModule?.let { f(it) }
        }
    }

    fun isRepl() = isReplCheckbox.isSelected
    fun isMakeBeforeRun() = isMakeBeforeRunCheckbox.isSelected

    @TestOnly
    fun setReplMode(isSelected: Boolean) {
        isReplCheckbox.isSelected = isSelected
    }

    private fun createActionsToolbar(): JComponent {
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(RunScratchAction())
        toolbarGroup.addSeparator()
        toolbarGroup.add(ClearScratchAction())

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true).component
    }

    private fun createModuleChooser(project: Project): ModulesComboBox {
        val modulesBox = ModulesComboBox()
        modulesBox.fillModules(project)
        modulesBox.selectedIndex = 0
        return modulesBox
    }
}
