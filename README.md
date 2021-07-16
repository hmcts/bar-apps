# Bank Accounting Returns for HMCTS online services
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7c2b85f8f9364357b1ac20e031d3bfd2)](https://app.codacy.com/app/jaleen/bar-apps?utm_source=github.com&utm_medium=referral&utm_content=hmcts/bar-apps&utm_campaign=badger)
[![Build Status](https://travis-ci.org/hmcts/bar-app.svg?branch=master)](https://travis-ci.org/hmcts/bar-app)
[![codecov](https://codecov.io/gh/hmcts/bar-app/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/bar-app)

This project provides REST based web services for exposing BAR related information for hearing fee and award fees.

### Getting Started

This is SpringBoot+maven based java application. Please see the Jenkinsfile in root folder to see the build and deployment pipeline.

### Prerequisites

You will need jdk and maven installed on your machine or use mvnw to install the prerequisites.

### Installing
1. Clone the repo to your machine using git clone git@git.reform.hmcts.net:bar/bar-app.git
2. Run $ ./gradlew build

## Running the tests

You can run the tests using 'gradle test or ./gradlew test'


## Deployment

See Jenkinsfile for the deployment details.

## Run the application
To run the application at local developer machine use following command

$ gradle  bootRun  or ./gradlew bootRun

Once application server is started use swagger ui to find the endpoints and test these. 
http://localhost:8080/swagger-ui.html

or in dev/test environment you can use this link
https://dev.api.bar.reform.hmcts.net/swagger-ui.html
or https://test.api.bar.reform.hmcts.net/swagger-ui.html

## Service Endpoints
Some of the end points are as below. These might be out of date. Please look at the swagger-ui to be sure. 

- GET /bar/hello


## Built With

* [Gradle](https://gradle.org/) - Dependency Management

## Service Versioning

We use [SemVer](http://semver.org/) for versioning.

