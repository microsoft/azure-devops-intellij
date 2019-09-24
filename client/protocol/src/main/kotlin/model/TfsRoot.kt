package model.tfs

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*

object TfsRoot : Root() {
    private val VersionNumber = structdef {
        field("major", int)
        field("minor", int)
    }

    init {
        property("version", VersionNumber)

        call("healthCheck", void, string.nullable)
            .doc("Performs a health check, returns optional error message")

        signal("shutdown", void)
            .doc("Shuts down the application")
    }
}