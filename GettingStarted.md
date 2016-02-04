## Eclipse users ##

### Checkout ###

  * Make sure you have subclipse installed
  * Open the SVN Repositories view
  * Click "Add SVN Repository" and use: https://ardorcraft-api-examples.googlecode.com/svn/trunk/
  * Right click on the newly added repo -> Checkout
  * Finish, and you should have the project in your workspace

### Setup ###

Everything should be setup ok already. The only thing you have to do is change the lwjgl.jar "native library location" if you are not on windows to point to the correct os subfolder.

### Run ###

There are four examples in the project to get you started. The reason the main method is in a small class called xxxApplication, next to the xxxGame where the meat of the code is, is to be able to run the same game both as an application and as an applet, without changing the core code. In the future there will be small xxxApplet classes next to these for running as applets (together with sample html code).