## DPM Dockerfile build
This is a stable version modified from the main branch of [https://ghe-pip2.fnal.gov/Controls/dpm](https://ghe-pip2.fnal.gov/Controls/dpm)

```
docker build -t adregistry.fnal.gov/acorn/dpm:standalone_<n> .
docker push adregistry.fnal.gov/acorn/dpm:standalone_<n>
```
* If the classpath needs to me modified or startup commands/parameters need to be added, the `scripts/start_dpm` shell script is the place to do this
* This image has a build argument which determines the version of DPM being released. The GH actions workflow passes the version to the build action as a build-arg.
* At build, the argument is then defined as an environment variable and passed to DPM as the `-Dversion` parameter. This is used by SCOPE to display release information
* If you wish to build this container yourself, you will need to define the argument through the `docker` command line (otherwise, `default` will be the value). For example:

```
docker build --build-arg DPM_RELEASE='test-version' -t adregistry.fnal.gov/acorn/dpm:test-version .
docker push adregistry.fnal.gov/acorn/dpm:test-version
```

