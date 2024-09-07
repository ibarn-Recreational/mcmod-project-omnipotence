package com.ibarnstormer.projectomnipotence.entity.data;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.data.*;
import net.minecraft.util.collection.Class2IntMap;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

// Server-side only data tracker (basically the DataTracker without serialization for networking)
public class ServersideDataTracker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_DATA_VALUE_ID = 254;
    static final Class2IntMap CLASS_TO_LAST_ID = new Class2IntMap();
    private final DataTracked trackedEntity;
    private final Map<Integer, DataTracker.Entry<?>> entries;

    ServersideDataTracker(DataTracked trackedEntity, Map<Integer, DataTracker.Entry<?>> entries) {
        this.trackedEntity = trackedEntity;
        this.entries = entries;
    }

    public static <T> TrackedData<T> registerData(Class<? extends DataTracked> entityClass, TrackedDataHandler<T> dataHandler) {
        int i;
        if (LOGGER.isDebugEnabled()) {
            try {
                Class<?> class_ = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
                if (!class_.equals(entityClass)) {
                    LOGGER.debug("Serverside: defineId called for: {} from {}", entityClass, class_, new RuntimeException());
                }
            } catch (ClassNotFoundException class_) {
                // empty catch block
            }
        }
        if ((i = CLASS_TO_LAST_ID.put(entityClass)) > MAX_DATA_VALUE_ID) {
            throw new IllegalArgumentException("Serverside: Data value id is too big with " + i + "! (Max is 254)");
        }
        return dataHandler.create(i);
    }

    private <T> DataTracker.Entry<T> getEntry(TrackedData<T> key) {
        return (DataTracker.Entry<T>) this.entries.get(key.id());
    }

    public <T> T get(TrackedData<T> data) {
        return this.getEntry(data).get();
    }

    public <T> void set(TrackedData<T> key, T value) {
        this.set(key, value, false);
    }

    public <T> void set(TrackedData<T> key, T value, boolean force) {
        DataTracker.Entry<T> entry = this.getEntry(key);
        if (force || ObjectUtils.notEqual(value, entry.get())) {
            entry.set(value);
            this.trackedEntity.onTrackedDataSet(key);
        }
    }

    public static class Builder {
        private final DataTracked entity;
        private final Map<Integer, DataTracker.Entry<?>> entries;

        public Builder(DataTracked entity) {
            this.entity = entity;
            this.entries = new HashMap<>();
        }

        public <T> ServersideDataTracker.Builder add(TrackedData<T> data, T value) {
            int i = data.id();

            if (i < this.entries.size() && this.entries.get(i) != null) {
                throw new IllegalArgumentException("Duplicate id value for " + i + "!");
            }
            if (TrackedDataHandlerRegistry.getId(data.dataType()) < 0) {
                throw new IllegalArgumentException("Unregistered serializer " + String.valueOf(data.dataType()) + " for " + i + "!");
            }
            this.entries.put(i, new DataTracker.Entry<>(data, value));
            return this;
        }

        public ServersideDataTracker build() {
            for (int i : this.entries.keySet()) {
                if (this.entries.get(i) != null) continue;
                throw new IllegalStateException("Entity " + String.valueOf(this.entity.getClass()) + " has not defined synched data value " + i);
            }
            return new ServersideDataTracker(this.entity, this.entries);
        }
    }


}
