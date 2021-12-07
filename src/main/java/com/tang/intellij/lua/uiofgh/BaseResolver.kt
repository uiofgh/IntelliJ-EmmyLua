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
    private var _dofileMapFlags: HashMap<String, Boolean> = HashMap()
    private var _dofileMapNames: Array<String> = arrayOf("class", "import", "initlog", "initxlib", "initglobals", "initprotos", "initsound", "initmodule", "initgame", "msganalyse", "events", "initinput")
    private var _classInheritMap: HashMap<String, HashSet<String>> = HashMap()

    init {
        for (name in _dofileMapNames) {
            _dofileMapFlags[name] = false
        }
    }

    fun dofileMap(project: Project, context: SearchContext): HashMap<String, LuaPsiFile> {
        val manager = LuaShortNamesManager.getInstance(project)
        for ((name, flag) in _dofileMapFlags) {
            if (flag) continue
            val module = resolveRequireFile(name, project)
            if (module is LuaPsiFile) {
                val members = manager.getClassMembers(module.cModuleName, context)
                members.forEach {
                    val tmp = it.name
                    if (tmp != null)
                        _dofileMap[tmp] = module
                    _dofileMapFlags[name] = true
                }
            }
        }
        return _dofileMap
    }

    fun dofiles(project: Project, context: SearchContext): Array<LuaPsiFile> {
        dofileMap(project, context)
        return _dofileMap.values.toTypedArray()
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

    fun getModuleName(refName: String, project: Project, context: SearchContext): String? {
        val map = dofileMap(project, context)
        if (map.containsKey(refName)) return map[refName]?.moduleName
        return null
    }
}
