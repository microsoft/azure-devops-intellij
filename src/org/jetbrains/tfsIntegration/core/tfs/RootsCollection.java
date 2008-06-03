/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public abstract class RootsCollection<T> implements Collection<T> {

  public static class FilePathRootsCollection extends RootsCollection<FilePath> {

    public FilePathRootsCollection() {
    }

    public FilePathRootsCollection(final Collection<FilePath> items) {
      super(items);
    }

    protected FilePath getFilePath(final FilePath filePath) {
      return filePath;
    }
  }

  public static class ItemPathRootsCollection extends RootsCollection<ItemPath> {

    public ItemPathRootsCollection() {
    }

    public ItemPathRootsCollection(final Collection<ItemPath> items) {
      super(items);
    }

    protected FilePath getFilePath(final ItemPath itemPath) {
      return itemPath.getLocalPath();
    }
  }

  private Collection<T> myRoots = new HashSet<T>();

  public RootsCollection() {
  }

  public RootsCollection(final Collection<T> items) {
    addAll(items);
  }

  public int size() {
    return myRoots.size();
  }

  public boolean isEmpty() {
    return myRoots.isEmpty();
  }

  public boolean contains(final Object path) {
    return myRoots.contains(path);
  }

  public Iterator<T> iterator() {
    return myRoots.iterator();
  }

  public Object[] toArray() {
    return myRoots.toArray();
  }

  public <T> T[] toArray(final T[] a) {
    return myRoots.toArray(a);
  }

  public boolean add(final T newItem) {
    FilePath path = getFilePath(newItem);

    Collection<T> toRemove = new ArrayList<T>();
    for (T existingItem : myRoots) {
      FilePath existingPath = getFilePath(existingItem);
      if (FileUtil.pathsEqual(path.getPath(), existingPath.getPath()) || path.isUnder(existingPath, false)) {
        return false;
      }
      if (existingPath.isUnder(path, false)) {
        toRemove.add(existingItem);
      }
    }
    myRoots.removeAll(toRemove);
    myRoots.add(newItem);
    return true;
  }

  public boolean remove(final Object path) {
    return myRoots.remove(path);
  }

  public boolean containsAll(final Collection<?> paths) {
    return myRoots.containsAll(paths);
  }

  public boolean addAll(final Collection<? extends T> items) {
    boolean modified = false;
    for (T item : items) {
      modified |= add(item);
    }
    return modified;
  }

  public boolean removeAll(final Collection<?> paths) {
    return myRoots.removeAll(paths);
  }

  public boolean retainAll(final Collection<?> paths) {
    return myRoots.retainAll(paths);
  }

  public void clear() {
    myRoots.clear();
  }

  protected abstract
  @NotNull
  FilePath getFilePath(T t);

}
