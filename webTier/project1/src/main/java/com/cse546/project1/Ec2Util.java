package com.cse546.project1;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class Ec2Util {
    private static final String ACCESS_KEY = "xxx";
    private static final String SECRET_KEY = "yyy";
    private final AWSStaticCredentialsProvider credentials;
    public Ec2Util(){
        credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                ACCESS_KEY,
                SECRET_KEY
        ));
    }
    protected void autoscaleAppTier(){
        while(true){
            int appTierCount = getNumOfActiveInstances() - 1;
            int queueSize = SqsUtil.getInputQueueSize();
            System.out.println("Message Count: " + queueSize + ", App Count: " + appTierCount);
            if(queueSize > appTierCount && queueSize > 0){
                int supply = 20 - appTierCount;
                if(supply > 0){
                    int demand = queueSize - appTierCount;
                    System.out.println("Supply: " + supply + ", Demand: " + demand);
                    if(demand > supply){
                        createAppTierInstances(supply, appTierCount);
                    }else {
                        createAppTierInstances(demand, appTierCount);
                    }
                }
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected int getNumOfActiveInstances(){
        AmazonEC2 ec2 = AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentials)
                .withRegion("us-east-1")
                .build();
        DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest().withIncludeAllInstances(true);
        List<InstanceStatus> runningInstances = ec2.describeInstanceStatus(request).getInstanceStatuses().stream()
                .filter(s -> s.getInstanceState().getName().equals(InstanceStateName.Running.toString()) ||
                        s.getInstanceState().getName().equals(InstanceStateName.Pending.toString()))
                .collect(Collectors.toList());
        return runningInstances.size();
    }

    protected void createAppTierInstances(int toCreate, int instanceCount){
        AmazonEC2 ec2 = AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentials)
                .withRegion("us-east-1")
                .build();
        for(int i = 1; i <= toCreate; i++){
            RunInstancesRequest run_request = new RunInstancesRequest()
                    .withImageId("ami-0d9d298039a4d7971")
                    .withInstanceType(InstanceType.T1Micro)
                    .withMaxCount(1)
                    .withMinCount(1);
            RunInstancesResult result = ec2.runInstances(run_request);
            Instance instance = result.getReservation().getInstances().get(0);
            CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                    .withResources(instance.getInstanceId())
                    .withTags(new Tag("Name", "AppTier_" + String.valueOf(instanceCount + i)));
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ec2.createTags(createTagsRequest);
        }
    }
}
