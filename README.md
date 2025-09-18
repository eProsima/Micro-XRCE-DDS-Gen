# eProsima Micro XRCE-DDS

[![Releases](https://img.shields.io/github/release/eProsima/Micro-XRCE-DDS-Gen.svg)](https://github.com/eProsima/Micro-XRCE-DDS-Gen/releases)
[![License](https://img.shields.io/github/license/eProsima/Micro-XRCE-DDS-Gen.svg)](https://github.com/eProsima/Micro-XRCE-DDS-Gen/blob/master/LICENSE)
[![Issues](https://img.shields.io/github/issues/eProsima/Micro-XRCE-DDS-Gen.svg)](https://github.com/eProsima/Micro-XRCE-DDS-Gen/issues)
[![Forks](https://img.shields.io/github/forks/eProsima/Micro-XRCE-DDS-Gen.svg)](https://github.com/eProsima/Micro-XRCE-DDS-Gen/network/members)
[![Stars](https://img.shields.io/github/stars/eProsima/Micro-XRCE-DDS-Gen.svg)](https://github.com/eProsima/Micro-XRCE-DDS-Gen/stargazers)

<a href="http://www.eprosima.com"><img src="docs/eprosima-logo.svg" align="left" hspace="8" vspace="2" width="100" height="100" ></a>

*eProsima Micro XRCE-DDS* is a software solution which allows to communicate eXtremely Resource Constrained Environments (XRCEs) with an existing DDS network.
 This implementation complies with the specification proposal, "eXtremely Resource Constrained Environments DDS (DDS-XRCE)" submitted to the Object Management Group (OMG) consortium.

The *Micro XRCE-DDS Gen* provides a tool for generate serialization topic code from an idl file using *Micro CDR* serialization library.
Although this tool is intended to be used with *Micro XRCE-DDS Client* library,
the code generated does not depend of it and can be used externally with the only dependence of *Micro CDR*, used for native serializations.

<p align="center"> <img src="docs/general_architecture.png" alt="Image"/> </p>

## Commercial support

Looking for commercial support? Write us to info@eprosima.com

Find more about us at [eProsima’s webpage](https://eprosima.com/).

## Documentation

You can access Micro XRCE-DDS documentation online, which is hosted on Read the Docs.

* [Start Page](http://micro-xrce-dds.readthedocs.io)
* [Installation manual](http://micro-xrce-dds.readthedocs.io/en/latest/installation.html)
* [Tool manual](http://micro-xrce-dds.readthedocs.io/en/latest/gen.html)
