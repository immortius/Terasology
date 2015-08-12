/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.engine.subsystem.common.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.Asset;
import org.terasology.context.Context;
import org.terasology.engine.GameEngine;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleExtension;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.module.StandardModuleExtension;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.EngineSubsystem;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataJsonAdapter;
import org.terasology.module.ModulePathScanner;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.APIScanner;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.ModuleSecurityManager;
import org.terasology.module.sandbox.ModuleSecurityPolicy;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.module.sandbox.StandardPermissionProviderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ReflectPermission;
import java.net.URISyntaxException;
import java.security.Policy;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
public class EnvironmentSubsystem implements EngineSubsystem, ModuleManager {

    private StandardPermissionProviderFactory permissionProviderFactory;

    private ModuleRegistry moduleRegistry;
    private ModuleMetadataJsonAdapter moduleMetadataReader;
    private volatile ModuleEnvironment currentEnvironment;

    @Override
    public String getName() {
        return "Environment";
    }

    @Override
    public void populateRootContext(Context rootContext) {
        moduleRegistry = new TableModuleRegistry();
        moduleMetadataReader = new ModuleMetadataJsonAdapter();
        for (ModuleExtension ext : StandardModuleExtension.values()) {
            moduleMetadataReader.registerExtension(ext.getKey(), ext.getValueType());
        }

        rootContext.put(ModuleManager.class, this);
    }

    @Override
    public void initialise(GameEngine engine, Context rootContext) {
        Module engineModule = createEngineModule();
        permissionProviderFactory = createPermissionProviderFactory(engineModule);

        Policy.setPolicy(new ModuleSecurityPolicy());
        System.setSecurityManager(new ModuleSecurityManager());

        moduleRegistry.add(engineModule);
        scanForModules();
    }

    public void switchEnvironment(ModuleEnvironment environment) {
        currentEnvironment = environment;
    }

    private void scanForModules() {
        ModulePathScanner scanner = new ModulePathScanner(new ModuleLoader(moduleMetadataReader));
        scanner.getModuleLoader().setModuleInfoPath(TerasologyConstants.MODULE_INFO_FILENAME);
        scanner.scan(moduleRegistry, PathManager.getInstance().getModulePaths());
    }

    private Module createEngineModule() {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/engine-module.txt"), TerasologyConstants.CHARSET)) {
            ModuleMetadata metadata = moduleMetadataReader.read(reader);
            return ClasspathModule.create(metadata, getClass(), Module.class, Asset.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read engine metadata", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert engine library location to path", e);
        }
    }

    private StandardPermissionProviderFactory createPermissionProviderFactory(Module engineModule) {
        // TODO: This one org.terasology entry is a hack and needs a proper fix
        StandardPermissionProviderFactory factory = new StandardPermissionProviderFactory();
        factory.getBasePermissionSet().addAPIPackage("org.terasology.world.biomes");
        factory.getBasePermissionSet().addAPIPackage("org.terasology.math.geom");
        factory.getBasePermissionSet().addAPIPackage("java.lang");
        factory.getBasePermissionSet().addAPIPackage("java.lang.invoke");
        factory.getBasePermissionSet().addAPIPackage("java.lang.ref");
        factory.getBasePermissionSet().addAPIPackage("java.math");
        factory.getBasePermissionSet().addAPIPackage("java.util");
        factory.getBasePermissionSet().addAPIPackage("java.util.concurrent");
        factory.getBasePermissionSet().addAPIPackage("java.util.concurrent.atomic");
        factory.getBasePermissionSet().addAPIPackage("java.util.concurrent.locks");
        factory.getBasePermissionSet().addAPIPackage("java.util.function");
        factory.getBasePermissionSet().addAPIPackage("java.util.regex");
        factory.getBasePermissionSet().addAPIPackage("java.util.stream");
        factory.getBasePermissionSet().addAPIPackage("java.awt");
        factory.getBasePermissionSet().addAPIPackage("java.awt.geom");
        factory.getBasePermissionSet().addAPIPackage("java.awt.image");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.annotations");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.cache");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.collect");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.base");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.math");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.primitives");
        factory.getBasePermissionSet().addAPIPackage("com.google.common.util.concurrent");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.decorator");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.function");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.iterator");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.iterator.hash");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.list");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.list.array");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.list.linked");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.map");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.map.hash");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.map.custom_hash");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.procedure");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.procedure.array");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.queue");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.set");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.set.hash");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.stack");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.stack.array");
        factory.getBasePermissionSet().addAPIPackage("gnu.trove.strategy");
        factory.getBasePermissionSet().addAPIPackage("javax.vecmath");
        factory.getBasePermissionSet().addAPIPackage("com.yourkit.runtime");
        factory.getBasePermissionSet().addAPIPackage("com.bulletphysics.linearmath");
        factory.getBasePermissionSet().addAPIPackage("sun.reflect");
        factory.getBasePermissionSet().addAPIClass(com.esotericsoftware.reflectasm.MethodAccess.class);
        factory.getBasePermissionSet().addAPIClass(IOException.class);
        factory.getBasePermissionSet().addAPIClass(InvocationTargetException.class);
        factory.getBasePermissionSet().addAPIClass(LoggerFactory.class);
        factory.getBasePermissionSet().addAPIClass(Logger.class);
        factory.getBasePermissionSet().addAPIClass(Reader.class);
        factory.getBasePermissionSet().addAPIClass(StringReader.class);
        factory.getBasePermissionSet().addAPIClass(BufferedReader.class);
        factory.getBasePermissionSet().addAPIClass(java.awt.datatransfer.UnsupportedFlavorException.class);

        APIScanner apiScanner = new APIScanner(factory);
        apiScanner.scan(engineModule);

        factory.getBasePermissionSet().grantPermission("com.google.gson", ReflectPermission.class);
        factory.getBasePermissionSet().grantPermission("com.google.gson.internal", ReflectPermission.class);

        factory.getBasePermissionSet().addAPIClass(java.nio.ByteBuffer.class);
        factory.getBasePermissionSet().addAPIClass(java.nio.IntBuffer.class);

        return factory;
    }

    @Override
    public ModuleRegistry getRegistry() {
        return moduleRegistry;
    }

    @Override
    public ModuleEnvironment getEnvironment() {
        return currentEnvironment;
    }

    public void setCurrentEnvironment(ModuleEnvironment environment) {
        this.currentEnvironment = environment;
    }

    @Override
    public ModuleMetadataJsonAdapter getMetadataReader() {
        return moduleMetadataReader;
    }

    @Override
    public ModuleEnvironment createEnvironment(Set<Module> modules) {
        return new ModuleEnvironment(modules, permissionProviderFactory, Collections.<BytecodeInjector>emptyList());
    }
}
