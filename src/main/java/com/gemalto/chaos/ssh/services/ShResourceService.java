package com.gemalto.chaos.ssh.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
public class ShResourceService {
    private static final Logger log = LoggerFactory.getLogger(ShResourceService.class);
    private static final String SCRIPT_SEARCH_PATTERN="classpath:ssh/experiments/*.sh";
    private HashMap<String,Resource> resourceHashMap = new HashMap<>();

    public Resource getScriptResource (String scriptName)  {
        return resourceHashMap.get(scriptName);
    }

    public ShResourceService () throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        log.debug("Loading sh scripts");
        Resource[] resources =resolver.getResources(SCRIPT_SEARCH_PATTERN);
        for(Resource resource: resources){
            resourceHashMap.put(resource.getFilename(),resource);
        }
        log.debug("Scripts loaded {}", resourceHashMap);
    }
}
