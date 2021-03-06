/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.agent.core.plugin;

import net.bytebuddy.dynamic.DynamicType;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassEnhancePluginDefine;
import org.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.skywalking.apm.agent.core.logging.api.ILog;
import org.skywalking.apm.agent.core.logging.api.LogManager;
import org.skywalking.apm.util.StringUtil;

/**
 * Basic abstract class of all sky-walking auto-instrumentation plugins.
 * <p>
 * It provides the outline of enhancing the target class.
 * If you want to know more about enhancing, you should go to see {@link ClassEnhancePluginDefine}
 */
public abstract class AbstractClassEnhancePluginDefine {
    private static final ILog logger = LogManager.getLogger(AbstractClassEnhancePluginDefine.class);

    /**
     * Main entrance of enhancing the class.
     *
     * @param transformClassName target class.
     * @param builder byte-buddy's builder to manipulate target class's bytecode.
     * @param classLoader load the given transformClass
     * @return the new builder, or <code>null</code> if not be enhanced.
     * @throws PluginException, when set builder failure.
     */
    public DynamicType.Builder<?> define(String transformClassName,
        DynamicType.Builder<?> builder, ClassLoader classLoader, EnhanceContext context) throws PluginException {
        String interceptorDefineClassName = this.getClass().getName();

        if (StringUtil.isEmpty(transformClassName)) {
            logger.warn("classname of being intercepted is not defined by {}.", interceptorDefineClassName);
            return null;
        }

        logger.debug("prepare to enhance class {} by {}.", transformClassName, interceptorDefineClassName);

        // 判断见证类列表是否都存在。若不存在，则插件不生效
        /**
         * find witness classes for enhance class
         */
        String[] witnessClasses = witnessClasses();
        if (witnessClasses != null) {
            for (String witnessClass : witnessClasses) {
                if (!WitnessClassFinder.INSTANCE.exist(witnessClass, classLoader)) {
                    logger.warn("enhance class {} by plugin {} is not working. Because witness class {} is not existed.", transformClassName, interceptorDefineClassName,
                        witnessClass);
                    return null;
                }
            }
        }

        // 使用拦截器增强目标类
        /**
         * find origin class source code for interceptor
         */
        DynamicType.Builder<?> newClassBuilder = this.enhance(transformClassName, builder, classLoader, context);

        // 标记完成增强
        context.initializationStageCompleted();
        logger.debug("enhance class {} by {} completely.", transformClassName, interceptorDefineClassName);

        return newClassBuilder;
    }

    protected abstract DynamicType.Builder<?> enhance(String enhanceOriginClassName,
        DynamicType.Builder<?> newClassBuilder, ClassLoader classLoader, EnhanceContext context) throws PluginException;

    /**
     * Define the {@link ClassMatch} for filtering class.
     *
     * @return {@link ClassMatch}
     */
    protected abstract ClassMatch enhanceClass();

    /**
     * Witness classname list. Why need witness classname? Let's see like this: A library existed two released versions
     * (like 1.0, 2.0), which include the same target classes, but because of version iterator, they may have the same
     * name, but different methods, or different method arguments list. So, if I want to target the particular version
     * (let's say 1.0 for example), version number is obvious not an option, this is the moment you need "Witness
     * classes". You can add any classes only in this particular release version ( something like class
     * com.company.1.x.A, only in 1.0 ), and you can achieve the goal.
     *
     * @return
     */
    protected String[] witnessClasses() {
        return new String[] {};
    }
}
