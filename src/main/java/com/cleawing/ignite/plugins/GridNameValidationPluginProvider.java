package com.cleawing.ignite.plugins;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.IgniteNodeAttributes;
import org.apache.ignite.plugin.*;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

public class GridNameValidationPluginProvider
        implements PluginProvider<GridNameValidationPluginConfiguration> {

    private String gridName;
    private boolean byPassValidation = false;

    @Override
    public String name() {
        return "GridNameValidation";
    }

    @Override
    public String version() {
        return "0.1";
    }

    @Override
    public String copyright() {
        return "Cleawing Inc";
    }

    @Override
    public GridNameValidationPlugin plugin() {
        return new GridNameValidationPlugin();
    }

    @Override
    public void initExtensions(PluginContext ctx, ExtensionRegistry registry) {
        gridName = ctx.grid().name();
        PluginConfiguration[] pluginConfigurations = ctx.igniteConfiguration().getPluginConfigurations();
        if (pluginConfigurations != null) {
            for(PluginConfiguration config : pluginConfigurations) {
                if (config instanceof GridNameValidationPluginConfiguration) {
                    byPassValidation = ((GridNameValidationPluginConfiguration) config).getByPassValidation();
                }
            }
        }
    }

    @Nullable
    @Override
    public <T> T createComponent(PluginContext ctx, Class<T> cls) {
        return null;
    }

    @Override
    public void start(PluginContext ctx) throws IgniteCheckedException {
        // No-op
    }

    @Override
    public void stop(boolean cancel) throws IgniteCheckedException {
        // No-op
    }

    @Override
    public void onIgniteStart() throws IgniteCheckedException {
        // No-op
    }

    @Override
    public void onIgniteStop(boolean cancel) {
        // No-op
    }

    @Nullable
    @Override
    public Serializable provideDiscoveryData(UUID nodeId) {
        // No-op
        return null;
    }

    @Override
    public void receiveDiscoveryData(UUID nodeId, Serializable data) {
        // No-op
    }

    @Override
    public void validateNewNode(ClusterNode node) throws PluginValidationException {
        if (!byPassValidation) {
            String remoteGridName = node.attribute(IgniteNodeAttributes.ATTR_GRID_NAME);
            if (remoteGridName == null || !remoteGridName.equals(gridName)) {
                String msg = "Join allowed only for nodes with the same grid name: '" + gridName +"'. But grid name given from local node is: '" + remoteGridName + "'.'";
                throw new PluginValidationException(msg, msg, node.id());
            }
        }
    }
}
