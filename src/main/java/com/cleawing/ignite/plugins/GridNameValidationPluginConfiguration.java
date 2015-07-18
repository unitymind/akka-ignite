package com.cleawing.ignite.plugins;

import org.apache.ignite.plugin.PluginConfiguration;

public class GridNameValidationPluginConfiguration implements PluginConfiguration {
    private boolean byPassValidation = false;

    public GridNameValidationPluginConfiguration() {
        // No-op.
    }

    public boolean getByPassValidation() {
        return byPassValidation;
    }

    public void setByPassValidation(boolean byPassValidation) {
        this.byPassValidation = byPassValidation;
    }
}
