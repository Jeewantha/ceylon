/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 */

package com.redhat.ceylon.compiler.java.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.ceylon.CeylonUtils;
import com.redhat.ceylon.cmr.impl.CachingRepositoryManager;
import com.redhat.ceylon.common.FileUtil;
import com.redhat.ceylon.common.config.Repositories;
import com.redhat.ceylon.common.log.Logger;
import com.redhat.ceylon.compiler.java.codegen.CeylonFileObject;
import com.redhat.ceylon.compiler.java.util.Util;
import com.redhat.ceylon.javax.tools.FileObject;
import com.redhat.ceylon.javax.tools.JavaFileManager;
import com.redhat.ceylon.javax.tools.JavaFileObject;
import com.redhat.ceylon.javax.tools.JavaFileObject.Kind;
import com.redhat.ceylon.javax.tools.StandardJavaFileManager;
import com.redhat.ceylon.javax.tools.StandardLocation;
import com.redhat.ceylon.langtools.tools.javac.api.MultiTaskListener;
import com.redhat.ceylon.langtools.tools.javac.file.BaseFileObject;
import com.redhat.ceylon.langtools.tools.javac.file.JavacFileManager;
import com.redhat.ceylon.langtools.tools.javac.file.RegularFileObject;
import com.redhat.ceylon.langtools.tools.javac.file.RelativePath.RelativeFile;
import com.redhat.ceylon.langtools.tools.javac.main.Option;
import com.redhat.ceylon.langtools.tools.javac.util.Context;
import com.redhat.ceylon.langtools.tools.javac.util.ListBuffer;
import com.redhat.ceylon.langtools.tools.javac.util.Log;
import com.redhat.ceylon.langtools.tools.javac.util.Options;
import com.redhat.ceylon.model.typechecker.model.Module;

public class CeyloncFileManager extends JavacFileManager implements StandardJavaFileManager {
    private Module currentModule;
    private JarOutputRepositoryManager jarRepository;
    private Context context;
    private Options options;
    private RepositoryManager repoManager;
    private RepositoryManager outputRepoManager;
    private Logger cmrLogger;

    public CeyloncFileManager(Context context, boolean register, Charset charset) {
        super(context, register, charset);
        options = Options.instance(context);
    }

    private Logger getLogger(){
        if(cmrLogger == null)
            cmrLogger = new JavacLogger(options, Log.instance(context));
        return cmrLogger;
    }
    
