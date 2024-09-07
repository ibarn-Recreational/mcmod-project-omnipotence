package com.ibarnstormer.projectomnipotence.entity;

import com.ibarnstormer.projectomnipotence.entity.data.ServersideDataTracker;

// Interface for entities with server-side only tracked data
public interface ServerTrackedData {

    void initServersideDataTracker(ServersideDataTracker.Builder builder);

    ServersideDataTracker getServersideDataTracker();

}
