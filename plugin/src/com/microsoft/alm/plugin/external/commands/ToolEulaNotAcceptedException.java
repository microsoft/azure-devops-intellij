package com.microsoft.alm.plugin.external.commands;

public class ToolEulaNotAcceptedException extends RuntimeException {
    public ToolEulaNotAcceptedException(Throwable throwable) {
        super("EULA not accepted", throwable);
    }
}
