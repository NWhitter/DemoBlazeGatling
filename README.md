# Demo Blaze Gatling

This is a Scala/Gatling test framework built for running load tests on the [Demo Blaze](https://www.demoblaze.com/) website

## Getting Started
### Prerequisities
* Scala
* Maven
* IDE (Intellj - preferred)

## Running tests
To test it out, simply execute the following command:

    $mvn gatling:test -Dgatling.simulationClass=demoblaze.DemoBlazeSimulation

or simply:

    $mvn gatling:test
