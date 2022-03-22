
set jarFolder=%~dp0
set jdkPath=D:\Program Files\jdk\jdk-1.8.0_261_64\bin
cd /d "%jdkPath%"

start javaw -jar -Xms32m -Xmx32m "%jarFolder%\TcpTool.jar" %~1
