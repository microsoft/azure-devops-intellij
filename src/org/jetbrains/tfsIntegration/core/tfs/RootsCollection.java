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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;

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

    protected boolean isAncestor(FilePath parent, FilePath child, boolean strict) {
      return child.isUnder(parent, strict);
    }

  }

  public static class ItemPathRootsCollection extends RootsCollection<ItemPath> {

    public ItemPathRootsCollection() {
    }

    public ItemPathRootsCollection(final Collection<ItemPath> items) {
      super(items);
    }

    protected boolean isAncestor(ItemPath parent, ItemPath child, boolean strict) {
      return child.getLocalPath().isUnder(parent.getLocalPath(), strict);
    }

  }

  public static class VirtualFileRootsCollection extends RootsCollection<VirtualFile> {

    public VirtualFileRootsCollection() {
    }

    public VirtualFileRootsCollection(final Collection<VirtualFile> items) {
      super(items);
    }

    public VirtualFileRootsCollection(final VirtualFile[] items) {
      super(items);
    }

    protected boolean isAncestor(VirtualFile parent, VirtualFile child, boolean strict) {
      return VfsUtil.isAncestor(parent, child, strict);
    }

  }

  private final Collection<T> myRoots = new HashSet<T>();

  public RootsCollection() {
  }

  public RootsCollection(final Collection<T> items) {
    addAll(items);
  }

  public RootsCollection(final T[] items) {
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
    //noinspection SuspiciousToArrayCall
    return myRoots.toArray(a);
  }

  public boolean add(final T newItem) {
    Collection<T> toRemove = new ArrayList<T>();
    for (T existingItem : myRoots) {
      if (isAncestor(existingItem, newItem, false)) {
        return false;
      }
      if (isAncestor(newItem, existingItem, true)) {
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

  public boolean addAll(final T[] items) {
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

  protected abstract boolean isAncestor(T parent, T child, boolean strict);

}
