# web extension

### build java native client

        mvn assembly:assembly
                
### browser extension

This directory contains a chrome application that uses native messaging API to allow to communication between the browser and the
native Java client.

In order for this example to work you must first install the native messaging host from the host directory.

To install the host:

On Windows:
  Add registry key
  HKEY_LOCAL_MACHINE\SOFTWARE\Google\Chrome\NativeMessagingHosts\org.votingsystem.webextension.native
  or
  HKEY_CURRENT_USER\SOFTWARE\Google\Chrome\NativeMessagingHosts\org.votingsystem.webextension.native and set its default value to the full path to
  host\org.votingsystem.webextension.native-win.json . Note that you need to have Java 8 installed.

On Mac and Linux:
  Run install_host.sh script in the host directory:
    host/install_host.sh
  The host is installed only for the user who runs the script. You can later use host/uninstall_host.sh
  to uninstall the host.



