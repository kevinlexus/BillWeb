prunsrv.exe //IS//BillWeb --DisplayName="BillWeb" --Description="Core of billing system" --Startup=auto --Install=C:\work\git\BillWeb\src\main\java\WindowsService\prunsrv.exe --Jvm=auto --Classpath=C:\work\git\BillWeb\src\main\java\WindowsService\BillWeb-0.0.1-SNAPSHOT.jar --StartMode=jvm --StartClass=com.ric.web.Bootstrap --StartMethod=start --StartParams=start --StopMode=jvm --StopClass=com.ric.web.Bootstrap --StopMethod=stop --StopParams=stop --StdOutput=auto --StdError=auto --LogPath=C:\work\git\BillWeb\src\main\java\WindowsService\ --LogLevel=Debug