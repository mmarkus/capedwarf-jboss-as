/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.capedwarf.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.capedwarf.services.ServletExecutor;

/**
 * Capedwarf API - tie between app and AS.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
final class CapedwarfApiProxy {

    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(CapedwarfApiProxy.class.getName());
    private static final Map<ClassLoader, String> classLoaders = new ConcurrentHashMap<ClassLoader, String>();
    private static final ThreadLocal<ServletRequest> requests = new ThreadLocal<ServletRequest>();

    static boolean isCapedwarfApp(ClassLoader classLoader) {
        return classLoaders.containsKey(classLoader);
    }

    static boolean isCapedwarfApp() {
        return isCapedwarfApp(SecurityActions.getAppClassLoader());
    }

    static String getAppId() {
        return classLoaders.get(SecurityActions.getAppClassLoader());
    }

    static void initialize(final String appId, final ServletContext context) {
        ServletExecutor.registerContext(appId, context);
        classLoaders.put(SecurityActions.getAppClassLoader(), appId);
    }

    static void initialize(final String appId, final EmbeddedCacheManager manager) {
        // do nothing atm
    }

    static void destroy(final String appId, final ServletContext context) {
        try {
            ServletExecutor.unregisterContext(appId);
        } finally {
            classLoaders.remove(SecurityActions.getAppClassLoader());
        }
    }

    static void destroy(final String appId, final EmbeddedCacheManager manager) {
        for (String cc : Constants.CACHES) {
            final String cacheName = cc + "_" + appId;
            final Cache cache = manager.getCache(cacheName, false); // name is impl detail ...
            if (cache != null)
                try {
                    cache.stop();
                } catch (Throwable t) {
                    log.log(Level.WARNING, "Exception stopping cache: " + cacheName, t);
                }
        }
    }

    static void setRequest(ServletRequest request) {
        requests.set(request);
    }

    static ServletRequest getRequest() {
        return requests.get();
    }

    static void removeRequest() {
        requests.remove();
    }
}
