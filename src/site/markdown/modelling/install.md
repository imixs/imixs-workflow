# Installation Guide

The architecture of **Open BPMN** makes it possible to run the modeller on various IDEs and platforms. It can be installed on different IDEs such as [Visual Studio Code](https://code.visualstudio.com/) or [Eclipse Theia](https://theia-ide.org/) or it can be run as a standalone web application.

## Install Eclipse Theia (💥 Recommended)

[Eclipse Theia](https://theia-ide.org) is the recommended platform for **Open-BPMN**. It provides a highly flexible and modern IDE experience, featuring a powerful AI integration that significantly enhances the modeling and development workflow.

To install the **Theia IDE**, follow these steps:

1. Download the latest installer for your operating system from the [official Eclipse Theia website](https://theia-ide.org/).
2. Run the installer and follow the on-screen instructions.
3. Theia is cross-platform and runs on Windows, macOS, and Linux, providing a seamless experience across all environments.
4. Once installed, launch the application to start your workspace.

**Installing the Open-BPMN Extension:** To run [Open BPMN Extension](https://open-vsx.org/extension/open-bpmn/open-bpmn-vscode-extension) in the Theia IDE, simply open the Extensions view from the sidebar (or press `Ctrl+Shift+X`). Search for **"Open-BPMN"** in the Open VSX Registry and click **Install**. After the installation, you can immediately start modeling by opening any .bpmn file.

<img src="../bpmn/theia-integration-install.png" width="500" />

## Install in Visual Studio Code

[Visual Studio Code](https://code.visualstudio.com/) (VS-Code) is a popular, lightweight editor that fully supports the Open-BPMN modeling suite through its extension mechanism.

To install **Visual Studio Code**, follow these steps:

1. Visit the VS-Code Download page and select the version for your OS (Windows, macOS, or Linux).
2. Download and run the setup file to install the application on your local machine.
3. Follow the installation wizard, which allows you to add VS-Code to your PATH for easy command-line access.
4. Launch VS-Code and you are ready to customize your environment.

**Installing the Open-BPMN Extension:** To add the [Open BPMN Extension](https://marketplace.visualstudio.com/items?itemName=open-bpmn.open-bpmn-vscode-extension), navigate to the **Extensions** view in the activity bar on the side of VS-Code. Search for "Open-BPMN" and click the Install button.

<img src="../bpmn/vscode-integration-install.png" width="500" />

## JDK 17 Support

Note: The Open-BPMN extension is based on JDK 17. Please ensure you have [Java 17 or higher installed on your system](https://adoptium.net/).

## Run With Docker

Open-BPMN can also be run as a Web Application in a Docker Container. This solution includes the Eclipse Theia Platform. To run Open-BPMN with docker just start a local Docker container:

    $ docker run -it --rm --name="open-bpmn" \
      -p 3000:3000 \
      imixs/open-bpmn

After starting the container the application is accessible from your Web Browser:

    http://localhost:3000

<img src="../bpmn/imixs-bpmn-001.png" width="500" />

### Workspace Directory

The Theia Client is using a local workspace directory `/usr/src/app/open-bpmn.glsp-client/workspace`. The workspace directory is the place to create and edit the BPMN files. With Docker you can change the workspace directory and map it to a local directory with the Docker param -v

In the following example the workspace is mapped to the local directory `/tmp/my-workspace`

    $ docker run -it --rm --name="open-bpmn" \
      -p 3000:3000 \
      -v /tmp/my-workspace:/usr/src/app/open-bpmn.glsp-client/workspace \
      imixs/open-bpmn

## Kubernetes

You can also run Open-BPMN in a Kubernetes cluster. The following is a deplyoment yaml file that you can use as a template for your own configuration. Note also in Kubernetes you can map the workspace directory to a persistence volume.

```
---
###################################################
# Deployment office-demo
###################################################
apiVersion: apps/v1
kind: Deployment
metadata:
  name: modeler-app
  namespace: open-bpmn
  labels:
    app: modeler-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: modeler-app
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: modeler-app
    spec:
      containers:
      - image: imixs/open-bpmn:latest
        name: modeler-app
        imagePullPolicy: Always
        env:
        - name: TZ
          value: Europe/Berlin
        ports:
          - name: web
            containerPort: 3000
        resources:
          requests:
            memory: "128M"
          limits:
            memory: "1G"
      restartPolicy: Always


---
###################################################
# Service open-bpmn
###################################################
apiVersion: v1
kind: Service
metadata:
  name: modeler-app
  namespace: open-bpmn
spec:
  ports:
  - protocol: TCP
    name: web
    port: 3000
  selector:
    app: modeler-app
```

To deploy Open-BPMN in your cluster create the namespace and apply your Pod configuration:

    $ kubectl create namespace open-bpmn
    $ kubectl apply -f apps/open-bpmn
