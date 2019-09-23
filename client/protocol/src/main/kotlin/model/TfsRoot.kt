package model.tfs

import com.jetbrains.rd.generator.nova.PredefinedType.void
import com.jetbrains.rd.generator.nova.Root
import com.jetbrains.rd.generator.nova.signal

object TfsRoot : Root() {
    init {
        signal("shutdown", void)
    }
}