    private JarOutputRepositoryManager getJarRepository(){
        if(jarRepository == null)
            jarRepository = new JarOutputRepositoryManager(CeylonLog.instance(context), options, this, MultiTaskListener.instance(context));
        return jarRepository;
    }
    
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
        super.setContext(context);
    }

    public JavaFileObject.Kind getKind(String name) {
        if (name.endsWith(JavaFileObject.Kind.CLASS.extension))
            return JavaFileObject.Kind.CLASS;
        else if (name.endsWith(JavaFileObject.Kind.SOURCE.extension) || name.endsWith(".ceylon"))
            return JavaFileObject.Kind.SOURCE;
        else if (name.endsWith(JavaFileObject.Kind.HTML.extension))
            return JavaFileObject.Kind.HTML;
        else
            return JavaFileObject.Kind.OTHER;
    }

    /**
     * Register a Context.Factory to create a JavacFileManager.
     */
    public static void preRegister(final Context context) {
        context.put(JavaFileManager.class, new Context.Factory<JavaFileManager>() {
            public JavaFileManager make(Context context) {
                return new CeyloncFileManager(context, true, null);
            }
        });
    }

    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {

        Iterable<? extends JavaFileObject> theCollection = super.getJavaFileObjectsFromFiles(files);
        ArrayList<JavaFileObject> result = new ArrayList<JavaFileObject>();
        for (JavaFileObject file : theCollection) {
            if (file.getName().endsWith(".ceylon")) {
                result.add(new CeylonFileObject(file));
            } else {
                result.add(file);
            }
        }
        return result;
    }

    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);
        ListBuffer<JavaFileObject> buf = new ListBuffer<JavaFileObject>();
        for (JavaFileObject f : result) {
            if (f.getName().endsWith(".ceylon")) {
                buf.add(new CeylonFileObject(f));
            } else {
                buf.add(f);
            }
        }
        return buf.toList();
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof CeylonFileObject) {
            CeylonFileObject fo = (CeylonFileObject) file;
            return super.inferBinaryName(location, fo.getFile());
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    protected JavaFileObject getFileForOutput(Location location, RelativeFile fileName, FileObject sibling) throws IOException {
        if (sibling instanceof CeylonFileObject) {
            sibling = ((CeylonFileObject) sibling).getFile();
        }
        if(location == StandardLocation.CLASS_OUTPUT){
            File siblingFile = null;
            if (sibling != null && sibling instanceof RegularFileObject) {
                siblingFile = ((RegularFileObject)sibling).getUnderlyingFile();
            }
            return getJarRepository().getFileObject(getOutputRepositoryManager(), currentModule, fileName.getPath(), siblingFile);
        }else
            return super.getFileForOutput(location, fileName, sibling);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location,
            String className,
            JavaFileObject.Kind kind) throws IOException {
        nullCheck(location);
        // validateClassName(className);
        nullCheck(className);
        nullCheck(kind);
        if (!sourceOrClass.contains(kind))
            throw new IllegalArgumentException("Invalid kind " + kind);
        JavaFileObject file = getFileForInput(location, forClass(className, kind));
        if (file != null && file.getName().endsWith(".ceylon")) {
            return new CeylonFileObject(file);
        } else {
            return file;
        }

    }

    static RelativeFile forClass(CharSequence className, JavaFileObject.Kind kind) {
        String extension;
        if(kind == Kind.SOURCE)
            extension = ".ceylon";
        else
            extension = kind.extension;
        return new RelativeFile(className.toString().replace('.', '/') + extension);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        try{
            getJarRepository().flush();
        }finally{
            clearOutputRepositoryManager();
        }
    }
    
    private void clearOutputRepositoryManager() {
        if(outputRepoManager instanceof CachingRepositoryManager){
            File tmpDir = ((CachingRepositoryManager)outputRepoManager).getCacheFolder();
            FileUtil.delete(tmpDir);
            // invalidate it
            outputRepoManager = null;
        }
    }

    public void setModule(Module module) {
        currentModule = module;
    }
    
    public RepositoryManager getRepositoryManager() {
        // caching
        if(repoManager != null)
            return repoManager;
        // lazy loading

        // any user defined repos
        List<String> userRepos = new LinkedList<String>();
        if (options.getMulti(Option.CEYLONREPO) != null) {
            userRepos.addAll(options.getMulti(Option.CEYLONREPO));
        }
        String systemRepo = getSystemRepoOption();
        String cacheRepo = getCacheRepoOption();
        String outRepo = getOutputRepoOption();
        String overrides = options.get(Option.CEYLONOVERRIDES);
        boolean upgradeDist = !options.getBoolean(Option.CEYLONDOWNGRADEDIST.text);
        
        repoManager = CeylonUtils.repoManager()
                .config(CompilerConfig.instance(context))
                .cwd(getCurrentWorkingDir())
                .upgradeDist(upgradeDist)
                .overrides(overrides)
                .systemRepo(systemRepo)
                .cacheRepo(cacheRepo)
                .noDefaultRepos(getNoDefaultRepos())
                .userRepos(userRepos)
                .outRepo(outRepo)
                .offline(getOfflineOption())
                .timeout(getTimeoutOption())
                .logger(getLogger())
                .buildManager();
        
        return repoManager;
    }

    public RepositoryManager getOutputRepositoryManager() {
        // caching
        if(outputRepoManager != null)
            return outputRepoManager;
        // lazy loading

        // any user defined repos
        String outRepo = getOutputRepoOption();
        
        // username and password for WebDAV
        String user = options.get(Option.CEYLONUSER);
        String password = options.get(Option.CEYLONPASS);
        
        outputRepoManager = CeylonUtils.repoManager()
                .config(CompilerConfig.instance(context))
                .cwd(getCurrentWorkingDir())
                .outRepo(outRepo)
                .logger(getLogger())
                .user(user)
                .password(password)
                .timeout(getTimeoutOption())
                .buildOutputManager();
        
        return outputRepoManager;
    }

    protected String getCurrentWorkingDir() {
        return options.get(Option.CEYLONCWD);
    }

    private String getSystemRepoOption() {
        return options.get(Option.CEYLONSYSTEMREPO);
    }

    private String getCacheRepoOption() {
        return options.get(Option.CEYLONCACHEREPO);
    }

    private boolean getNoDefaultRepos() {
        return options.get(Option.CEYLONNODEFREPOS) != null;
    }

    private String getOutputRepoOption() {
        // we use D and not CEYLONOUT here since that's where the option is stored
        return options.get(Option.D);
    }

    private boolean getOfflineOption() {
        return options.get(Option.CEYLONOFFLINE) != null;
    }

    private int getTimeoutOption() {
        String to = options.get(Option.CEYLONTIMEOUT);
        if(to != null){
            return Integer.parseInt(to);
        }
        return -1;
    }

    @Override
    protected File getClassOutDir() {
        File outDir = super.getClassOutDir();
        // set the default value for Ceylon
        if (outDir == null) {
            String dir = Repositories.withConfig(CompilerConfig.instance(context)).getOutputRepository().getUrl();
            try {
                super.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(new File(dir)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outDir;
    }
    
    @Override
    protected boolean isValidDirectory(String s) {
        return true;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        nullCheck(a);
        nullCheck(b);
        // don't check on subtypes
        return a.equals(b);
    }
}
