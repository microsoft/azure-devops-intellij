// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs

inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> =
    Array(size) { idx -> transform(this[idx]) }
