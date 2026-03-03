package com.oneworldstudiomc.paper.datacomponent.item;

import com.oneworldstudiomc.paper.datacomponent.DataComponentBuilder;

/**
 * Minimal Paper custom model data component bridge.
 */
public interface CustomModelData extends DataComponentBuilder {

    static Builder customModelData() {
        return new BuilderImpl();
    }

    interface Builder extends DataComponentBuilder {

        Builder addFloat(float value);
    }

    final class BuilderImpl implements Builder {
        @Override
        public Builder addFloat(float value) {
            return this;
        }
    }
}
