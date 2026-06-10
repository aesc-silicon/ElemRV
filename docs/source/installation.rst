Installation
############

This project uses `Taskfile <https://taskfile.dev>`_ as its task runner. Install
it along with the required system dependencies before proceeding.

Prerequisites
*************

Install system dependencies and Taskfile::

    sudo apt install virtualenv curl podman
    sudo sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b /usr/local/bin

Set Up the Project
******************

Run the install tasks to download and configure all dependencies::

    task install
    task install-zephyr

To list all available tasks::

    task -a

.. note::

   By default, the ``view-klayout`` and ``view-openroad`` tasks require a running
   X server. On headless systems (e.g. over SSH), prefix the command with
   ``IS_HEADLESS=true`` to skip the X server requirement::

       IS_HEADLESS=true task view-klayout

Using a Release Manifest
************************

By default, ``task install`` tracks the latest development state. To pin all
dependencies to a specific tape-out, pass ``manifest_suffix``::

    task install manifest_suffix=-tapeout-ihp-sg13cmos-2026-03-r1

Available manifests are listed in the repository root.

How to Update
*************

Pull the latest changes and synchronise all dependencies::

    git pull
    task repo-sync
