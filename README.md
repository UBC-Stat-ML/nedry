Summary
-------

<!-- [![Build Status](https://travis-ci.org/alexandrebouchard/nedry.png?branch=master)](https://travis-ci.org/alexandrebouchard/nedry) -->

Command line utilities:

- In package flow, for nextflow or similar workflow system
- In package mcli (Monte Carlo CLI), for generic operations related to Monte Carlo, e.g. processing of Monte Carlo output.


Installation
------------


There are several options available to install the package:

### From the command line

- Check out the source ``git clone git@github.com:alexandrebouchard/nedry.git``
- Compile using ``./gradlew installDist``
- Add the directory ``build/install/nedry/bin`` to your ``$PATH`` variable.

### Integrate to a gradle script

Simply add the following lines (replacing 1.0.0 by the current version (see git tags)):

```groovy
repositories {
 mavenCentral()
 jcenter()
 maven {
    url "https://www.stat.ubc.ca/~bouchard/maven/"
  }
}

dependencies {
  compile group: 'ca.ubc.stat', name: 'nedry', version: '1.0.0'
}
```

### Compile using the provided gradle script

- Check out the source ``git clone git@github.com:alexandrebouchard/nedry.git``
- Compile using ``./gradlew installDist``
- Add the jars in ``build/install/nedry/lib/`` into your classpath

### Use in eclipse

- Check out the source ``git clone git@github.com:alexandrebouchard/nedry.git``
- Type ``./gradlew eclipse`` from the root of the repository
- From eclipse:
  - ``Import`` in ``File`` menu
  - ``Import existing projects into workspace``
  - Select the root
  - Deselect ``Copy projects into workspace`` to avoid having duplicates


Usage 
----

To organize results of the last nextflow run: use

```
./nextflow run MY_WORKFLOW.nf -resume | nf-monitor --open true
```


