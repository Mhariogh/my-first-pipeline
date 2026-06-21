# Installing Java and Maven (Windows and Linux)

Maven needs a Java JDK to run, so install the JDK first, then Maven, then verify. These steps use JDK 17 and the latest Maven 3.9.x (3.9.16 at the time of writing). Always grab the current 3.9.x from the official download page: https://maven.apache.org/download.cgi

You do not strictly need Maven on your own machine for this project, because the Jenkins image already includes it. Install it locally only if you want to run the optional manual build (Step 0 in the README) or build directly on a server.

## Windows

### Option A: package manager (recommended, fastest)

If you have `winget` (built into Windows 10 and 11):

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
```

Or with Chocolatey:

```powershell
choco install temurin17 -y
choco install maven -y
```

Close and reopen your terminal, then jump to the Verify section.

### Option B: manual install

1. Install the JDK. Download the JDK 17 installer from https://adoptium.net, run it, and during setup enable the option "Set JAVA_HOME variable".
2. Download Maven. Go to https://maven.apache.org/download.cgi and download the Binary zip archive, for example `apache-maven-3.9.16-bin.zip`.
3. Extract it to a stable folder, for example `C:\tools\apache-maven-3.9.16`.
4. Set environment variables. Open the Start menu, search for "Edit the system environment variables", and click Environment Variables.
   - Add a System variable `MAVEN_HOME` with value `C:\tools\apache-maven-3.9.16`.
   - If `JAVA_HOME` is not already set, add it pointing to your JDK, for example `C:\Program Files\Eclipse Adoptium\jdk-17`.
   - Edit the `Path` variable and add two entries: `%MAVEN_HOME%\bin` and `%JAVA_HOME%\bin`.
5. Click OK on all dialogs, then open a new PowerShell or Command Prompt window so the changes take effect.

## Linux

You have two options: the distribution package manager (simplest) or a manual tarball (gets the latest version, works on any distribution including Amazon Linux).

### Ubuntu or Debian (package manager)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven
```

### Amazon Linux 2023, Fedora, or RHEL (package manager)

```bash
sudo dnf install -y java-17-amazon-corretto-devel maven
```

### Any Linux (manual install, for the latest Maven)

1. Install a JDK first using the package-manager command above (`openjdk-17-jdk` or `java-17-amazon-corretto-devel`).
2. Download and extract Maven:

```bash
cd /tmp
curl -LO https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz
sudo tar -xzf apache-maven-3.9.16-bin.tar.gz -C /opt
sudo ln -sfn /opt/apache-maven-3.9.16 /opt/maven
```

3. Add Maven to the PATH for all users:

```bash
echo 'export MAVEN_HOME=/opt/maven' | sudo tee /etc/profile.d/maven.sh
echo 'export PATH=$MAVEN_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/maven.sh
sudo chmod +x /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
```

If the download URL returns a 404, that version has rolled off the mirror. Get the current link from https://maven.apache.org/download.cgi, or use the archive at https://archive.apache.org/dist/maven/maven-3/

## macOS (for reference)

```bash
brew install temurin@17 maven
```

## Verify (all platforms)

```bash
mvn -version
```

You should see the Maven version, the Java version, and your operating system. If the command is not found, your PATH is not set correctly. On Windows, open a fresh terminal. On Linux, re-run the `source /etc/profile.d/maven.sh` command or open a new shell.
