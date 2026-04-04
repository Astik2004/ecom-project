# Fix for Windows Terminal multiple tabs parameter
$wtCmd = 'wt --window 0 new-tab --title "Eureka Server" -d "d:\ecomproject\eureka-server" cmd /k "mvn clean spring-boot:run" ; ' +
         'new-tab --title "API Gateway" -d "d:\ecomproject\api-gateway" cmd /k "echo Waiting 10s for Eureka... & timeout /t 10 >nul & mvn clean spring-boot:run" ; ' +
         'new-tab --title "User Service" -d "d:\ecomproject\user-service" cmd /k "echo Waiting 10s for Eureka... & timeout /t 10 >nul & mvn clean spring-boot:run" ; ' +
         'new-tab --title "Notification Service" -d "d:\ecomproject\notification-service" cmd /k "echo Waiting 10s for Eureka... & timeout /t 10 >nul & mvn clean spring-boot:run"'

Write-Host "Launching Windows Terminal with tabs..."
cmd.exe /c $wtCmd
