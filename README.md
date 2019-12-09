# NanoSat MO Framework - mission tailoring for OPS-SAT
This repository contains parts of the NanoSat MO Framework specific to the OPS-SAT mission.

The content of this repository is mostly relevant for a deployment on a representative hardware (MityArm, OPS-SAT FlatSat and OPS-SAT flight model) and serves mostly as a reference.

During the development it is recommended to use the NMF SDK (released as a source in the main NMF repository on [GitHub]).
The SDK is based on a simulator, providing most of the platform functionalities accessible to the experimenter. Simulator allows running NMF applications without an access to a real satellite hardware.

The official website of the NanoSat MO Framework is available [here].

# Building the project
You can build this project for two scenarios. In the first scenario you want a minimal build which gets put onto the SEPP. You can produce this build by just running 'mvn install' in the root directory of 
this repository. In the other scenario you want to test and simulate your apps in order to validate their correctness and later connect to your apps on the space-craft by using the Ground MO Proxy. 
This is done by providing a separate build profile which can be invoked by running 'mvn install -Pground' inside the main directory of this repository.

## Adding apps to the default build
If you want your app to be included in the minimal default build, you have to take two steps.
1. Add your app to the global dependencies in the opssat-package/pom.xml file.
2. Add a copy task for your app in the opssat-package/copy.xml file. An example is provided inside this file for the payloads-test app.
Remember that the startscript.sh should be mapped to "start\_YOURAPPNAME.sh", so it can be started by the supervisor.

## Adding applications to the ground build
1. Add your application to the dependencies inside the 'ground' profile in opssat-package/pom.xml.
2. Add a copy task for your app in the opssat-package/copy\_ground.xml file. 

# Bugs Reporting
Bug Reports can be submitted on: [Bug Reports]

Or directly in the respective source code repository.

# License
The NanoSat MO Framework is **licensed** under the **[European Space Agency Public License - v2.0]**.

[![][ESAImage]][website]
	
	
[NMFImage]: http://nanosat-mo-framework.github.io/img/NMF_logo_1124_63.png
[NanoSat MO Framework]: https://nanosat-mo-framework.github.io/
[ESAImage]: http://www.esa.int/esalogo/images/logotype/img_colorlogo_darkblue.gif
[here]: https://nanosat-mo-framework.github.io/
[European Space Agency Public License - v2.0]: https://github.com/esa/CCSDS_MO_TRANS/blob/master/LICENCE.md
[GitHub]: https://github.com/esa/nanosat-mo-framework
[Bug Reports]: https://github.com/esa/nanosat-mo-framework/issues
[website]: http://www.esa.int/
