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

package org.jetbrains.tfsIntegration.tests;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestUndoUpdateChanges extends TFSTestCase {

  /*
  Folder and file changes:
------------------------

+ 1. UP TO DATE [CONTENT CHANGED] in
	(nothing to check) up to date
	+ renamed
	+ moved

2. ADD in
	up to date
	added
	renamed
	moved

3. DELETE in
	up to date
	renamed
	moved
	deleted
	locally missing

4. RENAME [CONTENT CHANGED] in
	+ up to date
	renamed
	moved

5. MOVE [CONTENT CHANGED] in
	+ up to date -> up to date
	up to date -> added
	up to date -> renamed
	up to date -> moved (target)
	renamed (source) -> up to date
	renamed (source) -> added
	renamed (source) -> other one renamed (target)
	renamed (source) -> moved (target)
	moved (source) -> up to date
	moved (source) -> added
	moved (source) -> renamed (target)
	moved (source) -> other one moved (target)
	deleted (source) -> up to date
	deleted (source) -> added
	deleted (source) -> renamed (target)
	deleted (source) -> moved (target)



6. LOCALLY MISSING in
	added (added and externally deleted)
	up to date
	renamed
	moved

   */


}
