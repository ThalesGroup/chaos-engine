package com.thales.chaos.swagger;

import com.thales.chaos.admin.AdminController;
import com.thales.chaos.experiment.ExperimentController;
import com.thales.chaos.logging.LoggingController;
import com.thales.chaos.platform.PlatformController;
import com.thales.chaos.refresh.RefreshController;
import com.thales.chaos.security.impl.ChaosLoginEndpoints;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

import static com.thales.chaos.swagger.OpenApiConfig.*;

@Configuration
@SecurityScheme(name = JSESSIONID, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE)
@OpenAPIDefinition(info = @Info(title = OPENAPI_TITLE,
                                description = OPENAPI_DESCRIPTION,
                                license = @License(name = OPENAPI_LICENSE_NAME, url = OPENAPI_LICENSE_VERSION),
                                version = API_VERSION,
                                contact = @Contact(name = CONTACT_NAME, url = CONTACT_URL)),
                   security = @SecurityRequirement(name = JSESSIONID),
                   tags = {
                           @Tag(name = AdminController.ADMIN,
                                description = "Controls the administrative state of the Chaos Engine"),
                           @Tag(name = ExperimentController.EXPERIMENT,
                                description = "Create or retrieve information about Chaos Experiments"),
                           @Tag(name = LoggingController.LOGGING,
                                description = "Control the real time logging level of Chaos Engine classes"),
                           @Tag(name = PlatformController.PLATFORM,
                                description = "Retrieve information about platforms include for Chaos Experiments"),
                           @Tag(name = RefreshController.REFRESH,
                                description = "Refresh Spring Beans based on new information in Vault"),
                           @Tag(name = ChaosLoginEndpoints.SECURITY,
                                description = "Provide endpoints for controlling User Session for security")
                   },
                   servers = @Server(),
                   externalDocs = @ExternalDocumentation(description = "Packaged Help Documentation", url = "/help/"))
public class OpenApiConfig {
    static final String OPENAPI_TITLE = "Chaos Engine API";
    static final String OPENAPI_DESCRIPTION = "Controls experiment execution framework and configuration";
    static final String OPENAPI_LICENSE_NAME = "Apache License, Version 2.0";
    static final String OPENAPI_LICENSE_VERSION = "https://www.apache.org/licenses/LICENSE-2.0.txt";
    static final String API_VERSION = "1.2";
    static final String CONTACT_NAME = "Thales Digital Identity and Security Cloud Platform Licensing Chaos Engineering Team";
    static final String CONTACT_URL = "https://github.com/gemalto/chaos-engine";
    static final String JSESSIONID = "JSESSIONID";
}
