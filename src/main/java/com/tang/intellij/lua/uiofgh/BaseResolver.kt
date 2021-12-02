/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
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

package com.tang.intellij.lua.uiofgh

import com.intellij.openapi.project.Project
import com.tang.intellij.lua.editor.completion.ClassMemberCompletionProvider
import com.tang.intellij.lua.editor.completion.CompletionSession
import com.tang.intellij.lua.editor.completion.MemberCompletionMode
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.TyLazyClass

open class BaseResolver {
    open var resolverType: ResolverType = ResolverType.Base
    private var _dofileMap: HashMap<String, LuaPsiFile> = HashMap()
    private var _dofiles: ArrayList<LuaPsiFile> = ArrayList()
    private var _classInheritMap: HashMap<String, HashSet<String>> = HashMap()

    fun dofileMap(project: Project, context: SearchContext): HashMap<String, LuaPsiFile> {
        if (_dofileMap.isEmpty() || _dofileMap.size < 60) {
            val file = resolveRequireFile("initgenv", project) ?: return _dofileMap
            val manager = LuaShortNamesManager.getInstance(project)

            LuaPsiTreeUtil.walkTopLevelInFile(file.lastChild, LuaExprStat::class.java) { it2 ->
                val child = it2.firstChild
                if (child is LuaCallExpr) {
                    val func = child.firstChild
                    if (func is LuaNameExpr && func.name == "dofile") {
                        val arg = child.lastChild
                        if (arg is LuaListArgs) {
                            val args = arg.exprList
                            val path = args[0].text.substringAfter('"').substringBefore(".lua").substringAfter('/')
                            val module = resolveRequireFile(path, project)
                            if (module is LuaPsiFile) {
                                val members = manager.getClassMembers(module.cModuleName, context)
                                members.forEach {
                                    if (it.text != null)
                                        _dofileMap[it.text] = module
                                }
                            }
                        }
                    }
                }
                true
            }
        }
        return _dofileMap
    }

    fun dofiles(project: Project, context: SearchContext): ArrayList<LuaPsiFile> {
        if (_dofiles.size < 8) {
            val file = resolveRequireFile("initgenv", project) ?: return _dofiles

            LuaPsiTreeUtil.walkTopLevelInFile(file.lastChild, LuaExprStat::class.java) { it2 ->
                val child = it2.firstChild
                if (child is LuaCallExpr) {
                    val func = child.firstChild
                    if (func is LuaNameExpr && func.name == "dofile") {
                        val arg = child.lastChild
                        if (arg is LuaListArgs) {
                            val args = arg.exprList
                            val path = args[0].text.substringAfter('"').substringBefore(".lua").substringAfter('/')
                            val module = resolveRequireFile(path, project)
                            if (module is LuaPsiFile) {
                                _dofiles.add(module)
                            }
                        }
                    }
                }
                true
            }
        }
        return _dofiles
    }

    fun addDofileCompletions(session: CompletionSession, provider: ClassMemberCompletionProvider) {
        val completionParameters = session.parameters
        val completionResultSet = session.resultSet
        val cur = completionParameters.position
        val nameExpr = cur.parent
        val project = cur.project
        val context = SearchContext.get(project)
        for (t in dofiles(project, context)) {
            val moduleName = t.cModuleName
            val ty = TyLazyClass(moduleName)
            val contextTy = LuaPsiTreeUtil.findContextClass(nameExpr)
            provider.addClass(contextTy, ty, cur.project, MemberCompletionMode.Dot, completionResultSet, completionResultSet.prefixMatcher, null)
        }
    }

    fun regClassInherit(son: String, parent: String? = null) {
        if (!_classInheritMap.containsKey(son)) {
            val set = HashSet<String>();
            _classInheritMap[son] = set
        }
        val set = _classInheritMap[son]
        if (parent != null) {
            set?.add(parent)
        }
    }

    fun getLazyClass(son: String): ITy {
        if (_classInheritMap.containsKey(son)) {
            val set = _classInheritMap[son]
            if (set != null) {
                return TyLazyClass(son, superClassName2 = set.toTypedArray())
            }
        }
        return TyLazyClass(son)
    }
}
