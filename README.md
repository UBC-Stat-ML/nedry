Summary
-------

<!-- [![Build Status](https://travis-ci.org/alexandrebouchard/nedry.png?branch=master)](https://travis-ci.org/alexandrebouchard/nedry) -->

Command line utilities:

- In package flow, for nextflow or similar workflow system
- In package mcli (Monte Carlo CLI), for generic operations related to Monte Carlo, e.g. processing of Monte Carlo output.


Installation
------------

- Check out the source ``git clone git@github.com:alexandrebouchard/nedry.git``
- Compile using ``./gradlew installDist``
- Add the directory ``build/install/nedry/bin`` to your ``$PATH`` variable.


Usage 
----

To organize results of the last nextflow run via symlinks: add ``nf-monitor`` after you nextflow command, as in

```
./nextflow run MY_WORKFLOW.nf -resume | nf-monitor
```

This will create nicely organized links into ``links``. Use ``... | nf-monitor --open true`` if you are using a mac to open the directory right away.


