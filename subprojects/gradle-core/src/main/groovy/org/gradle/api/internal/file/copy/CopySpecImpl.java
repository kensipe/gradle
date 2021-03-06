/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.internal.file.copy;

import groovy.lang.Closure;
import org.apache.tools.zip.UnixStat;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.file.*;
import org.gradle.api.internal.ChainingTransformer;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.io.FilterReader;
import java.util.*;

/**
 * @author Steve Appling
 */
public class CopySpecImpl implements CopySpec, ReadableCopySpec {
    private final FileResolver resolver;
    private final boolean root;
    private final Set<Object> sourcePaths;
    private Object destDir;
    private final PatternSet patternSet;
    private final List<ReadableCopySpec> childSpecs;
    private final CopySpecImpl parentSpec;
    private final List<Action<? super FileCopyDetails>> actions = new ArrayList<Action<? super FileCopyDetails>>();
    private Integer dirMode;
    private Integer fileMode;
    private Boolean caseSensitive;

    private CopySpecImpl(FileResolver resolver, CopySpecImpl parentSpec, boolean root) {
        this.parentSpec = parentSpec;
        this.resolver = resolver;
        this.root = root;
        sourcePaths = new LinkedHashSet<Object>();
        childSpecs = new ArrayList<ReadableCopySpec>();
        patternSet = new PatternSet();
    }

    public CopySpecImpl(FileResolver resolver) {
        this(resolver, null, true);
    }

    protected FileResolver getResolver() {
        return resolver;
    }

    public CopySpec from(Object... sourcePaths) {
        for (Object sourcePath : sourcePaths) {
            if (sourcePath instanceof ReadableCopySpec) {
                childSpecs.add(new WrapperCopySpec(this, (ReadableCopySpec) sourcePath));
            } else {
                this.sourcePaths.add(sourcePath);
            }
        }
        return this;
    }

    public CopySpec from(Object sourcePath, Closure c) {
        if (c == null) {
            from(sourcePath);
            return this;
        } else {
            CopySpecImpl child = addChild();
            child.from(sourcePath);
            ConfigureUtil.configure(c, child);
            return child;
        }
    }

    private CopySpecImpl addChild() {
        CopySpecImpl child = new CopySpecImpl(resolver, this, false);
        childSpecs.add(child);
        return child;
    }

    public CopySpecImpl addNoInheritChild() {
        CopySpecImpl child = new CopySpecImpl(resolver, null, false);
        childSpecs.add(child);
        return child;
    }

    public Set<Object> getSourcePaths() {
        return sourcePaths;
    }

    public FileTree getSource() {
        return resolver.resolveFilesAsTree(sourcePaths).matching(getPatternSet());
    }

    public List<ReadableCopySpec> getAllSpecs() {
        List<ReadableCopySpec> result = new ArrayList<ReadableCopySpec>();
        result.add(this);
        for (ReadableCopySpec childSpec : childSpecs) {
            result.addAll(childSpec.getAllSpecs());
        }
        return result;
    }

    public CopySpec into(Object destDir) {
        this.destDir = destDir;
        return this;
    }

    public CopySpec into(Object destPath, Closure configureClosure) {
        if (configureClosure == null) {
            into(destPath);
            return this;
        } else {
            CopySpecImpl child = addChild();
            child.into(destPath);
            ConfigureUtil.configure(configureClosure, child);
            return child;
        }
    }

    public RelativePath getDestPath() {
        if (root) {
            return new RelativePath(false);
        }
        RelativePath parentPath;
        if (parentSpec == null) {
            parentPath = new RelativePath(false);
        } else {
            parentPath = parentSpec.getDestPath();
        }
        if (destDir == null) {
            return parentPath;
        }

        String path = destDir.toString();
        if (path.startsWith("/") || path.startsWith(File.separator)) {
            return RelativePath.parse(false, path);
        }

        return RelativePath.parse(false, parentPath, path);
    }

    public PatternSet getPatternSet() {
        PatternSet patterns = new PatternSet();
        patterns.setCaseSensitive(isCaseSensitive());
        patterns.include(getAllIncludes());
        patterns.includeSpecs(getAllIncludeSpecs());
        patterns.exclude(getAllExcludes());
        patterns.excludeSpecs(getAllExcludeSpecs());
        return patterns;
    }

