# Shell script that parses the protocol handler url and launches the correct JetBrains IDE with vsts args
#!/bin/sh
LOCATION_FILE=$HOME/.vsts/locations.csv

# Example arg: vsoi://checkout/?url=https://mseng.visualstudio.com/VSOnline/_git/Java.VSCode&EncFormat=UTF8&IdeType=IntelliJ&IdeExe=idea
url=$1

# Parse URL for IdeExe
ideExe=${url#*IdeExe=}
ideExe=${ideExe%&*}

# Read locations.csv file to get the IDE directory location
while read line;
do
    exe=${line%,*}
    if [ "$exe" == "$ideExe" ]
    then
        location=${line#*,}

        # Create and execute command to run IDE with args
        cmd="${location}/${ideExe}.sh vsts ${url}"
        ${cmd} &
        break
    fi
done < ${LOCATION_FILE}
