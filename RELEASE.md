# Release Workflow
The release automation is designed to quickly release updates to liquibase extensions. This routinely happens when there is an update to liquibase core. There are unique automates automated steps when a pull requests is created by dependabot for a `Bump liquibase-core from *.*.* to *.*.*`, but these steps can also be taken manually for a patch or other manual release.

## Triggers
### Pull Request Opened
When all pull requests are opened the Unit Tests will run and they must pass before the PR can be merged. For a liquibase core bump PR, the application version in the POM will automatically be set to match the liquibase core version. If creating a manual PR for release, the `<version>*.*.*</version>` tag in the POM will need to be set to the correct version without the `SNAPSHOT` suffix in order to release to Sonatype Nexus. For example, `<version>4.3.5.1/version>` to release a patch version for the extension release for liquibase core 4.3.5. 
### Pull Request Labeled as Release Candidate
If the `Extension Release Candidate :rocket:` label is applied to the PR, this is the trigger for GitHub Actions to run the full Integration Test suite matrix on the pull requests because this commit will become the next release. For a liquibase core bump, this label will automatically be applied to the dependabot PR. If this is a manual release, manually applying the label will also start the release testing and subsequent automation.
### Pull Request is Approved and Merged to Main
If a Pull Request is merged into main and is labeled as release candidate the following automation steps will be taken:
*   Signed artifact is built
*   A draft GitHub Release is created proper tagging, version name, and artifact
*   The application version in the POM is bumped to be the next SNAPSHOT version for development
### Draft Release is Published
Once the GitHub release is published, the signed artifact is uploaded to Sonatype Nexus. The `<autoReleaseAfterClose>true</autoReleaseAfterClose>` option is defined in the POM, so for all releases without the `SNAPSHOT` suffix, they will automatically release after all the staging test have passed. If everything goes well, no further manual action is required. 

## Testing
The workflow separates Unit Test from Integration Tests and runs them at separate times, as mentioned above. In order to separate the tests, they must be in separate files. Put all Unit Tests into files that end with `Test.java` and Integration Test files should end with `IT.java`. For example the tests for the Liquibase Postgresql Extension now look like:
```
> src
    > test
        > java
            > liquibase.ext
                > copy
                    CopyChangeIT.java
                    CopyChangeTest.java
                > vacuum
                    VacuumChangeTest.java
```
Any tests that require a JDBC connection to a running database are integration tests and should be in the `IT.java` files.

## Repository Configuration
The automation requires the below secrets and configuration in order to run.
### BOT TOKEN
Github secret named: `BOT_TOKEN`

Github Actions bot cannot trigger events, so a liquibase robot user is needed to trigger automated events. An access token belonging to the liquibase robot user should be added to the repository secrets and named `BOT_TOKEN`. 

### GPG SECRET
Github secret named: `GPG_SECRET`

According to [the advanced java setup docs for github actions](https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md#gpg) the GPG key should be exported by: `gpg --armor --export-secret-keys YOUR_ID`. From the datical/build-maven:jdk-8 docker container, this can be export by the following:
```bash
$ docker run -it -u root docker.artifactory.datical.net/datical/build-maven:jdk-8 bash

$  gpg -k
/home/jenkins/.gnupg/pubring.kbx
--------------------------------
pub   rsa2048 2020-02-12 [SC] [expires: 2022-02-11]
      **** OBFUSCATED ID ****
uid           [ultimate] Liquibase <support@liquibase.org>
sub   rsa2048 2020-02-12 [E] [expires: 2022-02-11]

$ gpg --armor --export-secret-keys --pinentry-mode loopback **** OBFUSCATED ID ****
Enter passphrase: *** GPG PASSPHRASE ***
-----BEGIN PGP PRIVATE KEY BLOCK-----
******
******
=XCvo
-----END PGP PRIVATE KEY BLOCK-----
```

### GPG PASSPHRASE
Github secret named: `GPG_PASSPHRASE`
The passphrase is the same one used previously for the manual release and is documented elsewhere for the manual release process.

### SONATYPE USERNAME
Github secret named: `SONATYPE_USERNAME`

The username or token for the sonatype account. Current managed and shared via lastpass for the Shared-DevOps group. 

### SONATYPE TOKEN
Github secret named: `SONATYPE_TOKEN`

The password or token for the sonatype account. Current managed and shared via lastpass for the Shared-DevOps group.

### Label Settings
Create a label with the following settings:
* Label name: `Extension Release Candidate :rocket:`
* Description: `Release Candidate for Extension`
* Color: `#ff3d00`

## Useful Links
*   [Advanced Java Setup for GitHub Actions](https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md#gpg)
*   [Deploying to Sonatype Nexus with Apache Maven](https://central.sonatype.org/publish/publish-maven/)