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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.NotUnderContentRootModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.testSourceInfo
import org.jetbrains.kotlin.idea.scratch.ui.scratchTopPanel
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.moduleInfo
import java.io.File

class ScratchFileModuleInfoProvider(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        myProject.messageBus.connect(myProject).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, ScratchFileModuleListener())
    }

    private inner class ScratchFileModuleListener : FileEditorManagerListener {
        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            if (!file.isValid) return
            if (!ScratchFileService.isInScratchRoot(file)) return

            val ktFile = PsiManager.getInstance(myProject).findFile(file) as? KtFile ?: return

            // Hack before api in IDEA will be introduces
            if (file.extension == KotlinFileType.EXTENSION) {
                runWriteAction {
                    val newName = file.nameWithoutExtension + KotlinParserDefinition.STD_SCRIPT_EXT
                    val existed = VfsUtil.findFileByIoFile(File(file.parent.path, newName), true)
                    if (existed == null) {
                        file.rename(this, newName)
                    }
                }
            }

            val fileEditorManager = FileEditorManager.getInstance(myProject)
            val editor = fileEditorManager.getAllEditors(file).firstOrNull()
            val panel = editor?.scratchTopPanel ?: return

            ktFile.moduleInfo = getModuleInfo(panel.getModule())

            panel.addModuleListener {
                ktFile.moduleInfo = getModuleInfo(it)
                // Drop caches for old module
                ProjectRootManager.getInstance(myProject).incModificationCount()
                // Force re-highlighting
                WriteCommandAction.runWriteCommandAction(myProject) { ktFile.add(KtPsiFactory(myProject).createColon()).delete() }
            }
        }
    }

    private fun getModuleInfo(it: Module?) =
        it?.testSourceInfo() ?: it?.productionSourceInfo() ?: NotUnderContentRootModuleInfo
}