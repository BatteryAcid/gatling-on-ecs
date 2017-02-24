# gatling-on-ecs: Running gatling on AWS Elastic Container Service (ECS) to horizontally scale the load

Allows to create an Amazon Elastic Container Service cluster from AWS [cloudformation] (gatling-on-ecs.cf) template. The docker container running on these instances can be created using the includes[Dockerfile](Dockerfile) that has scala, sbt and other required libraries installed along side of required [sbt based gatling](src)script.

- Uses S3 as repository to temporarily hold the logs from all agents running gatling client. A [script] (consolidate_report.sh) is also including that can consolidate the reports from all the clients and generate html gatling statistical report.
- Uses ECR for container repository 
- This project uses SBT 0.13.13, which is available [here](http://www.scala-sbt.org/download.html),  Scala [2.11>](http://www.scala-lang.org/download/), Java [8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), and [Gatling](http://gatling.io/docs/2.2.3/). Gatling API spec is avaiable here [DSL Specs] (http://gatling.io/#/cheat-sheet/2.2.3) and [src](https://github.com/gatling/gatling). Our version of sbt is heavily inspired by [sample](https://github.com/gatling/gatling-sbt-plugin-demo)

# Why 
Because gatling is awesome and so is AWS. But one client can only create so much of load! We want to scale the gatling client to test heavy volume of load.   
## Introduction 
Test simulation files are located here [here](src/test/scala/nearmap/) as per SBT structure. [SampleSimulation.scala](src/test/scala/nearmap/SampleSimulation.scala) is an example here. Other resources like gatling config etc are also location hereare located [here](src/test/resources/). 

[Launch] (run.sh) script is the entry point for docker. 


Depending on load specified in [setup](src/test/scala/nearmap/SampleSimulation.scala), the volume can be multiplied by number of tasks. How we are scaling is guided by [gatling scaling doc](http://gatling.io/docs/2.2.3/cookbook/scaling_out.html) but we have quite a few customization as in we use sbt, docker and ECS.

# Creating and managing stack
The attack starts when cluster stack is created and service starts the tasks. 

## Define ECS Repository for docker
Create ECS repository and build and push the docker containers:
```
`aws ecr get-login --region ap-southeast-2`
docker build -t gatling .
docker tag hyperweb/gatling:latest 999999999999.dkr.ecr.ap-southeast-2.amazonaws.com/gatling:latest
docker push 999999999999.dkr.ecr.ap-southeast-2.amazonaws.com/gatling:latest
```
AWS account is assumed to be 999999999999. Its also specified in [cf](gatling-on-ecs.cf) file.

##To create stack run:
```
aws cloudformation create-stack --stack-name=Gatling-Performance --region=ap-southeast-2 --template-body=file:///gatling-on-ecs/gatling-on-ecs.cf --capabilities=CAPABILITY_IAM --parameters ParameterKey=TaskCount,ParameterValue=2 ParameterKey=ReportBucket,ParameterValue=performance-test/
```
Where TaskCount is number of tasks and instances that ECS service will start and ReportBucket is s3 bucket where report will be created.


## Scaling out
Depending upon how many ECS tasks were started, the volume of load can multipled. The number of attacking nodes/tasks and load rule specified in simulation setup (as per gatling [DSL](http://gatling.io/docs/2.2.3/general/simulation_setup.html )) togather specifies the full attack gatling cluster.

Specifying the following load and 2 tasks will result in 2 simultenoues node hiting this attack:
```
setUp(
  scn.inject(
    nothingFor(4 seconds), // 1
    atOnceUsers(10), // 2
    rampUsers(10) over(5 seconds), // 3
    constantUsersPerSec(20) during(15 seconds), // 4
    constantUsersPerSec(20) during(15 seconds) randomized, // 5
    rampUsersPerSec(10) to(20) during(10 minutes), // 6
    rampUsersPerSec(10) to(20) during(10 minutes) randomized, // 7
    splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(10 seconds), // 8
    splitUsers(1000) into(rampUsers(10) over(10 seconds)) separatedBy(atOnceUsers(30)), // 9
    heavisideUsers(1000) over(20 seconds) // 10
    ).protocols(httpConf)
  )
```
which will make the actual rule number of users per rule X number of task:
```
setUp(
  scn.inject(
    nothingFor(4 seconds), // 1
    atOnceUsers(20), // 2
    rampUsers(20) over(5 seconds), // 3
    constantUsersPerSec(40) during(15 seconds), // 4
    constantUsersPerSec(40) during(15 seconds) randomized, // 5
    rampUsersPerSec(20) to(40) during(10 minutes), // 6
    rampUsersPerSec(20) to(40) during(10 minutes) randomized, // 7
    splitUsers(2000) into(rampUsers(20) over(10 seconds)) separatedBy(10 seconds), // 8
    splitUsers(2000) into(rampUsers(20) over(10 seconds)) separatedBy(atOnceUsers(60)), // 9
    heavisideUsers(2000) over(20 seconds) // 10
    ).protocols(httpConf)
  ) 
```

## Generating reports
Once the simulation finishes, the containers will sleep for long time, allowing to collect all the cloudwatch matrics and restricting ECS to start new test agents. Once simulation finishes... which can be seen from Cloudwatch logs Wait until terminate?", reports can be consolidated. 

Run followng with the bucket name where results are stored. This will generate reports and synch it back to same bucket under a bucket prefix Consolidated_Reports:
```
./consolidate_report.sh -r performance-test/
```


# TODO
-[Placement constraints](http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-placement-constraints.html) to allow one task per host is not in cloudformation template yet so we are creating same number of instance as number of tasks to allow AWS to distribute evently i.e. one task per host. WIN! But this needs to be updated to best use resources when Placement constraints is available in cloudformation.

