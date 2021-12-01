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

enum class ResolverType {
    Base, Dh25Client, Dh25Server
}

object ResolverFactory {
    var resolverCache: MutableMap<String, BaseResolver> = HashMap()
    fun rebuildReference(project: Project) {
        val key = project.locationHash
        resolverCache.remove(key)
    }

    fun getResolver(resolverType: ResolverType, project: Project): BaseResolver {
        val key = project.locationHash
        if (!resolverCache.containsKey(key) || resolverCache[key]!!.resolverType !== resolverType) {
            resolverCache[key] = createResolver(resolverType)
        }
        return resolverCache[key]!!
    }

    fun createResolver(resolverType: ResolverType): BaseResolver {
        if (resolverType === ResolverType.Dh25Client) {
            return Dh25Client()
        } else if (resolverType === ResolverType.Dh25Server) {
            return Dh25Server()
        }
        return BaseResolver()
    }
}
