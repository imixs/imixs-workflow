# WildFly Deployment Guide

This section will explain the configuration steps needed to successfully deploy the [Imixs-Sample Application](../sampleapplication.html) on Wildfly. The deployment guide can be used also for custom projects. See also the section [Deployment Guide](./deployment_guide.html) for general information about deployment of the Imixs-Workflow engine.

The configuration examples in this guide are based on **WildFly 39** running on **Java 21**.

## Install Wildfly

The Wildfly Server is supporting the Jakarta EE specification and can be downloaded in the latest version from the [Wildfly project site](http://www.wildfly.org). The site also includes an installation guide how to install Wildfly server on different platforms.

Imixs-Workflow is based on **Jakarta EE 10**. We recommend WildFly 39, which is the last WildFly release supporting Jakarta EE 10 in the standard distribution. Starting with WildFly 40 the standard distribution is based on Jakarta EE 11.

WildFly 39 runs on Java 17 and Java 21. We recommend **Java 21**, which is the current LTS release supported by Imixs-Workflow.

After the server is started it can be opened from a web browser with the following URL:

    http://localhost:8080/

### Docker

WildFly provides official container images on [quay.io](https://quay.io/repository/wildfly/wildfly?tab=tags). The image tags contain the WildFly version and the JDK version:

    quay.io/wildfly/wildfly:39.0.1.Final-2-jdk21

Note that some releases are only available as rebuild tags with an additional suffix (e.g. `-2`). Check the [tag list](https://quay.io/repository/wildfly/wildfly?tab=tags) for the exact name before building your image.

A Dockerfile for an Imixs application can extend the official image like this:

    ARG WILDFLY_VERSION=39.0.1.Final-2-jdk21
    FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION}
    ARG WILDFLY_VERSION

    # Raise JDK serialization filter limits (see section 'JDK Serialization Filter')
    ENV JDK_SERIAL_FILTER="maxbytes=1073741824;maxdepth=128;maxarray=1073741824;maxrefs=10000000"

    # Copy modules (EclipseLink, PostgreSQL) and the server configuration
    COPY ./configuration/wildfly/${WILDFLY_VERSION}/modules/ /opt/jboss/wildfly/modules/
    COPY ./configuration/wildfly/${WILDFLY_VERSION}/standalone.xml /opt/jboss/wildfly/standalone/configuration/

    # Deploy the application
    COPY ./target/my-app.war /opt/jboss/wildfly/standalone/deployments/

    CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]

**Note:** Do not reuse a _standalone.xml_ from an older WildFly version. The schema versions of several subsystems change between releases. Always start with the _standalone.xml_ of your target version and transfer your customizations into this file.

## Setting up a Imixs-Workflow database pool

The [Imixs-Sample Application](../sampleapplication.html) expects a database resource with the name "jdbc/workflow". The corresponding datasource configuration can be added into the file _wildfly/standalone/configuration/standalone.xml_ in the subsystem section 'datasources'.

The recommended way to provide a JDBC driver is to install it as a WildFly module. The following example shows the setup for PostgreSQL.

### PostgreSQL

Create the following directory and copy the PostgreSQL JDBC driver (e.g. _postgresql-42.7.8.jar_) into it:

    modules/system/layers/base/org/postgresql/main

In the same directory create the file _module.xml_:

    <?xml version="1.0" encoding="UTF-8"?>
    <module xmlns="urn:jboss:module:1.6" name="org.postgresql">
        <resources>
            <!-- Make sure this matches the name of the JAR you are installing -->
            <resource-root path="postgresql-42.7.8.jar"/>
        </resources>
    </module>

Next the driver and the datasource can be configured in the standalone.xml:

    ...
    <subsystem xmlns="urn:jboss:domain:datasources:7.2">
        <datasources>
            ...
            <datasource jta="true" jndi-name="java:/jdbc/workflow" pool-name="workflow" enabled="true" use-ccm="true">
                <connection-url>${env.POSTGRES_CONNECTION}</connection-url>
                <driver-class>org.postgresql.Driver</driver-class>
                <driver>postgresql</driver>
                <security user-name="${env.POSTGRES_USER}" password="${env.POSTGRES_PASSWORD}"/>
                <validation>
                    <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
                    <background-validation-millis>60000</background-validation-millis>
                    <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter"/>
                </validation>
            </datasource>
            <drivers>
                ...
                <driver name="postgresql" module="org.postgresql">
                    <driver-class>org.postgresql.Driver</driver-class>
                </driver>
            </drivers>
        </datasources>
    </subsystem>
    ...

In this example the connection URL, user and password are read from the environment variables `POSTGRES_CONNECTION`, `POSTGRES_USER` and `POSTGRES_PASSWORD`. You can also replace these expressions with fixed values.

Note that in the `datasources:7.2` schema user name and password are defined as attributes of the `security` element.

### MySQL and other Databases

The configuration for any other database like MySQL, MariaDB, Oracle or Microsoft SQL Server can be adapted in a similar way. Install the JDBC driver as a module, register it in the `drivers` section and use the corresponding driver class and validation classes, e.g. for MySQL:

    <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"/>
    <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter"/>

### EclipseLink

The Imixs-Sample Application uses EclipseLink for JPA. WildFly ships with Hibernate as the default JPA provider, so EclipseLink must be added to the existing module _org.eclipse.persistence_. For Jakarta EE 10 use EclipseLink version 4.0.x.

First download EclipseLink from [here](https://www.eclipse.org/eclipselink/downloads/) or from Maven Central. Copy the file _eclipselink.jar_ into the following location:

    modules/system/layers/base/org/eclipse/persistence/main

The directory already contains the WildFly integration library _jipijapa-eclipselink-[WILDFLY-VERSION].jar_ and the file _module.xml_. Edit the _module.xml_ and add _eclipselink.jar_ as an additional resource. EclipseLink also requires some additional JDK modules. The following example shows the complete _module.xml_ for WildFly 39:

    <?xml version="1.0" encoding="UTF-8"?>
    <module name="org.eclipse.persistence" xmlns="urn:jboss:module:1.9">
        <properties>
            <property name="jboss.api" value="public"/>
        </properties>

        <resources>
            <resource-root path="jipijapa-eclipselink-39.0.1.Final.jar"/>
            <!-- EclipseLink 4.0.x (Jakarta Persistence 3.1) -->
            <resource-root path="eclipselink.jar"/>
        </resources>

        <dependencies>
            <!-- Additional modules required by EclipseLink -->
            <module name="java.xml"/>
            <module name="java.rmi"/>
            <module name="java.desktop"/>
            <!-- Original WildFly dependencies -->
            <module name="java.logging"/>
            <module name="java.management"/>
            <module name="java.naming"/>
            <module name="jakarta.annotation.api"/>
            <module name="jakarta.enterprise.api"/>
            <module name="jakarta.json.api" optional="true"/>
            <module name="jakarta.persistence.api"/>
            <module name="jakarta.transaction.api"/>
            <module name="jakarta.validation.api"/>
            <module name="jakarta.xml.bind.api"/>
            <module name="org.antlr"/>
            <module name="org.jboss.as.jpa.spi"/>
            <module name="org.jboss.logging"/>
            <module name="org.jboss.vfs"/>
        </dependencies>
    </module>

**Note:** The name of the _jipijapa-eclipselink_ jar contains the WildFly version. When you upgrade WildFly, always take the _module.xml_ shipped with the new version as a starting point and add your changes again. A _module.xml_ referring to a jar of an older WildFly version will prevent the module from loading.

Finally add the org.jipijapa.eclipselink.JBossArchiveFactoryImpl to your configuration. This can be done by using the jboss-cli tool:

    ./jboss-cli.sh --connect '/system-property=eclipselink.archive.factory:add(value=org.jipijapa.eclipselink.JBossArchiveFactoryImpl)'

This command will add the following entry into your standalone.xml configuration file:

    ...
     <system-properties>
     ...
     <property name="eclipselink.archive.factory" value="org.jipijapa.eclipselink.JBossArchiveFactoryImpl"/>
     </system-properties>
    ...

You can also edit the standalone.xml file directly if you have problems to use the command line tool. The `system-properties` section must be placed directly after the `extensions` section.

### JDK Serialization Filter

**Important:** Newer WildFly versions activate a JVM-wide JDK serialization filter ([JEP 290](https://openjdk.org/jeps/290)) by default. Without adjusting this filter, Imixs-Workflow may fail to load documents from the database.

Imixs-Workflow stores the content of each document (the `ItemCollection`, including attached file content) as a serialized Java object in the column `DATA` of the table `DOCUMENT`. When EclipseLink reads this column, the data is deserialized by the JVM and is checked against the serialization filter.

The WildFly start script `bin/standalone.sh` sets the system property `jdk.serialFilter` with the following default limits, defined in `bin/standalone.conf`:

    maxbytes=10485760;maxdepth=128;maxarray=100000;maxrefs=300000

These limits are too small for typical Imixs workloads. For example, a single file attachment larger than about 100 KB exceeds the `maxarray` limit. In this case the deployment or the access to a document fails with an exception like this:

    Exception [EclipseLink-66] ... DescriptorException
    Exception Description: Could not deserialize object from byte array.
    Internal Exception: java.lang.RuntimeException: java.io.InvalidClassException: filter status: REJECTED
    Mapping: org.eclipse.persistence.mappings.DirectToFieldMapping[data-->DOCUMENT.DATA]

Note that the filter is applied only when data is **read**, not when it is written. A document exceeding the limits can be saved without any error, but can no longer be loaded afterwards. Therefore make sure that the limits are always well above the maximum document size allowed in your application.

To raise the limits, set the environment variable `JDK_SERIAL_FILTER` before WildFly is started. The default from `standalone.conf` is only used if this variable is not set:

    export JDK_SERIAL_FILTER="maxbytes=1073741824;maxdepth=128;maxarray=1073741824;maxrefs=10000000"

When running WildFly in a Docker container, add the variable to your Dockerfile. Note that the `ENV` instruction must be placed after the `FROM` instruction:

    FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION}
    ...
    # Raise JDK serialization filter limits (Imixs stores documents incl. file content as serialized maps)
    ENV JDK_SERIAL_FILTER="maxbytes=1073741824;maxdepth=128;maxarray=1073741824;maxrefs=10000000"

Alternatively, the variable can be set in the `environment` section of your docker-compose file or Kubernetes deployment.

The filter can also be disabled completely by setting `DISABLE_JDK_SERIAL_FILTER=true`. However, it is recommended to raise the limits instead, so that the protection against oversized or malicious serialization streams remains active.

## Setup a Security Realm

To login to the Imixs-Sample Application a security domain named 'imixsrealm' need to be provided. WildFly uses the [Elytron](https://docs.wildfly.org/39/WildFly_Elytron_Security.html) security subsystem, which supports a lot of different security realms.
Each user need to be mapped to one of the [Imixs security roles](../engine/acl.html).
The following table shows an example of a user list with different access levels:

| UserID  | GroupName              | Description                                                         |
| ------- | ---------------------- | ------------------------------------------------------------------- |
| manfred | IMIXS-WORKFLOW-Manager | This user will have maximum access                                  |
| eddy    | IMIXS-WORKFLOW-Editor  | User can edit all workitems                                         |
| anna    | IMIXS-WORKFLOW-Author  | User will be allowed to create workitems and edit his own           |
| ronny   | IMIXS-WORKFLOW-Reader  | This user will be only allowed to read workitems                    |
| guest   |                        | This user will have no access (just to be sure security works well) |

The setup of a security realm in Elytron consists of three parts, all configured in the standalone.xml:

1. A **security realm** which defines where users, passwords and groups are stored (database, properties files, LDAP, ...)
2. A **security domain** named 'imixsrealm' which uses this realm
3. An **application-security-domain** mapping in the `ejb3` and `undertow` subsystems, so that the application can refer to the security domain 'imixsrealm'

### Database Realm

The following example shows a `jdbc-realm` reading users and groups from the tables `USERID` and `USERID_USERGROUP`. The passwords are stored as SHA-256 hash in hex encoding. The realm uses the datasource 'workflow' defined before.

Add the realm into the section `security-realms` of the elytron subsystem:

    <subsystem xmlns="urn:wildfly:elytron:community:18.0" ...>
        ...
        <security-realms>
            ...
            <jdbc-realm name="imixsrealm">
                <principal-query sql="select PASSWORD from USERID where ID=? AND PASSWORD IS NOT NULL" data-source="workflow">
                    <simple-digest-mapper algorithm="simple-digest-sha-256" password-index="1" hash-encoding="hex"/>
                </principal-query>
                <principal-query sql="select GROUP_ID from USERID_USERGROUP where ID=?" data-source="workflow">
                    <attribute-mapping>
                        <attribute to="Roles" index="1"/>
                    </attribute-mapping>
                </principal-query>
            </jdbc-realm>
        </security-realms>
        ...

Next add the security domain into the section `security-domains`:

        <security-domains>
            ...
            <security-domain name="imixsrealm" default-realm="imixsrealm" permission-mapper="default-permission-mapper">
                <realm name="imixsrealm"/>
            </security-domain>
        </security-domains>

### File Based Realm

For testing purposes users and groups can also be loaded from Java properties files using a `properties-realm`:

    <properties-realm name="imixsrealm">
        <users-properties path="sampleapp-users.properties" relative-to="jboss.server.config.dir" digest-realm-name="imixsrealm" plain-text="true"/>
        <groups-properties path="sampleapp-roles.properties" relative-to="jboss.server.config.dir"/>
    </properties-realm>

In this case the security domain must use the role decoder `groups-to-roles`:

    <security-domain name="imixsrealm" default-realm="imixsrealm" permission-mapper="default-permission-mapper">
        <realm name="imixsrealm" role-decoder="groups-to-roles"/>
    </security-domain>

The sampleapp-users.properties file uses a username=password format with each user entry on a separate line:

    manfred=password1
    anna=password2
    ...


The sampleapp-roles.properties file uses the pattern username=group1,group2. For example:

    manfred=IMIXS-WORKFLOW-Manager
    anna=IMIXS-WORKFLOW-Author

### Application Security Domain

Finally the security domain 'imixsrealm' must be made available for the application in the subsystems `ejb3` and `undertow`:

    <subsystem xmlns="urn:jboss:domain:ejb3:10.0">
        ...
        <application-security-domains>
            <application-security-domain name="imixsrealm" security-domain="imixsrealm"/>
            <application-security-domain name="other" security-domain="ApplicationDomain"/>
        </application-security-domains>
        ...
    </subsystem>

    <subsystem xmlns="urn:jboss:domain:undertow:community:14.0" ...>
        ...
        <application-security-domains>
            <application-security-domain name="other" security-domain="ApplicationDomain" integrated-jaspi="false"/>
            <application-security-domain name="imixsrealm" security-domain="imixsrealm" integrated-jaspi="false"/>
        </application-security-domains>
    </subsystem>

The application refers to the security domain in the file _WEB-INF/jboss-web.xml_:

    <jboss-web>
        <security-domain>imixsrealm</security-domain>
    </jboss-web>

### RoleMapping

The groups provided by the security realm are mapped to roles. If your group names differ from the [Imixs security roles](../engine/acl.html), you can map them with a `mapped-role-mapper` in the `mappers` section of the elytron subsystem:

    <mapped-role-mapper name="imixs-role-mapper" keep-mapped="false" keep-non-mapped="true">
        <role-mapping from="IMIXS-WORKFLOW-Reader" to="org.imixs.ACCESSLEVEL.READERACCESS"/>
        <role-mapping from="IMIXS-WORKFLOW-Author" to="org.imixs.ACCESSLEVEL.AUTHORACCESS"/>
        <role-mapping from="IMIXS-WORKFLOW-Editor" to="org.imixs.ACCESSLEVEL.EDITORACCESS"/>
        <role-mapping from="IMIXS-WORKFLOW-Manager" to="org.imixs.ACCESSLEVEL.MANAGERACCESS"/>
    </mapped-role-mapper>

The role mapper is then added to the security domain:

    <security-domain name="imixsrealm" default-realm="imixsrealm" permission-mapper="default-permission-mapper" role-mapper="imixs-role-mapper">
        <realm name="imixsrealm"/>
    </security-domain>

The mapping can be used to map any other group into the Imixs security roles as well.

## Deploy the Imixs-Sample Application

After the database and security domain are configured, the Imixs-Sample Application can be deployed.
Therefore it is sufficient to copy the .war file into the folder

    /wildfly/standalone/deployments/

The application can be accessed from the URL

[http://localhost:8080/workflow](http://localhost:8080/workflow)

## Need Help?

If you have any difficulty in deployment of your application, [contact the community for help](https://www.imixs.org/sub_community.html). Also if you have any tips and suggestions for improvements, please share them as well.
