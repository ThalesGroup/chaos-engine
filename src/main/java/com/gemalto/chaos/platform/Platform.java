package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;
import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;

import java.util.List;

public interface Platform {
    List<Container> getRoster ();

    ApiStatus getApiStatus ();

    PlatformLevel getPlatformLevel ();

    PlatformHealth getPlatformHealth ();
}