    public boolean isCaseSensitive() {
        if (caseSensitive != null) {
            return caseSensitive;
        } else if (parentSpec != null) {
            return parentSpec.isCaseSensitive();
        }
        return true;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public CopySpec include(String... includes) {
        patternSet.include(includes);
        return this;
    }

    public CopySpec include(Iterable<String> includes) {
        patternSet.include(includes);
        return this;
    }

    public CopySpec include(Spec<FileTreeElement> includeSpec) {
        patternSet.include(includeSpec);
        return this;
    }

    public CopySpec include(Closure includeSpec) {
        patternSet.include(includeSpec);
        return this;
    }

    public Set<String> getIncludes() {
        return patternSet.getIncludes();
    }

    public CopySpec setIncludes(Iterable<String> includes) {
        patternSet.setIncludes(includes);
        return this;
    }

    public List<String> getAllIncludes() {
        List<String> result = new ArrayList<String>();
        if (parentSpec != null) {
            result.addAll(parentSpec.getAllIncludes());
        }
        result.addAll(getIncludes());
        return result;
    }

    public List<Spec<FileTreeElement>> getAllIncludeSpecs() {
        List<Spec<FileTreeElement>> result = new ArrayList<Spec<FileTreeElement>>();
        if (parentSpec != null) {
            result.addAll(parentSpec.getAllIncludeSpecs());
        }
        result.addAll(patternSet.getIncludeSpecs());
        return result;
    }

    public CopySpec exclude(String... excludes) {
        patternSet.exclude(excludes);
        return this;
    }

    public CopySpec exclude(Iterable<String> excludes) {
        patternSet.exclude(excludes);
        return this;
    }

    public CopySpec exclude(Spec<FileTreeElement> excludeSpec) {
        patternSet.exclude(excludeSpec);
        return this;
    }

    public CopySpec exclude(Closure excludeSpec) {
        patternSet.exclude(excludeSpec);
        return this;
    }

    public Set<String> getExcludes() {
        return patternSet.getExcludes();
    }

    public CopySpecImpl setExcludes(Iterable<String> excludes) {
        patternSet.setExcludes(excludes);
        return this;
    }

    public CopySpec rename(String sourceRegEx, String replaceWith) {
        actions.add(new RenamingCopyAction(new RegExpNameMapper(sourceRegEx, replaceWith)));
        return this;
    }

    public CopySpec filter(final Class<? extends FilterReader> filterType) {
        actions.add(new Action<FileCopyDetails>() {
            public void execute(FileCopyDetails fileCopyDetails) {
                fileCopyDetails.filter(filterType);
            }
        });
        return this;
    }

    public CopySpec filter(final Closure closure) {
        actions.add(new Action<FileCopyDetails>() {
            public void execute(FileCopyDetails fileCopyDetails) {
                fileCopyDetails.filter(closure);
            }
        });
        return this;
    }

    public CopySpec filter(final Map<String, ?> map, final Class<? extends FilterReader> filterType) {
        actions.add(new Action<FileCopyDetails>() {
            public void execute(FileCopyDetails fileCopyDetails) {
                fileCopyDetails.filter(map, filterType);
            }
        });
        return this;
    }

    public CopySpec rename(Closure closure) {
        ChainingTransformer<String> transformer = new ChainingTransformer<String>(String.class);
        transformer.add(closure);
        actions.add(new RenamingCopyAction(transformer));
        return this;
    }

    public int getDirMode() {
        if (dirMode != null) {
            return dirMode;
        }
        if (parentSpec != null) {
            return parentSpec.getDirMode();
        }
        return UnixStat.DEFAULT_DIR_PERM;
    }

    public int getFileMode() {
        if (fileMode != null) {
            return fileMode;
        }
        if (parentSpec != null) {
            return parentSpec.getFileMode();
        }
        return UnixStat.DEFAULT_FILE_PERM;
    }

    public CopyProcessingSpec setDirMode(int mode) {
        dirMode = mode;
        return this;
    }

    public CopyProcessingSpec setFileMode(int mode) {
        fileMode = mode;
        return this;
    }

    public CopySpec eachFile(Action<? super FileCopyDetails> action) {
        actions.add(action);
        return this;
    }

    public CopySpec eachFile(Closure closure) {
        actions.add((Action<? super FileCopyDetails>) DefaultGroovyMethods.asType(closure, Action.class));
        return this;
    }

    public List<String> getAllExcludes() {
        List<String> result = new ArrayList<String>();
        if (parentSpec != null) {
            result.addAll(parentSpec.getAllExcludes());
        }
        result.addAll(getExcludes());
        return result;
    }

    public List<Spec<FileTreeElement>> getAllExcludeSpecs() {
        List<Spec<FileTreeElement>> result = new ArrayList<Spec<FileTreeElement>>();
        if (parentSpec != null) {
            result.addAll(parentSpec.getAllExcludeSpecs());
        }
        result.addAll(patternSet.getExcludeSpecs());
        return result;
    }

    public List<Action<? super FileCopyDetails>> getAllCopyActions() {
        if (parentSpec == null) {
            return actions;
        }
        List<Action<? super FileCopyDetails>> allActions = new ArrayList<Action<? super FileCopyDetails>>();
        allActions.addAll(parentSpec.getAllCopyActions());
        allActions.addAll(actions);
        return allActions;
    }

    public boolean hasSource() {
        if (!sourcePaths.isEmpty()) {
            return true;
        }
        for (ReadableCopySpec spec : childSpecs) {
            if (spec.hasSource()) {
                return true;
            }
        }
        return false;
    }

    private static class WrapperCopySpec implements ReadableCopySpec {
        private final ReadableCopySpec root;
        private final ReadableCopySpec spec;

        public WrapperCopySpec(ReadableCopySpec root, ReadableCopySpec spec) {
            this.root = root;
            this.spec = spec;
        }

        public RelativePath getDestPath() {
            return root.getDestPath().append(spec.getDestPath());
        }

        public int getFileMode() {
            return spec.getFileMode();
        }

        public int getDirMode() {
            return spec.getDirMode();
        }

        public FileTree getSource() {
            return spec.getSource();
        }

        public Collection<? extends ReadableCopySpec> getAllSpecs() {
            List<WrapperCopySpec> specs = new ArrayList<WrapperCopySpec>();
            for (ReadableCopySpec child : spec.getAllSpecs()) {
                specs.add(new WrapperCopySpec(root, child));
            }
            return specs;
        }

        public boolean hasSource() {
            return spec.hasSource();
        }

        public Collection<? extends Action<? super FileCopyDetails>> getAllCopyActions() {
            return spec.getAllCopyActions();
        }
    }
}
