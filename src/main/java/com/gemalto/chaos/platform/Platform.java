package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.enums.ApiStatus;

import java.util.List;

public interface Platform {
    List<Container> getRoster();

    ApiStatus getApiStatus();
}
